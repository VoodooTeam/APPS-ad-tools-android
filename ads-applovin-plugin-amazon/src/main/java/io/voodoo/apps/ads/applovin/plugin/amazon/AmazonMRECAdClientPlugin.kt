package io.voodoo.apps.ads.applovin.plugin.amazon

import com.amazon.device.ads.AdError
import com.amazon.device.ads.AdRegistration
import com.amazon.device.ads.DTBAdCallback
import com.amazon.device.ads.DTBAdRequest
import com.amazon.device.ads.DTBAdResponse
import com.amazon.device.ads.DTBAdSize
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAdView
import io.voodoo.apps.ads.applovin.mrec.MaxMRECAdClientPlugin
import io.voodoo.apps.ads.applovin.mrec.MaxMRECAdWrapper
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AmazonMRECAdClientPlugin(
    private val amazonSlotId: String
) : MaxMRECAdClientPlugin {

    override suspend fun onPreLoadAd(adView: MaxAdView) {
        if (!AdRegistration.isInitialized()) return

        // Only run once per MaxAdView
        if (adView.findLoader() != null) return

        val amazonLoader = DTBAdRequest().also {
            it.setSizes(DTBAdSize(300, 250, amazonSlotId))
        }.also { adView.setLoader(it) }

        suspendCoroutine {
            amazonLoader.loadAd(object : DTBAdCallback {
                override fun onSuccess(dtbAdResponse: DTBAdResponse) {
                    adView.setLocalExtraParameter("amazon_ad_response", dtbAdResponse)
                    it.resume(true)
                }

                override fun onFailure(adError: AdError) {
                    adView.setLocalExtraParameter("amazon_ad_error", adError)
                    it.resume(false)
                }
            })
        }
    }

    override suspend fun onAdLoadException(adView: MaxAdView, error: MaxError) {
        // no-op
    }

    override suspend fun onAdLoaded(adView: MaxAdView, ad: MaxMRECAdWrapper) {
        // no-op
    }

    override fun onDestroyAd(ad: MaxMRECAdWrapper) {
        ad.view.findLoader()?.stop()
    }

    override fun close() {
        // no-op
    }

    private fun MaxAdView.findLoader(): DTBAdRequest? {
        return getTag(R.id.tag_amazon_ad_loader) as? DTBAdRequest
    }

    private fun MaxAdView.setLoader(loader: DTBAdRequest) {
        setTag(R.id.tag_amazon_ad_loader, loader)
    }
}
