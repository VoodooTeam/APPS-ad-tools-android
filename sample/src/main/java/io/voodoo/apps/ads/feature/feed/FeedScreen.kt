package io.voodoo.apps.ads.feature.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.voodoo.apps.ads.MockData
import io.voodoo.apps.ads.compose.lazylist.DefaultScrollAdBehaviorEffect
import io.voodoo.apps.ads.compose.lazylist.LazyListAdMediator
import io.voodoo.apps.ads.compose.lazylist.items
import io.voodoo.apps.ads.compose.lazylist.rememberLazyListAdMediator
import io.voodoo.apps.ads.compose.model.AdArbitrageurHolder
import io.voodoo.apps.ads.feature.feed.component.FeedAdItem
import io.voodoo.apps.ads.feature.feed.component.FeedErrorState
import io.voodoo.apps.ads.feature.feed.component.FeedItem
import io.voodoo.apps.ads.feature.feed.component.FeedTopAppBar
import java.util.concurrent.TimeUnit

@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    adArbitrageur: AdArbitrageurHolder?,
    onNavigateToMediationDebugger: () -> Unit,
    onNavigateToPrivacyEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val adMediator = rememberLazyListAdMediator(
        lazyListState = listState,
        adArbitrageur = adArbitrageur,
        adInterval = 3,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            FeedTopAppBar(
                profilePictureUrl = MockData.PROFILE_PICTURE,
                onProfilePictureClick = onNavigateToMediationDebugger,
                onPrivacyEditClick = onNavigateToPrivacyEdit
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
                        listState = listState,
                        adMediator = adMediator,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedScreenContent(
    content: FeedUiState.Content,
    listState: LazyListState,
    adMediator: LazyListAdMediator,
    modifier: Modifier = Modifier,
) {
    adMediator.DefaultScrollAdBehaviorEffect(
        localExtrasProvider = {
            // Provide extra if needed (see readme)
            arrayOf(
                "bigoads_age" to 32,
                "bigoads_gender" to 2, // gender male
                "bigoads_activated_time" to TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
            )
        }
    )

    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            adMediator = adMediator,
            items = content.items,
            adKey = { it },
            key = { it.id },
            // Not mandatory and maybe not efficient, because each update would cause
            // every content type to be re-computed
            // TODO: test and check how contentType is called when adIndices changes for perf
            // adContentType = { "ad" },
            // contentType = { _ -> "content" },
            adContent = { _, item ->
                FeedAdItem(ad = item)
            }
        ) { item ->
            FeedItem(
                item = item
            )
        }
    }
}
