package com.melodix.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodix.player.repo.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class AlbumDetailViewModel(
    private val musicRepository: MusicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var loaded = false

    fun load(albumId: Long) {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            combine(
                musicRepository.getAlbum(albumId),
                musicRepository.getTracksByAlbum(albumId),
            ) { album, tracks ->
                DetailUiState(
                    title = album?.name ?: "Album",
                    subtitle = album?.let { "${it.artist} · ${tracks.size} songs" }
                        ?: "${tracks.size} songs",
                    artUri = album?.albumArtUri ?: tracks.firstOrNull()?.albumArtUri,
                    tracks = tracks,
                    isLoading = false,
                )
            }.collect { _uiState.value = it }
        }
    }
}
