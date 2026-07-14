package com.melodix.player.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.melodix.player.R
import com.melodix.player.core.components.AlbumCard
import com.melodix.player.core.components.TrackListItem
import com.melodix.player.core.components.TrackOptionsSheet
import com.melodix.player.model.Album
import com.melodix.player.model.Artist
import com.melodix.player.model.SortKey
import com.melodix.player.model.SortSpec
import com.melodix.player.model.Track
import com.melodix.player.viewmodel.LibraryTab
import com.melodix.player.viewmodel.LibraryViewModel
import com.melodix.player.viewmodel.PlaylistViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun LibraryScreen(
    onTrackClick: (Track, List<Track>) -> Unit,
    onAddToQueue: (Track) -> Unit = {},
    onOpenAlbum: (Long) -> Unit = {},
    onOpenArtist: (Long) -> Unit = {},
    onOpenPlaylist: (String) -> Unit = {},
    onOpenLikedSongs: () -> Unit = {},
    viewModel: LibraryViewModel = koinViewModel(),
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

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 12.dp, top = 28.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.library_title),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            if (state.selectedTab == LibraryTab.SONGS && state.tracks.isNotEmpty()) {
                SortButton(
                    sortSpec = state.sortSpec,
                    onSortChange = { viewModel.setSortSpec(it) },
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val tabs = LibraryTab.entries
            items(tabs.size) { index ->
                val tab = tabs[index]
                FilterChip(
                    selected = state.selectedTab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    label = {
                        Text(
                            text = when (tab) {
                                LibraryTab.SONGS -> stringResource(R.string.library_songs)
                                LibraryTab.ALBUMS -> stringResource(R.string.library_albums)
                                LibraryTab.ARTISTS -> stringResource(R.string.library_artists)
                                LibraryTab.PLAYLISTS -> stringResource(R.string.library_playlists)
                            },
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = when (tab) {
                                LibraryTab.SONGS -> Icons.Rounded.MusicNote
                                LibraryTab.ALBUMS -> Icons.Rounded.Album
                                LibraryTab.ARTISTS -> Icons.Rounded.Person
                                LibraryTab.PLAYLISTS -> Icons.AutoMirrored.Rounded.PlaylistPlay
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    shape = RoundedCornerShape(50),
                )
            }
        }

        when (state.selectedTab) {
            LibraryTab.SONGS -> SongsTab(
                tracks = state.tracks,
                onTrackClick = { track -> onTrackClick(track, state.tracks) },
                onMoreClick = { selectedTrack = it },
            )
            LibraryTab.ALBUMS -> AlbumsTab(state.albums, onOpenAlbum)
            LibraryTab.ARTISTS -> ArtistsTab(state.artists, onOpenArtist)
            LibraryTab.PLAYLISTS -> PlaylistsTab(onOpenPlaylist, onOpenLikedSongs)
        }
    }
}

private fun sortKeyLabel(key: SortKey): String = when (key) {
    SortKey.TITLE -> "Title"
    SortKey.DATE_ADDED -> "Date added"
    SortKey.ARTIST -> "Artist"
    SortKey.DURATION -> "Duration"
}

@Composable
private fun SortButton(sortSpec: SortSpec, onSortChange: (SortSpec) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Rounded.SwapVert,
                contentDescription = "Sort",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text(
                text = "Sort by",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            SortKey.entries.forEach { key ->
                DropdownMenuItem(
                    text = { Text(sortKeyLabel(key)) },
                    onClick = {
                        onSortChange(sortSpec.copy(key = key))
                        expanded = false
                    },
                    trailingIcon = {
                        if (key == sortSpec.key) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            DropdownMenuItem(
                text = { Text("Ascending") },
                onClick = {
                    onSortChange(sortSpec.copy(ascending = true))
                    expanded = false
                },
                trailingIcon = {
                    if (sortSpec.ascending) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
            )
            DropdownMenuItem(
                text = { Text("Descending") },
                onClick = {
                    onSortChange(sortSpec.copy(ascending = false))
                    expanded = false
                },
                trailingIcon = {
                    if (!sortSpec.ascending) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
            )
        }
    }
}

@Composable
private fun SongsTab(tracks: List<Track>, onTrackClick: (Track) -> Unit, onMoreClick: (Track) -> Unit) {
    if (tracks.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.LibraryMusic,
            headline = stringResource(R.string.library_empty_songs),
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(tracks, key = { it.id }) { track ->
            TrackListItem(
                track = track,
                isPlaying = false,
                onClick = { onTrackClick(track) },
                onMoreClick = { onMoreClick(track) },
                showDuration = true,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

@Composable
private fun AlbumsTab(albums: List<Album>, onOpenAlbum: (Long) -> Unit) {
    if (albums.isEmpty()) {
        EmptyState(icon = Icons.Rounded.Album, headline = stringResource(R.string.library_empty_albums))
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(albums, key = { it.id }) { album ->
            AlbumCard(
                title = album.name,
                subtitle = "${album.artist} · ${album.trackCount} tracks",
                onClick = { onOpenAlbum(album.id) },
                artUri = album.albumArtUri,
            )
        }
    }
}

@Composable
private fun ArtistsTab(artists: List<Artist>, onOpenArtist: (Long) -> Unit) {
    if (artists.isEmpty()) {
        EmptyState(icon = Icons.Rounded.Person, headline = stringResource(R.string.library_empty_artists))
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(artists, key = { it.id }) { artist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenArtist(artist.id) }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 1.dp,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${artist.trackCount} tracks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistsTab(
    onOpenPlaylist: (String) -> Unit,
    onOpenLikedSongs: () -> Unit,
    viewModel: PlaylistViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                viewModel.createPlaylist(name)
                showCreateDialog = false
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            FilledTonalButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.library_create_playlist))
            }
        }

        LikedSongsRow(onClick = onOpenLikedSongs)

        if (state.playlists.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                headline = stringResource(R.string.library_empty_playlists),
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(state.playlists, key = { it.id }) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPlaylist(playlist.id) }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${playlist.trackIds.size} tracks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = { viewModel.deletePlaylist(playlist.id) },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LikedSongsRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = "Liked Songs",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text(stringResource(R.string.library_new_playlist)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Playlist name") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.library_create_playlist))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    headline: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = headline,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
