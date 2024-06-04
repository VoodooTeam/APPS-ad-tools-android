package io.voodoo.apps.ads.feature.ads

import android.app.Activity
import android.content.Context
import io.voodoo.apps.ads.MockData
import io.voodoo.apps.ads.api.AdArbitrageur
import io.voodoo.apps.ads.api.AdClient
import io.voodoo.apps.ads.api.model.Ad
import io.voodoo.apps.ads.applovin.mrec.MaxMRECAdClient
import io.voodoo.apps.ads.applovin.nativ.MaxNativeAdClient
import io.voodoo.apps.ads.applovin.plugin.amazon.AmazonMRECAdClientPlugin

class AdArbitrageurFactory(private val context: Context) {

    private val adTracker = AdTracker(
        nativeAdUnit = MockData.NATIVE_AD_UNIT,
        mrecAdUnit = MockData.MREC_AD_UNIT
    )

    fun create(activity: Activity): AdArbitrageur {
        return AdArbitrageur(
            clients = listOf(createNativeClient(), createMRECClient(activity))
        )
    }

    private fun createNativeClient(): AdClient<Ad.Native> {
        return MaxNativeAdClient(
            config = AdClient.Config(
                adCacheSize = 3,
                adUnit = MockData.NATIVE_AD_UNIT
            ),
            context = context,
            adViewFactory = MaxNativeAdViewFactory(),
            // Provide extras via here if more convenient than the UI
            localExtrasProvider = null,
            loadingListener = adTracker,
            revenueListener = adTracker,
            moderationListener = adTracker,
        )
    }

    private fun createMRECClient(activity: Activity): AdClient<Ad.MREC> {
        return MaxMRECAdClient(
            config = AdClient.Config(
                adCacheSize = 3,
                adUnit = MockData.MREC_AD_UNIT
            ),
            // Provide extras via here if more convenient than the UI
            localExtrasProvider = null,
            plugins = listOf(AmazonMRECAdClientPlugin(MockData.AMAZON_SLOT_ID)),
            loadingListener = adTracker,
            revenueListener = adTracker,
            moderationListener = adTracker,
        ).apply { init(activity) }
    }
}
