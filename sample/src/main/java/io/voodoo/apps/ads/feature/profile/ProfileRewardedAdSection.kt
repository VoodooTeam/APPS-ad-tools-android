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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.voodoo.apps.ads.api.flow.AdClientStatus
import io.voodoo.apps.ads.api.flow.getAvailableAdCountFlow
import io.voodoo.apps.ads.api.flow.getStatusFlow
import io.voodoo.apps.ads.api.model.AdAlreadyLoadingException
import io.voodoo.apps.ads.compose.model.AdClientHolder
import io.voodoo.apps.ads.feature.ads.RewardedAdClientFactory
import io.voodoo.apps.ads.util.activity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.runningReduce
import kotlinx.coroutines.isActive
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
            while (isActive) {
                clientHolder
                    .getAvailableAdCountFlow()
                    .firstOrNull { it.total == 0 } ?: return@LaunchedEffect

                try {
                    clientHolder.fetchAd()
                } catch (e: AdAlreadyLoadingException) {
                    Log.e("ProfileScreen", "Ad already loading")
                    delay(1.seconds)
                } catch (e: Exception) {
                    Log.e("ProfileScreen", "Failed to load ad", e)
                }
            }
        }

        // Collect status (loading/error/ready)
        val clientStatus by remember {
            clientHolder.getStatusFlow()
                .runningReduce { acc, value ->
                    // Keep error state even if a loading is starting
                    if (acc == AdClientStatus.Error && value == AdClientStatus.Loading) {
                        acc
                    } else {
                        value
                    }
                }
        }.collectAsStateWithLifecycle(AdClientStatus.Loading)

        Text(
            text = "Rewarded ad:",
            style = MaterialTheme.typography.titleLarge
        )

        Button(
            enabled = clientStatus is AdClientStatus.Ready,
            onClick = {
                try {
                    val ad = checkNotNull(clientHolder.getAvailableAd())
                    ad.render(container)
                } catch (e: Exception) {
                    Log.e("ProfileScreen", "Failed to play ad")
                }
            },
            modifier = Modifier
                .widthIn(240.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            when (clientStatus) {
                AdClientStatus.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }

                AdClientStatus.Ready -> {
                    Text("Play ad ▶\uFE0F")
                }

                AdClientStatus.Error -> {
                    Text("⛔ Error ⛔")
                }
            }
        }
    }
}