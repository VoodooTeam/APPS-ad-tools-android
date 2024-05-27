package io.voodoo.apps.ads.applovin.util

import com.appharbr.sdk.engine.AdResult
import com.appharbr.sdk.engine.AdStateResult
import com.applovin.mediation.MaxAd
import io.voodoo.apps.ads.model.Ad

val MaxAd.id: Ad.Id get() = Ad.Id(System.identityHashCode(this).toString())

fun MaxAd.buildAnalyticsInfo(moderationResult: AdResult?): Ad.AnalyticsInfo {
    return Ad.AnalyticsInfo(
        network = this.networkName,
        revenue = this.revenue,
        cohortId = this.waterfall?.testName,
        creativeId = this.creativeId,
        placement = this.placement,
        reviewCreativeId = this.adReviewCreativeId,
        formatLabel = this.format?.label,
        moderationResult = when (moderationResult?.adStateResult) {
            AdStateResult.UNKNOWN -> "unknown"
            AdStateResult.VERIFIED -> "verified"
            AdStateResult.BLOCKED -> "blocked"
            AdStateResult.REPORTED -> "reported"
            null -> ""
        }
    )
}
