package io.voodoo.apps.ads.admob.nativ

import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

interface AdMobNativeAdViewRenderer {
    fun render(nativeAdView: NativeAdView, nativeAd: NativeAd)
}