package io.voodoo.apps.ads.feature.ads

import android.content.Context
import android.view.ViewGroup
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.applovin.mediation.nativeAds.MaxNativeAdViewBinder
import io.voodoo.apps.ads.R
import io.voodoo.apps.ads.applovin.nativ.MaxNativeAdViewFactory

class MaxNativeAdViewFactory : MaxNativeAdViewFactory {

    override fun create(context: Context): MaxNativeAdView {
        val binder = MaxNativeAdViewBinder.Builder(R.layout.layout_feed_ad_item)
            .setIconImageViewId(R.id.icon_image_view)
            .setTitleTextViewId(R.id.title_text_view)
            .setBodyTextViewId(R.id.body_text_view)
            .setStarRatingContentViewGroupId(R.id.star_rating_view)
            .setAdvertiserTextViewId(R.id.advertiser_textView)
            .setMediaContentViewGroupId(R.id.media_view_container)
            .setOptionsContentViewGroupId(R.id.ad_options_view)
            .setCallToActionButtonId(R.id.cta_button)
            .build()

        return MaxNativeAdView(binder, context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }
}
