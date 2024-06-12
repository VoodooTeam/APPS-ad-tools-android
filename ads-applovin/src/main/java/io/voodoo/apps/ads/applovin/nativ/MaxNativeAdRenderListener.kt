package io.voodoo.apps.ads.applovin.nativ

import com.applovin.mediation.nativeAds.MaxNativeAdView

interface MaxNativeAdRenderListener {

    fun onPreRender(ad: MaxNativeAdWrapper, view: MaxNativeAdView)
    fun onPostRender(ad: MaxNativeAdWrapper, view: MaxNativeAdView)
}
