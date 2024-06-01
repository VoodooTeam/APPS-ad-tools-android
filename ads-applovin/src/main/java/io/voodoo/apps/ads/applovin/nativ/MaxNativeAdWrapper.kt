package io.voodoo.apps.ads.applovin.nativ

import android.view.View
import android.view.ViewGroup
import com.appharbr.sdk.engine.AdResult
import com.appharbr.sdk.engine.AdStateResult
import com.applovin.mediation.MaxAd
import com.applovin.mediation.nativeAds.MaxNativeAdLoader
import com.applovin.mediation.nativeAds.MaxNativeAdView
import io.voodoo.apps.ads.api.model.Ad
import io.voodoo.apps.ads.applovin.util.buildAnalyticsInfo
import io.voodoo.apps.ads.applovin.util.id
import io.voodoo.apps.ads.applovin.util.removeFromParent

class MaxNativeAdWrapper internal constructor(
    internal val ad: MaxAd,
    internal val loader: MaxNativeAdLoader,
    internal val viewPool: MaxNativeAdViewPool,
    internal val moderationResult: AdResult? = null,

    override var analyticsInfo: AnalyticsInfo = ad.buildAnalyticsInfo(moderationResult)
) : Ad.Native() {

    override val id: Id = ad.id

    override val isExpired: Boolean
        get() = ad.nativeAd?.isExpired == true
    override val isBlocked: Boolean
        get() = moderationResult?.adStateResult == AdStateResult.BLOCKED

    internal var view: MaxNativeAdView? = null
        private set

    override fun render(parent: View) {
        require(parent is ViewGroup) { "parent is not a ViewGroup" }

        val view = viewPool.getOrCreate(parent.context)
        parent.addView(view)
        loader.render(view, ad)
        markAsRendered()
    }

    override fun release() {
        view?.removeFromParent()
        view?.let(viewPool::release)
        view = null
    }
}
