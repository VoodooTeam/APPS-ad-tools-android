package io.voodoo.apps.ads.applovin.mrec

import android.app.Activity
import android.graphics.Color
import android.util.Log
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.appharbr.sdk.engine.AdBlockReason
import com.appharbr.sdk.engine.AdResult
import com.appharbr.sdk.engine.AdSdk
import com.appharbr.sdk.engine.AdStateResult
import com.appharbr.sdk.engine.AppHarbr
import com.appharbr.sdk.engine.listeners.AHListener
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAdView
import com.applovin.sdk.AppLovinSdkUtils
import io.voodoo.apps.ads.api.AdClient
import io.voodoo.apps.ads.api.BaseAdClient
import io.voodoo.apps.ads.api.LocalExtrasProvider
import io.voodoo.apps.ads.api.listener.AdLoadingListener
import io.voodoo.apps.ads.api.listener.AdModerationListener
import io.voodoo.apps.ads.api.listener.AdRevenueListener
import io.voodoo.apps.ads.api.model.Ad
import io.voodoo.apps.ads.api.mrec.MRECAdClientPlugin
import io.voodoo.apps.ads.applovin.exception.MaxAdLoadException
import io.voodoo.apps.ads.applovin.listener.DefaultMaxAdViewAdListener
import io.voodoo.apps.ads.applovin.listener.MultiMaxAdViewAdListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MaxMRECAdClient(
    config: AdClient.Config,
    private val plugins: List<MRECAdClientPlugin> = emptyList(),
    private val localExtrasProvider: LocalExtrasProvider? = null,
    private val loadingListener: AdLoadingListener? = null,
    private val revenueListener: AdRevenueListener? = null,
    private val moderationListener: AdModerationListener? = null,
    adViewListener: MaxAdViewAdListener? = null,
) : BaseAdClient<MaxMRECAdWrapper, Ad.MREC>(config = config) {

    private val type: Ad.Type = Ad.Type.MREC

    private val useModeration by lazy { AppHarbr.isInitialized() }
    private val appharbrListener: AHListener

    private val listener = MultiMaxAdViewAdListener()

    private var activity: Activity? = null

    init {
        adViewListener?.let(listener::add)
        appharbrListener = AHListener { view, _, _, reasons ->
            markAdAsBlocked(view as MaxAdView, reasons)
        }
    }

    fun init(activity: Activity) {
        this.activity = activity
    }

    override fun close() {
        super.close()
        activity = null
    }

    override fun destroyAd(ad: MaxMRECAdWrapper) {
        if (useModeration) {
            AppHarbr.removeBannerView(ad.view)
        }
        ad.view.destroy()
    }

    /** see https://developers.applovin.com/en/android/ad-formats/banner-mrec-ads/ */
    override suspend fun fetchAd(vararg localExtras: Pair<String, Any>): MaxMRECAdWrapper {
        val context = activity
        require(context != null) { "client was not initialized (missing init(activity) call?)" }
        loadingListener?.onAdLoadingStarted(type)

        val view = getOrCreateView(context).apply {
            // see https://developers.applovin.com/en/android/ad-formats/banner-mrec-ads#stopping-and-starting-auto-refresh
            setExtraParameter("allow_pause_auto_refresh_immediately", "true")
            stopAutoRefresh()
        }

        val ad = withContext(Dispatchers.IO) {
            try {
                plugins.forEach { it.onPreLoadAd(view) }

                // Wrap ad loading into a coroutine
                suspendCancellableCoroutine<MaxMRECAdWrapper> {
                    val callback = object : DefaultMaxAdViewAdListener() {
                        override fun onAdLoaded(ad: MaxAd) {
                            listener.remove(this)
                            val adWrapper = MaxMRECAdWrapper(ad = ad, view = view)
                            try {
                                it.resume(adWrapper)
                            } catch (e: Exception) {
                                // Avoid crashes if callback is called multiple times
                                Log.e("MaxMRECAdClient", "Failed to notify fetchAd", e)
                            }
                        }

                        override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                            listener.remove(this)
                            try {
                                it.resumeWithException(MaxAdLoadException(error))
                            } catch (e: Exception) {
                                // Avoid crashes if callback is called multiple times
                                Log.e("MaxMRECAdClient", "Failed to notify fetchAd error", e)
                            }
                        }
                    }

                    Log.i("MaxMRECAdClient", "fetchAd")
                    listener.add(callback)
                    localExtrasProvider?.getLocalExtras()?.forEach { (key, value) ->
                        view.setLocalExtraParameter(key, value)
                    }
                    localExtras.forEach { (key, value) ->
                        view.setLocalExtraParameter(key, value)
                    }
                    view.loadAd()

                    it.invokeOnCancellation {
                        listener.remove(callback)
                    }
                }
            } catch (e: MaxAdLoadException) {
                Log.e("MaxMRECAdClient", "Failed to load ad", e)
                plugins.forEach { it.onAdLoadException(view, e) }
                loadingListener?.onAdLoadingFailed(type, e)
                throw e
            }
        }

        plugins.forEach { it.onAdLoaded(view, ad) }
        Log.i("MaxMRECAdClient", "fetchAd success")
        loadingListener?.onAdLoadingFinished(ad)
        addLoadedAd(ad)
        return ad
    }

    private suspend fun getOrCreateView(activity: Activity): MaxAdView {
        return withContext(Dispatchers.Main.immediate) {
            val previousView = getReusableAd()?.view
            if (previousView != null) return@withContext previousView

            MaxAdView(config.adUnit, MaxAdFormat.MREC, activity).apply {
                val widthPx = AppLovinSdkUtils.dpToPx(activity, 300)
                val heightPx = AppLovinSdkUtils.dpToPx(activity, 250)
                layoutParams = ViewGroup.LayoutParams(widthPx, heightPx)

                setBackgroundColor(Color.TRANSPARENT)
                setListener(listener)
                setRevenueListener { ad ->
                    val adWrapper = findAdOrNull { it.ad === ad }
                        ?: MaxMRECAdWrapper(ad, this)

                    revenueListener?.onAdRevenuePaid(adWrapper)
                }

                if (useModeration) {
                    AppHarbr.addBannerView(
                        AdSdk.MAX,
                        this,
                        (activity as LifecycleOwner).lifecycle,
                        appharbrListener
                    )
                }
            }
        }
    }

    // TODO: the ad value could change before apphrbr listener call
    //  thus calling this listener with incorrect ad
    private fun markAdAsBlocked(view: MaxAdView, reasons: Array<AdBlockReason>) {
        val ad = findAdOrNull { it.view === view } ?: return

        // Ad was already moderated, drop event
        if (ad.moderationResult != null) return

        val moderationResult = AdResult(AdStateResult.BLOCKED).apply {
            blockReasons.addAll(reasons)
        }
        ad.updateModerationResult(moderationResult)
        moderationListener?.onAdBlocked(ad)
    }
}
