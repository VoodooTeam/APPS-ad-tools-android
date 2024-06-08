package io.voodoo.apps.ads.api.listener

import io.voodoo.apps.ads.api.AdClient
import io.voodoo.apps.ads.api.model.Ad

interface AdListenerHolder {

    fun addAdLoadingListener(listener: AdLoadingListener)
    fun removeAdLoadingListener(listener: AdLoadingListener)

    fun addAdModerationListener(listener: AdModerationListener)
    fun removeAdModerationListener(listener: AdModerationListener)

    fun addAdRevenueListener(listener: AdRevenueListener)
    fun removeAdRevenueListener(listener: AdRevenueListener)

    fun addOnAvailableAdCountChangedListener(listener: OnAvailableAdCountChangedListener)
    fun removeOnAvailableAdCountChangedListener(listener: OnAvailableAdCountChangedListener)
}

// Wrap a list of listeners as one
internal class AdListenerHolderWrapper(
    private val adLoadingListeners: Iterable<AdLoadingListener>,
    private val adModerationListeners: Iterable<AdModerationListener>,
    private val adRevenueListeners: Iterable<AdRevenueListener>,
    private val onAvailableAdCountChangedListeners: Iterable<OnAvailableAdCountChangedListener>,
) : AdLoadingListener, AdModerationListener, AdRevenueListener, OnAvailableAdCountChangedListener {

    override fun onAdLoadingStarted(type: Ad.Type) {
        adLoadingListeners.forEach { it.onAdLoadingStarted(type) }
    }

    override fun onAdLoadingFailed(type: Ad.Type, exception: Exception) {
        adLoadingListeners.forEach { it.onAdLoadingFailed(type, exception) }
    }

    override fun onAdLoadingFinished(ad: Ad) {
        adLoadingListeners.forEach { it.onAdLoadingFinished(ad) }
    }

    override fun onAdBlocked(ad: Ad) {
        adModerationListeners.forEach { it.onAdBlocked(ad) }
    }

    override fun onAdRevenuePaid(ad: Ad) {
        adRevenueListeners.forEach { it.onAdRevenuePaid(ad) }
    }

    override fun onAvailableAdCountChanged(count: AdClient.AdCount) {
        onAvailableAdCountChangedListeners.forEach {
            it.onAvailableAdCountChanged(count)
        }
    }
}
