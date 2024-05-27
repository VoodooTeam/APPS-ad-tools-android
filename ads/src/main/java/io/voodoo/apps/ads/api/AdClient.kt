package io.voodoo.apps.ads.api

import androidx.annotation.CallSuper
import io.voodoo.apps.ads.model.Ad
import java.io.Closeable

interface AdClient<T : Ad> : Closeable {

    fun getAvailableAdCount(): Int
    fun getPreviousAd(requestId: String?): T?
    fun getAd(requestId: String?): T?
    fun releaseAd(ad: Ad)

    suspend fun fetchAd(vararg localKeyValues: Pair<String, Any>): T
}

abstract class BaseAdClient<ActualType : PublicType, PublicType : Ad>(
    private val bufferSize: Int = 3
) : AdClient<PublicType> {

    private val loadedAds = ArrayList<ActualType>(bufferSize)

    private val adIdByRequestIdMap = mutableMapOf<String, Ad.Id>()
    private val adsLockList = mutableMapOf<Ad.Id, Int>()

    init {
        require(bufferSize > 0) { "bufferSize must be > 0" }
    }

    @CallSuper
    override fun close() {
        synchronized(loadedAds) {
            loadedAds.forEach(::destroyAd)
            loadedAds.clear()

            adIdByRequestIdMap.clear()
            adsLockList.clear()
        }
    }

    protected abstract fun destroyAd(ad: ActualType)

    override fun getAvailableAdCount(): Int {
        return synchronized(loadedAds) {
            loadedAds.count { it.isAvailable() }
        }
    }

    override fun getPreviousAd(requestId: String?): ActualType? {
        return synchronized(loadedAds) {
            // Try to find the previously returned ad for this request id
            val previousAdId = if (requestId != null) adIdByRequestIdMap[requestId] else null
            val previousAd = loadedAds.firstOrNull { it.id == previousAdId }
            if (previousAd != null) {
                incrementLock(previousAd.id, increment = 1)
            }

            previousAd
        }
    }

    override fun getAd(requestId: String?): ActualType? {
        return synchronized(loadedAds) {
            val previousAd = getPreviousAd(requestId)
            if (previousAd != null) {
                return@synchronized previousAd
            }

            // Take first ad ready for display
            val ad = loadedAds.firstOrNull { it.isAvailable() }
            if (ad != null) {
                incrementLock(ad.id, increment = 1)
                if (requestId != null) {
                    adIdByRequestIdMap[requestId] = ad.id
                }
            }

            ad
        }
    }

    override fun releaseAd(ad: Ad) {
        synchronized(loadedAds) {
            incrementLock(ad.id, increment = -1)

            // Destroy ad if it's not used anymore and was dropped from buffer
            if (!ad.isLocked() && ad !in loadedAds) {
                @Suppress("UNCHECKED_CAST")
                destroyAd(ad as ActualType)
            }
        }
    }

    protected fun findAdOrNull(predicate: (ActualType) -> Boolean): ActualType? {
        return synchronized(loadedAds) {
            loadedAds.firstOrNull(predicate)
        }
    }

    protected fun popBuffer(): ActualType? {
        return synchronized(loadedAds) {
            if (loadedAds.size < bufferSize) return@synchronized null

            loadedAds.firstOrNull { !it.isAvailable() }
                ?.also { loadedAds.remove(it) }
        }
    }

    protected fun addToBuffer(ad: ActualType) {
        synchronized(loadedAds) {
            loadedAds.add(ad)
            ensureBufferSize()
        }
    }

    private fun ensureBufferSize() {
        synchronized(loadedAds) {
            while (loadedAds.size > bufferSize) {
                val ad = loadedAds.removeAt(0)
                if (!ad.isLocked()) {
                    destroyAd(ad)
                }
            }
        }
    }

    private fun ActualType.isAvailable(): Boolean {
        return !isExpired && !isBlocked && !isLocked()
    }

    /** unsafe threading, call in syncrhonized(loadedAds) block */
    private fun Ad.isLocked(): Boolean {
        return (adsLockList[id] ?: 0) > 0
    }

    /** unsafe threading, call in syncrhonized(loadedAds) block */
    private fun incrementLock(id: Ad.Id, increment: Int) {
        val oldValue = adsLockList[id] ?: 0
        adsLockList[id] = (oldValue + increment).coerceAtLeast(0)
    }
}
