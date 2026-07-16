package com.melodix.player.service

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.melodix.player.core.audio.AudioEffects
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PlaybackService : MediaSessionService(), KoinComponent {

    private var mediaSession: MediaSession? = null
    private val audioEffects: AudioEffects by inject()

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            // Hold only a CPU wakelock during playback (released on pause) — no network wakelock for local files.
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        // Attach Melodix's own equalizer to the player's audio session.
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                audioEffects.attach(audioSessionId)
            }
        })
        audioEffects.attach(player.audioSessionId)

        val sessionActivityIntent = packageManager
            ?.getLaunchIntentForPackage(packageName)
            ?.let { intent ->
                PendingIntent.getActivity(
                    this, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            }

        mediaSession = MediaSession.Builder(this, player)
            .apply { sessionActivityIntent?.let { setSessionActivity(it) } }
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player ?: return
        // Keep the service (and its media notification) alive whenever there's something to control —
        // even when paused — so the user can resume from the notification after swiping the app away.
        // Only tear down when the queue is empty and there's nothing left to resume.
        if (player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        audioEffects.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
