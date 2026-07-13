package com.melodix.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodix.player.repo.FavoritesRepository
import com.melodix.player.repo.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class LikedSongsViewModel(
    private val musicRepository: MusicRepository,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState(title = "Liked Songs"))
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                musicRepository.getTracks(),
                favoritesRepository.getFavoriteIds(),
            ) { tracks, favorites ->
                val liked = tracks.filter { it.id in favorites }
                DetailUiState(
                    title = "Liked Songs",
                    subtitle = "${liked.size} songs",
                    artUri = liked.firstOrNull()?.albumArtUri,
                    tracks = liked,
                    isLoading = false,
                )
            }.collect { _uiState.value = it }
        }
    }
}
