@file:OptIn(ExperimentalCoroutinesApi::class)

package io.voodoo.apps.ads.compose.lazylist

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import io.voodoo.apps.ads.api.flow.getAdFetchResultFlow
import io.voodoo.apps.ads.api.model.Ad
import io.voodoo.apps.ads.compose.model.AdClientArbitrageurHolder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlin.math.max

@Composable
fun rememberLazyListAdMediator(
    lazyListState: LazyListState?,
    adClientArbitrageur: AdClientArbitrageurHolder?,
    adInterval: Int,
    adPreferredInitialIndex: Int = adInterval,
    adRequiredMinInitialIndex: Int = 1,
    lazyListIndexOffset: Int = 0,
): LazyListAdMediator {
    return rememberSaveable(saver = LazyListAdMediator.Saver) {
        LazyListAdMediator(
            lazyListState = lazyListState,
            adClientArbitrageur = adClientArbitrageur,
            adInterval = adInterval,
            adPreferredInitialIndex = adPreferredInitialIndex,
            adRequiredMinInitialIndex = adRequiredMinInitialIndex,
            lazyListIndexOffset = lazyListIndexOffset,
        )
    }.apply {
        this.lazyListState = lazyListState
        this.adClientArbitrageur = adClientArbitrageur
        this.adInterval = adInterval
        this.adPreferredInitialIndex = adPreferredInitialIndex
        this.adRequiredMinInitialIndex = adRequiredMinInitialIndex
        this.lazyListIndexOffset = lazyListIndexOffset
    }
}

@Stable
class LazyListAdMediator internal constructor(
    lazyListState: LazyListState?,
    adClientArbitrageur: AdClientArbitrageurHolder?,
    adInterval: Int,
    adIndices: IntArray = intArrayOf(),
    adPreferredInitialIndex: Int,
    adRequiredMinInitialIndex: Int,
    lazyListIndexOffset: Int,
) {

    var lazyListState by mutableStateOf(lazyListState)
        internal set
    var adClientArbitrageur by mutableStateOf(adClientArbitrageur)
        internal set

    /** how many actual items between two ads */
    var adInterval by mutableIntStateOf(adInterval)
        internal set

    /** number of actual items (without the ads) */
    var itemCount by mutableIntStateOf(0)

    /**
     * the first index to display an ad at
     * (might not be respected if [itemCount] is less than this value and
     * if [adRequiredMinInitialIndex] allows it
     */
    var adPreferredInitialIndex by mutableIntStateOf(adPreferredInitialIndex)

    /**
     * the enforced minimum index to display an ad at.
     * this is useful if your [itemCount] is less than [adPreferredInitialIndex]
     */
    var adRequiredMinInitialIndex by mutableIntStateOf(adRequiredMinInitialIndex)

    /**
     * offset for lazy list indices (useful when using the `LazyListScope.items` convenience function
     * while having items added before
     */
    var lazyListIndexOffset by mutableIntStateOf(lazyListIndexOffset)

    /**
     * the actual item count + the ad count,
     * pass this value to [androidx.compose.foundation.lazy.LazyListScope.items] builder
     */
    val totalItemCount by derivedStateOf { computeTotalItemCount() }

    val firstAdIndex get() = _adIndices.firstOrNull()
    val lastAdIndex get() = _adIndices.lastOrNull()
    val adCount get() = _adIndices.size
    val adIndices get() = _adIndices.toList()

    private val _adIndices = mutableStateListOf(*adIndices.toTypedArray())

    suspend fun fetchAdIfNecessary(localExtrasProvider: () -> Array<Pair<String, Any>>) {
        val arbitrageur = adClientArbitrageur?.arbitrageur ?: return
        Log.d("LazyListAdMediator", "fetchAdIfNecessary")

        // This is a blocking call, only returns when all operations are finished
        arbitrageur.fetchAdIfNecessary(localExtrasProvider)
    }

    fun hasAdAt(index: Int): Boolean {
        // index in adIndices but for sorted array
        _adIndices.forEach { adIndex ->
            when {
                adIndex == index -> return true
                adIndex > index -> return false
            }
        }

        return false
    }

    fun getAdCountUntil(indexExclusive: Int): Int {
        // adIndices.count { it < index } but for sorted array
        if (_adIndices.isEmpty()) return 0
        var count = 0
        for (adIndex in _adIndices) {
            if (adIndex >= indexExclusive) break
            count++
        }
        return count
    }

    /** @return the real index in the original dataset (without the ads) from the lazylist index */
    fun getItemIndex(index: Int): Int {
        return index - getAdCountUntil(index)
    }

    /**
     * Remove all ads from the list.
     * A good moment to call this is when you refresh the list content.
     * Adding items doesn't necessarily requires a call,
     * since you can keep the ads depending on how many items were added.
     */
    fun clearAdIndices() {
        Log.w("LazyListAdMediator", "clearAdIndices")
        _adIndices.clear()
    }

    /**
     * Remove all ads from the list that are below the list of rendered items.
     * A good moment to call this is when the app comes back to foreground.
     */
    fun clearAdIndicesBelowViewport() {
        Log.w("LazyListAdMediator", "clearAdIndicesBelowViewport")
        val lastRenderedItemIndex = getLastRenderedItemIndex()
        _adIndices.removeAll { it > lastRenderedItemIndex }
    }

    /**
     * @return number of elements being destroyed
     */
    fun destroyAdsIf(predicate: (Ad) -> Boolean): Int {
        return adClientArbitrageur?.arbitrageur?.destroyAdsIf(predicate) ?: 0
    }

    fun getAdAt(index: Int): Ad? {
        val ad = adClientArbitrageur?.arbitrageur?.getAd(requestId = index.toString())
        Log.d("LazyListAdMediator", "Returning ad $ad at $index")
        return ad
    }

    fun releaseAd(ad: Ad) {
        Log.d("LazyListAdMediator", "Release ad $ad")
        adClientArbitrageur?.arbitrageur?.releaseAd(ad)
    }

    fun checkAndInsertAvailableAds() {
        // If we're before the last ad added, we may have scrolled back, since ads will be used
        // for previous indices, don't insert for now
        val lazyListState = lazyListState ?: return
        val arbitrageur = adClientArbitrageur?.arbitrageur ?: return

        val firstVisibleItemIndex =
            (lazyListState.firstVisibleItemIndex - lazyListIndexOffset)
                .coerceAtLeast(0)
        if (
            firstVisibleItemIndex > (_adIndices.firstOrNull() ?: Int.MAX_VALUE) &&
            firstVisibleItemIndex < (lastAdIndex ?: Int.MIN_VALUE)
        ) {
            Log.d("LazyListAdMediator", "checkAndInsertAvailableAds skip")
            return
        }

        val availableAdCount = arbitrageur.getAvailableAdCount().notLocked
        val insertedNextAdCount = _adIndices.count { it > firstVisibleItemIndex }
        val adsToInsert = (availableAdCount - insertedNextAdCount).coerceAtLeast(0)
        Log.d("LazyListAdMediator", "checkAndInsertAvailableAds insert $adsToInsert ads")

        repeat(adsToInsert) {
            insertAdIndex()
        }
    }

    internal fun getLastRenderedItemIndex(): Int {
        return lazyListState?.layoutInfo?.visibleItemsInfo
            ?.maxByOrNull { it.index }?.index
            ?.minus(lazyListIndexOffset)
            ?.coerceAtLeast(0)
            ?: 0
    }

    private fun insertAdIndex() {
        val totalItemCount = totalItemCount

        val index = max(
            (getLastRenderedItemIndex() + 1)
                .coerceAtLeast(adPreferredInitialIndex)
                .coerceAtMost(totalItemCount),
            lastAdIndex?.plus(adInterval + 1) ?: Int.MIN_VALUE
        )

        if (index !in adRequiredMinInitialIndex..totalItemCount) {
            // We can't display the ad
            Log.d("LazyListAdMediator", "can't insert ad at $index")
            return
        }

        Log.d("LazyListAdMediator", "insert ad at $index (totalItemCount $totalItemCount)")
        _adIndices.add(index)
    }

    private fun computeTotalItemCount(): Int {
        return (itemCount + _adIndices.size)
            // Remove all ads if actual content changed and we have ads outside the dataset
            // Note: this is just a nice-to-have, you should call clearAdIndices() manually when content changes
            .also { if ((lastAdIndex ?: Int.MIN_VALUE) > it) clearAdIndices() }
    }

    companion object {
        /**
         * To keep current page and current page offset saved
         */
        val Saver: Saver<LazyListAdMediator, *> = listSaver(
            save = {
                listOf(
                    it.adInterval,
                    it._adIndices.toIntArray(),
                    it.adPreferredInitialIndex,
                    it.adRequiredMinInitialIndex,
                    it.lazyListIndexOffset,
                )
            },
            restore = {
                LazyListAdMediator(
                    lazyListState = null,
                    adClientArbitrageur = null,
                    adInterval = it[0] as Int,
                    adIndices = it[1] as IntArray,
                    adPreferredInitialIndex = it[2] as Int,
                    adRequiredMinInitialIndex = it[3] as Int,
                    lazyListIndexOffset = it[4] as Int,
                )
            }
        )
    }
}

