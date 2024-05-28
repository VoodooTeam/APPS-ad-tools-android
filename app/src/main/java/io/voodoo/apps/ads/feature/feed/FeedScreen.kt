package io.voodoo.apps.ads.feature.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.voodoo.apps.ads.MockData
import io.voodoo.apps.ads.feature.feed.component.FeedErrorState
import io.voodoo.apps.ads.feature.feed.component.FeedItem
import io.voodoo.apps.ads.feature.feed.component.FeedTopAppBar
import kotlinx.coroutines.flow.conflate

@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    adArbitrageur: FeedAdArbitrageur?,
    onNavigateToMediationDebugger: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val feedState = remember { FeedState(adInterval = 3, adArbitrageur = adArbitrageur) }.also {
        it.adArbitrageur = adArbitrageur
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            FeedTopAppBar(
                profilePictureUrl = MockData.PROFILE_PICTURE,
                onProfilePictureClick = onNavigateToMediationDebugger
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val currentUiState = uiState) {
                FeedUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                FeedUiState.Error -> {
                    FeedErrorState(
                        onRetryClick = viewModel::onRetryClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is FeedUiState.Content -> {
                    FeedScreenContent(
                        content = currentUiState,
                        feedState = feedState,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedScreenContent(
    content: FeedUiState.Content,
    feedState: FeedState,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(feedState) {
        snapshotFlow { feedState.lazyListState.firstVisibleItemIndex }
            .conflate()
            .collect {
                feedState.fetchAdIfNecessary()
            }
    }

    LazyColumn(
        state = feedState.lazyListState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            count = feedState.itemCount,
            // Must use the index as a position, in case we add an ad at visible index
            key = { it },
            contentType = {
                if (feedState.hasAdAt(it)) {
                    "ad"
                } else {
                    "item"
                }
            }
        ) { index ->
            val item = feedState.getItem(index, content)
            FeedContentItem(
                item = item
            )
        }
    }
}

@Composable
fun FeedContentItem(
    item: FeedUiState.Content.ContentItem?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, propagateMinConstraints = true) {
        when (item) {
            is FeedUiState.Content.ContentItem.Item -> {
                FeedItem(
                    item = item.item
                )
            }

            is FeedUiState.Content.ContentItem.Ad -> {
                if (item.ad != null) {
                    // Show ad

                } else {
                    // Shouldn't happen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.Red)
                    )
                }
            }

            null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.Green)
                )
            }
        }
    }
}
