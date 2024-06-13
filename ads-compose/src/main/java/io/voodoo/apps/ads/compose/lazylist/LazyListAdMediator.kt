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
import io.voodoo.apps.ads.api.model.Ad
import io.voodoo.apps.ads.compose.model.AdClientArbitrageurHolder
import io.voodoo.apps.ads.compose.util.getAdFetchResults
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
): LazyListAdMediator {
    return rememberSaveable(saver = LazyListAdMediator.Saver) {
        LazyListAdMediator(
            lazyListState = lazyListState,
            adClientArbitrageur = adClientArbitrageur,
            adInterval = adInterval,
            adPreferredInitialIndex = adPreferredInitialIndex,
            adRequiredMinInitialIndex = adRequiredMinInitialIndex,
        )
    }.apply {
        this.lazyListState = lazyListState
        this.adClientArbitrageur = adClientArbitrageur
        this.adInterval = adInterval
        this.adPreferredInitialIndex = this.adPreferredInitialIndex
    }
}

@Stable
class LazyListAdMediator internal constructor(
    lazyListState: LazyListState?,
    adClientArbitrageur: AdClientArbitrageurHolder?,
    adInterval: Int,
    adIndices: IntArray = intArrayOf(),
    adPreferredInitialIndex: Int,
    adRequiredMinInitialIndex: Int
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

    private val adIndices = mutableStateListOf(*adIndices.toTypedArray())
    private val latestAdIndex get() = adIndices.lastOrNull()

    /**
     * the actual item count + the ad count,
     * pass this value to [androidx.compose.foundation.lazy.LazyListScope.items] builder
     */
    val totalItemCount by derivedStateOf { computeTotalItemCount() }

    suspend fun fetchAdIfNecessary(localExtrasProvider: () -> Array<Pair<String, Any>>) {
        val arbitrageur = adClientArbitrageur?.arbitrageur ?: return
        Log.d("LazyListAdMediator", "fetchAdIfNecessary")

        // This is a blocking call, only returns when all operations are finished
        arbitrageur.fetchAdIfNecessary(localExtrasProvider)
    }

    fun hasAdAt(index: Int): Boolean {
        // index in adIndices but for sorted array
        adIndices.forEach { adIndex ->
            when {
                adIndex == index -> return true
                adIndex > index -> return false
            }
        }

        return false
    }

    fun getAdCountUntil(indexExclusive: Int): Int {
        // adIndices.count { it < index } but for sorted array
        if (adIndices.isEmpty()) return 0
        var count = 0
        for (adIndex in adIndices) {
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
        adIndices.clear()
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

        val firstVisibleItemIndex = lazyListState.firstVisibleItemIndex
        if (
            firstVisibleItemIndex > (adIndices.firstOrNull() ?: Int.MAX_VALUE) &&
            firstVisibleItemIndex < (latestAdIndex ?: Int.MIN_VALUE)
        ) {
            Log.d("LazyListAdMediator", "checkAndInsertAvailableAds skip")
            return
        }

        val availableAdCount = arbitrageur.getAvailableAdCount()
        val insertedNextAdCount = adIndices.count { it > firstVisibleItemIndex }
        val adsToInsert = (availableAdCount - insertedNextAdCount).coerceAtLeast(0)
        Log.d("LazyListAdMediator", "checkAndInsertAvailableAds insert $adsToInsert ads")

        repeat(adsToInsert) {
            insertAdIndex()
        }
    }

    private fun insertAdIndex() {
        val lastRenderedItem =
            lazyListState?.layoutInfo?.visibleItemsInfo?.maxByOrNull { it.index }?.index ?: 0
        val totalItemCount = totalItemCount

        val index = max(
            (lastRenderedItem + 1)
                .coerceAtLeast(adPreferredInitialIndex)
                .coerceAtMost(totalItemCount),
            latestAdIndex?.plus(adInterval + 1) ?: Int.MIN_VALUE
        )

        if (index !in adRequiredMinInitialIndex..totalItemCount) {
            // We can't display the ad
            Log.d("LazyListAdMediator", "can't insert ad at $index")
            return
        }

        Log.d("LazyListAdMediator", "insert ad at $index (totalItemCount $totalItemCount)")
        adIndices.add(index)
    }

    private fun computeTotalItemCount(): Int {
        return (itemCount + adIndices.size)
            // Remove all ads if actual content changed and we have ads outside the dataset
            // Note: this is just a nice-to-have, you should call clearAdIndices() manually when content changes
            .also { if ((latestAdIndex ?: Int.MIN_VALUE) > it) clearAdIndices() }
    }

    companion object {
        /**
         * To keep current page and current page offset saved
         */
        val Saver: Saver<LazyListAdMediator, *> = listSaver(
            save = {
                listOf(
                    it.adInterval,
                    it.adIndices.toIntArray(),
                    it.adPreferredInitialIndex,
                    it.adRequiredMinInitialIndex,
                )
            },
            restore = {
                LazyListAdMediator(
                    lazyListState = null,
                    adClientArbitrageur = null,
                    adInterval = it[0] as Int,
                    adIndices = it[1] as IntArray,
                    adPreferredInitialIndex = it[2] as Int,
                    adRequiredMinInitialIndex = it[3] as Int
                )
            }
        )
    }
}

@Composable
fun LazyListAdMediator.DefaultScrollAdBehaviorEffect(
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
            .flatMapLatest { it?.getAdFetchResults() ?: emptyFlow() }
            .filter { it.isSuccess }
            .collect {
                body()
            }
    }
}