@Composable
fun LazyListAdMediator.DefaultScrollAdBehaviorEffect(
    clearAdIndicesOnScrollTop: Boolean = true,
    localExtrasProvider: () -> Array<Pair<String, Any>> = { emptyArray() }
) {
    val coroutineScope = rememberCoroutineScope()
    val currentLocalExtraProvider by rememberUpdatedState(localExtrasProvider)

    LaunchedEffect(this) {
        snapshotFlow { adClientArbitrageur }
            .flatMapLatest {
                snapshotFlow { lazyListState?.firstVisibleItemIndex to itemCount }
                    .conflate()
            }
            .filterNotNull()
            .collect {
                // fetchAdIfNecessary will block until all launched fetch operations are finished
                // we don't want to block the collection because one ad client could be slow
                // while the other responsive, and we should keep serving and loading ads regardless
                coroutineScope.launch {
                    fetchAdIfNecessary(currentLocalExtraProvider)
                    checkAndInsertAvailableAds()
                }

                // Because ads are not always added in adIndices when loaded
                // We check if we should add one everytime the current index in LazyList changes
                checkAndInsertAvailableAds()
            }
    }

    if (clearAdIndicesOnScrollTop) {
        LaunchedEffect(this) {
            snapshotFlow { adClientArbitrageur }
                .flatMapLatest {
                    snapshotFlow { getLastRenderedItemIndex() }
                        .conflate()
                }
                .filter { it < (firstAdIndex ?: 0) && adCount > 1 }
                .collect {
                    clearAdIndices()
                }
        }
    }

    AdFetchResultEffect {
        checkAndInsertAvailableAds()
    }
}

@Composable
inline fun LazyListAdMediator.AdFetchResultEffect(
    crossinline body: suspend () -> Unit,
) {
    LaunchedEffect(this) {
        snapshotFlow { adClientArbitrageur?.arbitrageur }
            .flatMapLatest { it?.getAdFetchResultFlow() ?: emptyFlow() }
            .filter { it.isSuccess }
            .collect {
                body()
            }
    }
}
