package io.voodoo.apps.ads.api

import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import io.voodoo.apps.ads.api.lifecycle.CloseOnDestroyLifecycleObserver
import io.voodoo.apps.ads.api.listener.AdListenerHolder
import io.voodoo.apps.ads.api.listener.AdListenerHolderWrapper
import io.voodoo.apps.ads.api.listener.AdLoadingListener
import io.voodoo.apps.ads.api.listener.AdModerationListener
import io.voodoo.apps.ads.api.listener.AdRevenueListener
import io.voodoo.apps.ads.api.model.Ad
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArraySet

// TODO: check if we can factorize some AdClient api in a common interface (registerToLifecycle, availableAdCount, ...)
class AdClientArbitrageur(
    clients: List<AdClient<*>>,
    private val requiredAvailableAdCount: Int = 1
) : Closeable, AdListenerHolder {

    private val clients = clients.toList()
    private val mutexByClientMap = clients.associateWith { Mutex() }

    private val clientIndexByRequestIdMap = mutableMapOf<String, Int>()
    private val clientIndexByAdIdMap = mutableMapOf<Ad.Id, Int>()

    private val adLoadingListeners = CopyOnWriteArraySet<AdLoadingListener>()
    private val adModerationListeners = CopyOnWriteArraySet<AdModerationListener>()
    private val adRevenueListeners = CopyOnWriteArraySet<AdRevenueListener>()
    private val listenersWrapper = AdListenerHolderWrapper(
        adLoadingListeners = adLoadingListeners,
        adModerationListeners = adModerationListeners,
        adRevenueListeners = adRevenueListeners
    )

    private var lifecycleObserver: CloseOnDestroyLifecycleObserver? = null

    init {
        clients.forEach {
            it.addAdLoadingListener(listenersWrapper)
            it.addAdModerationListener(listenersWrapper)
            it.addAdRevenueListener(listenersWrapper)
        }
    }

    @MainThread
    fun registerToLifecycle(lifecycle: Lifecycle) {
        lifecycleObserver?.removeFromLifecycle()
        lifecycleObserver = CloseOnDestroyLifecycleObserver(lifecycle, this)
            .also { lifecycle.addObserver(it) }
    }

    /**
     * @return the sum of [AdClient.getAvailableAdCount]
     */
    fun getAvailableAdCount(): Int {
        return clients.sumOf { it.getAvailableAdCount() }
    }

    /**
     * @return true if any client has an available ad
     */
    fun hasAnyAvailableAd(): Boolean {
        return clients.any { it.getAvailableAdCount() > 0 }
    }

    /**
     * Get an ad for the given [requestId].
     *
     * @return in order of priority:
     * - Previously served ad if a call was already made with the same [requestId] and if the ad is still in memory
     * - The most profitable ad ([Ad.Info.revenue]) that's available (not already displayed)
     * - Any ad that was already displayed but is still in memory and not used by another component
     */
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
            val bestAd = ads.maxByOrNull { (_, ad) -> ad.info.revenue }

            // Release non-retained ad (since render was not called, it won't be destroyed)
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

    /** Forward call to [AdClient.releaseAd] to the client that served the given [ad] */
    fun releaseAd(ad: Ad) {
        synchronized(clientIndexByAdIdMap) {
            val clientIndex = clientIndexByAdIdMap[ad.id]
            clients.getOrNull(clientIndex ?: -1)?.releaseAd(ad)
        }
    }

    /**
     * Start a [AdClient.fetchAd] call for each clients that doesn't have at least
     * [requiredAvailableAdCount] ads available.
     *
     * If a client has enough available ads, no call is made.
     *
     * Suspends until all the calls are completed.
     * A failure in one client will not impact the other calls.
     * If a call is already in progress for a particular client, no additional call is made.
     *
     * @return a mapping for each client in the result being the result if a call was made, or null otherwise.
     */
    suspend fun fetchAdIfNecessary(
        localExtrasProvider: () -> Array<Pair<String, Any>>
    ): List<Result<Ad>?> = supervisorScope {
        val localExtras by lazy(LazyThreadSafetyMode.PUBLICATION) { localExtrasProvider() }

        clients
            .map { client ->
                async {
                    val mutex = mutexByClientMap[client] ?: return@async null

                    if (client.getAvailableAdCount() < requiredAvailableAdCount) {
                        // lock to prevent simultaneous requests
                        if (mutex.tryLock()) {
                            try {
                                runCatching { client.fetchAd(*localExtras) }
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

    override fun close() {
        clients.forEach { it.close() }
    }

    override fun addAdLoadingListener(listener: AdLoadingListener) {
        adLoadingListeners.add(listener)
    }

    override fun removeAdLoadingListener(listener: AdLoadingListener) {
        adLoadingListeners.remove(listener)
    }

    override fun addAdModerationListener(listener: AdModerationListener) {
        adModerationListeners.add(listener)
    }

    override fun removeAdModerationListener(listener: AdModerationListener) {
        adModerationListeners.remove(listener)
    }

    override fun addAdRevenueListener(listener: AdRevenueListener) {
        adRevenueListeners.add(listener)
    }

    override fun removeAdRevenueListener(listener: AdRevenueListener) {
        adRevenueListeners.remove(listener)
    }
}
