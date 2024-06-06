package io.voodoo.apps.ads.feature.ads

import androidx.activity.ComponentActivity
import io.voodoo.apps.ads.MockData
import io.voodoo.apps.ads.api.AdClient
import io.voodoo.apps.ads.api.model.Ad
import io.voodoo.apps.ads.applovin.rewarded.MaxRewardedAdClient

class RewardedAdClientFactory {

    @OptIn(ExperimentalStdlibApi::class)
    fun create(activity: ComponentActivity): AdClient<Ad.Rewarded> {
        return MaxRewardedAdClient(
            config = AdClient.Config(
                adCacheSize = 1,
                adUnit = MockData.REWARDED_AD_UNIT
            ),
            activity = activity,
            plugins = listOf(),
            // Provide extras via here if more convenient than the UI
            localExtrasProviders = emptyList(),
        )
    }
}
