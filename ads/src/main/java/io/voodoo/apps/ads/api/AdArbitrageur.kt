package io.voodoo.apps.ads.api

import io.voodoo.apps.ads.model.Ad
import kotlinx.coroutines.async
import kotlinx.coroutines.selects.whileSelect
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AdArbitrageur(
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
    ): Boolean = supervisorScope {
        mutex.withLock {
            val results = clients
                .mapNotNull { client ->
                    if (client.getAvailableAdCount() < requiredAvailableAdCount) {
                        async {
                            runCatching { client.fetchAd(*localKeyValues) }
                        }
                    } else {
                        null
                    }
                }

            // TODO: check to clean this up
            whileSelect {
                results
                    .filterNot { it.isCompleted && it.getCompleted().isFailure }
                    .forEach { result ->
                        result.onAwait { it.isFailure }
                    }
            }

            results.any { it.isCompleted && it.getCompleted().isSuccess }
        }
    }
}
