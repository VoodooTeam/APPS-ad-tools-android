package io.voodoo.apps.ads.admob.listener

import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd

interface AdMobNativeAdViewListener {
    fun onAdClicked(ad: NativeAd?)

    fun onAdClosed(ad: NativeAd?)

    fun onAdFailedToLoad(error: LoadAdError?)

    fun onAdImpression(ad: NativeAd?)

    fun onAdLoaded(ad: NativeAd?)

    fun onAdOpened(ad: NativeAd?)

    fun onAdSwipeGestureClicked(ad: NativeAd?)
}