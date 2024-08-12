package io.voodoo.apps.ads.admob.nativ

import android.view.View
import android.view.ViewGroup
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import io.voodoo.apps.ads.api.model.Ad
import io.voodoo.apps.ads.admob.util.buildInfo
import io.voodoo.apps.ads.admob.util.id
import io.voodoo.apps.ads.admob.util.removeFromParent
import java.util.Date

class AdmobNativeAdWrapper internal constructor(
    val ad: NativeAd,
    internal val viewPool: AdMobNativeAdViewPool,
    internal val adViewRenderer: AdMobNativeAdViewRenderer,
    override val loadedAt: Date,
) : Ad.Native() {

    override val id: Id = ad.id
    override val info: Info = ad.buildInfo()

    override val moderationResult: ModerationResult = ModerationResult.UNKNOWN // TODO

    override val isExpired: Boolean = false // TODO

    internal var view: NativeAdView? = null
        private set

    override fun render(parent: View) {
        // safety in case the view is already render (shouldn't happen, but be safe)
        release()
        require(parent is ViewGroup) { "parent is not a ViewGroup" }

        val view = viewPool.getOrCreate(parent.context)
            .also { this.view = it }

        adViewRenderer.render(view, ad)

        parent.addView(view)

        markAsRendered()
    }

    internal fun markAsPaidInternal() {
        super.markAsRevenuePaid()
    }

    override fun release() {
        view?.removeFromParent()
        view?.let(viewPool::release)
        view = null
    }
}
