package com.melodix.player.ui.nowplaying

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.melodix.player.R
import com.melodix.player.core.components.AlbumArtImage
import com.melodix.player.core.components.formatDuration
import com.melodix.player.model.Track
import com.melodix.player.viewmodel.NowPlayingViewModel
import com.melodix.player.viewmodel.RepeatMode

@Composable
fun NowPlayingScreen(
    onBack: () -> Unit,
    onOpenQueue: () -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    onOpenAlbum: (Long) -> Unit = {},
    viewModel: NowPlayingViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val track = state.currentTrack
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }

    if (showSleepDialog) {
        SleepTimerDialog(
            remainingMs = state.sleepTimerRemainingMs,
            onStart = { viewModel.setSleepTimer(it) },
            onStop = { viewModel.cancelSleepTimer() },
            onDismiss = { showSleepDialog = false },
        )
    }
    if (showDetails && track != null) {
        SongDetailsDialog(track = track, onDismiss = { showDetails = false })
    }
    if (showPlaylistSheet && track != null) {
        AddToPlaylistSheet(track = track, onDismiss = { showPlaylistSheet = false })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.now_playing_title).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 3.sp,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sleep timer — dedicated icon that tints to the accent while a timer is running.
                    IconButton(onClick = { showSleepDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Bedtime,
                            contentDescription = "Sleep timer",
                            tint = if (state.sleepTimerRemainingMs > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add to playlist") },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null)
                                },
                                enabled = track != null,
                                onClick = {
                                    showMenu = false
                                    showPlaylistSheet = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Go to album") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Album, contentDescription = null)
                                },
                                enabled = track != null,
                                onClick = {
                                    showMenu = false
                                    track?.let { onOpenAlbum(it.albumId) }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Song details") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Info, contentDescription = null)
                                },
                                enabled = track != null,
                                onClick = {
                                    showMenu = false
                                    showDetails = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Share") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Share, contentDescription = null)
                                },
                                enabled = track != null,
                                onClick = {
                                    showMenu = false
                                    track?.let { shareTrack(context, it) }
                                },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                VinylDisc(
                    artUri = track?.albumArtUri,
                    isPlaying = state.isPlaying,
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .aspectRatio(1f),
                )
            }

            Spacer(Modifier.height(28.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = track?.title ?: stringResource(R.string.now_playing_no_track),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = track?.artist ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (track != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = track.album.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        letterSpacing = 2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Slider(
                value = state.progress,
                onValueChange = { viewModel.seekTo(it) },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatDuration(state.currentPositionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatDuration(track?.duration ?: 0L),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (state.isShuffleOn) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
                IconButton(
                    onClick = { viewModel.skipPrevious() },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(34.dp),
                    )
                }
                Surface(
                    modifier = Modifier.size(68.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 6.dp,
                ) {
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }
                IconButton(
                    onClick = { viewModel.skipNext() },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(34.dp),
                    )
                }
                IconButton(onClick = { viewModel.toggleRepeat() }) {
                    Icon(
                        imageVector = when (state.repeatMode) {
                            RepeatMode.ONE -> Icons.Rounded.RepeatOne
                            else -> Icons.Rounded.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (state.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ActionButton(
                    icon = if (state.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    label = stringResource(R.string.now_playing_like),
                    isActive = state.isLiked,
                    onClick = { viewModel.toggleLike() },
                )
                ActionButton(
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    label = stringResource(R.string.now_playing_queue),
                    onClick = onOpenQueue,
                )
                ActionButton(
                    icon = Icons.Rounded.Lyrics,
                    label = stringResource(R.string.now_playing_lyrics),
                    onClick = { },
                )
                ActionButton(
                    icon = Icons.Rounded.GraphicEq,
                    label = stringResource(R.string.now_playing_eq),
                    onClick = onOpenEqualizer,
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Shares the track's audio file via the Android share sheet. */
private fun shareTrack(context: Context, track: Track) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "audio/*"
        putExtra(Intent.EXTRA_STREAM, track.uri)
        putExtra(Intent.EXTRA_TITLE, track.title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share song"))
}

@Composable
private fun VinylDisc(
    artUri: Uri?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val rotation = remember { Animatable(0f) }
    // Spin continuously while playing; pause (freeze at the current angle) otherwise.
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                rotation.animateTo(
                    targetValue = rotation.value + 360f,
                    animationSpec = tween(durationMillis = 9000, easing = LinearEasing),
                )
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(elevation = 18.dp, shape = CircleShape, clip = false)
                .rotate(rotation.value)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF2A2A2A), Color(0xFF0B0B0B)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Concentric vinyl grooves.
            Canvas(modifier = Modifier.fillMaxSize()) {
                val maxRadius = size.minDimension / 2f
                val rings = 16
                for (i in 4..rings) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = maxRadius * (i / (rings + 1f)),
                        style = Stroke(width = 1f),
                    )
                }
            }
            // Album art forms the record label at the center.
            Box(
                modifier = Modifier
                    .fillMaxSize(0.42f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                AlbumArtImage(
                    artUri = artUri,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                )
            }
        }
        // Spindle hole, fixed at the very center.
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF9E9E9E)),
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isActive: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            letterSpacing = 1.5.sp,
        )
    }
}
