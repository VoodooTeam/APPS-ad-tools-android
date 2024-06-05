package io.voodoo.apps.ads.applovin.listener

import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxError
import com.applovin.mediation.nativeAds.MaxNativeAdListener
import com.applovin.mediation.nativeAds.MaxNativeAdView
import java.util.Collections

internal class MultiMaxNativeAdListener : MaxNativeAdListener() {

    private val _delegates = Collections.synchronizedSet(mutableSetOf<MaxNativeAdListener>())
    private val delegates get() = synchronized(_delegates) { _delegates.toList() }

    fun add(listener: MaxNativeAdListener) {
        _delegates.add(listener)
    }

    fun remove(listener: MaxNativeAdListener) {
        _delegates.remove(listener)
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
