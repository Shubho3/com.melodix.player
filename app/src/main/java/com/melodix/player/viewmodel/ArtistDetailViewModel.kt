package com.melodix.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodix.player.repo.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArtistDetailViewModel(
    private val musicRepository: MusicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var loaded = false

    fun load(artistId: Long) {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            musicRepository.getArtist(artistId).collect { artist ->
                if (artist == null) {
                    _uiState.value = DetailUiState(title = "Artist", isLoading = false)
                    return@collect
                }
                musicRepository.getTracksByArtist(artist.name).collect { tracks ->
                    _uiState.value = DetailUiState(
                        title = artist.name,
                        subtitle = "${tracks.size} songs",
                        artUri = tracks.firstOrNull()?.albumArtUri,
                        tracks = tracks,
                        isLoading = false,
                    )
                }
            }
        }
    }
}
