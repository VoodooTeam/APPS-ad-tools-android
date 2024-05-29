package io.voodoo.apps.ads.applovin.mrec.plugin

import android.view.View
import com.amazon.device.ads.AdError
import com.amazon.device.ads.AdRegistration
import com.amazon.device.ads.DTBAdCallback
import com.amazon.device.ads.DTBAdRequest
import com.amazon.device.ads.DTBAdResponse
import com.amazon.device.ads.DTBAdSize
import com.applovin.mediation.ads.MaxAdView
import io.voodoo.apps.ads.api.mrec.MRECAdClientPlugin
import io.voodoo.apps.ads.model.Ad
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AmazonMRECAdClientPlugin(
    private val amazonSlotId: String
) : MRECAdClientPlugin {

    override suspend fun onPreLoadAd(adView: View) {
        if (!AdRegistration.isInitialized()) return
        require(adView is MaxAdView)

        if (adView.getTag(TAG_AMAZON_AD) == true) return
        adView.setTag(TAG_AMAZON_AD, true)

        val amazonLoader = DTBAdRequest().also {
            it.setSizes(DTBAdSize(300, 250, amazonSlotId))
        }

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

    override suspend fun onAdLoadException(adView: View, exception: Exception) {
        // no-op
    }

    override suspend fun onAdLoaded(adView: View, ad: Ad.MREC) {
        // no-op
    }

    companion object {
        private val TAG_AMAZON_AD = View.generateViewId()
    }
}
