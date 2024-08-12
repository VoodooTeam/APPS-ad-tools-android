package io.voodoo.apps.ads.feature.ads.nativ.admob

import android.content.Context
import android.view.LayoutInflater
import com.google.android.gms.ads.nativead.NativeAdView
import io.voodoo.apps.ads.R
import io.voodoo.apps.ads.admob.nativ.AdMobNativeAdViewFactory

class MyAdMobNativeAdViewFactory : AdMobNativeAdViewFactory {

    override fun create(context: Context): NativeAdView {
        val inflater = LayoutInflater.from(context)
        val nativeAdView =
            inflater.inflate(R.layout.layout_admob_feed_ad_item, null) as NativeAdView

        // Set the media view.
        nativeAdView.mediaView = nativeAdView.findViewById(R.id.media_view_container)
        //adView.mediaView.imageScaleType = ImageView.ScaleType.CENTER_CROP

        // Set other ad assets.
        //nativeAdView.headlineView = unifiedAdBinding.adHeadline
        nativeAdView.bodyView = nativeAdView.findViewById(R.id.body_text_view)
        nativeAdView.callToActionView = nativeAdView.findViewById(R.id.cta_button)
        //nativeAdView.iconView = unifiedAdBinding.adAppIcon
        //nativeAdView.priceView = unifiedAdBinding.adPrice
        //nativeAdView.starRatingView = unifiedAdBinding.adStars
        //nativeAdView.storeView = unifiedAdBinding.adStore
        nativeAdView.advertiserView = nativeAdView.findViewById(R.id.advertiser_textView)


        /*
        adView.addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                doOnVisible(pollInterval = 1.seconds) {
                    updateState(visible = true)
                }
            }

            override fun onViewDetachedFromWindow(v: View) {
                updateState(visible = false)
            }
        })
         */

        return nativeAdView
    }

    /*
    // TODO: implement animation in bg_feed_ad_button.xml and txt_feed_ad_button.xml
    //   to crossfade colors
    private fun NativeAdView.updateState(visible: Boolean) {
        if (visible) {
            postDelayed(3_000) {
                callToActionButton.isActivated = true
            }
        } else {
            // Reset for next ad
            callToActionButton.isActivated = false
        }
    }
     */

    /*
    private fun MaxNativeAdView.doOnVisible(
        pollInterval: Duration,
        body: () -> Unit
    ) {
        if (isVisible()) {
            body()
        } else {
            // Wait for active status
            postDelayed(pollInterval.inWholeMilliseconds) {
                doOnVisible(pollInterval, body)
            }
        }
    }

    private fun View.isVisible(): Boolean {
        if (!isShown) {
            return false
        }
        val actualPosition = Rect()
        val isGlobalVisible = getGlobalVisibleRect(actualPosition)
        val (screenWidth, screenHeight) = Resources.getSystem().displayMetrics.let {
            it.widthPixels to it.heightPixels
        }
        val screen = Rect(0, 0, screenWidth, screenHeight)
        return isGlobalVisible && Rect.intersects(actualPosition, screen)
    }
     */
}
