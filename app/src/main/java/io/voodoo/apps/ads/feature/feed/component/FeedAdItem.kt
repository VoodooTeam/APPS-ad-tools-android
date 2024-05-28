package io.voodoo.apps.ads.feature.feed.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.applovin.mediation.nativeAds.MaxNativeAdViewBinder
import io.voodoo.apps.ads.R
import io.voodoo.apps.ads.feature.ads.MRECAdContent
import io.voodoo.apps.ads.feature.ads.NativeAdContent
import io.voodoo.apps.ads.feature.feed.FeedUiState
import io.voodoo.apps.ads.model.Ad

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
            // Shouldn't happen
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
                painter = painterResource(io.voodoo.apps.ads.R.drawable.ic_launcher_foreground),
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
            // TODO: report
        },
        content = {
            Box(contentAlignment = Alignment.Center) {
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
        binderProvider = {
            MaxNativeAdViewBinder.Builder(io.voodoo.apps.ads.R.layout.layout_feed_ad_item)
                .setIconImageViewId(io.voodoo.apps.ads.R.id.icon_image_view)
                .setTitleTextViewId(io.voodoo.apps.ads.R.id.title_text_view)
                .setBodyTextViewId(io.voodoo.apps.ads.R.id.body_text_view)
                .setStarRatingContentViewGroupId(io.voodoo.apps.ads.R.id.star_rating_view)
                .setAdvertiserTextViewId(io.voodoo.apps.ads.R.id.advertiser_textView)
                .setMediaContentViewGroupId(io.voodoo.apps.ads.R.id.media_view_container)
                .setOptionsContentViewGroupId(io.voodoo.apps.ads.R.id.ad_options_view)
                .setCallToActionButtonId(R.id.cta_button)
                .build()
        }
    )
}
