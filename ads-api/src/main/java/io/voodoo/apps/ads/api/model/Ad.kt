package io.voodoo.apps.ads.api.model

import android.view.View
import java.util.Date

sealed class Ad {

    abstract val id: Id
    abstract val type: Type
    abstract val info: Info
    abstract val loadedAt: Date
    abstract val moderationResult: ModerationResult?

    val isBlocked: Boolean get() = moderationResult == ModerationResult.BLOCKED
    abstract val isExpired: Boolean

    var rendered: Boolean = false
        private set

    var isRevenuePaid: Boolean = false
        private set

    open fun canBeServed() = !isBlocked && !isExpired

    abstract fun render(parent: View)
    open fun release() {}

    protected fun markAsRendered() {
        rendered = true
    }

    protected fun markAsRevenuePaid() {
        isRevenuePaid = true
    }

    // Inner class

    data class Info(
        val adUnit: String,
        val network: String,
        val revenue: Double,
        val revenuePrecision: String,
        val cohortId: String?,
        val creativeId: String?,
        val placement: String?,
        val reviewCreativeId: String?,
        val formatLabel: String?,
        val requestLatencyMillis: Long,
    )

    enum class ModerationResult(val analyticsValue: String) {
        UNKNOWN("unknown"),
        VERIFIED("verified"),
        BLOCKED("blocked"),
        REPORTED("reported")
    }

    @JvmInline
    value class Id(val id: String)

    enum class Type {
        NATIVE, MREC, REWARDED
    }

    // Implem

    abstract class Native : Ad() {

        override val type: Type
            get() = Type.NATIVE
    }

    abstract class MREC : Ad() {

        override val type: Type
            get() = Type.MREC
    }

    abstract class Rewarded : Ad() {

        override val type: Type
            get() = Type.REWARDED
    }
}
