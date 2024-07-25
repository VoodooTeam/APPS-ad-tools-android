package io.voodoo.apps.ads.applovin.mrec

import android.view.View
import android.view.ViewGroup
import com.appharbr.sdk.engine.AdResult
import com.applovin.mediation.MaxAd
import com.applovin.mediation.ads.MaxAdView
import io.voodoo.apps.ads.api.model.Ad
import io.voodoo.apps.ads.applovin.util.MaxDummyAd
import io.voodoo.apps.ads.applovin.util.buildInfo
import io.voodoo.apps.ads.applovin.util.id
import io.voodoo.apps.ads.applovin.util.removeFromParent
import io.voodoo.apps.ads.applovin.util.toModerationResult
import java.util.Date

class MaxMRECAdWrapper internal constructor(
    val ad: MaxAd,
    val view: MaxAdView,
    override val loadedAt: Date,
) : Ad.MREC() {

    override val id: Id = ad.id
    override val info: Info = ad.buildInfo()

    internal var apphrbrModerationResult: AdResult? = null
    override val moderationResult: ModerationResult?
        get() = apphrbrModerationResult?.adStateResult?.toModerationResult()

    override val isExpired: Boolean
        get() = false

    override fun canBeServed(): Boolean {
        return super.canBeServed() && ad !is MaxDummyAd
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
