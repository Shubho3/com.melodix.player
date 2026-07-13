package com.melodix.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodix.player.repo.MusicRepository
import com.melodix.player.repo.PlayHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class RecentlyPlayedViewModel(
    private val musicRepository: MusicRepository,
    private val playHistoryRepository: PlayHistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState(title = "Recently Played"))
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                musicRepository.getTracks(),
                playHistoryRepository.getHistoryIds(),
            ) { tracks, ids ->
                val byId = tracks.associateBy { it.id }
                val recent = ids.mapNotNull { byId[it] }
                DetailUiState(
                    title = "Recently Played",
                    subtitle = "${recent.size} songs",
                    artUri = recent.firstOrNull()?.albumArtUri,
                    tracks = recent,
                    isLoading = false,
                )
            }.collect { _uiState.value = it }
        }
    }
}
