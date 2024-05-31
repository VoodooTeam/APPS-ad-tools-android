package io.voodoo.app.privacy.model

data class VoodooPrivacyConsent(
    val adConsent: Boolean,
    val analyticsConsent: Boolean,
    val privacyApplicable: Boolean
)
