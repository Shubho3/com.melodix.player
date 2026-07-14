package com.melodix.player.core.audio

import android.media.audiofx.PresetReverb

/**
 * A built-in equalizer preset. [bands] are per-band gains in millibels (applied to the first N of the
 * device's equalizer bands, low→high frequency); [bassStrength] drives BassBoost (0..1000);
 * [reverbPreset] is a [PresetReverb] preset.
 */
enum class EqMode(
    val label: String,
    val bands: IntArray,
    val bassStrength: Short,
    val reverbPreset: Short,
) {
    NORMAL("Normal", intArrayOf(0, 0, 0, 0, 0), 0, PresetReverb.PRESET_NONE),
    BASS("Bass", intArrayOf(700, 400, 0, 0, 0), 600, PresetReverb.PRESET_NONE),
    EXTRA_BASS("Extra Bass", intArrayOf(1000, 700, 100, 0, 0), 900, PresetReverb.PRESET_NONE),
    BASS_BOOSTER("Bass Booster", intArrayOf(900, 600, 200, 0, 0), 1000, PresetReverb.PRESET_NONE),
    CLASSIC("Classic", intArrayOf(500, 300, 0, 300, 500), 200, PresetReverb.PRESET_NONE),
    HIP_HOP("Hip Hop", intArrayOf(800, 500, 0, 200, 400), 700, PresetReverb.PRESET_NONE),
    VOCAL("Vocals", intArrayOf(-200, 0, 500, 400, 100), 0, PresetReverb.PRESET_NONE),
    REST("Rest", intArrayOf(-100, 0, 200, -100, -300), 100, PresetReverb.PRESET_MEDIUMROOM),
    SLEEP("Sleep", intArrayOf(-300, -100, 0, -400, -600), 0, PresetReverb.PRESET_LARGEHALL),
    SOFT("Soft", intArrayOf(-100, 100, 300, 0, -200), 100, PresetReverb.PRESET_SMALLROOM),
    REVERB("Reverb", intArrayOf(0, 0, 0, 0, 0), 0, PresetReverb.PRESET_LARGEHALL),
}
