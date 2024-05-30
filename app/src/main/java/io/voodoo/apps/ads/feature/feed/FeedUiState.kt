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

            val id: String

            @Immutable
            data class Item(val item: FeedItemUiState) : ContentItem {
                override val id: String
                    get() = item.id
            }

            @Immutable
            data class Ad(val ad: io.voodoo.apps.ads.api.model.Ad?) : ContentItem {
                override val id: String
                    get() = ad?.id?.id.orEmpty()
            }
        }
    }
}
