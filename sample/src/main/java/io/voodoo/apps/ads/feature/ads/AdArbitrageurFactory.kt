package io.voodoo.apps.ads.feature.ads

import android.app.Activity
import androidx.activity.ComponentActivity
import io.voodoo.apps.ads.MockData
import io.voodoo.apps.ads.api.AdArbitrageur
import io.voodoo.apps.ads.api.AdClient
import io.voodoo.apps.ads.api.model.Ad
import io.voodoo.apps.ads.applovin.mrec.MaxMRECAdClient
import io.voodoo.apps.ads.applovin.nativ.MaxNativeAdClient
import io.voodoo.apps.ads.applovin.plugin.amazon.AmazonMRECAdClientPlugin
import io.voodoo.apps.ads.feature.ads.nativ.MaxNativeAdViewFactory

class AdArbitrageurFactory {

    private val adTracker = AdTracker(
        nativeAdUnit = MockData.NATIVE_AD_UNIT,
        mrecAdUnit = MockData.MREC_AD_UNIT
    )

    fun create(activity: ComponentActivity): AdArbitrageur {
        return AdArbitrageur(
            clients = listOf(createNativeClient(activity), createMRECClient(activity))
        ).also {
            it.registerToLifecycle(activity.lifecycle)

            it.addAdLoadingListener(adTracker)
            it.addAdRevenueListener(adTracker)
            it.addAdModerationListener(adTracker)
        }
    }

    private fun createNativeClient(activity: Activity): AdClient<Ad.Native> {
        return MaxNativeAdClient(
            config = AdClient.Config(
                adCacheSize = 3,
                adUnit = MockData.NATIVE_AD_UNIT
            ),
            activity = activity,
            adViewFactory = MaxNativeAdViewFactory(),
            // Provide extras via here if more convenient than the UI
            localExtrasProviders = emptyList(),
        )
    }

    private fun createMRECClient(activity: Activity): AdClient<Ad.MREC> {
        return MaxMRECAdClient(
            config = AdClient.Config(
                adCacheSize = 3,
                adUnit = MockData.MREC_AD_UNIT
            ),
            activity = activity,
            plugins = listOf(AmazonMRECAdClientPlugin(MockData.AMAZON_SLOT_ID)),
            // Provide extras via here if more convenient than the UI
            localExtrasProviders = emptyList(),
        )
    }
}
