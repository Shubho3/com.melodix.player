package com.melodix.player.service

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.melodix.player.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffleOn: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val sleepTimerMinutes: Int = 0,
)

/**
 * Bridges the UI to the [PlaybackService]'s player. The whole queue is loaded into ExoPlayer via
 * [MediaController.setMediaItems] so that skip/shuffle/repeat, gapless playback, and the system
 * media notification's transport controls all work natively. [queue] mirrors the player's timeline
 * order 1:1 so a timeline index resolves back to its [Track].
 */
class PlaybackController(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controller: MediaController? = null
    private var progressJob: Job? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val queue = mutableListOf<Track>()
    private var sleepJob: Job? = null

    /** A play request issued before the async controller connection completed. */
    private var pendingPlay: (() -> Unit)? = null

    /** Set when the app launches before the controller connects — pause the session once connected. */
    private var pauseOnConnect = false

    init {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                val ctrl = future.get()
                ctrl.addListener(playerListener)
                controller = ctrl
                // Don't resume a leftover session on launch — playback starts only when the user asks.
                if (pauseOnConnect && pendingPlay == null && ctrl.playWhenReady) {
                    ctrl.pause()
                }
                pauseOnConnect = false
                syncFromPlayer()
                pendingPlay?.invoke()
                pendingPlay = null
            },
            // Media3 requires the controller be touched on the thread it was built on (main).
            ContextCompat.getMainExecutor(context),
        )
    }

    fun playTrack(track: Track, trackList: List<Track>? = null) {
        val ctrl = controller
        if (ctrl == null) {
            pendingPlay = { playTrack(track, trackList) }
            return
        }
        val list = when {
            trackList != null -> trackList
            queue.any { it.id == track.id } -> queue.toList()
            else -> listOf(track)
        }
        val startIndex = list.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        queue.clear()
        queue.addAll(list)

        ctrl.setMediaItems(list.map { it.toMediaItem() }, startIndex, 0L)
        ctrl.prepare()
        ctrl.play()
        syncFromPlayer(resetProgress = true)
        startProgressUpdates()
    }

    fun shufflePlay(tracks: List<Track>) {
        val ctrl = controller
        if (ctrl == null) {
            pendingPlay = { shufflePlay(tracks) }
            return
        }
        if (tracks.isEmpty()) return
        ctrl.shuffleModeEnabled = true
        playTrack(tracks[tracks.indices.random()], tracks)
    }

    fun addToQueue(track: Track) {
        val ctrl = controller ?: return
        if (queue.none { it.id == track.id }) {
            queue.add(track)
            ctrl.addMediaItem(track.toMediaItem())
            syncFromPlayer()
        }
    }

    fun removeFromQueue(index: Int) {
        val ctrl = controller ?: return
        if (index < 0 || index >= queue.size) return
        if (index == ctrl.currentMediaItemIndex) return
        queue.removeAt(index)
        ctrl.removeMediaItem(index)
        syncFromPlayer()
    }

    fun moveInQueue(from: Int, to: Int) {
        val ctrl = controller ?: return
        if (from == to) return
        if (from < 0 || from >= queue.size || to < 0 || to >= queue.size) return
        val item = queue.removeAt(from)
        queue.add(to, item)
        ctrl.moveMediaItem(from, to)
        syncFromPlayer()
    }

    fun clearQueue() {
        val ctrl = controller ?: return
        val currentIdx = ctrl.currentMediaItemIndex
        if (queue.isEmpty() || currentIdx !in queue.indices) return
        val current = queue[currentIdx]
        // Trim the timeline down to just the currently-playing item (after, then before).
        if (currentIdx + 1 < queue.size) ctrl.removeMediaItems(currentIdx + 1, queue.size)
        if (currentIdx > 0) ctrl.removeMediaItems(0, currentIdx)
        queue.clear()
        queue.add(current)
        syncFromPlayer()
    }

    fun playFromQueue(index: Int) {
        val ctrl = controller ?: return
        if (index < 0 || index >= queue.size) return
        ctrl.seekToDefaultPosition(index)
        ctrl.play()
        syncFromPlayer(resetProgress = true)
        startProgressUpdates()
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    /** Called when the app is opened fresh — never resume a session the user didn't just start. */
    fun pauseForLaunch() {
        val ctrl = controller
        if (ctrl == null) {
            pauseOnConnect = true
        } else if (pendingPlay == null) {
            ctrl.pause()
        }
    }

    fun seekTo(fraction: Float) {
        val ctrl = controller ?: return
        val duration = ctrl.duration
        if (duration > 0) {
            ctrl.seekTo((duration * fraction).toLong())
        }
    }

    fun skipNext() {
        val ctrl = controller ?: return
        if (ctrl.hasNextMediaItem()) {
            ctrl.seekToNextMediaItem()
        } else if (queue.isNotEmpty()) {
            ctrl.seekToDefaultPosition(0)
        }
        ctrl.play()
        syncFromPlayer(resetProgress = true)
    }

    fun skipPrevious() {
        val ctrl = controller ?: return
        // Media3's seekToPrevious restarts the current track if past the threshold, else goes back.
        ctrl.seekToPrevious()
        ctrl.play()
        syncFromPlayer(resetProgress = true)
    }

    fun toggleShuffle() {
        val ctrl = controller ?: return
        ctrl.shuffleModeEnabled = !ctrl.shuffleModeEnabled
        syncFromPlayer()
    }

    fun toggleRepeat() {
        val ctrl = controller ?: return
        ctrl.repeatMode = when (ctrl.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        syncFromPlayer()
    }

    /** Schedules playback to pause after [minutes]; pass 0 to cancel a running timer. */
    fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        if (minutes <= 0) {
            _state.value = _state.value.copy(sleepTimerMinutes = 0)
            return
        }
        _state.value = _state.value.copy(sleepTimerMinutes = minutes)
        sleepJob = scope.launch {
            delay(minutes * 60_000L)
            controller?.pause()
            _state.value = _state.value.copy(sleepTimerMinutes = 0)
        }
    }

    fun release() {
        progressJob?.cancel()
        sleepJob?.cancel()
        scope.cancel()
        controller?.release()
        controller = null
    }

    /** Snapshots the player's authoritative state into [_state]. */
    private fun syncFromPlayer(resetProgress: Boolean = false) {
        val ctrl = controller ?: return
        val idx = ctrl.currentMediaItemIndex
        val duration = ctrl.duration.takeIf { it > 0 } ?: _state.value.durationMs
        _state.value = _state.value.copy(
            currentTrack = queue.getOrNull(idx) ?: _state.value.currentTrack,
            isPlaying = ctrl.isPlaying,
            isShuffleOn = ctrl.shuffleModeEnabled,
            repeatMode = ctrl.repeatMode,
            queue = queue.toList(),
            currentIndex = if (idx in queue.indices) idx else -1,
            durationMs = duration,
            progress = if (resetProgress) 0f else _state.value.progress,
            currentPositionMs = if (resetProgress) 0L else _state.value.currentPositionMs,
        )
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val ctrl = controller
                if (ctrl != null && ctrl.duration > 0) {
                    val pos = ctrl.currentPosition
                    val dur = ctrl.duration
                    _state.value = _state.value.copy(
                        progress = (pos.toFloat() / dur).coerceIn(0f, 1f),
                        currentPositionMs = pos,
                        durationMs = dur,
                    )
                }
                // 500ms keeps the seekbar smooth while halving wakeups vs 250ms.
                delay(500)
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncFromPlayer()
            if (player.isPlaying) startProgressUpdates() else progressJob?.cancel()
        }
    }

    private fun Track.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(albumArtUri)
                    .build(),
            )
            .build()
}
