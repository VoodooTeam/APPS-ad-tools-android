package io.voodoo.apps.ads.applovin.nativ

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LifecycleOwner
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
    private val activity: Activity,
    appLovinSdk: AppLovinSdk = AppLovinSdk.getInstance(activity.applicationContext),
    adViewFactory: MaxNativeAdViewFactory,
    private val renderListener: MaxNativeAdRenderListener? = null,
    localExtrasProviders: List<LocalExtrasProvider> = emptyList(),
) : BaseAdClient<MaxNativeAdWrapper, Ad.Native>(config = config) {

    private val type: Ad.Type = Ad.Type.NATIVE

    private val maxNativeAdListener = MultiMaxNativeAdListener()

    private val loader = MaxNativeAdLoader(
        config.adUnit,
        appLovinSdk,
        activity.applicationContext
    )
    private val adViewPool = MaxNativeAdViewPool(adViewFactory)

    private val localExtrasProviders = localExtrasProviders.toList()

    init {
        require(appLovinSdk.isInitialized) { "AppLovin instance not initialized" }
        loader.setNativeAdListener(maxNativeAdListener)
        loader.setRevenueListener { ad ->
            val adWrapper = findAdOrNull { it.ad === ad }
                ?: MaxNativeAdWrapper(ad, loader, null, adViewPool)

            runRevenueListener { it.onAdRevenuePaid(adWrapper) }
        }

        maxNativeAdListener.add(object : MaxNativeAdListener() {
            override fun onNativeAdExpired(ad: MaxAd) {
                // ad expired, can't be served anymore
                checkAndNotifyAvailableAdCountChanges()
            }
        })

        (activity as? LifecycleOwner)?.lifecycle?.let(::registerToLifecycle)
    }

    fun addMaxNativeAdListener(listener: MaxNativeAdListener) {
        maxNativeAdListener.add(listener)
    }

    fun removeMaxNativeAdListener(listener: MaxNativeAdListener) {
        maxNativeAdListener.remove(listener)
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
        runLoadingListeners { it.onAdLoadingStarted(type) }

        val reusedAd = getReusableAd()

        val providersExtras = localExtrasProviders.flatMap { it.getLocalExtras() }
        val ad = withContext(Dispatchers.IO) {
            try {
                // Wrap ad loading into a coroutine
                suspendCancellableCoroutine<MaxNativeAdWrapper> { continuation ->
                    val callback = object : MaxNativeAdListener() {
                        override fun onNativeAdLoaded(view: MaxNativeAdView?, ad: MaxAd) {
                            maxNativeAdListener.remove(this)
                            val adWrapper = MaxNativeAdWrapper(
                                ad = ad,
                                loader = loader,
                                renderListener = renderListener,
                                viewPool = adViewPool,
                                apphrbrModerationResult = if (AppHarbr.isInitialized()) {
                                    ad.getNativeAdModerationResult()
                                } else {
                                    null
                                }
                            )
                            try {
                                continuation.resume(adWrapper)
                            } catch (e: Exception) {
                                // Avoid crashes if callback is called multiple times
                                Log.e("MaxNativeAdClient", "Failed to notify fetchAd", e)
                            }
                        }

                        override fun onNativeAdLoadFailed(adUnitId: String, error: MaxError) {
                            maxNativeAdListener.remove(this)
                            try {
                                continuation.resumeWithException(MaxAdLoadException(error))
                            } catch (e: Exception) {
                                // Avoid crashes if callback is called multiple times
                                Log.e("MaxNativeAdClient", "Failed to notify fetchAd error", e)
                            }
                        }
                    }

                    Log.i("MaxNativeAdClient", "fetchAd")
                    maxNativeAdListener.add(callback)
                    providersExtras.forEach { (key, value) ->
                        loader.setLocalExtraParameter(key, value)
                    }
                    localExtras.forEach { (key, value) ->
                        loader.setLocalExtraParameter(key, value)
                    }
                    loader.loadAd()

                    continuation.invokeOnCancellation {
                        maxNativeAdListener.remove(callback)
                    }
                }
            } catch (e: MaxAdLoadException) {
                Log.e("MaxNativeAdClient", "Failed to load ad", e)
                runLoadingListeners { it.onAdLoadingFailed(type, e) }

                // Keep reused ad instead of destroying it
                reusedAd?.let { addLoadedAd(it, isAlreadyServed = true) }

                throw e
            }
        }

        if (ad.isBlocked) {
            runModerationListener { it.onAdBlocked(ad) }
        }

        Log.i("MaxNativeAdClient", "fetchAd success")
        addLoadedAd(ad)
        runLoadingListeners { it.onAdLoadingFinished(ad) }
        return ad
    }
}

private fun MaxAd.getNativeAdModerationResult(): AdResult {
    return AppHarbr.shouldBlockNativeAd(AdSdk.MAX, this, null, adUnitId)
}
