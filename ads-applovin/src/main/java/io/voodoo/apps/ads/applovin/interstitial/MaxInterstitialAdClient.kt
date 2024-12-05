package io.voodoo.apps.ads.applovin.interstitial

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.appharbr.sdk.engine.AdBlockReason
import com.appharbr.sdk.engine.AdResult
import com.appharbr.sdk.engine.AdSdk
import com.appharbr.sdk.engine.AdStateResult
import com.appharbr.sdk.engine.AppHarbr
import com.applovin.impl.mediation.MaxErrorImpl
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxInterstitialAd
import io.voodoo.apps.ads.api.AdClient
import io.voodoo.apps.ads.api.BaseAdClient
import io.voodoo.apps.ads.api.LocalExtrasProvider
import io.voodoo.apps.ads.api.model.Ad
import io.voodoo.apps.ads.applovin.exception.MaxAdLoadException
import io.voodoo.apps.ads.applovin.listener.DefaultMaxAdListener
import io.voodoo.apps.ads.applovin.listener.MultiMaxAdListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.seconds

@ExperimentalStdlibApi
class MaxInterstitialAdClient(
    config: AdClient.Config,
    activity: Activity,
    plugins: List<MaxInterstitialAdClientPlugin> = emptyList(),
    localExtrasProviders: List<LocalExtrasProvider> = emptyList(),
) : BaseAdClient<MaxInterstitialAdWrapper, Ad.Interstitial>(config = config) {

    override val adType: Ad.Type = Ad.Type.INTERSTITIAL

    private val loader = MaxInterstitialAd(config.adUnit, activity.applicationContext)

    private val useModeration by lazy { AppHarbr.isInitialized() }

    private val plugins = plugins.toList()
    private val localExtrasProviders = localExtrasProviders.toList()

    private val maxAdListener = MultiMaxAdListener()
    private var isShowing = false

    init {
        require(config.adCacheSize == 1) {
            "Invalid adCacheSize. Only one rewarded ad can be loaded at a time. adCacheSize must be 1."
        }

        val loaderListener = if (useModeration) {
            AppHarbr.addInterstitial<MaxAdListener>(
                AdSdk.MAX,
                loader,
                null,
                maxAdListener,
                (activity as LifecycleOwner).lifecycle
            ) { infos ->
                markAdAsBlocked(infos?.blockReasons.orEmpty())
            }
        } else {
            maxAdListener
        }
        loader.setListener(loaderListener)
        loader.setRevenueListener { ad ->
            // in wizz this is done in onAdHidden, check what behavior we actually want
            // this listener is called when the video start playing whereas onAdHidden is called
            // after the video is closed (after being watched completely)
        }
        maxAdListener.add(object : DefaultMaxAdListener() {

            override fun onAdDisplayed(ad: MaxAd) {
                isShowing = true
            }

            override fun onAdHidden(ad: MaxAd) {
                val adWrapper = findOrCreateAdWrapper(ad)
                adWrapper.markAsPaidInternal()
                runRevenueListener {
                    it.onAdRevenuePaid(this@MaxInterstitialAdClient, adWrapper)
                }

                isShowing = false
                releaseAd(findAdOrNull { true } ?: return)
            }

            override fun onAdClicked(ad: MaxAd) {
                val adWrapper = findOrCreateAdWrapper(ad)
                runClickListener { it.onAdClick(this@MaxInterstitialAdClient, adWrapper) }
            }
        })

        loader.setExpirationListener { _, _ ->
            // TODO: special case to check with applovin team: the ad is auto reloading
            //   we might need to force remove from cache and add a new?
        }

        (activity as? LifecycleOwner)?.lifecycle?.let(::registerToLifecycle)
    }

    fun addMaxAdListener(listener: MaxAdListener) {
        maxAdListener.add(listener)
    }

    fun removeMaxAdListener(listener: MaxAdListener) {
        maxAdListener.remove(listener)
    }

    override fun close() {
        super.close()
        runPlugin { it.close() }
        loader.destroy()
    }

    override fun destroyAd(ad: MaxInterstitialAdWrapper) {
        runPlugin { it.onDestroyAd(ad) }
        // don't destroy the unique loader
    }

    override fun releaseAd(ad: Ad) {
        if (!isShowing) {
            super.releaseAd(ad)
        }
    }

    /** see https://developers.applovin.com/en/android/ad-formats/banner-Rewarded-ads/ */
    override suspend fun fetchAdSafe(
        vararg localExtras: Pair<String, Any>
    ): MaxInterstitialAdWrapper {
        require(getAvailableAdCount().total == 0) { "Only one ad can be loaded at a time" }

        // When an add is already showing, no listener is called but the request will fail...
        // to fix the race condition when releasing ad with client#renderAsync, use this hack
        withTimeoutOrNull(5.seconds) {
            while (isShowing) {
                delay(1.seconds)
            }
        } ?: throw MaxAdLoadException(
            MaxErrorImpl(
                -27,
                "Can not load another ad while the ad is showing"
            )
        )

        // Remove any previous ad from pool (but don't call destroyAd, applovin handles it itself)
        getReusableAd()

        runLoadingListeners { it.onAdLoadingStarted(this) }

        val providersExtras = localExtrasProviders.flatMap { it.getLocalExtras() }
        val ad = withContext(Dispatchers.IO) {
            try {
                runPlugin { it.onPreLoadAd(loader) }

                // Wrap ad loading into a coroutine
                suspendCancellableCoroutine<MaxInterstitialAdWrapper> { continuation ->
                    val callback = object : DefaultMaxAdListener() {
                        override fun onAdLoaded(ad: MaxAd) {
                            maxAdListener.remove(this)
                            val adWrapper = MaxInterstitialAdWrapper(
                                ad = ad,
                                loader = loader,
                                apphrbrModerationResult = if (AppHarbr.isInitialized()) {
                                    loader.getInterstitialAdModerationResult()
                                } else {
                                    null
                                },
                                placement = config.placement,
                                loadedAt = Date(),
                            )
                            try {
                                continuation.resume(adWrapper)
                            } catch (e: Exception) {
                                // Avoid crashes if callback is called multiple times
                                Log.e("MaxInterstitialAdClient", "Failed to notify fetchAd", e)
                            }
                        }

                        override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                            maxAdListener.remove(this)
                            try {
                                continuation.resumeWithException(MaxAdLoadException(error))
                            } catch (e: Exception) {
                                // Avoid crashes if callback is called multiple times
                                Log.e(
                                    "MaxInterstitialAdClient",
                                    "Failed to notify fetchAd error",
                                    e
                                )
                            }
                        }
                    }

                    Log.i("MaxInterstitialAdClient", "fetchAd")
                    maxAdListener.add(callback)
                    providersExtras.forEach { (key, value) ->
                        loader.setLocalExtraParameter(key, value)
                    }
                    localExtras.forEach { (key, value) ->
                        loader.setLocalExtraParameter(key, value)
                    }
                    loader.loadAd()

                    continuation.invokeOnCancellation {
                        maxAdListener.remove(callback)
                    }
                }
            } catch (e: MaxAdLoadException) {
                Log.e("MaxInterstitialAdClient", "Failed to load ad", e)
                runPlugin { it.onAdLoadException(loader, e.error) }
                runLoadingListeners { it.onAdLoadingFailed(this@MaxInterstitialAdClient, e) }

                throw e
            }
        }

        runPlugin { it.onAdLoaded(ad = ad) }
        Log.i("MaxInterstitialAdClient", "fetchAd success")
        addLoadedAd(ad)
        runLoadingListeners { it.onAdLoadingFinished(this, ad) }
        return ad
    }

    // TODO: the ad value could change before apphrbr listener call
    //  thus calling this listener with incorrect ad
    private fun markAdAsBlocked(reasons: Array<out AdBlockReason>) {
        Log.e(
            "MaxInterstitialAdClient",
            "Ad blocked: ${reasons.joinToString { it.reason }}"
        )
        val ad = findAdOrNull { true } ?: return

        // Ad was already moderated, drop event
        if (ad.apphrbrModerationResult != null) return

        val moderationResult = AdResult(AdStateResult.BLOCKED).apply {
            blockReasons.addAll(reasons)
        }
        ad.apphrbrModerationResult = moderationResult
        runModerationListener { it.onAdBlocked(this, ad) }
        checkAndNotifyAvailableAdCountChanges()
    }

    private inline fun runPlugin(body: (MaxInterstitialAdClientPlugin) -> Unit) {
        plugins.forEach {
            // try/catch plugin to not crash if an error occurs
            try {
                body(it)
            } catch (e: Exception) {
                Log.e("MaxInterstitialAdClient", "Failed to run plugin", e)
            }
        }
    }

    private fun findOrCreateAdWrapper(ad: MaxAd): MaxInterstitialAdWrapper {
        return findAdOrNull { it.ad === ad }
            ?: MaxInterstitialAdWrapper(
                ad = ad,
                loader = loader,
                apphrbrModerationResult = null,
                placement = config.placement,
                loadedAt = Date(),
            )
    }
}

private fun MaxInterstitialAd.getInterstitialAdModerationResult(): AdResult {
    return AppHarbr.getInterstitialResult(this)
}
