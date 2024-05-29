package io.voodoo.apps.ads.feature.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import io.voodoo.apps.ads.MockData
import io.voodoo.apps.ads.api.AdArbitrageur
import io.voodoo.apps.ads.api.AdClient
import io.voodoo.apps.ads.api.listener.AdLoadingListener
import io.voodoo.apps.ads.api.listener.AdRevenueListener
import io.voodoo.apps.ads.applovin.mrec.MaxMRECAdClient
import io.voodoo.apps.ads.applovin.nativ.MaxNativeAdClient
import io.voodoo.apps.ads.model.Ad

class AdArbitrageurFactory(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())

    fun create(activity: Activity): AdArbitrageur {
        return AdArbitrageur(
            clients = listOf(createNativeClient(), createMRECClient(activity))
        )
    }

    private fun createNativeClient(): AdClient<Ad.Native> {
        return MaxNativeAdClient(
            config = AdClient.Config(
                servedAdsBufferSize = 3,
                adUnit = MockData.NATIVE_AD_UNIT
            ),
            context = context,
            loadingListener = object : AdLoadingListener {
                override fun onAdLoadingStarted(type: Ad.Type) {
                    // no-op
                }

                override fun onAdLoadingFailed(type: Ad.Type, exception: Exception) {
                    showInfo("NATIVE load failed")
                }

                override fun onAdLoadingFinished(ad: Ad) {
                    showInfo("NATIVE load success")
                }
            },
            revenueListener = object : AdRevenueListener {
                override fun onAdRevenuePaid(ad: Ad) {
                    Log.e("Limitless", "NATIVE ad paid")
                }
            }
        )
    }

    private fun createMRECClient(activity: Activity): AdClient<Ad.MREC> {
        return MaxMRECAdClient(
            config = AdClient.Config(
                servedAdsBufferSize = 3,
                adUnit = MockData.MREC_AD_UNIT
            ),
            // plugins = listOf(AmazonMRECAdClientPlugin(MockData.AMAZON_SLOT_ID)),
            loadingListener = object : AdLoadingListener {
                override fun onAdLoadingStarted(type: Ad.Type) {
                    // no-op
                }

                override fun onAdLoadingFailed(type: Ad.Type, exception: Exception) {
                    showInfo("MREC load failed")
                }

                override fun onAdLoadingFinished(ad: Ad) {
                    showInfo("MREC load success")
                }
            },
            revenueListener = object : AdRevenueListener {
                override fun onAdRevenuePaid(ad: Ad) {
                    Log.e("Limitless", "MREC ad paid")
                }
            }
        ).apply { init(activity) }
    }

    private fun showInfo(text: String) {
        handler.post { Toast.makeText(context, text, Toast.LENGTH_SHORT).show() }
    }
}
