package io.voodoo.apps.ads.feature.feed

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.voodoo.apps.ads.compose.lazylist.LazyListAdMediator

@Composable
internal fun ClearWatchedAdsWhenScreenExit(
    adMediator: LazyListAdMediator,
) {
    LifecycleEventEffect(event = Lifecycle.Event.ON_PAUSE) {
        Log.d("TAG_TAG", "ClearRenderedAdsWhenScreenExit")
        adMediator.destroyAdsIf {
            it.isRevenuePaid
        }
        // do not call clear indices
    }
}
