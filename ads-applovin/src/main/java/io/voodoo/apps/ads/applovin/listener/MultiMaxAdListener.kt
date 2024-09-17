package io.voodoo.apps.ads.applovin.listener

import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import java.util.concurrent.CopyOnWriteArraySet

class MultiMaxAdListener : MaxAdListener {

    // TODO: check the implementation, we're re-creating the backing list for every request (because we add a listener)
    private val delegates = CopyOnWriteArraySet<MaxAdListener>()

    fun add(listener: MaxAdListener) {
        delegates.add(listener)
    }

    fun remove(listener: MaxAdListener) {
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
}
