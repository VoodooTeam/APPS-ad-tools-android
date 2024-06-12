package io.voodoo.apps.ads.feature.profile

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.voodoo.apps.ads.compose.model.AdClientHolder
import io.voodoo.apps.ads.compose.util.AdClientStatus
import io.voodoo.apps.ads.compose.util.getAvailableAdCountFlow
import io.voodoo.apps.ads.compose.util.getStatus
import io.voodoo.apps.ads.feature.ads.RewardedAdClientFactory
import io.voodoo.apps.ads.util.activity
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onNavigateBackClick: () -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBackClick) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                title = { Text("Profile") }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            ProfileContent()
        }
    }
}

@Composable
private fun ProfileContent(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        val activity = LocalContext.current.activity
        val container = LocalView.current

        val clientHolder = remember { AdClientHolder(RewardedAdClientFactory().create(activity)) }

        // Infinite load retry
        LaunchedEffect(Unit) {
            while (isActive) {
                clientHolder
                    .getAvailableAdCountFlow()
                    .firstOrNull { it.total == 0 }

                // Note: if a fetchAd is made elsewhere, this will loop retry
                while (clientHolder.getAvailableAdCount().total == 0) {
                    runCatching { clientHolder.fetchAd() }
                }
            }
        }

        val clientStatus by clientHolder.getStatus(loadOnce = true)
            .collectAsStateWithLifecycle(AdClientStatus.Loading)

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
            modifier = Modifier.widthIn(240.dp)
        ) {
            when (clientStatus) {
                AdClientStatus.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }

                AdClientStatus.Ready -> {
                    Text("Play ad")
                }

                AdClientStatus.Error -> {
                    Text("Error")
                }
            }
        }
    }
}
