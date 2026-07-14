package com.melodix.player.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.melodix.player.R
import com.melodix.player.core.components.AlbumCard
import com.melodix.player.core.components.TrackListItem
import com.melodix.player.core.components.TrackOptionsSheet
import com.melodix.player.model.Track
import com.melodix.player.viewmodel.HomeViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    onTrackClick: (Track, List<Track>) -> Unit,
    onAddToQueue: (Track) -> Unit = {},
    onViewAllRecent: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTrack by remember { mutableStateOf<Track?>(null) }

    selectedTrack?.let { track ->
        TrackOptionsSheet(
            track = track,
            onDismiss = { selectedTrack = null },
            onAddToQueue = {
                onAddToQueue(track)
                selectedTrack = null
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Column(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 8.dp),
            ) {
                Text(
                    text = state.greeting,
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.isLoading) {
            item { ShimmerSection() }
        }

        if (state.recentlyPlayed.isNotEmpty()) {
            item { Spacer(Modifier.height(28.dp)) }

            item {
                SectionHeader(
                    title = stringResource(R.string.home_recently_played),
                    action = stringResource(R.string.home_view_all),
                    onActionClick = onViewAllRecent,
                )
            }

            item { Spacer(Modifier.height(12.dp)) }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(state.recentlyPlayed, key = { it.id }) { track ->
                        AlbumCard(
                            title = track.title,
                            subtitle = track.artist,
                            onClick = { onTrackClick(track, state.recentlyPlayed) },
                            modifier = Modifier.width(152.dp),
                            artUri = track.albumArtUri,
                        )
                    }
                }
            }
        }

        if (state.recentlyAdded.isNotEmpty()) {
            item { Spacer(Modifier.height(32.dp)) }

            item {
                Text(
                    text = stringResource(R.string.home_recently_added),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }

            item { Spacer(Modifier.height(12.dp)) }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(state.recentlyAdded, key = { it.id }) { track ->
                        AlbumCard(
                            title = track.title,
                            subtitle = track.artist,
                            onClick = { onTrackClick(track, state.recentlyAdded) },
                            modifier = Modifier.width(152.dp),
                            artUri = track.albumArtUri,
                        )
                    }
                }
            }
        }

        if (state.quickPicks.isNotEmpty()) {
            item { Spacer(Modifier.height(36.dp)) }

            item {
                Text(
                    text = stringResource(R.string.home_quick_picks),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }

            item { Spacer(Modifier.height(8.dp)) }

            items(state.quickPicks, key = { it.id }) { track ->
                TrackListItem(
                    track = track,
                    isPlaying = false,
                    onClick = { onTrackClick(track, state.quickPicks) },
                    onMoreClick = { selectedTrack = track },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }

        if (!state.isLoading && state.recentlyPlayed.isEmpty() && state.quickPicks.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.home_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: String,
    onActionClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        TextButton(onClick = onActionClick) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ShimmerSection() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offsetX by transition.animateFloat(
        initialValue = -300f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerX",
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
        ),
        start = Offset(offsetX, 0f),
        end = Offset(offsetX + 300f, 0f),
    )

    Column(modifier = Modifier.padding(24.dp)) {
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(shimmerBrush),
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(152.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(shimmerBrush),
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        repeat(4) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(shimmerBrush),
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush),
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush),
                    )
                }
            }
        }
    }
}
