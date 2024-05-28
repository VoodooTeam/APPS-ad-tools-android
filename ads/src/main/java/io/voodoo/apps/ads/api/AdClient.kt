package io.voodoo.apps.ads.api

import androidx.annotation.CallSuper
import io.voodoo.apps.ads.model.Ad
import timber.log.Timber
import java.io.Closeable

/**
 * Ad lexicon:
 * - canBeServed: the ad can be displayed to the user (not expired, not blocked)
 * - served: an ad released after being rendered
 * - locked: currently used by a ui component (free by calling [releaseAd])
 * - rendered: rendered to the UI framework (composed/attached to window): doesn't mean it was actually seen
 * - available: a "fresh" ad (canBeServed, not locked, not rendered), see [isAvailable]
 */
interface AdClient<T : Ad> : Closeable {

    /**
     * @return number of available "fresh" ads to display (ads that weren't already rendered,
     * aren't blocked, aren't currently used by another component, ...)
     */
    fun getAvailableAdCount(): Int

    /**
     * @return the ad previously served via [getAvailableAd] with the same [requestId], or null if:
     * - no previous call found for this [requestId]
     * - the ad is not in memory anymore
     * - the ad is used by another component
     * - the ad can't be served anymore (eg: blocked)
     */
    fun getServedAd(requestId: String?): T?

    /**
     * @return either [getServedAd] for the given [requestId], or the first available ad (see lexicon).
     */
    fun getAvailableAd(requestId: String?): T?

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
    suspend fun fetchAd(vararg localKeyValues: Pair<String, Any>): T
}

abstract class BaseAdClient<ActualType : PublicType, PublicType : Ad>(
    private val servedAdsBufferSize: Int = 3
) : AdClient<PublicType> {

    private val loadedAds = ArrayList<ActualType>(servedAdsBufferSize + 1)
    private val adIdByRequestIdMap = mutableMapOf<String, Ad.Id>()
    private val lockedAdIdList = mutableSetOf<Ad.Id>()

    init {
        require(servedAdsBufferSize >= 0) { "servedAdBufferCount must be >= 0" }
    }

    @CallSuper
    override fun close() {
        synchronized(loadedAds) {
            loadedAds.forEach(::destroyAd)
            loadedAds.clear()

            adIdByRequestIdMap.clear()
            lockedAdIdList.clear()
        }
    }

    protected abstract fun destroyAd(ad: ActualType)

    override fun getAvailableAdCount(): Int {
        return synchronized(loadedAds) {
            loadedAds.count { it.isAvailable() }
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
            ad.unlock()
            ensureBufferSize()
        }
    }

    protected fun findAdOrNull(predicate: (ActualType) -> Boolean): ActualType? {
        return synchronized(loadedAds) {
            loadedAds.firstOrNull(predicate)
        }
    }

    protected fun getReusableAd(): ActualType? {
        return synchronized(loadedAds) {
            val servedAds = loadedAds.filter { !it.isAvailable() && !it.isLocked() }
            val ad = servedAds.dropLast(servedAdsBufferSize).firstOrNull()

            if (ad != null) {
                loadedAds.remove(ad)
                removeRequestIdMapping(listOf(ad.id))
            }

            ad
        }
    }

    protected fun addLoadedAd(ad: ActualType) {
        synchronized(loadedAds) {
            loadedAds.add(ad)
        }
    }

    private fun ensureBufferSize() {
        synchronized(loadedAds) {
            // Special case, make sure to keep at least once ad to be re-used for improved perf
            if (servedAdsBufferSize == 0 && loadedAds.size == 1) return

            val servedAds = loadedAds.filter { !it.isAvailable() && !it.isLocked() }
            val adsToDestroy = servedAds.dropLast(servedAdsBufferSize)

            Timber.d("Destroying %s ads", adsToDestroy.size)
            loadedAds.removeAll(adsToDestroy.toSet())
            removeRequestIdMapping(adsToDestroy.map { it.id })
            adsToDestroy.forEach(::destroyAd)
        }
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
