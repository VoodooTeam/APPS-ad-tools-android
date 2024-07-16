package io.voodoo.apps.ads.applovin.rewarded

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.appharbr.sdk.engine.AdBlockReason
import com.appharbr.sdk.engine.AdResult
import com.appharbr.sdk.engine.AdSdk
import com.appharbr.sdk.engine.AdStateResult
import com.appharbr.sdk.engine.AppHarbr
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxReward
import com.applovin.mediation.MaxRewardedAdListener
import com.applovin.mediation.ads.MaxRewardedAd
import com.applovin.sdk.AppLovinSdk
import io.voodoo.apps.ads.api.AdClient
import io.voodoo.apps.ads.api.BaseAdClient
import io.voodoo.apps.ads.api.LocalExtrasProvider
import io.voodoo.apps.ads.api.model.Ad
import io.voodoo.apps.ads.applovin.exception.MaxAdLoadException
import io.voodoo.apps.ads.applovin.listener.DefaultMaxRewardedAdListener
import io.voodoo.apps.ads.applovin.listener.MultiMaxRewardedAdListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@ExperimentalStdlibApi
class MaxRewardedAdClient(
    config: AdClient.Config,
    private val activity: Activity,
    appLovinSdk: AppLovinSdk = AppLovinSdk.getInstance(activity.applicationContext),
    plugins: List<MaxRewardedAdClientPlugin> = emptyList(),
    localExtrasProviders: List<LocalExtrasProvider> = emptyList(),
) : BaseAdClient<MaxRewardedAdWrapper, Ad.Rewarded>(config = config) {

    private val type: Ad.Type = Ad.Type.REWARDED

    // Only one loader can be created at a time for a given adUnit
    private val loader = MaxRewardedAd.getInstance(config.adUnit, appLovinSdk, activity)

    private val useModeration by lazy { AppHarbr.isInitialized() }

    private val plugins = plugins.toList()
    private val localExtrasProviders = localExtrasProviders.toList()

    private val maxRewardedAdListener = MultiMaxRewardedAdListener()

    init {
        require(config.adCacheSize == 1) {
            "Invalid adCacheSize. Only one rewarded ad can be loaded at a time. adCacheSize must be == 1."
        }

        val loaderListener = if (useModeration) {
            AppHarbr.addRewardedAd<MaxRewardedAdListener>(
                AdSdk.MAX,
                this,
                null,
                maxRewardedAdListener,
                (activity as LifecycleOwner).lifecycle
            ) { _, _, _, reasons ->
                markAdAsBlocked(reasons)
            }
        } else {
            maxRewardedAdListener
        }
        loader.setListener(loaderListener)
        loader.setRevenueListener {
            // in wizz this is done in onUserRewarded, check what behavior we actually want
        }
        maxRewardedAdListener.add(object : DefaultMaxRewardedAdListener() {
            override fun onUserRewarded(ad: MaxAd, reward: MaxReward) {
                val adWrapper = findAdOrNull { it.ad === ad }
                    ?: MaxRewardedAdWrapper(
                        ad = ad,
                        loader = loader,
                        apphrbrModerationResult = null,
                        placement = config.placement
                    )

                runRevenueListener { it.onAdRevenuePaid(adWrapper) }
            }
        })

        loader.setExpirationListener { _, _ ->
            // TODO: special case to check with applovin team: the ad is auto reloading
            //   we might need to force remove from cache and add a new?
        }

        (activity as? LifecycleOwner)?.lifecycle?.let(::registerToLifecycle)
    }

    fun addMaxRewardedAdListener(listener: MaxRewardedAdListener) {
        maxRewardedAdListener.add(listener)
    }

    fun removeMaxRewardedAdListener(listener: MaxRewardedAdListener) {
        maxRewardedAdListener.remove(listener)
    }

    override fun close() {
        super.close()
        runPlugin { it.close() }
        loader.destroy()
    }

    override fun destroyAd(ad: MaxRewardedAdWrapper) {
        runPlugin { it.onDestroyAd(ad) }
        // don't destroy the unique loader
        Log.e("MaxRewardedAdClient", "destroyAd called, this should never happen")
    }

    /** see https://developers.applovin.com/en/android/ad-formats/banner-Rewarded-ads/ */
    override suspend fun fetchAdSafe(vararg localExtras: Pair<String, Any>): MaxRewardedAdWrapper {
        require(getAvailableAdCount().total == 0) { "Only one ad can be loaded at a time" }

        runLoadingListeners { it.onAdLoadingStarted(type) }

        val providersExtras = localExtrasProviders.flatMap { it.getLocalExtras() }
        val ad = withContext(Dispatchers.IO) {
            try {
                runPlugin { it.onPreLoadAd(loader) }

                // Wrap ad loading into a coroutine
                suspendCancellableCoroutine<MaxRewardedAdWrapper> { continuation ->
                    val callback = object : DefaultMaxRewardedAdListener() {
                        override fun onAdLoaded(ad: MaxAd) {
                            maxRewardedAdListener.remove(this)
                            val adWrapper = MaxRewardedAdWrapper(
                                ad = ad,
                                loader = loader,
                                apphrbrModerationResult = if (AppHarbr.isInitialized()) {
                                    loader.getRewardedAdModerationResult()
                                } else {
                                    null
                                },
                                placement = config.placement
                            )
                            try {
                                continuation.resume(adWrapper)
                            } catch (e: Exception) {
                                // Avoid crashes if callback is called multiple times
                                Log.e("MaxRewardedAdClient", "Failed to notify fetchAd", e)
                            }
                        }

                        override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                            maxRewardedAdListener.remove(this)
                            try {
                                continuation.resumeWithException(MaxAdLoadException(error))
                            } catch (e: Exception) {
                                // Avoid crashes if callback is called multiple times
                                Log.e("MaxRewardedAdClient", "Failed to notify fetchAd error", e)
                            }
                        }
                    }

                    Log.i("MaxRewardedAdClient", "fetchAd")
                    maxRewardedAdListener.add(callback)
                    providersExtras.forEach { (key, value) ->
                        loader.setLocalExtraParameter(key, value)
                    }
                    localExtras.forEach { (key, value) ->
                        loader.setLocalExtraParameter(key, value)
                    }
                    loader.loadAd()

                    continuation.invokeOnCancellation {
                        maxRewardedAdListener.remove(callback)
                    }
                }
            } catch (e: MaxAdLoadException) {
                Log.e("MaxRewardedAdClient", "Failed to load ad", e)
                runPlugin { it.onAdLoadException(loader, e.error) }
                runLoadingListeners { it.onAdLoadingFailed(type, e) }

                throw e
            }
        }

        runPlugin { it.onAdLoaded(ad = ad) }
        Log.i("MaxRewardedAdClient", "fetchAd success")
        addLoadedAd(ad)
        runLoadingListeners { it.onAdLoadingFinished(ad) }
        return ad
    }

    // TODO: the ad value could change before apphrbr listener call
    //  thus calling this listener with incorrect ad
    private fun markAdAsBlocked(reasons: Array<AdBlockReason>) {
        Log.e(
            "MaxRewardedAdClient",
            "Ad blocked: ${reasons.joinToString { it.reason }}"
        )
        val ad = findAdOrNull { true } ?: return

        // Ad was already moderated, drop event
        if (ad.apphrbrModerationResult != null) return

        val moderationResult = AdResult(AdStateResult.BLOCKED).apply {
            blockReasons.addAll(reasons)
        }
        ad.apphrbrModerationResult = moderationResult
        runModerationListener { it.onAdBlocked(ad) }
        checkAndNotifyAvailableAdCountChanges()
    }

    private inline fun runPlugin(body: (MaxRewardedAdClientPlugin) -> Unit) {
        plugins.forEach {
            // try/catch plugin to not crash if an error occurs
            try {
                body(it)
            } catch (e: Exception) {
                Log.e("MaxRewardedAdClient", "Failed to run plugin", e)
            }
        }
    }
}

private fun MaxRewardedAd.getRewardedAdModerationResult(): AdResult {
    return AppHarbr.getRewardedResult(this)
}
