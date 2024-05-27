package io.voodoo.apps.ads.api

import io.voodoo.apps.ads.model.Ad
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AdArbitrageClient(
    private val clients: List<AdClient<*>>
) {

    private val mutex = Mutex()

    fun getAdReadyToDisplay(): Ad? {
        return clients
            .mapNotNull { it.getAdReadyToDisplay() }
            .maxByOrNull { it.analyticsInfo.revenue }
    }

    suspend fun fetchAdIfNecessary(
        vararg localKeyValues: Pair<String, Any>
    ): List<Result<Ad>> {
        return mutex.withLock {
            supervisorScope {
                clients
                    .mapNotNull { client ->
                        if (client.getAdReadyToDisplay() == null) {
                            async {
                                client.fetchAd(*localKeyValues)
                            }
                        } else {
                            null
                        }
                    }
                    .map { runCatching { it.await() } }
            }
        }
    }
}
