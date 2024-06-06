package io.voodoo.apps.ads.applovin.mrec

import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAdView
import java.io.Closeable

interface MaxMRECAdClientPlugin : Closeable {

    suspend fun onPreLoadAd(adView: MaxAdView)
    suspend fun onAdLoadException(adView: MaxAdView, error: MaxError)
    suspend fun onAdLoaded(adView: MaxAdView, ad: MaxMRECAdWrapper)

    fun onDestroyAd(ad: MaxMRECAdWrapper)
}
