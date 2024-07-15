package io.voodoo.apps.ads.applovin.rewarded

import android.view.View
import com.appharbr.sdk.engine.AdResult
import com.applovin.mediation.MaxAd
import com.applovin.mediation.ads.MaxRewardedAd
import io.voodoo.apps.ads.api.model.Ad
import io.voodoo.apps.ads.applovin.util.MaxDummyAd
import io.voodoo.apps.ads.applovin.util.buildInfo
import io.voodoo.apps.ads.applovin.util.id
import io.voodoo.apps.ads.applovin.util.toModerationResult

class MaxRewardedAdWrapper internal constructor(
    val ad: MaxAd,
    internal val loader: MaxRewardedAd,
    private val placement: String?,
) : Ad.Rewarded() {

    override val id: Id = ad.id
    override val info: Info = ad.buildInfo(placement = placement)

    internal var apphrbrModerationResult: AdResult? = null
    override val moderationResult: ModerationResult?
        get() = apphrbrModerationResult?.adStateResult?.toModerationResult()

    override val isExpired: Boolean
        get() = !loader.isReady

    override fun canBeServed(): Boolean {
        return super.canBeServed() && ad !is MaxDummyAd
    }

    override fun render(parent: View) {
        loader.showAd(placement, loader.activity)
        markAsRendered()
    }

    override fun release() {

    }
}
