package io.voodoo.apps.ads.feature.feed

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.max

// TODO
//@Composable
//fun rememberFeedState(
//    adArbitrageur: FeedAdArbitrageur?
//): FeedState {
//    return rememberSaveable {
//    }
//}

@Stable
class FeedState(
    adInterval: Int,
    adArbitrageur: FeedAdArbitrageur?,
    updatedPageCount: () -> Int
) {

    val lazyListState = LazyListState()
    var pageCount by mutableStateOf(updatedPageCount)

    var adInterval by mutableIntStateOf(adInterval)
    var adArbitrageur by mutableStateOf(adArbitrageur)
    val adIndices = mutableStateListOf<Int>()

    val itemCount by derivedStateOf { pageCount() + adIndices.size }

    suspend fun fetchAdIfNecessary() {
        val arbitrageur = adArbitrageur?.arbitrageur ?: return
        Log.i("AdClient", "fetchAdIfNecessary")
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

    fun getAdItem(index: Int): FeedUiState.Content.ContentItem.Ad {
        val ad = adArbitrageur?.arbitrageur?.getAd(requestId = index.toString())
        Log.i("AdClient", "Returning ad $ad at $index")
        return FeedUiState.Content.ContentItem.Ad(ad)
    }

    fun releaseAd(ad: FeedUiState.Content.ContentItem.Ad) {
        Log.i("AdClient", "Release ad ${ad.ad}")
        adArbitrageur?.arbitrageur?.releaseAd(ad.ad ?: return)
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
            Log.i("AdClient", "checkAndInsertAvailableAds skip")
            return
        }

        val availableAdCount = adArbitrageur?.arbitrageur?.getAvailableAdCount() ?: return
        val insertedNextAdCount = adIndices.count { it > firstVisibleItemIndex }
        val adsToInsert = (availableAdCount - insertedNextAdCount).coerceAtLeast(0)
        Log.i("AdClient", "checkAndInsertAvailableAds insert $adsToInsert ads")

        repeat(adsToInsert) {
            insertAdIndex()
        }
    }

    private fun insertAdIndex() {
        val index = max(
            // TODO: compute first non visible item?
            lazyListState.firstVisibleItemIndex + 2,
            (adIndices.maxOrNull() ?: -1) + adInterval + 1
        ).coerceAtMost(itemCount)
        Log.i("AdClient", "insert ad at $index")
        adIndices.add(index)
    }
}
