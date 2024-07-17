package io.voodoo.apps.ads.api.listener

import io.voodoo.apps.ads.api.AdClient
import io.voodoo.apps.ads.api.model.Ad

interface AdLoadingListener {
    fun onAdLoadingStarted(adClient: AdClient<*>)
    fun onAdLoadingFailed(adClient: AdClient<*>, exception: Exception)
    fun onAdLoadingFinished(adClient: AdClient<*>, ad: Ad)
}
