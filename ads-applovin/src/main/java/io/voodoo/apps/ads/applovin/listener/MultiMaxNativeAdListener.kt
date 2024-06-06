package io.voodoo.apps.ads.applovin.listener

import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxError
import com.applovin.mediation.nativeAds.MaxNativeAdListener
import com.applovin.mediation.nativeAds.MaxNativeAdView
import java.util.concurrent.CopyOnWriteArraySet

internal class MultiMaxNativeAdListener : MaxNativeAdListener() {

    // TODO: check the implementation, we're re-creating the backing list for every request (because we add a listener)
    private val delegates = CopyOnWriteArraySet<MaxNativeAdListener>()

    fun add(listener: MaxNativeAdListener) {
        delegates.add(listener)
    }

    fun remove(listener: MaxNativeAdListener) {
        delegates.remove(listener)
    }

    override fun onNativeAdLoaded(view: MaxNativeAdView?, ad: MaxAd) {
        delegates.forEach { it.onNativeAdLoaded(view, ad) }
    }

    override fun onNativeAdLoadFailed(adUnitId: String, error: MaxError) {
        delegates.forEach { it.onNativeAdLoadFailed(adUnitId, error) }
    }

    override fun onNativeAdClicked(ad: MaxAd) {
        delegates.forEach { it.onNativeAdClicked(ad) }
    }
}
