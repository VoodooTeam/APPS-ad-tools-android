package io.voodoo.apps.ads.applovin.rewarded

import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxRewardedAd
import java.io.Closeable

interface MaxRewardedAdClientPlugin : Closeable {

    suspend fun onPreLoadAd(loader: MaxRewardedAd)
    suspend fun onAdLoadException(loader: MaxRewardedAd, error: MaxError)
    suspend fun onAdLoaded(ad: MaxRewardedAdWrapper)

    fun onDestroyAd(ad: MaxRewardedAdWrapper)
}
