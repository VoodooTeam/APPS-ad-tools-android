package io.voodoo.apps.ads.api.listener

import io.voodoo.apps.ads.model.Ad

interface AdModerationListener {
    fun onAdBlocked(ad: Ad)
}
