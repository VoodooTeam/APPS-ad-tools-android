package io.voodoo.apps.ads.feature.ads.nativ.admob

import android.content.res.Resources
import android.graphics.Rect
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.postDelayed
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import io.voodoo.apps.ads.admob.nativ.AdMobNativeAdViewRenderer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class MyAdMobNativeAdViewRenderer : AdMobNativeAdViewRenderer {

    override fun render(nativeAdView: NativeAdView, nativeAd: NativeAd) {
        // Set the media view.
        nativeAdView.mediaView?.let { mediaView ->
            nativeAd.mediaContent?.let { content -> mediaView.mediaContent = content }
            mediaView.setImageScaleType(ImageView.ScaleType.CENTER_CROP)
        }

        // Set other ad assets.
        (nativeAdView.bodyView as? TextView)?.let {
            it.text = nativeAd.body
        }
        (nativeAdView.callToActionView as? Button)?.let {
            it.text = nativeAd.callToAction
        }
        (nativeAdView.iconView as? ImageView)?.let {
            it.setImageDrawable(nativeAd.icon?.drawable)
        }
        //nativeAdView.priceView = unifiedAdBinding.adPrice
        //nativeAdView.starRatingView = unifiedAdBinding.adStars
        //nativeAdView.storeView = unifiedAdBinding.adStore
        (nativeAdView.advertiserView as? TextView)?.let {
            it.text = nativeAd.advertiser
        }

        (nativeAdView.headlineView as? TextView)?.let {
            it.text = nativeAd.headline
        }

        nativeAdView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                nativeAdView.doOnVisible(pollInterval = 1.seconds) {
                    nativeAdView.updateState(visible = true)
                }
            }

            override fun onViewDetachedFromWindow(v: View) {
                nativeAdView.updateState(visible = false)
            }
        })

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        nativeAdView.setNativeAd(nativeAd)

    }

    // TODO: implement animation in bg_feed_ad_button.xml and txt_feed_ad_button.xml
    //   to crossfade colors
    private fun NativeAdView.updateState(visible: Boolean) {
        if (visible) {
            postDelayed(3_000) {
                callToActionView?.isActivated = true
            }
        } else {
            // Reset for next ad
            callToActionView?.isActivated = false
        }
    }

    private fun NativeAdView.doOnVisible(
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
}
