package com.melodix.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodix.player.model.Track
import com.melodix.player.service.PlaybackController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QueueUiState(
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
)

class QueueViewModel(
    private val playbackController: PlaybackController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QueueUiState())
    val uiState: StateFlow<QueueUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            playbackController.state.collect { ps ->
                _uiState.value = QueueUiState(
                    queue = ps.queue,
                    currentIndex = ps.currentIndex,
                    isPlaying = ps.isPlaying,
                )
            }
        }
    }

    fun playFromQueue(index: Int) {
        playbackController.playFromQueue(index)
    }

    fun removeFromQueue(index: Int) {
        playbackController.removeFromQueue(index)
    }

    fun moveItem(from: Int, to: Int) {
        playbackController.moveInQueue(from, to)
    }

    fun clearQueue() {
        playbackController.clearQueue()
    }
}
