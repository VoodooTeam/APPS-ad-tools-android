package io.voodoo.apps.ads.compose.content

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.voodoo.apps.ads.api.model.Ad

@Composable
fun NativeAdContent(
    ad: Ad.Native,
    modifier: Modifier = Modifier
) {
    // According to florent, ConstraintLayout inside an AndroidView is broken, and we need to wrap
    // it in a Column
    Column(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                FrameLayout(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    ad.render(this)
                }
            }
        )
    }
}
