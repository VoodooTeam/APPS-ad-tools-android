package io.voodoo.apps.ads.compose.util

import io.voodoo.apps.ads.api.AdClient
import io.voodoo.apps.ads.api.listener.AdLoadingListener
import io.voodoo.apps.ads.api.listener.OnAvailableAdCountChangedListener
import io.voodoo.apps.ads.api.model.Ad
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

fun AdClient<*>.isRequestInProgressFlow(): Flow<Boolean> {
    return callbackFlow {
        val listener = object : AdLoadingListener {
            override fun onAdLoadingStarted(type: Ad.Type) {
                trySendBlocking(true)
            }

            override fun onAdLoadingFailed(type: Ad.Type, exception: Exception) {
                trySendBlocking(false)
            }

            override fun onAdLoadingFinished(ad: Ad) {
                trySendBlocking(false)
            }
        }

        addAdLoadingListener(listener)
        trySendBlocking(isRequestInProgress())

        awaitClose { removeAdLoadingListener(listener) }
    }.buffer(capacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
}

/**
 * @return a flow that'll emit the number of available ads every time it changes
 *
 * @see AdClient.getAvailableAdCount
 */
fun AdClient<*>.getAvailableAdCountFlow(): Flow<AdClient.AdCount> {
    return callbackFlow {
        val listener = OnAvailableAdCountChangedListener(::trySendBlocking)

        addOnAvailableAdCountChangedListener(listener)
        trySendBlocking(getAvailableAdCount())

        awaitClose { removeOnAvailableAdCountChangedListener(listener) }
    }.buffer(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        .distinctUntilChanged()
}
