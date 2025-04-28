package io.voodoo.apps.ads.api

import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import io.voodoo.apps.ads.api.lifecycle.CloseOnDestroyLifecycleObserver
import io.voodoo.apps.ads.api.listener.AdClickListener
import io.voodoo.apps.ads.api.listener.AdListenerHolder
import io.voodoo.apps.ads.api.listener.AdListenerHolderWrapper
import io.voodoo.apps.ads.api.listener.AdLoadingListener
import io.voodoo.apps.ads.api.listener.AdModerationListener
import io.voodoo.apps.ads.api.listener.AdRevenueListener
import io.voodoo.apps.ads.api.model.Ad
import io.voodoo.apps.ads.api.model.AdAlreadyLoadingException
import io.voodoo.apps.ads.api.model.BackoffConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArraySet

// TODO: check if we can factorize some AdClient api in a common interface (registerToLifecycle, availableAdCount, ...)
class AdClientArbitrageur(
    clients: List<AdClient<*>>,
    private val requiredAvailableAdCount: Int = 1,
    private val overrideClientCacheSize: Boolean = false,
    private val backoffConfig: BackoffConfig? = null,
) : Closeable, AdListenerHolder {

    private val clients = clients.toList()

    private val clientIndexByRequestIdMap = mutableMapOf<String, Int>()
    private val clientIndexByAdIdMap = mutableMapOf<Ad.Id, Int>()
    private val lastFailInfoByClientMap = mutableMapOf<AdClient<*>, FetchFailInfo>()

    private val adLoadingListeners = CopyOnWriteArraySet<AdLoadingListener>()
    private val adModerationListeners = CopyOnWriteArraySet<AdModerationListener>()
    private val adRevenueListeners = CopyOnWriteArraySet<AdRevenueListener>()
    private val adClickListeners = CopyOnWriteArraySet<AdClickListener>()
    private val listenersWrapper: AdListenerHolderWrapper = AdListenerHolderWrapper(
        adLoadingListeners = adLoadingListeners,
        adModerationListeners = adModerationListeners,
        adRevenueListeners = adRevenueListeners,
        adClickListeners = adClickListeners,
    )

    private var lifecycleObserver: CloseOnDestroyLifecycleObserver? = null

    init {
        clients.forEach { client ->
            client.addAdLoadingListener(listenersWrapper)
            client.addAdModerationListener(listenersWrapper)
            client.addAdRevenueListener(listenersWrapper)
        }
    }

    fun destroyAdsIf(predicate: (Ad) -> Boolean): Int {
        return clients.sumOf { client ->
            client.destroyAdsIf(predicate)
        }
    }

    @MainThread
    fun registerToLifecycle(lifecycle: Lifecycle) {
        lifecycleObserver?.removeFromLifecycle()
        lifecycleObserver = CloseOnDestroyLifecycleObserver(lifecycle, this)
            .also { lifecycle.addObserver(it) }
    }

    /**
     * @return the sum of each client's [AdClient.getAvailableAdCount]
     */
    fun getAvailableAdCount(): AdClient.AdCount {
        return clients.fold(AdClient.AdCount.ZERO) { acc, adClient ->
            adClient.getAvailableAdCount() + acc
        }
    }

    /**
     * @return true if any client has an available ad
     */
    fun hasAnyAvailableAd(): Boolean {
        return clients.any { it.getAvailableAdCount().notLocked > 0 }
    }

    /**
     * @return true if any client has an available ad
     */
    fun hasAnyRequestInProgress(): Boolean {
        return clients.any { it.isRequestInProgress() }
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
            // Use revenueThreshold parameter to avoid locking ad for no reason in client
            // (which would lead to useless calls of onOnAvailableAdCountChangedListeners
            var revenue = -1.0
            val ads = clients.mapNotNull { client ->
                val ad = client.getAvailableAd(
                    requestId = requestId,
                    revenueThreshold = revenue
                ) ?: return@mapNotNull null
                revenue = ad.info.revenue

                client to ad
            }
            val bestAd = ads.maxByOrNull { (_, ad) -> ad.info.revenue }

            // Release non-retained ad (since render was not called, it won't be destroyed)
            ads.forEach { (client, ad) ->
                if (ad !== bestAd?.second) {
                    client.releaseAd(ad)
                }
            }

            // If no previous ad or no fresh ad found, return any ad that can be displayed
            // to avoid a blank UI
            val ad = bestAd
                ?: clients.firstNotNullOfOrNull { client ->
                    client.getAnyAd()?.let { client to it }
                }

            if (ad != null) {
                // Store info of which client returned the ad
                val clientIndex = clients.indexOf(ad.first)
                clientIndexByRequestIdMap[requestId] = clientIndex
                clientIndexByAdIdMap[ad.second.id] = clientIndex
            }

            ad?.second
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
        val localExtras = lazy(LazyThreadSafetyMode.PUBLICATION) { localExtrasProvider() }

        clients
            .map { client ->
                async {
                    fetchAdIfNecessary(client, localExtras)
                }
            }
            .awaitAll()
    }

    private suspend fun fetchAdIfNecessary(
        client: AdClient<*>,
        localExtrasProvider: Lazy<Array<Pair<String, Any>>>
    ): Result<Ad>? {
        val availableAdCount = client.getAvailableAdCount().notLocked
        if (availableAdCount >= requiredAvailableAdCount) {
            return null
        }
        if (
            !overrideClientCacheSize &&
            client.config.adCacheSize <= requiredAvailableAdCount &&
            availableAdCount >= client.config.adCacheSize
        ) {
            return null
        }

        // Backoff
        val lastFailInfo = lastFailInfoByClientMap[client]
        if (backoffConfig != null && lastFailInfo != null) {
            val delayForNextAttemptMillis =
                backoffConfig.getDelay(lastFailInfo.attempt)
                    ?.inWholeMilliseconds
                    ?: 0
            val timeSinceLastAttempt = System.currentTimeMillis() - lastFailInfo.timestampMillis
            if (timeSinceLastAttempt < delayForNextAttemptMillis) {
                return null
            }
        }

        return runCatching { client.fetchAd(*localExtrasProvider.value) }
            // client will throw AdAlreadyLoadingException if already loading
            .takeIf { it.exceptionOrNull() !is AdAlreadyLoadingException }
            ?.also {
                if (it.isFailure) {
                    lastFailInfoByClientMap[client] = FetchFailInfo(
                        attempt = lastFailInfo?.attempt?.plus(1) ?: 0,
                        timestampMillis = System.currentTimeMillis()
                    )
                } else {
                    lastFailInfoByClientMap.remove(client)
                }
            }
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

    override fun addAdClickListener(listener: AdClickListener) {
        adClickListeners.add(listener)
    }

    override fun removeAdClickListener(listener: AdClickListener) {
        adClickListeners.remove(listener)
    }

    private class FetchFailInfo(val attempt: Int, val timestampMillis: Long)
}
