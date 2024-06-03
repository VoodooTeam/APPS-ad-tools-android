package io.voodoo.apps.ads.feature.ads

import android.content.Context
import android.util.Log
import com.amazon.device.ads.AdRegistration
import com.amazon.device.ads.DTBAdNetwork
import com.amazon.device.ads.DTBAdNetworkInfo
import com.amazon.device.ads.MRAIDPolicy
import com.appharbr.sdk.configuration.AHSdkConfiguration
import com.appharbr.sdk.engine.AppHarbr
import com.appharbr.sdk.engine.InitializationFailureReason
import com.appharbr.sdk.engine.listeners.OnAppHarbrInitializationCompleteListener
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinPrivacySettings
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkInitializationConfiguration
import io.voodoo.apps.ads.MockData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdsInitiliazer {

    fun init(context: Context, doNotSellDataEnabled: Boolean) {
        // Amazon config
        AdRegistration.getInstance(MockData.AMAZON_APP_KEY, context)
        AdRegistration.setAdNetworkInfo(DTBAdNetworkInfo(DTBAdNetwork.MAX))
        AdRegistration.setMRAIDSupportedVersions(arrayOf("1.0", "2.0", "3.0"))
        AdRegistration.setMRAIDPolicy(MRAIDPolicy.CUSTOM)

        val config =
            AppLovinSdkInitializationConfiguration.builder(MockData.APPLOVIN_SDK_KEY, context)
                .setMediationProvider(AppLovinMediationProvider.MAX)
                .setAdUnitIds(listOf(MockData.NATIVE_AD_UNIT, MockData.MREC_AD_UNIT))
                // .setTestDeviceAdvertisingIds(listOf("a6e26802-9759-4801-b8b6-10ca1ad1abe1"))
                .build()

        AppLovinSdk.getInstance(context).apply {
            settings.let {
                // Shake to see debug info about ads
                it.isCreativeDebuggerEnabled = true
                it.setVerboseLogging(true)
            }

            initialize(config) {
                // initModerationSdk()
                // mark ads as enabled and ready to use in your code
                // only from context point you should instantiate AdClients
                _adsEnabled.value = true
            }
        }

        // SDK shouldn't be initialized if we don't have user's consent
        AppLovinPrivacySettings.setHasUserConsent(true, context)
        AppLovinPrivacySettings.setDoNotSell(doNotSellDataEnabled, context)
        AppLovinPrivacySettings.setIsAgeRestrictedUser(false, context)
    }

    private fun initModerationSdk(context: Context) {
        val ahSdkConfiguration = AHSdkConfiguration.Builder(MockData.MODERATION_SDK_KEY)
            // .withDebugConfig(AHSdkDebug(true).withBlockAll(true))
            .build()

        AppHarbr.initialize(
            context,
            ahSdkConfiguration,
            object : OnAppHarbrInitializationCompleteListener {
                override fun onSuccess() {
                    Log.e("Limitless", "AppHarbr SDK Initialization success")
                }

                override fun onFailure(reason: InitializationFailureReason) {
                    Log.e(
                        "Limitless",
                        "AppHarbr SDK Initialization Failed: ${reason.readableHumanReason}"
                    )
                }
            }
        )
    }

    companion object {

        // In a real app this should be in a repository somewhere
        private val _adsEnabled = MutableStateFlow(false)
        val adsEnabled = _adsEnabled.asStateFlow()
    }
}
