package io.voodoo.apps.ads.feature.profile

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.voodoo.apps.ads.api.flow.AdClientStatus
import io.voodoo.apps.ads.api.flow.getAvailableAdCountFlow
import io.voodoo.apps.ads.api.flow.getStatusFlow
import io.voodoo.apps.ads.api.model.AdAlreadyLoadingException
import io.voodoo.apps.ads.api.util.renderAsync
import io.voodoo.apps.ads.compose.model.AdClientHolder
import io.voodoo.apps.ads.feature.ads.RewardedAdClientFactory
import io.voodoo.apps.ads.util.activity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@Composable
fun ProfileRewardedAdSection(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val activity = LocalContext.current.activity
        val container = LocalView.current

        val coroutineScope = rememberCoroutineScope()
        val clientHolder = remember { AdClientHolder(RewardedAdClientFactory().create(activity)) }

        // Destroy client when leaving screen
        // In a real app, the client would be stored in an activity scope and destroyed with the activity
        DisposableEffect(clientHolder) {
            onDispose {
                clientHolder.client.close()
            }
        }

        // Infinite load retry
        // This should run in background or your app, even if the screen is not visible
        // if you want to always have an available ad
        LaunchedEffect(Unit) {
            activity.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    clientHolder
                        .getAvailableAdCountFlow()
                        .firstOrNull { it.total == 0 } ?: return@repeatOnLifecycle

                    try {
                        clientHolder.fetchAd()
                    } catch (e: AdAlreadyLoadingException) {
                        delay(1.seconds)
                    } catch (e: Exception) {
                        Log.e("ProfileScreen", "Failed to load ad", e)
                        delay(1.seconds)
                    }
                }
            }
        }

        // Collect status (loading/error/ready)
        val clientStatus by remember { clientHolder.getStatusFlow() }
            .collectAsStateWithLifecycle(AdClientStatus.LOADING)

        Text(
            text = "Rewarded ad:",
            style = MaterialTheme.typography.titleLarge
        )

        Button(
            enabled = clientStatus == AdClientStatus.READY,
            onClick = {
                coroutineScope.launch {
                    try {
                        val ad = clientHolder.client.renderAsync(container)
                        Log.e("ProfileScreen", "Ad rendered and rewarded ${ad.info.revenue}")
                    } catch (e: Exception) {
                        Log.e("ProfileScreen", "Failed to play ad", e)
                    }
                }
            },
            modifier = Modifier
                .widthIn(240.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            when (clientStatus) {
                AdClientStatus.LOADING -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }

                AdClientStatus.READY -> {
                    Text("Play ad ▶\uFE0F")
                }

                AdClientStatus.ERROR -> {
                    Text("⛔ Error ⛔")
                }
            }
        }
    }
}
