package io.voodoo.apps.ads.applovin.plugin.amazon

import com.amazon.device.ads.AdError
import com.amazon.device.ads.AdRegistration
import com.amazon.device.ads.DTBAdCallback
import com.amazon.device.ads.DTBAdRequest
import com.amazon.device.ads.DTBAdResponse
import com.amazon.device.ads.DTBAdSize
import com.applovin.mediation.ads.MaxRewardedAd
import io.voodoo.apps.ads.api.model.Ad
import io.voodoo.apps.ads.api.rewarded.RewardedAdClientPlugin
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AmazonRewardedAdClientPlugin(
    private val amazonSlotId: String
) : RewardedAdClientPlugin {

    private var loaded = false

    override suspend fun onPreLoadAd(loader: Any) {
        if (!AdRegistration.isInitialized()) return
        require(loader is MaxRewardedAd)

        if (loaded) return
        loaded = true

        val amazonLoader = DTBAdRequest().also {
            it.setSizes(DTBAdSize.DTBVideo(320, 480, amazonSlotId))
        }

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

    override suspend fun onAdLoadException(loader: Any, exception: Exception) {
        // no-op
    }

    override suspend fun onAdLoaded(ad: Ad.Rewarded) {
        // no-op
    }
}
