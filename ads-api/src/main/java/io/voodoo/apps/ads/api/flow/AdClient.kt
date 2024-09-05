package io.voodoo.apps.ads.api.flow

import io.voodoo.apps.ads.api.AdClient
import io.voodoo.apps.ads.api.listener.AdLoadingListener
import io.voodoo.apps.ads.api.listener.OnAvailableAdCountChangedListener
import io.voodoo.apps.ads.api.model.Ad
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

fun AdClient<*>.isRequestInProgressFlow(): Flow<Boolean> {
    return callbackFlow {
        val listener = object : AdLoadingListener {
            override fun onAdLoadingStarted(adClient: AdClient<*>) {
                trySendBlocking(true)
            }

            override fun onAdLoadingFailed(adClient: AdClient<*>, exception: Exception) {
                trySendBlocking(false)
            }

            override fun onAdLoadingFinished(adClient: AdClient<*>, ad: Ad) {
                trySendBlocking(false)
            }
        }

        addAdLoadingListener(listener)
        trySendBlocking(isRequestInProgress())

        awaitClose { removeAdLoadingListener(listener) }
    }.conflate()
}

/**
 * @return a flow that'll emit the number of available ads every time it changes
 *
 * @see AdClient.getAvailableAdCount
 */
fun AdClient<*>.getAvailableAdCountFlow(): Flow<AdClient.AdCount> {
    return callbackFlow {
        val listener = OnAvailableAdCountChangedListener { _, count ->
            trySendBlocking(count)
        }

        addOnAvailableAdCountChangedListener(listener)
        trySendBlocking(getAvailableAdCount())

        awaitClose { removeOnAvailableAdCountChangedListener(listener) }
    }.conflate()
}


enum class AdClientLoadingEvent { STARTED, ERROR, SUCCESS }

/**
 * @return a flow that'll emit all [AdClientLoadingEvent] (wrapping [AdLoadingListener])
 */
fun AdClient<*>.getLoadingEvents(): Flow<AdClientLoadingEvent> {
    return callbackFlow {
        val loadingListener = object : AdLoadingListener {
            override fun onAdLoadingStarted(adClient: AdClient<*>) {
                trySendBlocking(AdClientLoadingEvent.STARTED)
            }

            override fun onAdLoadingFailed(adClient: AdClient<*>, exception: Exception) {
                trySendBlocking(AdClientLoadingEvent.ERROR)
            }

            override fun onAdLoadingFinished(adClient: AdClient<*>, ad: Ad) {
                trySendBlocking(AdClientLoadingEvent.SUCCESS)
            }
        }

        addAdLoadingListener(loadingListener)
        if (isRequestInProgress()) {
            loadingListener.onAdLoadingStarted(this@getLoadingEvents)
        }

        awaitClose {
            removeAdLoadingListener(loadingListener)
        }
    }.conflate()
}
