package io.voodoo.apps.ads.model

import android.view.View

sealed class Ad {

    abstract val id: Id
    abstract val type: Type
    abstract val analyticsInfo: AnalyticsInfo

    abstract val isBlocked: Boolean
    abstract val isExpired: Boolean

    var rendered: Boolean = false
        private set

    fun canBeServed() = !isBlocked && !isExpired

    abstract fun render(view: View)

    protected fun markAsRendered() {
        rendered = true
    }

    // Inner class

    data class AnalyticsInfo(
        val network: String,
        val revenue: Double,
        val cohortId: String?,
        val creativeId: String?,
        val placement: String?,
        val reviewCreativeId: String?,
        val formatLabel: String?,
        val moderationResult: String?
    )

    @JvmInline
    value class Id(private val id: String)

    enum class Type {
        NATIVE, MREC,
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
}
