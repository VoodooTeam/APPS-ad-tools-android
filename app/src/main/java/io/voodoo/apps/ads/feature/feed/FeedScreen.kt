package io.voodoo.apps.ads.feature.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.voodoo.apps.ads.MockData
import io.voodoo.apps.ads.feature.feed.component.FeedErrorState
import io.voodoo.apps.ads.feature.feed.component.FeedItem
import io.voodoo.apps.ads.feature.feed.component.FeedTopAppBar

@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            FeedTopAppBar(
                profilePictureUrl = MockData.PROFILE_PICTURE,
                onProfilePictureClick = {}
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
                    FeedScreenContent(currentUiState)
                }
            }
        }
    }
}

@Composable
private fun FeedScreenContent(
    content: FeedUiState.Content,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = content.items,
            key = { it.picture }
        ) { item ->
            FeedItem(item = item)
        }
    }
}
