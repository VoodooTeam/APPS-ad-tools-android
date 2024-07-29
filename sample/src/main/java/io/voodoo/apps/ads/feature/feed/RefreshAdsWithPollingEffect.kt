package io.voodoo.apps.ads.feature.feed

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.voodoo.apps.ads.compose.lazylist.AdFetchResultEffect
import io.voodoo.apps.ads.compose.lazylist.LazyListAdMediator
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration

@Composable
internal fun LazyListAdMediator.RefreshAdsWithPollingEffect(
    delayBetweenAdsPollings: Duration,
    localExtrasProvider: () -> Array<Pair<String, Any>> = { emptyArray() },
) {
    val coroutineScope = rememberCoroutineScope()
    val currentLocalExtraProvider by rememberUpdatedState(localExtrasProvider)

    RepeatWhileActive(this) {
        Log.d("ADS", "RefreshAdsWithPollinEffect")
        // fetchAdIfNecessary will block until all launched fetch operations are finished
        // we don't want to block the collection because one ad client could be slow
        // while the other responsive, and we should keep serving and loading ads regardless
        coroutineScope.launch {
            fetchAdIfNecessary(currentLocalExtraProvider)
            checkAndInsertAvailableAds()
        }

        // Because ads are not always added in adIndices when loaded
        // We check if we should add one everytime the current index in LazyList changes
        checkAndInsertAvailableAds()
        delay(delayBetweenAdsPollings)
    }

    AdFetchResultEffect {
        checkAndInsertAvailableAds()
    }
}

@Composable
fun RepeatWhileActive(key: Any, action: suspend () -> Unit) {
    val rememberedAction by rememberUpdatedState(action)
    var isLifecycleActive by remember { mutableStateOf(false) }
    LifecycleEventEffect(event = Lifecycle.Event.ON_PAUSE) {
        isLifecycleActive = false
    }
    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        isLifecycleActive = true
    }
    LaunchedEffect(key, isLifecycleActive) {
        while (isActive && isLifecycleActive) {
            rememberedAction()
        }
    }
}