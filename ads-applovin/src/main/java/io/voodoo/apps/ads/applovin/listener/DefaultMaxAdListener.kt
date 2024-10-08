package io.voodoo.apps.ads.applovin.listener

import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError

open class DefaultMaxAdListener : MaxAdListener {

    override fun onAdLoaded(ad: MaxAd) {
    }

    override fun onAdDisplayed(ad: MaxAd) {
    }

    override fun onAdHidden(ad: MaxAd) {
    }

    override fun onAdClicked(ad: MaxAd) {
    }

    override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
    }

    override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
    }
}
