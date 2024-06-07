package io.voodoo.apps.ads.api

import android.util.Log
import androidx.annotation.CallSuper
import androidx.annotation.IntRange
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import io.voodoo.apps.ads.api.lifecycle.CloseOnDestroyLifecycleObserver
import io.voodoo.apps.ads.api.listener.AdListenerHolder
import io.voodoo.apps.ads.api.listener.AdLoadingListener
import io.voodoo.apps.ads.api.listener.AdModerationListener
import io.voodoo.apps.ads.api.listener.AdRevenueListener
import io.voodoo.apps.ads.api.model.Ad
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Ad lexicon:
 * - canBeServed: the ad can be displayed to the user (not expired, not blocked)
 * - rendered: rendered to the UI framework (composed/attached to window): doesn't mean it was actually seen, but we consider as if
 * - served (alias of rendered): an ad released after being rendered
 * - locked: currently used by a ui component (free by calling [releaseAd])
 * - available: a "fresh" ad (canBeServed + not locked + not served), see [BaseAdClient.isAvailable]
 */
interface AdClient<T : Ad> : Closeable, AdListenerHolder {

    /**
     * @return number of available "fresh" ads to display (ads that weren't already rendered,
     * aren't blocked, aren't currently used by another component, ...)
     *
     * canBeServed + not locked + not served
     */
    fun getAvailableAdCount(filterLocked: Boolean = true): Int

    /**
     * @return the ad previously served via [getAvailableAd] with the same [requestId], or null if:
     * - no previous call found for this [requestId]
     * - the ad is not in memory anymore
     * - the ad is used by another component
     * - the ad can't be served anymore (eg: blocked)
     */
    fun getServedAd(requestId: String? = null): T?

    /**
     * @return either [getServedAd] for the given [requestId], or the first available ad (see lexicon).
     */
    fun getAvailableAd(requestId: String? = null): T?

    /**
     * @return any ad that can be served and not used by another component (even if already rendered)
     */
    fun getAnyAd(): T?

    /**
     * call once the UI component is removed from the hierarchy (composable/view from window)
     */
    fun releaseAd(ad: Ad)

    /**
     * trigger a new ad load
     *
     * Note: there's no limit to the number of ads that can be loaded,
     * if you call this 10 times, it'll load 10 different ads.
     */
    suspend fun fetchAd(vararg localExtras: Pair<String, Any>): T

    data class Config(
        /**
         * Controls how many ads will be kept in memory to re-display/be re-used to fetch new ads.
         * If there's enough ads in memory, the most ancien one will be recycled for the next fetch call.
         */
        @IntRange(from = 1) val adCacheSize: Int,
        val adUnit: String,
    )
}

