package io.voodoo.apps.privacy.config

data class SourcepointConfiguration(
    val accountId: Int,
    val propertyId: Int,
    val gdprPrivacyManagerId: String,
    val usMspsPrivacyManagerId: String,
    val propertyName: String
)
