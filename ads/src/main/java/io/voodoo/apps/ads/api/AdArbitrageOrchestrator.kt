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
    private val clientIndexByAdIdMap = mutableMapOf<Ad.Id, Int>()
    private val mutex = Mutex()

    fun getAd(requestId: String): Ad? {
        return synchronized(clientIndexByRequestIdMap) {
            // Re-serve previous ad even if a more profitable one is available in another client
            val previousClientIndex = clientIndexByRequestIdMap[requestId]
            val previousAdClient = previousClientIndex?.let { clients.getOrNull(it) }
            val previousAd = previousAdClient?.getServedAd(requestId)
            if (previousAd != null) {
                return previousAd
            }

            // Get all available ads and take most profitable
            val ads = clients.mapNotNull { client ->
                client to (client.getAvailableAd(requestId) ?: return@mapNotNull null)
            }
            val bestAd = ads.maxByOrNull { (_, ad) -> ad.analyticsInfo.revenue }

            // Release non-retained ad
            ads.forEach { (client, ad) ->
                if (ad !== bestAd?.second) {
                    client.releaseAd(ad)
                }
            }

            if (bestAd != null) {
                // Store info of which client returned the ad
                val clientIndex = clients.indexOf(bestAd.first)
                clientIndexByRequestIdMap[requestId] = clientIndex
                clientIndexByAdIdMap[bestAd.second.id] = clientIndex

                bestAd.second
            } else {
                // If no previous ad or no fresh ad found, return any ad that can be displayed
                // to avoid a blank UI
                clients.firstNotNullOfOrNull { it.getAnyAd() }
            }
        }
    }

    fun releaseAd(ad: Ad) {
        synchronized(clientIndexByAdIdMap) {
            val clientIndex = clientIndexByAdIdMap[ad.id]
            clients.getOrNull(clientIndex ?: -1)?.releaseAd(ad)
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
