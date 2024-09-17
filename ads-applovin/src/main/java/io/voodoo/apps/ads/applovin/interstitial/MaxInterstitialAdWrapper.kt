package io.voodoo.apps.ads.applovin.interstitial

import android.view.View
import com.appharbr.sdk.engine.AdResult
import com.applovin.mediation.MaxAd
import com.applovin.mediation.ads.MaxInterstitialAd
import io.voodoo.apps.ads.api.model.Ad
import io.voodoo.apps.ads.applovin.util.MaxDummyAd
import io.voodoo.apps.ads.applovin.util.buildInfo
import io.voodoo.apps.ads.applovin.util.id
import io.voodoo.apps.ads.applovin.util.toModerationResult
import java.util.Date

class MaxInterstitialAdWrapper(
    val ad: MaxAd,
    internal val loader: MaxInterstitialAd,
    internal var apphrbrModerationResult: AdResult? = null,
    private val placement: String?,
    override val loadedAt: Date,
) : Ad.Interstitial() {

    override val id: Id = ad.id
    override val info: Info = ad.buildInfo(placement = placement)

    override val moderationResult: ModerationResult?
        get() = apphrbrModerationResult?.adStateResult?.toModerationResult()

    override val isExpired: Boolean
        get() = !loader.isReady

    override fun canBeServed(): Boolean {
        return super.canBeServed() && loader.isReady && ad !is MaxDummyAd
    }

    override fun render(parent: View) {
        loader.showAd(placement, loader.activity)
        markAsRendered()
    }

    internal fun markAsPaidInternal() {
        super.markAsRevenuePaid()
    }

    override fun release() {

    }
}
