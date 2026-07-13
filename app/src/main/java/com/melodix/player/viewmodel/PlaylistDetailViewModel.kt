package com.melodix.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodix.player.repo.MusicRepository
import com.melodix.player.repo.PlaylistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaylistDetailViewModel(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var playlistId: String? = null

    fun load(id: String) {
        if (playlistId == id) return
        playlistId = id
        viewModelScope.launch {
            // Re-emits when the playlist's tracks change (DataStore-backed).
            playlistRepository.getPlaylists().collect { playlists ->
                val playlist = playlists.find { it.id == id }
                if (playlist == null) {
                    _uiState.value = DetailUiState(title = "Playlist", isLoading = false)
                    return@collect
                }
                musicRepository.getTracksByIds(playlist.trackIds).collect { tracks ->
                    _uiState.value = DetailUiState(
                        title = playlist.name,
                        subtitle = "${tracks.size} songs",
                        artUri = tracks.firstOrNull()?.albumArtUri,
                        tracks = tracks,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun removeTrack(trackId: Long) {
        val id = playlistId ?: return
        viewModelScope.launch {
            playlistRepository.removeTrackFromPlaylist(id, trackId)
        }
    }
}
