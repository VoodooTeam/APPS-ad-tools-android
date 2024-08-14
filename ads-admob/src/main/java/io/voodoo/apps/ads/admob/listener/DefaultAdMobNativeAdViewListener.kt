package io.voodoo.apps.ads.admob.listener

import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd

abstract class DefaultAdMobNativeAdViewListener : AdMobNativeAdViewListener {
    override fun onAdClicked(ad: NativeAd?) {}

    override fun onAdClosed(ad: NativeAd?) {}

    override fun onAdFailedToLoad(error: LoadAdError?) {}

    override fun onAdImpression(ad: NativeAd?) {}

    override fun onAdLoaded(ad: NativeAd?) {}

    override fun onAdOpened(ad: NativeAd?) {}

    override fun onAdSwipeGestureClicked(ad: NativeAd?) {}
}