abstract class BaseAdClient<ActualType : PublicType, PublicType : Ad>(
    protected val config: AdClient.Config,
) : AdClient<PublicType> {

    private val loadedAds = ArrayList<ActualType>(config.adCacheSize)
    private val adIdByRequestIdMap = mutableMapOf<String, Ad.Id>()
    private val lockedAdIdList = mutableSetOf<Ad.Id>()

    private var lifecycleObserver: CloseOnDestroyLifecycleObserver? = null

    protected val adLoadingListeners = CopyOnWriteArraySet<AdLoadingListener>()
    protected val adModerationListeners = CopyOnWriteArraySet<AdModerationListener>()
    protected val adRevenueListeners = CopyOnWriteArraySet<AdRevenueListener>()

    init {
        require(config.adCacheSize > 0) { "adCacheSize must be > 0" }
    }

    @CallSuper
    override fun close() {
        Log.w("AdClient", "close()")
        synchronized(loadedAds) {
            loadedAds.forEach(::destroyAd)
            loadedAds.clear()

            adIdByRequestIdMap.clear()
            lockedAdIdList.clear()
        }
    }

    @MainThread
    fun registerToLifecycle(lifecycle: Lifecycle) {
        lifecycleObserver?.removeFromLifecycle()
        lifecycleObserver = CloseOnDestroyLifecycleObserver(lifecycle, this)
            .also { lifecycle.addObserver(it) }
    }

    protected abstract fun destroyAd(ad: ActualType)

    override fun getAvailableAdCount(filterLocked: Boolean): Int {
        return synchronized(loadedAds) {
            loadedAds.count { ad ->
                ad.canBeServed() && !ad.rendered && (!filterLocked || ad.isLocked())
            }
        }
    }

    override fun getServedAd(requestId: String?): ActualType? {
        return synchronized(loadedAds) {
            // Try to find the previously returned ad for this request id
            val previousAdId = if (requestId != null) adIdByRequestIdMap[requestId] else null
            val previousAd = loadedAds.firstOrNull { it.id == previousAdId }

            // Cleanup adIdByRequestIdMap (shouldn't be needed, unless ad was blocked)
            if (previousAdId != null && previousAd == null) {
                adIdByRequestIdMap.remove(requestId)
            }

            previousAd
                ?.takeIf { it.canBeServed() && !it.isLocked() }
                ?.also { it.lock() }
        }
    }

    override fun getAvailableAd(requestId: String?): ActualType? {
        return synchronized(loadedAds) {
            // Return previously served ad for the same requestId if still exists
            val previousAd = getServedAd(requestId)
            if (previousAd != null) {
                return@synchronized previousAd
            }

            // Take first ad ready for display
            val ad = loadedAds.firstOrNull { it.isAvailable() }
            if (ad != null) {
                ad.lock()
                if (requestId != null) {
                    adIdByRequestIdMap[requestId] = ad.id
                }
            }

            ad
        }
    }

    override fun getAnyAd(): PublicType? {
        return synchronized(loadedAds) {
            loadedAds
                .firstOrNull { it.canBeServed() && !it.isLocked() }
                ?.also { it.lock() }
        }
    }

    override fun releaseAd(ad: Ad) {
        synchronized(loadedAds) {
            ad.release()
            ad.unlock()
            ensureBufferSize()
        }
    }

    override fun addAdLoadingListener(listener: AdLoadingListener) {
        adLoadingListeners.add(listener)
    }

    override fun removeAdLoadingListener(listener: AdLoadingListener) {
        adLoadingListeners.remove(listener)
    }

    override fun addAdModerationListener(listener: AdModerationListener) {
        adModerationListeners.add(listener)
    }

    override fun removeAdModerationListener(listener: AdModerationListener) {
        adModerationListeners.remove(listener)
    }

    override fun addAdRevenueListener(listener: AdRevenueListener) {
        adRevenueListeners.add(listener)
    }

    override fun removeAdRevenueListener(listener: AdRevenueListener) {
        adRevenueListeners.remove(listener)
    }

    protected fun findAdOrNull(predicate: (ActualType) -> Boolean): ActualType? {
        return synchronized(loadedAds) {
            loadedAds.firstOrNull(predicate)
        }
    }

    protected fun getReusableAd(): ActualType? {
        return synchronized(loadedAds) {
            val servedAds = loadedAds.filter { !it.isAvailable() && !it.isLocked() }
            // Since we want to keep N ads in memory, we don't re-use if we have less than that
            if (servedAds.size < config.adCacheSize) return null
            val ad = servedAds.firstOrNull() ?: return null

            loadedAds.remove(ad)
            removeRequestIdMapping(listOf(ad.id))

            ad
        }
    }

    protected fun addLoadedAd(ad: ActualType, isAlreadyServed: Boolean = false) {
        synchronized(loadedAds) {
            if (isAlreadyServed) {
                loadedAds.add(0, ad)
            } else {
                loadedAds.add(ad)
            }
        }
    }

    private fun ensureBufferSize() {
        synchronized(loadedAds) {
            // Special case, make sure to keep at least once ad to be re-used for improved perf
            if (loadedAds.size == 1) return

            val servedAds = loadedAds.filter { !it.isAvailable() && !it.isLocked() }
            val adsToDestroy = servedAds.dropLast(config.adCacheSize)

            Log.d("AdClient", "Destroying ${adsToDestroy.size} ads")
            loadedAds.removeAll(adsToDestroy.toSet())
            removeRequestIdMapping(adsToDestroy.map { it.id })
            adsToDestroy.forEach(::destroyAd)
        }
    }

    protected inline fun runLoadingListeners(body: (AdLoadingListener) -> Unit) {
        adLoadingListeners.forEach(body)
    }

    protected inline fun runModerationListener(body: (AdModerationListener) -> Unit) {
        adModerationListeners.forEach(body)
    }

    protected inline fun runRevenueListener(body: (AdRevenueListener) -> Unit) {
        adRevenueListeners.forEach(body)
    }

    /** @return true if the ad is "fresh" and can be displayed as a new unseen ad */
    private fun ActualType.isAvailable(): Boolean {
        return canBeServed() && !isLocked() && !rendered
    }

    /** unsafe threading, call in `syncrhonized(loadedAds)` block */
    private fun Ad.isLocked(): Boolean {
        return id in lockedAdIdList
    }

    /** unsafe threading, call in `syncrhonized(loadedAds)` block */
    private fun Ad.lock() {
        lockedAdIdList.add(id)
    }

    /** unsafe threading, call in `syncrhonized(loadedAds)` block */
    private fun Ad.unlock() {
        lockedAdIdList.remove(id)
    }

    /** unsafe threading, call in `syncrhonized(loadedAds)` block */
    private fun removeRequestIdMapping(
        adIds: List<Ad.Id>
    ) {
        adIdByRequestIdMap -= adIdByRequestIdMap.filter { (_, adId) -> adId in adIds }.keys
    }
}
