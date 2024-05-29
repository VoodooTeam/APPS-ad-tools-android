package io.voodoo.apps.ads.feature.feed.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.applovin.mediation.nativeAds.MaxNativeAdViewBinder
import io.voodoo.apps.ads.R
import io.voodoo.apps.ads.api.model.Ad
import io.voodoo.apps.ads.feature.ads.MRECAdContent
import io.voodoo.apps.ads.feature.ads.MaxNativeAdContent
import io.voodoo.apps.ads.feature.feed.FeedUiState

@Composable
fun FeedAdItem(
    ad: FeedUiState.Content.ContentItem.Ad,
    modifier: Modifier = Modifier
) {
    when (ad.ad) {
        is Ad.MREC -> {
            FeedMRECAdItem(
                ad = ad.ad,
                modifier = modifier
            )
        }

        is Ad.Native -> {
            FeedNativeAdItem(
                ad = ad.ad,
                modifier = modifier
            )
        }

        null -> {
            // Shouldn't happen, debug UI
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.Red)
            )
        }
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
            Text("Sponsored")
        },
        subtitle = {
            Text("Thank you for seeing this ad from our partner")
        },
        actions = {
            ReportActionMenu(onReportClick = {})
        },
        content = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.background(Color(0xFF0E0E0E), RoundedCornerShape(16.dp))
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF161616), RoundedCornerShape(16.dp))
                        .padding(vertical = 32.dp)
                ) {
                    MRECAdContent(ad = ad)
                }
            }
        }
    )
}

@Composable
fun FeedNativeAdItem(
    ad: Ad.Native,
    modifier: Modifier = Modifier
) {
    MaxNativeAdContent(
        modifier = modifier,
        ad = ad,
        binderProvider = {
            MaxNativeAdViewBinder.Builder(R.layout.layout_feed_ad_item)
                .setIconImageViewId(R.id.icon_image_view)
                .setTitleTextViewId(R.id.title_text_view)
                .setBodyTextViewId(R.id.body_text_view)
                .setStarRatingContentViewGroupId(R.id.star_rating_view)
                .setAdvertiserTextViewId(R.id.advertiser_textView)
                .setMediaContentViewGroupId(R.id.media_view_container)
                .setOptionsContentViewGroupId(R.id.ad_options_view)
                .setCallToActionButtonId(R.id.cta_button)
                .build()
        }
    )
}
