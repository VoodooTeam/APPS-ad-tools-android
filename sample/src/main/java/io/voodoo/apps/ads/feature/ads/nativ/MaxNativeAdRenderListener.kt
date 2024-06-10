package io.voodoo.apps.ads.feature.ads.nativ

import android.util.Log
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.core.view.doOnNextLayout
import androidx.core.view.updateLayoutParams
import com.applovin.mediation.nativeAds.MaxNativeAdView
import io.voodoo.apps.ads.applovin.nativ.MaxNativeAdRenderListener
import io.voodoo.apps.ads.applovin.nativ.MaxNativeAdWrapper

class MaxNativeAdRenderListener : MaxNativeAdRenderListener {

    override fun onPreRender(ad: MaxNativeAdWrapper, view: MaxNativeAdView) {
        // Because ad view need to have a fixed size, we need to update it before rendering
        val ratio = ad.ad.nativeAd?.mediaContentAspectRatio?.takeIf { it > 0.0f }
        Log.e("MaxNativeAdRenderListener", "onPreRender ratio $ratio")

        view.mediaContentViewGroup.updateLayoutParams<ConstraintLayout.LayoutParams> {
            dimensionRatio = ratio?.toString() ?: "3:4"
        }
    }

    override fun onPostRender(ad: MaxNativeAdWrapper, view: MaxNativeAdView) {
        // log errors
        view.mediaContentViewGroup.doOnNextLayout {
            Log.v(
                "MaxNativeAdRenderListener",
                "mediaContentViewGroup nextLayout height ${it.measuredHeight}"
            )
            if (view.mediaContentViewGroup.children.firstOrNull()?.measuredHeight == 0) {
                Log.e("MaxNativeAdRenderListener", "media not displayed (height = 0)")
            }
        }
    }
}
