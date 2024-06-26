package io.voodoo.apps.ads.feature.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import io.voodoo.apps.ads.MockData
import io.voodoo.apps.ads.feature.feed.component.FeedItemUiState
import io.voodoo.apps.ads.feature.unsplash.UnsplashClient
import io.voodoo.apps.ads.feature.unsplash.UnsplashPhoto
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class FeedViewModel(
    private val unsplashClient: UnsplashClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val isLoading = MutableStateFlow(false)

    init {
        loadContent()
    }

    fun onRetryClick() {
        loadContent()
    }

    private fun loadContent(query: String = MockData.QUERY) {
        viewModelScope.launch {
            if (!isLoading.compareAndSet(expect = false, update = true)) return@launch
            _uiState.value = FeedUiState.Loading

            try {
                val content = unsplashClient.getContent(query)

                // Chunk content and add with delay to emulate load more
                // and make sure size update works correctly
                val contentChunks = content.chunked(5)
                repeat(contentChunks.size) { index ->
                    val items = contentChunks.take(index).flatten().map { it.toFeedItem() }

                    _uiState.value = FeedUiState.Content(
                        items = items.toImmutableList()
                    )
                    delay(5.seconds)
                }
            } catch (e: Exception) {
                Log.e("Limitless", "Failed to get content", e)
                _uiState.value = FeedUiState.Error
            } finally {
                isLoading.value = false
            }
        }
    }

    private fun UnsplashPhoto.toFeedItem(): FeedItemUiState {
        return FeedItemUiState(
            id = id,
            iconUrl = user.profileImage.medium,
            title = description.orEmpty(),
            subtitle = user.name,
            picture = urls.regular,
        )
    }

    companion object {

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application =
                    checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])

                return FeedViewModel(
                    UnsplashClient(application)
                ) as T
            }
        }
    }
}
