package io.voodoo.apps.ads.applovin.interstitial

import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxInterstitialAd
import java.io.Closeable

interface MaxInterstitialAdClientPlugin : Closeable {

    suspend fun onPreLoadAd(loader: MaxInterstitialAd)
    suspend fun onAdLoadException(loader: MaxInterstitialAd, error: MaxError)
    suspend fun onAdLoaded(ad: MaxInterstitialAdWrapper)

    fun onDestroyAd(ad: MaxInterstitialAdWrapper)
}
