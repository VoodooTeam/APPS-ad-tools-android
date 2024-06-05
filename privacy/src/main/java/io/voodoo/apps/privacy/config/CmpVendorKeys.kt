package io.voodoo.apps.privacy.config

enum class CmpVendor(private val key: String) : BaseCmpEnum {
    ADJUST("5f1b2fbeb8e05c306d7249ec"),
    FIREBASE_CRASHLYTICS("5e68f9f669e7a93e0b25906d");

    override fun getKey(): String {
        return key
    }
}

object CmpVendorHelper : BaseCmpHelper<CmpVendor>()
