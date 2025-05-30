package io.voodoo.apps.ads.applovin.plugin.amazon

import android.content.Context
import com.amazon.aps.ads.model.ApsAdNetwork
import com.amazon.device.ads.AdError
import com.amazon.device.ads.AdRegistration
import com.amazon.device.ads.DTBAdCallback
import com.amazon.device.ads.DTBAdNetworkInfo
import com.amazon.device.ads.DTBAdRequest
import com.amazon.device.ads.DTBAdResponse
import com.amazon.device.ads.DTBAdSize
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxInterstitialAd
import io.voodoo.apps.ads.applovin.interstitial.MaxInterstitialAdClientPlugin
import io.voodoo.apps.ads.applovin.interstitial.MaxInterstitialAdWrapper
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// Note: create a new instance for each AdClient
class AmazonMaxInterstitialAdClientPlugin(
    context: Context,
    private val amazonSlotId: String,
    private val isVideo: Boolean
) : MaxInterstitialAdClientPlugin {

    private val context = context.applicationContext
    private var amazonLoader: DTBAdRequest? = null

    override suspend fun onPreLoadAd(loader: MaxInterstitialAd) {
        if (!AdRegistration.isInitialized()) return

        // Only run once per MaxInterstitialAd
        if (amazonLoader != null) return
        val sizes = if (isVideo) {
            DTBAdSize.DTBVideo(320, 480, amazonSlotId)
        } else {
            DTBAdSize.DTBInterstitialAdSize(amazonSlotId)
        }

        val amazonLoader = DTBAdRequest(context, DTBAdNetworkInfo(ApsAdNetwork.MAX)).also {
            it.setSizes(sizes)
        }.also { this.amazonLoader = it }

        suspendCoroutine {
            amazonLoader.loadAd(object : DTBAdCallback {
                override fun onSuccess(dtbAdResponse: DTBAdResponse) {
                    loader.setLocalExtraParameter("amazon_ad_response", dtbAdResponse)
                    it.resume(true)
                }

                override fun onFailure(adError: AdError) {
                    loader.setLocalExtraParameter("amazon_ad_error", adError)
                    it.resume(false)
                }
            })
        }
    }

    override suspend fun onAdLoadException(loader: MaxInterstitialAd, error: MaxError) {
        // no-op
    }

    override suspend fun onAdLoaded(ad: MaxInterstitialAdWrapper) {
        // no-op
    }

    override fun onDestroyAd(ad: MaxInterstitialAdWrapper) {
        // no-op
    }

    override fun close() {
        amazonLoader?.stop()
        amazonLoader = null
    }
}
