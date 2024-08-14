package io.voodoo.apps.ads.feature.ads.nativ.admob

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAdView
import io.voodoo.apps.ads.R
import io.voodoo.apps.ads.admob.nativ.AdMobNativeAdViewFactory

class MyAdMobNativeAdViewFactory : AdMobNativeAdViewFactory {


    override fun create(context: Context): NativeAdView {
        val inflater = LayoutInflater.from(context)
        val nativeAdView =
            inflater.inflate(R.layout.layout_admob_feed_ad_item, null) as NativeAdView

        // Set the media view.
        nativeAdView.mediaView = nativeAdView.findViewById<MediaView>(R.id.media_view_container)
        // Set other ad assets.
        nativeAdView.bodyView = nativeAdView.findViewById<TextView>(R.id.body_text_view)
        nativeAdView.callToActionView = nativeAdView.findViewById<Button>(R.id.cta_button)
        nativeAdView.iconView = nativeAdView.findViewById<ImageView>(R.id.icon_image_view)
        //nativeAdView.priceView = unifiedAdBinding.adPrice
        //nativeAdView.starRatingView = unifiedAdBinding.adStars
        //nativeAdView.storeView = unifiedAdBinding.adStore
        nativeAdView.advertiserView = nativeAdView.findViewById<TextView>(R.id.advertiser_textView)
        nativeAdView.headlineView = nativeAdView.findViewById<TextView>(R.id.title_text_view)

        return nativeAdView
    }

}
