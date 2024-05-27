package io.voodoo.apps.ads.applovin.nativ

import android.content.Context
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
import io.voodoo.apps.ads.api.listener.AdLoadingListener
import io.voodoo.apps.ads.api.listener.AdModerationListener
import io.voodoo.apps.ads.api.listener.AdRevenueListener
import io.voodoo.apps.ads.applovin.exception.MaxAdLoadException
import io.voodoo.apps.ads.applovin.listener.MultiMaxNativeAdListener
import io.voodoo.apps.ads.model.Ad
import io.voodoo.apps.ads.model.AdConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MaxNativeAdClient(
    private val context: Context,
    private val config: AdConfig,
    private val appLovinSdk: AppLovinSdk,
    private val loadingListener: AdLoadingListener? = null,
    private val revenueListener: AdRevenueListener? = null,
    private val moderationListener: AdModerationListener? = null,
    nativeAdListener: MaxNativeAdListener? = null,
) : AdClient<Ad.Native> {

    private val type: Ad.Type = Ad.Type.NATIVE

    private val listener = MultiMaxNativeAdListener()

    private val loader = MaxNativeAdLoader(
        requireNotNull(config.nativeAdUnit) { "Attempt to init MaxNativeAdClient without nativeAdUnit" },
        appLovinSdk,
        context.applicationContext
    )
    private var ad: MaxNativeAdWrapper? = null

    init {
        nativeAdListener?.let(listener::add)
        loader.setNativeAdListener(listener)
        loader.setRevenueListener { ad ->
            val adWrapper = this@MaxNativeAdClient.ad?.takeIf { it.ad === ad }
                ?: MaxNativeAdWrapper(ad, loader)

            revenueListener?.onAdRevenuePaid(adWrapper)
        }
    }

    override fun close() {
        Timber.w("close()")
        loader.destroy()
        ad = null
    }

    override fun getAdReadyToDisplay(): Ad.Native? {
        return ad?.takeUnless { it.seen || it.isExpired || it.isBlocked }
    }

    /** see https://developers.applovin.com/en/android/ad-formats/native-ads#templates */
    override suspend fun fetchAd(vararg localKeyValues: Pair<String, Any>): MaxNativeAdWrapper {
        loadingListener?.onAdLoadingStarted(type)

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
                                Timber.e(e, "Failed to notify fetchAd")
                            }
                        }

                        override fun onNativeAdLoadFailed(adUnitId: String, error: MaxError) {
                            listener.remove(this)
                            try {
                                it.resumeWithException(MaxAdLoadException(error))
                            } catch (e: Exception) {
                                // Avoid crashes if callback is called multiple times
                                Timber.e(e, "Failed to notify fetchAd error")
                            }
                        }
                    }

                    Timber.i("fetchAd")
                    listener.add(callback)
                    localKeyValues.forEach { (key, value) ->
                        loader.setLocalExtraParameter(key, value)
                    }
                    loader.loadAd()

                    it.invokeOnCancellation {
                        listener.remove(callback)
                    }
                }
            } catch (e: MaxAdLoadException) {
                Timber.e(e, "Failed to load ad")
                loadingListener?.onAdLoadingFailed(type, e)
                throw e
            }
        }

        if (ad.isBlocked) {
            moderationListener?.onAdBlocked(ad)
        }

        loadingListener?.onAdLoadingFinished(ad)
        this.ad = ad
        return ad
    }
}

private fun MaxAd.getNativeAdModerationResult(): AdResult {
    return AppHarbr.shouldBlockNativeAd(AdSdk.MAX, this, null, adUnitId)
}
