package io.voodoo.apps.ads.api.flow

import io.voodoo.apps.ads.api.AdClient
import io.voodoo.apps.ads.api.listener.AdListenerHolder
import io.voodoo.apps.ads.api.listener.AdLoadingListener
import io.voodoo.apps.ads.api.model.Ad
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

/**
 * @return a flow that'll emit every client's [AdClient.fetchAd] results
 */
fun AdListenerHolder.getAdFetchResultFlow(): Flow<Result<Ad>> {
    return callbackFlow<Result<Ad>> {
        val listener = object : AdLoadingListener {
            override fun onAdLoadingStarted(type: Ad.Type) {
                // no-op
            }

            override fun onAdLoadingFailed(type: Ad.Type, exception: Exception) {
                trySendBlocking(Result.failure(exception))
            }

            override fun onAdLoadingFinished(ad: Ad) {
                trySendBlocking(Result.success(ad))
            }
        }

        addAdLoadingListener(listener)

        awaitClose { removeAdLoadingListener(listener) }
    }.buffer(capacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
}
