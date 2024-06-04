package io.voodoo.apps.ads.compose.content

import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.voodoo.apps.ads.api.model.Ad

@Composable
fun MRECAdContent(
    ad: Ad.MREC,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.defaultMinSize(minWidth = 300.dp, minHeight = 250.dp),
        factory = { context ->
            FrameLayout(context).apply {
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                ad.render(this)
            }
        }
    )
}
