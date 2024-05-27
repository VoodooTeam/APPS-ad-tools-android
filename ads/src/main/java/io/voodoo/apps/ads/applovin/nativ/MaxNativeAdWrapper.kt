package io.voodoo.apps.ads.applovin.nativ

import android.view.View
import com.appharbr.sdk.engine.AdResult
import com.appharbr.sdk.engine.AdStateResult
import com.applovin.mediation.MaxAd
import com.applovin.mediation.nativeAds.MaxNativeAdLoader
import com.applovin.mediation.nativeAds.MaxNativeAdView
import io.voodoo.apps.ads.applovin.util.buildAnalyticsInfo
import io.voodoo.apps.ads.applovin.util.id
import io.voodoo.apps.ads.model.Ad

class MaxNativeAdWrapper internal constructor(
    internal val ad: MaxAd,
    internal val loader: MaxNativeAdLoader,
    internal val moderationResult: AdResult? = null,

    override var analyticsInfo: AnalyticsInfo = ad.buildAnalyticsInfo(moderationResult)
) : Ad.Native() {

    override val id: Id = ad.id

    override val isExpired: Boolean
        get() = ad.nativeAd?.isExpired == true
    override val isBlocked: Boolean
        get() = moderationResult?.adStateResult == AdStateResult.BLOCKED

    override fun render(view: View) {
        require(view is MaxNativeAdView) { "MaxNativeAdWrapper requires a MaxNativeAdView to render" }
        loader.render(view, ad)
    }
}
