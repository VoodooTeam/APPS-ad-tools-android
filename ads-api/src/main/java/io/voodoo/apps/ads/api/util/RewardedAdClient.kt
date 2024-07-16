package io.voodoo.apps.ads.api.util

import android.util.Log
import android.view.View
import io.voodoo.apps.ads.api.AdClient
import io.voodoo.apps.ads.api.listener.AdRevenueListener
import io.voodoo.apps.ads.api.model.Ad
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * render the ad and suspends until the revenue listener is called
 */
suspend fun AdClient<Ad.Rewarded>.renderAsync(view: View): Ad.Rewarded {
    val ad = getAvailableAd() ?: throw IllegalStateException("No ad available")

    suspendCancellableCoroutine { cont ->
        val listener = AdRevenueListener {
            cont.resume(Unit)
        }
        try {
            addAdRevenueListener(listener)
            ad.render(view)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("RewardedAdClient", "Failed to showAd/getReward", e)
            cont.resumeWithException(e)
        }

        cont.invokeOnCancellation { removeAdRevenueListener(listener) }
    }

    releaseAd(ad)
    return ad
}
