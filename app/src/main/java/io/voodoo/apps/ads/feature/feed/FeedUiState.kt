package io.voodoo.apps.ads.feature.feed

import androidx.compose.runtime.Immutable
import io.voodoo.apps.ads.feature.feed.component.FeedItemUiState
import kotlinx.collections.immutable.ImmutableList

@Immutable
sealed interface FeedUiState {
    data object Loading : FeedUiState
    data object Error : FeedUiState

    data class Content(
        val items: ImmutableList<ContentItem>
    ) : FeedUiState {

        @Immutable
        sealed interface ContentItem {

            @Immutable
            data class Item(val item: FeedItemUiState) : ContentItem

            @Immutable
            data class Ad(val ad: io.voodoo.apps.ads.model.Ad?) : ContentItem
        }
    }
}
