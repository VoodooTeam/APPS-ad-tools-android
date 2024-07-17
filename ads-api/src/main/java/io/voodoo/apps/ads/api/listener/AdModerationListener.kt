package io.voodoo.apps.ads.api.listener

import io.voodoo.apps.ads.api.AdClient
import io.voodoo.apps.ads.api.model.Ad

fun interface AdModerationListener {
    fun onAdBlocked(adClient: AdClient<*>, ad: Ad)
}
