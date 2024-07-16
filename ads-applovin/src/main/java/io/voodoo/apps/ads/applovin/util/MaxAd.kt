package io.voodoo.apps.ads.applovin.util

import com.appharbr.sdk.engine.AdStateResult
import com.applovin.mediation.MaxAd
import io.voodoo.apps.ads.api.model.Ad

val MaxAd.id: Ad.Id get() = Ad.Id(System.identityHashCode(this).toString())

fun MaxAd.buildInfo(
    // Special case to allow override in rewarded ads, because the placement value before the display
    // is the value defined in the web console
    placement: String? = this.placement,
): Ad.Info {
    return Ad.Info(
        adUnit = this.adUnitId,
        network = this.networkName,
        revenue = this.revenue,
        revenuePrecision = this.revenuePrecision,
        cohortId = this.waterfall?.testName,
        creativeId = this.creativeId,
        placement = this.placement,
        reviewCreativeId = this.adReviewCreativeId,
        formatLabel = this.format?.label,
        requestLatencyMillis = this.requestLatencyMillis,
    )
}

fun AdStateResult.toModerationResult(): Ad.ModerationResult {
    return when (this) {
        AdStateResult.UNKNOWN -> Ad.ModerationResult.UNKNOWN
        AdStateResult.VERIFIED -> Ad.ModerationResult.VERIFIED
        AdStateResult.BLOCKED -> Ad.ModerationResult.BLOCKED
        AdStateResult.REPORTED -> Ad.ModerationResult.REPORTED
    }
}
