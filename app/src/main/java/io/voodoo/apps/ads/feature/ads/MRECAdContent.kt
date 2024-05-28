package io.voodoo.apps.ads.feature.ads

import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.voodoo.apps.ads.applovin.mrec.MaxMRECAdWrapper
import io.voodoo.apps.ads.model.Ad

@Composable
fun MRECAdContent(
    ad: Ad.MREC,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            check(ad is MaxMRECAdWrapper)

            FrameLayout(context).apply {
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                ad.render(this)
            }
        }
    )
}
