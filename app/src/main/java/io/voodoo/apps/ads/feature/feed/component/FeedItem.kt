package io.voodoo.apps.ads.feature.feed.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

data class FeedItemUiState(
    val id: String,
    val iconUrl: String,
    val title: String,
    val subtitle: String,
    val picture: String,
)

@Composable
fun FeedItem(
    item: FeedItemUiState,
    modifier: Modifier = Modifier
) {
    FeedItem(
        modifier = modifier,
        icon = {
            AsyncImage(
                model = item.iconUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.clip(CircleShape)
            )
        },
        title = {
            Text(
                item.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        subtitle = {
            Text(
                item.subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {
            ReportActionMenu(onReportClick = {})
        },
        content = {
            AsyncImage(
                model = item.picture,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
            )
        }
    )
}

@Composable
fun FeedItem(
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    subtitle: @Composable () -> Unit,
    actions: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Header
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Box(modifier = Modifier.size(32.dp), propagateMinConstraints = true) {
                icon()
            }

            Column(modifier = Modifier.weight(1f)) {
                ProvideTextStyle(MaterialTheme.typography.titleMedium) {
                    title()
                }

                ProvideTextStyle(MaterialTheme.typography.labelSmall) {
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.secondary
                    ) {
                        subtitle()
                    }
                }
            }

            actions()
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content
        Box(
            modifier = Modifier.aspectRatio(3 / 4f),
            propagateMinConstraints = true,
        ) {
            content()
        }
    }
}
