package com.melodix.player.repo.local.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Metadata for a Drive audio file downloaded for offline playback. */
@Entity(tableName = "cached_tracks")
data class CachedTrackEntity(
    @PrimaryKey val driveFileId: String,
    val name: String,              // original Drive filename
    val title: String,             // metadata title (falls back to filename)
    val artist: String,            // metadata artist (falls back to "Google Drive")
    val durationMs: Long,
    val localPath: String,
    val sizeBytes: Long,
    val mimeType: String,
    val downloadedAt: Long,
    val artPath: String? = null,
)
