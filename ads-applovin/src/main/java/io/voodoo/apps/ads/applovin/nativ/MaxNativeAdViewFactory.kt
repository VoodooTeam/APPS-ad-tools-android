package io.voodoo.apps.ads.applovin.nativ

import android.content.Context
import androidx.annotation.UiThread
import com.applovin.mediation.nativeAds.MaxNativeAdView

interface MaxNativeAdViewFactory {

    @UiThread
    fun create(context: Context): MaxNativeAdView
}
