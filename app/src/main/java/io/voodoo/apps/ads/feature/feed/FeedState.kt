package io.voodoo.apps.ads.feature.feed

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.voodoo.apps.ads.model.Ad
import kotlin.math.max

@Composable
fun rememberFeedState(
    adInterval: Int,
    adArbitrageur: FeedAdArbitrageur?,
    itemCount: () -> Int,
): FeedState {
    return rememberSaveable(saver = FeedState.Saver) {
        FeedState(
            adArbitrageur = adArbitrageur,
            adInterval = adInterval,
            updatedItemCount = itemCount
        )
    }.apply {
        this.adArbitrageur = adArbitrageur
        this.adInterval = adInterval
        this.pageCount = itemCount
    }
}

@Stable
class FeedState(
    adArbitrageur: FeedAdArbitrageur?,
    adInterval: Int,
    adIndices: IntArray = intArrayOf(),
    firstVisibleItemIndex: Int = 0,
    firstVisibleItemScrollOffset: Int = 0,
    updatedItemCount: () -> Int
) {

    val lazyListState = LazyListState(firstVisibleItemIndex, firstVisibleItemScrollOffset)
    var pageCount by mutableStateOf(updatedItemCount)

    var adInterval by mutableIntStateOf(adInterval)
    var adArbitrageur by mutableStateOf(adArbitrageur)
    private val adIndices = mutableStateListOf(*adIndices.toTypedArray())

    /**
     * the actual item count + the ad count,
     * pass this value to [androidx.compose.foundation.lazy.LazyListScope.items] builder
     */
    val totalItemCount by derivedStateOf { pageCount() + adIndices.size }

    suspend fun fetchAdIfNecessary() {
        val arbitrageur = adArbitrageur?.arbitrageur ?: return
        Log.d("FeedState", "fetchAdIfNecessary")
        checkAndInsertAvailableAds()
        arbitrageur.fetchAdIfNecessary()
        checkAndInsertAvailableAds()
    }

    fun hasAdAt(index: Int): Boolean = index in adIndices

    fun adsCountInRange(startInclusive: Int, endExclusive: Int): Int {
        return adsCountInRange(startInclusive until endExclusive)
    }

    fun adsCountInRange(range: IntRange): Int {
        return adIndices.count { it in range }
    }

    fun getAdAt(index: Int): Ad? {
        val ad = adArbitrageur?.arbitrageur?.getAd(requestId = index.toString())
        Log.d("FeedState", "Returning ad $ad at $index")
        return ad
    }

    fun releaseAd(ad: Ad) {
        Log.d("FeedState", "Release ad $ad")
        adArbitrageur?.arbitrageur?.releaseAd(ad)
    }

    /**
     * Because we only wait for 1 positive result when fetching an ad, maybe we didn't insert
     * every available ads
     */
    private fun checkAndInsertAvailableAds() {
        // If we're before the last ad added, we may have scrolled back, since ads will be used
        // for previous indices, don't insert for now
        val firstVisibleItemIndex = lazyListState.firstVisibleItemIndex
        if (
            firstVisibleItemIndex > (adIndices.firstOrNull() ?: Int.MAX_VALUE) &&
            firstVisibleItemIndex < (adIndices.lastOrNull() ?: Int.MIN_VALUE)
        ) {
            Log.d("FeedState", "checkAndInsertAvailableAds skip")
            return
        }

        val availableAdCount = adArbitrageur?.arbitrageur?.getAvailableAdCount() ?: return
        val insertedNextAdCount = adIndices.count { it > firstVisibleItemIndex }
        val adsToInsert = (availableAdCount - insertedNextAdCount).coerceAtLeast(0)
        Log.d("FeedState", "checkAndInsertAvailableAds insert $adsToInsert ads")

        repeat(adsToInsert) {
            insertAdIndex()
        }
    }

    private fun insertAdIndex() {
        val index = max(
            // TODO: compute first non visible item?
            lazyListState.firstVisibleItemIndex + 2,
            (adIndices.maxOrNull() ?: -1) + adInterval + 1
        ).coerceAtMost(totalItemCount)
        Log.d("FeedState", "insert ad at $index")
        adIndices.add(index)
    }

    companion object {
        /**
         * To keep current page and current page offset saved
         */
        val Saver: Saver<FeedState, *> = listSaver(
            save = {
                listOf(
                    it.adInterval,
                    it.adIndices.toIntArray(),
                    it.lazyListState.firstVisibleItemIndex,
                    it.lazyListState.firstVisibleItemScrollOffset,
                    it.pageCount(),
                )
            },
            restore = {
                FeedState(
                    adArbitrageur = null,
                    adInterval = it[0] as Int,
                    adIndices = it[1] as IntArray,
                    firstVisibleItemIndex = it[2] as Int,
                    firstVisibleItemScrollOffset = it[3] as Int,
                    updatedItemCount = { it[4] as Int }
                )
            }
        )
    }
}
