package io.voodoo.apps.ads.noop

import io.voodoo.apps.ads.api.AdClient
import io.voodoo.apps.ads.api.listener.AdLoadingListener
import io.voodoo.apps.ads.api.listener.AdModerationListener
import io.voodoo.apps.ads.api.listener.AdRevenueListener
import io.voodoo.apps.ads.api.model.Ad

class NoOpAdClient : AdClient<NoOpMRECAd> {

    override fun getAvailableAdCount(filterLocked: Boolean): Int = 0

    override fun getServedAd(requestId: String?): NoOpMRECAd? = null
    override fun getAvailableAd(requestId: String?): NoOpMRECAd? = null
    override fun getAnyAd(): NoOpMRECAd? = null

    override fun releaseAd(ad: Ad) {
        // no-op
    }

    override suspend fun fetchAd(vararg localExtras: Pair<String, Any>): NoOpMRECAd {
        throw NotImplementedError("no op")
    }

    override fun close() {
        // no-op
    }

    override fun addAdLoadingListener(listener: AdLoadingListener) {
        // no-op
    }

    override fun removeAdLoadingListener(listener: AdLoadingListener) {
        // no-op
    }

    override fun addAdModerationListener(listener: AdModerationListener) {
        // no-op
    }

    override fun removeAdModerationListener(listener: AdModerationListener) {
        // no-op
    }

    override fun addAdRevenueListener(listener: AdRevenueListener) {
        // no-op
    }

    override fun removeAdRevenueListener(listener: AdRevenueListener) {
        // no-op
    }
}
