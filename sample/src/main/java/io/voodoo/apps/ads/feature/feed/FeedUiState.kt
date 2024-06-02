package io.voodoo.apps.ads.feature.feed

import androidx.compose.runtime.Immutable
import io.voodoo.apps.ads.feature.feed.component.FeedItemUiState
import kotlinx.collections.immutable.ImmutableList

@Immutable
sealed interface FeedUiState {
    data object Loading : FeedUiState
    data object Error : FeedUiState

    data class Content(
        val items: ImmutableList<FeedItemUiState>
    ) : FeedUiState
}
