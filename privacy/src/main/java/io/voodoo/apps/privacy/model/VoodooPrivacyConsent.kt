package io.voodoo.apps.privacy.model

data class VoodooPrivacyConsent(
    val adConsent: Boolean,
    val analyticsConsent: Boolean,
    val doNotSellDataEnabled: Boolean,
    val privacyApplicable: Boolean
)
