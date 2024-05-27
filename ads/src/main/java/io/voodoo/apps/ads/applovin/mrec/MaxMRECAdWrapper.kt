package io.voodoo.apps.ads.applovin.mrec

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
    override var seen: Boolean = false
        internal set
    internal var moderationResult: AdResult? = null

    override val isBlocked: Boolean
        get() = moderationResult?.adStateResult == AdStateResult.BLOCKED

    override fun isReady(): Boolean {
        return !isBlocked
    }

    fun updateModerationResult(moderationResult: AdResult) {
        this.moderationResult = moderationResult
        analyticsInfo = ad.buildAnalyticsInfo(moderationResult)
    }
}
