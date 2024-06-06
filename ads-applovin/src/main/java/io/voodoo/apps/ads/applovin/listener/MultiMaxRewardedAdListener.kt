package io.voodoo.apps.ads.applovin.listener

import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxReward
import com.applovin.mediation.MaxRewardedAdListener
import java.util.concurrent.CopyOnWriteArraySet

internal class MultiMaxRewardedAdListener : MaxRewardedAdListener {

    // TODO: check the implementation, we're re-creating the backing list for every request (because we add a listener)
    private val delegates = CopyOnWriteArraySet<MaxRewardedAdListener>()

    fun add(listener: MaxRewardedAdListener) {
        delegates.add(listener)
    }

    fun remove(listener: MaxRewardedAdListener) {
        delegates.remove(listener)
    }

    override fun onAdLoaded(ad: MaxAd) {
        delegates.forEach { it.onAdLoaded(ad) }
    }

    override fun onAdDisplayed(ad: MaxAd) {
        delegates.forEach { it.onAdDisplayed(ad) }
    }

    override fun onAdHidden(ad: MaxAd) {
        delegates.forEach { it.onAdHidden(ad) }
    }

    override fun onAdClicked(ad: MaxAd) {
        delegates.forEach { it.onAdClicked(ad) }
    }

    override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
        delegates.forEach { it.onAdLoadFailed(adUnitId, error) }
    }

    override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
        delegates.forEach { it.onAdDisplayFailed(ad, error) }
    }

    @Deprecated("")
    override fun onRewardedVideoStarted(ad: MaxAd) {
        delegates.forEach { it.onRewardedVideoStarted(ad) }
    }

    @Deprecated("")
    override fun onRewardedVideoCompleted(ad: MaxAd) {
        delegates.forEach { it.onRewardedVideoCompleted(ad) }
    }

    override fun onUserRewarded(ad: MaxAd, reward: MaxReward) {
        delegates.forEach { it.onUserRewarded(ad, reward) }
    }
}
