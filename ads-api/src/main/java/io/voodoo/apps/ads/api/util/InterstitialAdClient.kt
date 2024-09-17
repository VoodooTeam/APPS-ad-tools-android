package io.voodoo.apps.ads.api.util

import android.util.Log
import android.view.View
import io.voodoo.apps.ads.api.AdClient
import io.voodoo.apps.ads.api.listener.AdRevenueListener
import io.voodoo.apps.ads.api.model.Ad
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * render the ad and suspends until the revenue listener is called
 */
suspend fun AdClient<Ad.Interstitial>.renderAsync(
    view: View,
    onPreRender: (Ad) -> Unit = {}
): Ad.Interstitial {
    val ad = getAvailableAd() ?: throw IllegalStateException("No ad available")
    onPreRender(ad)

    suspendCancellableCoroutine { cont ->
        val listener = AdRevenueListener { _, paidAd ->
            if (paidAd.id == ad.id) {
                cont.resume(Unit)
            }
        }
        try {
            addAdRevenueListener(listener)
            ad.render(view)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("InterstitialAdClient", "Failed to showAd/getReward", e)
            cont.resumeWithException(e)
        }

        cont.invokeOnCancellation { removeAdRevenueListener(listener) }
    }

    releaseAd(ad)
    return ad
}
