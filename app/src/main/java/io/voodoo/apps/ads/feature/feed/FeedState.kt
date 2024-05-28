package io.voodoo.apps.ads.feature.feed

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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
    adArbitrageur: FeedAdArbitrageur?
) {

    val lazyListState = LazyListState()

    var adInterval by mutableIntStateOf(adInterval)
    var adArbitrageur by mutableStateOf(adArbitrageur)
    val adIndices = mutableStateListOf<Int>()

    val itemCount by derivedStateOf { lazyListState.layoutInfo.totalItemsCount + adIndices.size }

    suspend fun fetchAdIfNecessary() {
        val success = adArbitrageur?.arbitrageur?.fetchAdIfNecessary() ?: return
        if (success) {
            val index = (lazyListState.firstVisibleItemIndex + adInterval).coerceAtMost(itemCount)
            adIndices.add(index)
        }
    }

    fun hasAdAt(index: Int): Boolean = index in adIndices

    fun getItem(index: Int, content: FeedUiState.Content): FeedUiState.Content.ContentItem? {
        if (hasAdAt(index)) {
            val ad = adArbitrageur?.arbitrageur?.getAd(requestId = index.toString())
            return FeedUiState.Content.ContentItem.Ad(ad)
        }

        val offset = adIndices.count { it < index }
        return content.items.getOrNull(index + offset)
    }
}
