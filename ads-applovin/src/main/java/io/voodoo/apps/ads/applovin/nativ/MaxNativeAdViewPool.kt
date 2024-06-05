package io.voodoo.apps.ads.applovin.nativ

import android.content.Context
import com.applovin.mediation.nativeAds.MaxNativeAdView
import io.voodoo.apps.ads.applovin.util.ViewPool

internal class MaxNativeAdViewPool(
    private val factory: MaxNativeAdViewFactory,
) : ViewPool<MaxNativeAdView>() {

    override fun createView(context: Context): MaxNativeAdView {
        return factory.create(context)
    }
}
