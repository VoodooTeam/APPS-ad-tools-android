package io.voodoo.apps.ads.applovin.nativ

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnNextLayout
import androidx.core.view.size
import com.appharbr.sdk.engine.AdResult
import com.applovin.mediation.MaxAd
import com.applovin.mediation.nativeAds.MaxNativeAdLoader
import com.applovin.mediation.nativeAds.MaxNativeAdView
import io.voodoo.apps.ads.api.model.Ad
import io.voodoo.apps.ads.applovin.util.MaxDummyAd
import io.voodoo.apps.ads.applovin.util.buildInfo
import io.voodoo.apps.ads.applovin.util.id
import io.voodoo.apps.ads.applovin.util.removeFromParent
import io.voodoo.apps.ads.applovin.util.toModerationResult

class MaxNativeAdWrapper internal constructor(
    val ad: MaxAd,
    internal val loader: MaxNativeAdLoader,
    internal val viewPool: MaxNativeAdViewPool,
    internal val apphrbrModerationResult: AdResult? = null,
) : Ad.Native() {

    override val id: Id = ad.id
    override val info: Info = ad.buildInfo()

    override val moderationResult: ModerationResult?
        get() = apphrbrModerationResult?.adStateResult?.toModerationResult()

    override val isExpired: Boolean
        get() = ad.nativeAd?.isExpired == true

    internal var view: MaxNativeAdView? = null
        private set

    override fun canBeServed(): Boolean {
        return super.canBeServed() && ad !is MaxDummyAd
    }

    override fun render(parent: View) {
        require(parent is ViewGroup) { "parent is not a ViewGroup" }

        val view = viewPool.getOrCreate(parent.context)
        parent.addView(view)
        loader.render(view, ad)
        markAsRendered()

        // Sometimes it looks like the network is either not attaching the content, or doesn't have one
        Log.v("MaxNativeAdWrapper", "render children count: " + view.mediaContentViewGroup.size)
        view.doOnNextLayout {
            Log.v(
                "MaxNativeAdWrapper",
                "layout size: " + view.measuredWidth + "x" + view.measuredHeight
            )
        }
    }

    override fun release() {
        view?.removeFromParent()
        view?.let(viewPool::release)
        view = null
    }
}
