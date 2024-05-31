package io.voodoo.apps.ads.applovin.compose.content

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.applovin.mediation.nativeAds.MaxNativeAdViewBinder
import io.voodoo.apps.ads.api.model.Ad
import io.voodoo.apps.ads.applovin.nativ.MaxNativeAdWrapper

@Composable
fun MaxNativeAdContent(
    ad: Ad.Native,
    binderProvider: () -> MaxNativeAdViewBinder,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            check(ad is MaxNativeAdWrapper) { "MaxNativeAdContent called with a non-max native ad" }

            MaxNativeAdView(binderProvider(), context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                ad.render(this)
            }
        }
    )
}
