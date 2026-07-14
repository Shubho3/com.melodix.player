package com.melodix.player.model

/** A Drive audio file that has been downloaded locally for offline playback. */
data class CachedTrack(
    val driveFileId: String,
    val name: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val localPath: String,
    val sizeBytes: Long,
    val mimeType: String,
    val downloadedAt: Long,
    val artPath: String? = null,
)
