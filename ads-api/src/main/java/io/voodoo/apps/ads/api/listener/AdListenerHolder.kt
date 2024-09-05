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

    fun addAdClickListener(listener: AdClickListener)
    fun removeAdClickListener(listener: AdClickListener)
}

// Wrap a list of listeners as one
internal class AdListenerHolderWrapper(
    private val adLoadingListeners: Iterable<AdLoadingListener>,
    private val adModerationListeners: Iterable<AdModerationListener>,
    private val adRevenueListeners: Iterable<AdRevenueListener>,
    private val adClickListeners: Iterable<AdClickListener>,
) : AdLoadingListener, AdModerationListener, AdRevenueListener, AdClickListener {

    override fun onAdLoadingStarted(adClient: AdClient<*>) {
        adLoadingListeners.forEach { it.onAdLoadingStarted(adClient) }
    }

    override fun onAdLoadingFailed(adClient: AdClient<*>, exception: Exception) {
        adLoadingListeners.forEach { it.onAdLoadingFailed(adClient, exception) }
    }

    override fun onAdLoadingFinished(adClient: AdClient<*>, ad: Ad) {
        adLoadingListeners.forEach { it.onAdLoadingFinished(adClient, ad) }
    }

    override fun onAdBlocked(adClient: AdClient<*>, ad: Ad) {
        adModerationListeners.forEach { it.onAdBlocked(adClient, ad) }
    }

    override fun onAdRevenuePaid(adClient: AdClient<*>, ad: Ad) {
        adRevenueListeners.forEach { it.onAdRevenuePaid(adClient, ad) }
    }

    override fun onAdClick(adClient: AdClient<*>, ad: Ad) {
        adClickListeners.forEach { it.onAdClick(adClient, ad) }
    }
}
