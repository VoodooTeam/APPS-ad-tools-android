package io.voodoo.apps.ads.api.rewarded

import io.voodoo.apps.ads.api.model.Ad

interface RewardedAdClientPlugin {

    // TODO: ugly upcast to Any...
    suspend fun onPreLoadAd(loader: Any)
    suspend fun onAdLoadException(loader: Any, exception: Exception)
    suspend fun onAdLoaded(ad: Ad.Rewarded)
}
