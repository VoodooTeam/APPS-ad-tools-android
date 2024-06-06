package io.voodoo.apps.ads.applovin.listener

import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.MaxError
import java.util.concurrent.CopyOnWriteArraySet

internal class MultiMaxAdViewAdListener : MaxAdViewAdListener {

    private val delegates = CopyOnWriteArraySet<MaxAdViewAdListener>()

    fun add(listener: MaxAdViewAdListener) {
        delegates.add(listener)
    }

    fun remove(listener: MaxAdViewAdListener) {
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

    override fun onAdExpanded(ad: MaxAd) {
        delegates.forEach { it.onAdExpanded(ad) }
    }

    override fun onAdCollapsed(ad: MaxAd) {
        delegates.forEach { it.onAdCollapsed(ad) }
    }
}
