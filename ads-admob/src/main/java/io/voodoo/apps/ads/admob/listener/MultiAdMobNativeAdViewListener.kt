package io.voodoo.apps.ads.admob.listener

import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import java.util.concurrent.CopyOnWriteArraySet

internal class MultiAdMobNativeAdViewListener : AdMobNativeAdViewListener {

    // TODO: check the implementation, we're re-creating the backing list for every request (because we add a listener)
    private val delegates = CopyOnWriteArraySet<AdMobNativeAdViewListener>()

    fun add(listener: AdMobNativeAdViewListener) {
        delegates.add(listener)
    }

    fun remove(listener: AdMobNativeAdViewListener) {
        delegates.remove(listener)
    }

    override fun onAdClosed(ad: NativeAd?) {
        delegates.forEach { it.onAdClosed(ad) }
    }

    override fun onAdFailedToLoad(error: LoadAdError?) {
        delegates.forEach { it.onAdFailedToLoad(error) }
    }

    override fun onAdImpression(ad: NativeAd?) {
        delegates.forEach { it.onAdImpression(ad) }
    }

    override fun onAdOpened(ad: NativeAd?) {
        delegates.forEach { it.onAdOpened(ad) }
    }

    override fun onAdClicked(ad: NativeAd?) {
        delegates.forEach { it.onAdClicked(ad) }
    }

    override fun onAdLoaded(ad: NativeAd?) {
        delegates.forEach { it.onAdLoaded(ad) }
    }

    override fun onAdSwipeGestureClicked(ad: NativeAd?) {
        delegates.forEach { it.onAdSwipeGestureClicked(ad) }
    }
}
