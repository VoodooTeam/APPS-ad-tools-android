package io.voodoo.apps.ads.admob.nativ

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import io.voodoo.apps.ads.admob.exception.AdMobAdLoadException
import io.voodoo.apps.ads.admob.listener.AdMobNativeAdViewListener
import io.voodoo.apps.ads.admob.listener.MultiAdMobNativeAdViewListener
import io.voodoo.apps.ads.api.AdClient
import io.voodoo.apps.ads.api.BaseAdClient
import io.voodoo.apps.ads.api.LocalExtrasProvider
import io.voodoo.apps.ads.api.model.Ad
import kotlinx.coroutines.CompletableDeferred
import java.util.Date

private sealed interface LoadingAd {
    data class Success(val ad: NativeAd) : LoadingAd
    data class Failure(val error: LoadAdError) : LoadingAd
}

class AdMobNativeAdClient(
    config: AdClient.Config,
    private val activity: Activity,
    adViewFactory: AdMobNativeAdViewFactory,
    private val adViewRenderer: AdMobNativeAdViewRenderer,
    //private val renderListener: MaxNativeAdRenderListener? = null,
    localExtrasProviders: List<LocalExtrasProvider> = emptyList(),
) : BaseAdClient<AdmobNativeAdWrapper, Ad.Native>(config = config) {

    override val adType: Ad.Type = Ad.Type.NATIVE

    private val adMobNativeAdListener = MultiAdMobNativeAdViewListener()

    private val adViewPool = AdMobNativeAdViewPool(
        adViewFactory,
    )

    private val localExtrasProviders = localExtrasProviders.toList()

    init {
        /*
        require(appLovinSdk.isInitialized) { "AppLovin instance not initialized" }
        loader.setNativeAdListener(maxNativeAdListener)
        loader.setRevenueListener { ad ->
            val adWrapper = findOrCreateAdWrapper(ad)
            runRevenueListener { it.onAdRevenuePaid(this, adWrapper) }
        }

        maxNativeAdListener.add(object : MaxNativeAdListener() {
            override fun onNativeAdExpired(ad: MaxAd) {
                // ad expired, can't be served anymore
                checkAndNotifyAvailableAdCountChanges()
            }

            override fun onNativeAdClicked(ad: MaxAd) {
                val adWrapper = findOrCreateAdWrapper(ad)
                runClickListener { it.onAdClick(this@MaxNativeAdClient, adWrapper) }
            }
        })
         */

        (activity as? LifecycleOwner)?.lifecycle?.let(::registerToLifecycle)
        // config.placement?.let { loader.placement = it }
    }

    fun addAdMobNativeAdViewListener(listener: AdMobNativeAdViewListener) {
        adMobNativeAdListener.add(listener)
    }

    fun removeAdMobNativeAdViewListener(listener: AdMobNativeAdViewListener) {
        adMobNativeAdListener.remove(listener)
    }

    override fun close() {
        super.close()
        //loader.destroy()
    }

    override fun destroyAd(ad: AdmobNativeAdWrapper) {
        Log.w("AdClient", "destroyAd ${ad.id}")
        //loader.destroy(ad.ad)
    }

    /** see https://developers.applovin.com/en/android/ad-formats/native-ads#templates */
    override suspend fun fetchAdSafe(vararg localExtras: Pair<String, Any>): AdmobNativeAdWrapper {
        runLoadingListeners { it.onAdLoadingStarted(this) }

        var _currentAdWrapper: AdmobNativeAdWrapper? = null

        val loadingAdLocal = CompletableDeferred<LoadingAd>()

        lateinit var loader: AdLoader
        loader = AdLoader.Builder(
            activity,
            config.adUnit,
        ).forNativeAd { ad: NativeAd ->
            if (activity.isDestroyed) {
                ad.destroy()
                return@forNativeAd
            } else if (!loader.isLoading) {
                loadingAdLocal.complete(LoadingAd.Success(ad))
            }
        }.withAdListener(object : AdListener() {
            override fun onAdClicked() {
                _currentAdWrapper?.let { adWrapper ->
                    adMobNativeAdListener.onAdClicked(adWrapper.ad)
                    runClickListener { it.onAdClick(this@AdMobNativeAdClient, adWrapper) }
                }
            }

            override fun onAdClosed() {
                adMobNativeAdListener.onAdClosed(_currentAdWrapper?.ad)
            }

            override fun onAdImpression() {
                _currentAdWrapper?.let { adWrapper ->
                    adMobNativeAdListener.onAdImpression(_currentAdWrapper?.ad)
                    runRevenueListener { it.onAdRevenuePaid(this@AdMobNativeAdClient, adWrapper) }
                }
            }

            override fun onAdLoaded() {
                // will be called with deffered
            }

            override fun onAdOpened() {
                adMobNativeAdListener.onAdOpened(_currentAdWrapper?.ad)
            }

            override fun onAdSwipeGestureClicked() {
                adMobNativeAdListener.onAdSwipeGestureClicked(_currentAdWrapper?.ad)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                loadingAdLocal.complete(LoadingAd.Failure(adError))
            }
        })
            //.withNativeAdOptions(
            //    NativeAdOptions.Builder()
            //        // Methods in the NativeAdOptions.Builder class can be
            //        // used here to specify individual options settings.
            //        .build()
            //)
            .build()

        runLoadingListeners { it.onAdLoadingStarted(this@AdMobNativeAdClient) }

        loader.loadAd(
            AdRequest.Builder()
                .apply {
                    //this.setNeighboringContentUrls() TODO
                }.build()
        )

        val result = loadingAdLocal.await()

        /*

        val providersExtras = localExtrasProviders.flatMap { it.getLocalExtras() }
        val ad = withContext(Dispatchers.IO) {
            try {
                // Wrap ad loading into a coroutine
                suspendCancellableCoroutine<AdmobNativeAdWrapper> { continuation ->
                    val callback = object : MaxNativeAdListener() {
                        override fun onNativeAdLoaded(view: MaxNativeAdView?, ad: MaxAd) {
                            maxNativeAdListener.remove(this)
                            val adWrapper = AdmobNativeAdWrapper(
                                ad = ad,
                                loadedAt = Date(),
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
                runLoadingListeners { it.onAdLoadingFailed(this@MaxNativeAdClient, e) }

                // Keep reused ad instead of destroying it
                reusedAd?.let { addLoadedAd(it, isAlreadyServed = true) }

                throw e
            }
        }
         */

        //reusedAd?.let(::destroyAd)

        //if (ad.isBlocked) {
        //    runModerationListener { it.onAdBlocked(this, ad) }
        //}
        when (result) {
            is LoadingAd.Failure -> {
                _currentAdWrapper = null
                val error = AdMobAdLoadException(result.error)
                runLoadingListeners { it.onAdLoadingFailed(this@AdMobNativeAdClient, error) }
                adMobNativeAdListener.onAdFailedToLoad(result.error)
                throw error
            }

            is LoadingAd.Success -> {
                Log.i("AdMobNativeAdClient", "fetchAd success")

                val adWrapper = AdmobNativeAdWrapper(
                    ad = result.ad,
                    loadedAt = Date(),
                    viewPool = adViewPool,
                    adViewRenderer = adViewRenderer,
                )

                _currentAdWrapper = adWrapper

                runLoadingListeners { it.onAdLoadingFinished(this, adWrapper) }
                addLoadedAd(adWrapper)
                return adWrapper
            }
        }
    }
}