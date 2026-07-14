package com.melodix.player.core.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import com.melodix.player.repo.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Melodix's own equalizer — Android [Equalizer] + [BassBoost] + [PresetReverb] attached to the
 * player's audio session (not the system EQ). Applies the persisted [EqMode] and reacts to changes.
 */
class AudioEffects(settingsRepository: SettingsRepository) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var reverb: PresetReverb? = null

    @Volatile private var mode: EqMode = EqMode.NORMAL

    init {
        scope.launch {
            settingsRepository.getEqMode().collect { newMode ->
                mode = newMode
                applyPreset()
            }
        }
    }

    /** Binds the effects to the given ExoPlayer audio session and applies the current preset. */
    @Synchronized
    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0) return
        release()
        // Create each effect independently — some devices don't support all of them (e.g. reverb).
        equalizer = runCatching { Equalizer(EFFECT_PRIORITY, audioSessionId).apply { enabled = true } }.getOrNull()
        bassBoost = runCatching { BassBoost(EFFECT_PRIORITY, audioSessionId).apply { enabled = true } }.getOrNull()
        reverb = runCatching { PresetReverb(EFFECT_PRIORITY, audioSessionId).apply { enabled = true } }.getOrNull()
        applyPreset()
    }

    @Synchronized
    private fun applyPreset() {
        val m = mode
        equalizer?.let { eq ->
            runCatching {
                val range = eq.bandLevelRange
                val min = range[0]
                val max = range[1]
                for (band in 0 until eq.numberOfBands.toInt()) {
                    val gain = m.bands.getOrElse(band) { 0 }.toShort().coerceIn(min, max)
                    eq.setBandLevel(band.toShort(), gain)
                }
            }
        }
        bassBoost?.let { bb -> runCatching { if (bb.strengthSupported) bb.setStrength(m.bassStrength) } }
        reverb?.let { rv -> runCatching { rv.preset = m.reverbPreset } }
    }

    @Synchronized
    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { reverb?.release() }
        equalizer = null
        bassBoost = null
        reverb = null
    }

    private companion object {
        const val EFFECT_PRIORITY = 0
    }
}
