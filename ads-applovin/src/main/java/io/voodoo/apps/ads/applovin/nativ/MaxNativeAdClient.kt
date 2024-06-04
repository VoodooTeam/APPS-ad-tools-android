package io.voodoo.apps.ads.applovin.nativ

import android.content.Context
import android.util.Log
import com.appharbr.sdk.engine.AdResult
import com.appharbr.sdk.engine.AdSdk
import com.appharbr.sdk.engine.AppHarbr
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxError
import com.applovin.mediation.nativeAds.MaxNativeAdListener
import com.applovin.mediation.nativeAds.MaxNativeAdLoader
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.applovin.sdk.AppLovinSdk
import io.voodoo.apps.ads.api.AdClient
import io.voodoo.apps.ads.api.BaseAdClient
import io.voodoo.apps.ads.api.LocalExtrasProvider
import io.voodoo.apps.ads.api.listener.AdLoadingListener
import io.voodoo.apps.ads.api.listener.AdModerationListener
import io.voodoo.apps.ads.api.listener.AdRevenueListener
import io.voodoo.apps.ads.api.model.Ad
import io.voodoo.apps.ads.applovin.exception.MaxAdLoadException
import io.voodoo.apps.ads.applovin.listener.MultiMaxNativeAdListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MaxNativeAdClient(
    config: AdClient.Config,
    private val context: Context,
    appLovinSdk: AppLovinSdk = AppLovinSdk.getInstance(context),
    adViewFactory: MaxNativeAdViewFactory,
    private val localExtrasProvider: LocalExtrasProvider? = null,
    private val loadingListener: AdLoadingListener? = null,
    private val revenueListener: AdRevenueListener? = null,
    private val moderationListener: AdModerationListener? = null,
    nativeAdListener: MaxNativeAdListener? = null,
) : BaseAdClient<MaxNativeAdWrapper, Ad.Native>(config = config) {

    private val type: Ad.Type = Ad.Type.NATIVE

    private val listener = MultiMaxNativeAdListener()

    private val loader = MaxNativeAdLoader(
        config.adUnit,
        appLovinSdk,
        context.applicationContext
    )
    private val adViewPool = MaxNativeAdViewPool(adViewFactory)

    init {
        require(appLovinSdk.isInitialized) { "AppLovin instance not initialized" }
        nativeAdListener?.let(listener::add)
        loader.setNativeAdListener(listener)
        loader.setRevenueListener { ad ->
            val adWrapper = findAdOrNull { it.ad === ad }
                ?: MaxNativeAdWrapper(ad, loader, adViewPool)

            revenueListener?.onAdRevenuePaid(adWrapper)
        }
    }

    override fun close() {
        super.close()
        loader.destroy()
    }

    override fun destroyAd(ad: MaxNativeAdWrapper) {
        loader.destroy(ad.ad)
    }

    /** see https://developers.applovin.com/en/android/ad-formats/native-ads#templates */
    override suspend fun fetchAd(vararg localExtras: Pair<String, Any>): MaxNativeAdWrapper {
        loadingListener?.onAdLoadingStarted(type)

        val reusedAd = getReusableAd()

        val ad = withContext(Dispatchers.IO) {
            try {
                // Wrap ad loading into a coroutine
                suspendCancellableCoroutine<MaxNativeAdWrapper> {
                    val callback = object : MaxNativeAdListener() {
                        override fun onNativeAdLoaded(view: MaxNativeAdView?, ad: MaxAd) {
                            listener.remove(this)
                            val adWrapper = MaxNativeAdWrapper(
                                ad = ad,
                                loader = loader,
                                viewPool = adViewPool,
                                moderationResult = if (AppHarbr.isInitialized()) {
                                    ad.getNativeAdModerationResult()
                                } else {
                                    null
                                }
                            )
                            try {
                                it.resume(adWrapper)
                            } catch (e: Exception) {
                                // Avoid crashes if callback is called multiple times
                                Log.e("MaxNativeAdClient", "Failed to notify fetchAd", e)
                            }
                        }

                        override fun onNativeAdLoadFailed(adUnitId: String, error: MaxError) {
                            listener.remove(this)
                            try {
                                it.resumeWithException(MaxAdLoadException(error))
                            } catch (e: Exception) {
                                // Avoid crashes if callback is called multiple times
                                Log.e("MaxNativeAdClient", "Failed to notify fetchAd error", e)
                            }
                        }
                    }

                    Log.i("MaxNativeAdClient", "fetchAd")
                    listener.add(callback)
                    localExtrasProvider?.getLocalExtras()?.forEach { (key, value) ->
                        loader.setLocalExtraParameter(key, value)
                    }
                    localExtras.forEach { (key, value) ->
                        loader.setLocalExtraParameter(key, value)
                    }
                    loader.loadAd()

                    it.invokeOnCancellation {
                        listener.remove(callback)
                    }
                }
            } catch (e: MaxAdLoadException) {
                Log.e("MaxNativeAdClient", "Failed to load ad", e)
                loadingListener?.onAdLoadingFailed(type, e)

                // Keep reused ad instead of destroying it
                reusedAd?.let { addLoadedAd(it, isAlreadyServed = true) }

                throw e
            }
        }

        if (ad.isBlocked) {
            moderationListener?.onAdBlocked(ad)
        }

        Log.i("MaxNativeAdClient", "fetchAd success")
        loadingListener?.onAdLoadingFinished(ad)
        addLoadedAd(ad)
        return ad
    }
}

private fun MaxAd.getNativeAdModerationResult(): AdResult {
    return AppHarbr.shouldBlockNativeAd(AdSdk.MAX, this, null, adUnitId)
}
