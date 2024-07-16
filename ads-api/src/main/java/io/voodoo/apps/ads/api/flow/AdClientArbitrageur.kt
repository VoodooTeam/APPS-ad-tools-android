package io.voodoo.apps.ads.api.flow

import io.voodoo.apps.ads.api.AdClient
import io.voodoo.apps.ads.api.AdClientArbitrageur
import io.voodoo.apps.ads.api.listener.OnAvailableAdCountChangedListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/**
 * @return a flow that'll emit the number of available ads every time it changes
 *
 * @see AdClient.getAvailableAdCount
 */
fun AdClientArbitrageur.getAvailableAdCountFlow(): Flow<AdClient.AdCount> {
    return callbackFlow {
        val listener = OnAvailableAdCountChangedListener(::trySendBlocking)

        addOnAvailableAdCountChangedListener(listener)
        trySendBlocking(getAvailableAdCount())

        awaitClose { removeOnAvailableAdCountChangedListener(listener) }
    }.conflate()
}
