package io.voodoo.apps.ads.api

import io.voodoo.apps.ads.model.Ad
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AdArbitrageOrchestrator(
    clients: List<AdClient<*>>,
    private val requiredAvailableAdCount: Int = 1
) {

    private val clients = clients.toList()
    private val clientIndexByRequestIdMap = mutableMapOf<String, Int>()
    private val mutex = Mutex()

    fun getAd(requestId: String): Ad? {
        return synchronized(clientIndexByRequestIdMap) {
            // Re-serve previous ad even if a more profitable one is available in another client
            val previousClientIndex = clientIndexByRequestIdMap[requestId]
            val previousAdClient = previousClientIndex?.let { clients.getOrNull(it) }
            val previousAd = previousAdClient?.getPreviousAd(requestId)
            if (previousAd != null) {
                return previousAd
            }

            // Get all available ads and take most profitable
            val ads = clients.map { it to it.getAd(requestId) }
            val bestAd = ads.maxByOrNull { (_, ad) -> ad?.analyticsInfo?.revenue ?: 0.0 }

            // Release non-retained ad
            ads.forEach { (client, ad) ->
                if (ad !== bestAd) {
                    ad?.let(client::releaseAd)
                }
            }

            if (bestAd != null) {
                clientIndexByRequestIdMap[requestId] = clients.indexOf(bestAd.first)
                bestAd.second
            } else {
                null
            }
        }
    }

    suspend fun fetchAdIfNecessary(
        vararg localKeyValues: Pair<String, Any>
    ): List<Result<Ad>> {
        return mutex.withLock {
            supervisorScope {
                clients
                    .mapNotNull { client ->
                        if (client.getAvailableAdCount() < requiredAvailableAdCount) {
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
