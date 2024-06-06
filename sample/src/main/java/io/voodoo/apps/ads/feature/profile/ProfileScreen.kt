package io.voodoo.apps.ads.feature.profile

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import io.voodoo.apps.ads.api.model.Ad
import io.voodoo.apps.ads.compose.model.AdClientHolder
import io.voodoo.apps.ads.compose.model.AdHolder
import io.voodoo.apps.ads.compose.util.getAdFetchResults
import io.voodoo.apps.ads.feature.ads.RewardedAdClientFactory
import io.voodoo.apps.ads.util.activity
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

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
        var ad by remember { mutableStateOf<AdHolder<Ad.Rewarded>?>(null) }

        // TODO WIP: real API in compose module to work with rewarded (auto fetch with retry, ...)
        LaunchedEffect(Unit) {
            snapshotFlow { ad }
                .conflate()
                .collect {
                    if (it == null) {
                        clientHolder.fetchAd()
                    }
                }
        }

        LaunchedEffect(clientHolder) {
            clientHolder.getAdFetchResults()
                .map { it.getOrNull() }
                .filterIsInstance<Ad.Rewarded>()
                .filterNotNull()
                .collect {
                    ad = it.let(::AdHolder)
                }
        }

        Button(
            enabled = ad != null,
            onClick = {
                ad?.ad?.render(container)
                ad = null
            },
            modifier = Modifier.widthIn(240.dp)
        ) {
            if (ad == null) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Play ad")
            }
        }
    }
}
