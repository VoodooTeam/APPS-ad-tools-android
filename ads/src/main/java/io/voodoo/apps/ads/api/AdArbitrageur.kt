package io.voodoo.apps.ads.api

import io.voodoo.apps.ads.model.Ad
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex

class AdArbitrageur(
    clients: List<AdClient<*>>,
    private val requiredAvailableAdCount: Int = 1
) {

    private val clients = clients.toList()
    private val mutexByClientMap = clients.associateWith { Mutex() }

    private val clientIndexByRequestIdMap = mutableMapOf<String, Int>()
    private val clientIndexByAdIdMap = mutableMapOf<Ad.Id, Int>()

    private val adFetchResults = MutableSharedFlow<Result<Ad>>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    fun getAdFetchResults(): Flow<Result<Ad>> {
        return adFetchResults.asSharedFlow()
    }

    fun getAvailableAdCount(): Int {
        return clients.sumOf { it.getAvailableAdCount() }
    }

    fun hasAnyAvailableAd(): Boolean {
        return clients.any { it.getAvailableAdCount() > 0 }
    }

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
    ): List<Result<Ad>?> = supervisorScope {
        clients
            .map { client ->
                async {
                    val mutex = mutexByClientMap[client] ?: return@async null

                    if (client.getAvailableAdCount() < requiredAvailableAdCount) {
                        if (mutex.tryLock()) {
                            try {
                                runCatching { client.fetchAd(*localKeyValues) }
                                    .also { adFetchResults.emit(it) }
                            } finally {
                                mutex.unlock()
                            }
                        } else {
                            // Request already in progress in another call
                            null
                        }
                    } else {
                        // No fetch needed
                        null
                    }
                }
            }
            .awaitAll()
    }

//    private suspend fun List<Deferred<Result<Ad>>>.awaitFirstSuccess(): Boolean {
//        do {
//            val filtered = filterNot { it.isCompleted && it.getCompleted().isFailure }
//            if (filtered.isEmpty()) return false
//
//            val result = select {
//                filtered.forEach { deferred -> deferred.onAwait { it } }
//            }
//
//            if (result.isSuccess) return true
//        } while (true)
//    }
}
