package io.voodoo.apps.ads.feature.ads.nativ

import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup
import androidx.core.view.postDelayed
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.applovin.mediation.nativeAds.MaxNativeAdViewBinder
import io.voodoo.apps.ads.R
import io.voodoo.apps.ads.applovin.nativ.MaxNativeAdViewFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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

            addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    doOnVisible(pollInterval = 1.seconds) {
                        updateState(visible = true)
                    }
                }

                override fun onViewDetachedFromWindow(v: View) {
                    updateState(visible = false)
                }
            })
        }
    }

    private fun MaxNativeAdView.updateState(visible: Boolean) {
        if (visible) {
            postDelayed(3_000) {
                callToActionButton.isActivated = true
            }
        } else {
            // Reset for next ad
            callToActionButton.isActivated = false
        }
    }

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
}
