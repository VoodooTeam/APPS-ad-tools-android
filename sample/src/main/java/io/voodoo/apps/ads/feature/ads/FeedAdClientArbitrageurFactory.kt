package io.voodoo.apps.ads.feature.ads

import android.app.Activity
import androidx.activity.ComponentActivity
import io.voodoo.apps.ads.MockData
import io.voodoo.apps.ads.api.AdClient
import io.voodoo.apps.ads.api.AdClientArbitrageur
import io.voodoo.apps.ads.api.model.Ad
import io.voodoo.apps.ads.api.model.BackoffConfig
import io.voodoo.apps.ads.applovin.mrec.MaxMRECAdClient
import io.voodoo.apps.ads.applovin.nativ.MaxNativeAdClient
import io.voodoo.apps.ads.applovin.plugin.amazon.AmazonMaxMRECAdClientPlugin
import io.voodoo.apps.ads.feature.ads.nativ.MaxNativeAdRenderListener
import io.voodoo.apps.ads.feature.ads.nativ.MaxNativeAdViewFactory
import kotlin.time.Duration.Companion.seconds

class FeedAdClientArbitrageurFactory {

    private val adTracker = AdTracker(
        nativeAdUnit = MockData.NATIVE_AD_UNIT,
        mrecAdUnit = MockData.MREC_AD_UNIT
    )

    fun create(activity: ComponentActivity): AdClientArbitrageur {
        return AdClientArbitrageur(
            clients = listOf(
                createNativeClient(activity),
                createMRECClient(activity),
            ),
            backoffConfig = BackoffConfig(
                maxDelay = 30.seconds,
            ),
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
                adUnit = MockData.NATIVE_AD_UNIT,
                placement = "feed"
            ),
            activity = activity,
            renderListener = MaxNativeAdRenderListener(),
            adViewFactory = MaxNativeAdViewFactory(),
            // Provide extras via here if more convenient than the UI
            localExtrasProviders = emptyList(),
        )
    }

    private fun createMRECClient(activity: Activity): AdClient<Ad.MREC> {
        return MaxMRECAdClient(
            config = AdClient.Config(
                adCacheSize = 3,
                adUnit = MockData.MREC_AD_UNIT,
                placement = "feed"
            ),
            activity = activity,
            plugins = listOf(AmazonMaxMRECAdClientPlugin(activity.applicationContext, MockData.AMAZON_SLOT_ID)),
            // Provide extras via here if more convenient than the UI
            localExtrasProviders = emptyList(),
        )
    }
}
