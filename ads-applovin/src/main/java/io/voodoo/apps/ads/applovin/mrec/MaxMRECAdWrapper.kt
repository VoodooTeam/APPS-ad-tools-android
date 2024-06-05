package io.voodoo.apps.ads.applovin.mrec

import android.view.View
import android.view.ViewGroup
import com.appharbr.sdk.engine.AdResult
import com.appharbr.sdk.engine.AdStateResult
import com.applovin.mediation.MaxAd
import com.applovin.mediation.ads.MaxAdView
import io.voodoo.apps.ads.api.model.Ad
import io.voodoo.apps.ads.applovin.util.MaxDummyAd
import io.voodoo.apps.ads.applovin.util.buildAnalyticsInfo
import io.voodoo.apps.ads.applovin.util.id
import io.voodoo.apps.ads.applovin.util.removeFromParent

class MaxMRECAdWrapper internal constructor(
    val ad: MaxAd,
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

    override fun canBeServed(): Boolean {
        return super.canBeServed() && ad !is MaxDummyAd
    }

    fun updateModerationResult(moderationResult: AdResult) {
        this.moderationResult = moderationResult
        analyticsInfo = ad.buildAnalyticsInfo(moderationResult)
    }

    override fun render(parent: View) {
        // Shouldn't be necessary, but addView will crash if it's still attached
        this.view.removeFromParent()

        require(parent is ViewGroup) { "parent is not a ViewGroup" }
        parent.addView(this.view)
        markAsRendered()
    }

    override fun release() {
        view.removeFromParent()
    }
}
