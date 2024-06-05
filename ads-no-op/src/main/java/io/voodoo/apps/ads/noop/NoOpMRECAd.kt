package io.voodoo.apps.ads.noop

import android.view.View
import io.voodoo.apps.ads.api.model.Ad
import java.util.UUID

class NoOpMRECAd(
    override val id: Id = Id(UUID.randomUUID().toString()),
) : Ad.MREC() {

    override val analyticsInfo: AnalyticsInfo = AnalyticsInfo(
        network = "",
        revenue = 0.0,
        cohortId = null,
        creativeId = null,
        placement = null,
        reviewCreativeId = null,
        formatLabel = null,
        moderationResult = null
    )
    override val isBlocked: Boolean
        get() = false
    override val isExpired: Boolean
        get() = false

    override fun canBeServed() = false

    override fun render(parent: View) {
        // no-op
    }
}
