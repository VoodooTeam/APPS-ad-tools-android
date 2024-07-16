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
    }.conflate()
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
    }.conflate()
}


sealed interface AdClientStatus {
    data object Loading : AdClientStatus
    data object Error : AdClientStatus
    data object Ready : AdClientStatus
}

/**
 * @return a flow that'll emit the [AdClientStatus]
 *
 * Note: this implementation is based on the assumption that the client is
 * configured with [AdClient.Config.adCacheSize] == 1.
 *
 * Note: A failure will always emit [io.voodoo.apps.ads.compose.util.AdClientStatus.Error] even if an ad is available.
 */
fun <T : Ad> AdClient<T>.getStatusFlow(): Flow<AdClientStatus> {
    return callbackFlow {
        val loadingListener = object : AdLoadingListener {
            override fun onAdLoadingStarted(type: Ad.Type) {
                trySendBlocking(AdClientStatus.Loading)
            }

            override fun onAdLoadingFailed(type: Ad.Type, exception: Exception) {
                trySendBlocking(AdClientStatus.Error)
            }

            override fun onAdLoadingFinished(ad: Ad) {
                trySendBlocking(AdClientStatus.Ready)
            }
        }
        val adCountListener = OnAvailableAdCountChangedListener {
            if (it.total == 0) {
                trySendBlocking(AdClientStatus.Loading)
            }
        }

        addAdLoadingListener(loadingListener)
        addOnAvailableAdCountChangedListener(adCountListener)

        when {
            getAvailableAdCount().total > 0 -> AdClientStatus.Ready
            isRequestInProgress() -> AdClientStatus.Loading
            else -> AdClientStatus.Error
        }.let(::trySendBlocking)

        awaitClose {
            removeAdLoadingListener(loadingListener)
            removeOnAvailableAdCountChangedListener(adCountListener)
        }
    }.conflate()
}
