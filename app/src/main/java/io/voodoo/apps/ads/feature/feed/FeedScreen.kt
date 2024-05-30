package io.voodoo.apps.ads.feature.feed

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.voodoo.apps.ads.MockData
import io.voodoo.apps.ads.feature.feed.component.FeedAdItem
import io.voodoo.apps.ads.feature.feed.component.FeedErrorState
import io.voodoo.apps.ads.feature.feed.component.FeedItem
import io.voodoo.apps.ads.feature.feed.component.FeedTopAppBar

@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    adArbitrageur: FeedAdArbitrageur?,
    onNavigateToMediationDebugger: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val itemCount =
        remember { derivedStateOf { (uiState as? FeedUiState.Content)?.items?.size ?: 0 } }
    val feedState = rememberFeedState(
        adInterval = 3,
        adArbitrageur = adArbitrageur,
        itemCount = itemCount::value
    )

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
    feedState.DefaultScrollAdBehaviorEffect()

    LazyColumn(
        state = feedState.lazyListState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            count = feedState.totalItemCount,
            // Must use the index as a position, in case we add an ad at visible index
            key = { it },
            // Not mandatory and maybe not efficient, because each update would cause
            // every content type to be re-computed
            // contentType = {
            //     if (feedState.hasAdAt(it)) {
            //         "ad"
            //     } else {
            //         "item"
            //     }
            // }
        ) { index ->
            val isAd by remember(index) { derivedStateOf { feedState.hasAdAt(index) } }
            val item = if (isAd) {
                val adItem = remember(index) {
                    FeedUiState.Content.ContentItem.Ad(feedState.getAdAt(index))
                }

                if (adItem.ad != null) {
                    DisposableEffect(adItem) {
                        val ad = adItem.ad
                        onDispose { feedState.releaseAd(ad) }
                    }
                }

                adItem
            } else {
                val offset = feedState.adsCountInRange(0 until index)
                content.items.getOrNull(index + offset)
            }

            LaunchedEffect(Unit) {
                // TODO: This should be logged on crashlytics/anywhere to monitor
                //  it can happen when content changes/ad is blocked async, but shouldn't happen too often
                if (item == null) {
                    Log.wtf("Limitless", "null item at $index")
                } else if (item is FeedUiState.Content.ContentItem.Ad && item.ad == null) {
                    Log.wtf("Limitless", "null ad at $index")
                }
            }

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
                FeedAdItem(ad = item)
            }

            null -> {
                // Edge-case after a content change (before totalItemCount gets recomputed)
            }
        }
    }
}
