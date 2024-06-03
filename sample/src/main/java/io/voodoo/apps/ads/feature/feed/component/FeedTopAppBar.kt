package io.voodoo.apps.ads.feature.feed.component

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage

@Composable
fun FeedTopAppBar(
    profilePictureUrl: String,
    onProfilePictureClick: () -> Unit,
    onPrivacyEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Text(
                "Limitless",
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            IconButton(
                onClick = onProfilePictureClick
            ) {
                AsyncImage(
                    model = profilePictureUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.clip(CircleShape)
                )
            }
            IconButton(onClick = onPrivacyEditClick) {
                Icon(Icons.Filled.Lock, contentDescription = "Privacy")
            }
        }
    )
}
