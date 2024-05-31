package io.voodoo.apps.ads.api.listener

import io.voodoo.apps.ads.api.model.Ad

interface AdLoadingListener {
    fun onAdLoadingStarted(type: Ad.Type)
    fun onAdLoadingFailed(type: Ad.Type, exception: Exception)
    fun onAdLoadingFinished(ad: Ad)
}
