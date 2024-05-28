package io.voodoo.apps.ads.feature.ads

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.applovin.mediation.nativeAds.MaxNativeAdViewBinder
import io.voodoo.apps.ads.model.Ad

@Composable
fun NativeAdContent(
    ad: Ad.Native,
    binderProvider: () -> MaxNativeAdViewBinder,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
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
