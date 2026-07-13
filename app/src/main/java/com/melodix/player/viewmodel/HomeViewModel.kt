package com.melodix.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodix.player.model.Track
import com.melodix.player.repo.MusicRepository
import com.melodix.player.repo.PlayHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HomeUiState(
    val recentlyPlayed: List<Track> = emptyList(),
    val quickPicks: List<Track> = emptyList(),
    val greeting: String = "",
    val isLoading: Boolean = true,
)

class HomeViewModel(
    private val musicRepository: MusicRepository,
    private val playHistoryRepository: PlayHistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(greeting = resolveGreeting())
        loadTracks()
    }

    fun refresh() {
        loadTracks()
    }

    private fun loadTracks() {
        viewModelScope.launch {
            combine(
                musicRepository.getTracks(),
                playHistoryRepository.getHistoryIds(),
            ) { tracks, historyIds ->
                val byId = tracks.associateBy { it.id }
                // Real play history first; fall back to recently-added while history is empty.
                val recent = historyIds.mapNotNull { byId[it] }.ifEmpty { tracks.take(10) }
                HomeUiState(
                    recentlyPlayed = recent.take(10),
                    quickPicks = tracks.drop(10).take(20).ifEmpty { tracks.take(20) },
                    greeting = _uiState.value.greeting,
                    isLoading = false,
                )
            }
                .catch { /* permission not granted yet — show empty */ }
                .collect { _uiState.value = it }
        }
    }

    private fun resolveGreeting(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }
}
