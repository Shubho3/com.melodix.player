package com.melodix.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.melodix.player.model.Track
import com.melodix.player.repo.FavoritesRepository
import com.melodix.player.repo.PlayHistoryRepository
import com.melodix.player.service.PlaybackController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class NowPlayingUiState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val currentPositionMs: Long = 0L,
    val isShuffleOn: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isLiked: Boolean = false,
    val sleepTimerMinutes: Int = 0,
)

enum class RepeatMode { OFF, ALL, ONE }

class NowPlayingViewModel(
    private val playbackController: PlaybackController,
    private val favoritesRepository: FavoritesRepository,
    private val playHistoryRepository: PlayHistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NowPlayingUiState())
    val uiState: StateFlow<NowPlayingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                playbackController.state,
                favoritesRepository.getFavoriteIds(),
            ) { ps, favorites ->
                NowPlayingUiState(
                    currentTrack = ps.currentTrack,
                    isPlaying = ps.isPlaying,
                    progress = ps.progress,
                    currentPositionMs = ps.currentPositionMs,
                    isShuffleOn = ps.isShuffleOn,
                    repeatMode = ps.repeatMode.toUiRepeatMode(),
                    isLiked = ps.currentTrack?.id in favorites,
                    sleepTimerMinutes = ps.sleepTimerMinutes,
                )
            }.collect { _uiState.value = it }
        }
        // Record each distinct track that becomes current (taps, skips, and auto-advances).
        viewModelScope.launch {
            playbackController.state
                .map { it.currentTrack?.id }
                .distinctUntilChanged()
                .filterNotNull()
                .collect { playHistoryRepository.recordPlay(it) }
        }
    }

    fun playTrack(track: Track, queue: List<Track>? = null) {
        playbackController.playTrack(track, queue)
    }

    fun shufflePlay(tracks: List<Track>) {
        playbackController.shufflePlay(tracks)
    }

    fun setSleepTimer(minutes: Int) {
        playbackController.setSleepTimer(minutes)
    }

    fun togglePlayPause() {
        playbackController.togglePlayPause()
    }

    fun seekTo(progress: Float) {
        playbackController.seekTo(progress)
    }

    fun skipNext() {
        playbackController.skipNext()
    }

    fun skipPrevious() {
        playbackController.skipPrevious()
    }

    fun toggleShuffle() {
        playbackController.toggleShuffle()
    }

    fun toggleRepeat() {
        playbackController.toggleRepeat()
    }

    fun addToQueue(track: Track) {
        playbackController.addToQueue(track)
    }

    fun toggleLike() {
        val trackId = _uiState.value.currentTrack?.id ?: return
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(trackId)
        }
    }

    private fun Int.toUiRepeatMode(): RepeatMode = when (this) {
        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
        Player.REPEAT_MODE_ONE -> RepeatMode.ONE
        else -> RepeatMode.OFF
    }
}
