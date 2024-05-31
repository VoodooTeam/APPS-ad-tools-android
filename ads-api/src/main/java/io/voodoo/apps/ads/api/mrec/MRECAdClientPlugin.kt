package io.voodoo.apps.ads.api.mrec

import android.view.View
import io.voodoo.apps.ads.api.model.Ad

interface MRECAdClientPlugin {

    suspend fun onPreLoadAd(adView: View)
    suspend fun onAdLoadException(adView: View, exception: Exception)
    suspend fun onAdLoaded(adView: View, ad: Ad.MREC)
}
