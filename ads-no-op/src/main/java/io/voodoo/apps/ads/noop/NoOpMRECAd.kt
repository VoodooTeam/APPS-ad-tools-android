package io.voodoo.apps.ads.noop

import android.view.View
import io.voodoo.apps.ads.api.model.Ad
import java.util.UUID

class NoOpMRECAd(
    override val id: Id = Id(UUID.randomUUID().toString()),
) : Ad.MREC() {

    override val moderationResult: ModerationResult
        get() = ModerationResult.UNKNOWN

    override val info: Info = Info(
        adUnit = "",
        network = "",
        revenue = 0.0,
        revenuePrecision = "",
        cohortId = null,
        creativeId = null,
        placement = null,
        reviewCreativeId = null,
        formatLabel = null,
        requestLatencyMillis = 0L
    )
    override val isExpired: Boolean
        get() = false

    override fun canBeServed() = false

    override fun render(parent: View) {
        // no-op
    }
}
