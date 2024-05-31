package io.voodoo.apps.ads.feature.feed

import android.util.Log
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
import io.voodoo.apps.ads.applovin.compose.list.DefaultScrollAdBehaviorEffect
import io.voodoo.apps.ads.applovin.compose.list.LazyListAdMediator
import io.voodoo.apps.ads.applovin.compose.list.rememberLazyListAdMediator
import io.voodoo.apps.ads.applovin.compose.model.AdArbitrageurHolder
import io.voodoo.apps.ads.feature.feed.component.FeedAdItem
import io.voodoo.apps.ads.feature.feed.component.FeedErrorState
import io.voodoo.apps.ads.feature.feed.component.FeedItem
import io.voodoo.apps.ads.feature.feed.component.FeedTopAppBar

@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    adArbitrageur: AdArbitrageurHolder?,
    onNavigateToMediationDebugger: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val itemCount =
        remember { derivedStateOf { (uiState as? FeedUiState.Content)?.items?.size ?: 0 } }
    val listState = rememberLazyListState()
    val lazyListAdMediator = rememberLazyListAdMediator(
        lazyListState = listState,
        adArbitrageur = adArbitrageur,
        adInterval = 3,
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
                        listState = listState,
                        adMediator = lazyListAdMediator,
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
    adMediator.DefaultScrollAdBehaviorEffect()

    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            count = adMediator.totalItemCount,
            key = { index ->
                when {
                    adMediator.hasAdAt(index) -> null
                    else -> content.items.getOrNull(adMediator.getRealIndex(index))?.id
                } ?: index
            },
            // Not mandatory and maybe not efficient, because each update would cause
            // every content type to be re-computed
            // contentType = {
            //     if (lazyListAdMediator.hasAdAt(it)) {
            //         "ad"
            //     } else {
            //         "item"
            //     }
            // }
        ) { index ->
            val isAd by remember(index) { derivedStateOf { adMediator.hasAdAt(index) } }
            val item = if (isAd) {
                val adItem = remember(index) {
                    FeedUiState.Content.ContentItem.Ad(adMediator.getAdAt(index))
                }

                if (adItem.ad != null) {
                    DisposableEffect(adItem) {
                        val ad = adItem.ad
                        onDispose { adMediator.releaseAd(ad) }
                    }
                }

                adItem
            } else {
                content.items.getOrNull(adMediator.getRealIndex(index))
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
