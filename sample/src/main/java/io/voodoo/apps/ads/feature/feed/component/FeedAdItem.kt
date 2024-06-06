package io.voodoo.apps.ads.feature.feed.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.voodoo.apps.ads.R
import io.voodoo.apps.ads.api.model.Ad
import io.voodoo.apps.ads.compose.content.MRECAdContent
import io.voodoo.apps.ads.compose.content.NativeAdContent
import io.voodoo.apps.ads.compose.model.AdHolder

@Composable
fun FeedAdItem(
    ad: AdHolder,
    modifier: Modifier = Modifier
) {
    when (val actualAd = ad.ad) {
        is Ad.MREC -> {
            FeedMRECAdItem(
                ad = actualAd,
                modifier = modifier
            )
        }

        is Ad.Native -> {
            FeedNativeAdItem(
                ad = actualAd,
                modifier = modifier
            )
        }

        else -> throw IllegalArgumentException("Unsupported adType ${ad.ad.type}")
    }
}

@Composable
fun FeedMRECAdItem(
    ad: Ad.MREC,
    modifier: Modifier = Modifier
) {
    FeedItem(
        modifier = modifier,
        icon = {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null
            )
        },
        title = {
            Text("Time for an add")
        },
        subtitle = {
            Text("Sponsored")
        },
        actions = {
            // ReportActionMenu(onReportClick = {})
        },
        content = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0E0E0E), RoundedCornerShape(16.dp))
            ) {
                MRECAdContent(ad = ad)
            }
        }
    )
}

@Composable
fun FeedNativeAdItem(
    ad: Ad.Native,
    modifier: Modifier = Modifier
) {
    NativeAdContent(
        modifier = modifier,
        ad = ad,
    )
}
