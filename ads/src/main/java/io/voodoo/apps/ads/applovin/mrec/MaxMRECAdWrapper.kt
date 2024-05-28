package io.voodoo.apps.ads.applovin.mrec

import android.view.View
import com.appharbr.sdk.engine.AdResult
import com.appharbr.sdk.engine.AdStateResult
import com.applovin.mediation.MaxAd
import com.applovin.mediation.ads.MaxAdView
import io.voodoo.apps.ads.applovin.util.buildAnalyticsInfo
import io.voodoo.apps.ads.applovin.util.id
import io.voodoo.apps.ads.model.Ad

class MaxMRECAdWrapper internal constructor(
    internal val ad: MaxAd,
    internal val view: MaxAdView,
) : Ad.MREC() {

    override val id: Id = ad.id

    override var analyticsInfo: AnalyticsInfo = ad.buildAnalyticsInfo(null)
        internal set
    internal var moderationResult: AdResult? = null

    override val isExpired: Boolean
        get() = false
    override val isBlocked: Boolean
        get() = moderationResult?.adStateResult == AdStateResult.BLOCKED

    fun updateModerationResult(moderationResult: AdResult) {
        this.moderationResult = moderationResult
        analyticsInfo = ad.buildAnalyticsInfo(moderationResult)
    }

    override fun render(view: View) {
        markAsRendered()
        // TODO: add this.view to view
    }
}
