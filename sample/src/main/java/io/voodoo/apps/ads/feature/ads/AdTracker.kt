package io.voodoo.apps.ads.feature.ads

import android.util.Log
import io.voodoo.apps.ads.api.listener.AdLoadingListener
import io.voodoo.apps.ads.api.listener.AdModerationListener
import io.voodoo.apps.ads.api.listener.AdRevenueListener
import io.voodoo.apps.ads.api.model.Ad

class AdTracker(
    private val nativeAdUnit: String,
    private val mrecAdUnit: String,
) : AdLoadingListener, AdRevenueListener, AdModerationListener {

    override fun onAdLoadingStarted(type: Ad.Type) {
        track(AD_LOADING_STARTED, type, null)
        startTimedEvent(AD_LOADING_FINISHED)
        startTimedEvent(AD_LOADING_FAILED)
    }

    override fun onAdLoadingFailed(type: Ad.Type, exception: Exception) {
        track(AD_LOADING_FAILED, type, null)
    }

    override fun onAdLoadingFinished(ad: Ad) {
        track(AD_LOADING_FINISHED, ad.type, ad.analyticsInfo)
    }

    override fun onAdBlocked(ad: Ad) {
        track(AD_LOADING_BLOCKED, ad.type, ad.analyticsInfo)
    }

    override fun onAdRevenuePaid(ad: Ad) {
        track(AD_WATCHED, ad.type, ad.analyticsInfo)
    }

    private fun track(eventName: String, adType: Ad.Type, analyticsInfo: Ad.AnalyticsInfo?) {
        Log.i("AdTracker", "track($eventName): (adUnit: ${adType.adUnit}")
        // TODO: track via firebase analytics, mixpanel, segment, ...
        // Additional info available in analyticsInfo when ad is loaded
    }

    private fun startTimedEvent(eventName: String) {
        Log.i("AdTracker", "startTimedEvent($eventName")
        // TODO: call `tracker.timed` on mixpanel
    }

    private val Ad.Type.adUnit: String
        get() {
            return when (this) {
                Ad.Type.NATIVE -> nativeAdUnit
                Ad.Type.MREC -> mrecAdUnit
            }
        }

    companion object {
        private const val AD_LOADING_STARTED = "Ad Loading Started"
        private const val AD_LOADING_FAILED = "Ad Loading Failed"
        private const val AD_LOADING_FINISHED = "Ad Loading Finished"
        private const val AD_LOADING_BLOCKED = "Ad Loading Finished Blocked"
        private const val AD_WATCHED = "Ad Watched"
    }
}