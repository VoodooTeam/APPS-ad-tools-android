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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.voodoo.apps.ads.MockData
import io.voodoo.apps.ads.api.model.Ad
import io.voodoo.apps.ads.compose.lazylist.DefaultScrollAdBehaviorEffect
import io.voodoo.apps.ads.compose.lazylist.LazyListAdMediator
import io.voodoo.apps.ads.compose.lazylist.items
import io.voodoo.apps.ads.compose.lazylist.rememberLazyListAdMediator
import io.voodoo.apps.ads.compose.model.AdClientArbitrageurHolder
import io.voodoo.apps.ads.feature.feed.component.FeedAdItem
import io.voodoo.apps.ads.feature.feed.component.FeedErrorState
import io.voodoo.apps.ads.feature.feed.component.FeedItem
import io.voodoo.apps.ads.feature.feed.component.FeedTopAppBar
import java.util.Date
import java.util.concurrent.TimeUnit

@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    adClientArbitrageur: AdClientArbitrageurHolder?,
    onNavigateToMediationDebugger: () -> Unit,
    onNavigateToPrivacyEdit: () -> Unit,
    onNavigateToProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val adMediator = rememberLazyListAdMediator(
        lazyListState = listState,
        adClientArbitrageur = adClientArbitrageur,
        adInterval = 3,
    )
    ClearRenderedAdsWhenFirstCellVisible(
        listState = listState,
        adMediator = adMediator,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            FeedTopAppBar(
                profilePictureUrl = MockData.PROFILE_PICTURE,
                onPrivacyEditClick = onNavigateToPrivacyEdit,
                onMediationDebuggerClick = onNavigateToMediationDebugger,
                onProfilePictureClick = onNavigateToProfileClick,
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
private fun ClearRenderedAdsWhenFirstCellVisible(
    adMediator: LazyListAdMediator,
    listState: LazyListState,
) {
    val firstVisibleItemIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    var lastVisibleIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(firstVisibleItemIndex) {
        if (lastVisibleIndex != firstVisibleItemIndex && firstVisibleItemIndex == 0) {
            val olderThan = Date(System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(30))

            adMediator.destroyAdsIf {
                it.rendered && it.isOlderThan(olderThan)
            }
        }
        lastVisibleIndex = firstVisibleItemIndex
    }
}

private fun Ad.isOlderThan(olderThan: Date?): Boolean {
    return if (olderThan == null) {
        true
    } else {
        this.loadedAt <= olderThan
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
