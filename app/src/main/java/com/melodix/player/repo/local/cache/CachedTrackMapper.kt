package com.melodix.player.repo.local.cache

import android.net.Uri
import com.melodix.player.model.CachedTrack
import com.melodix.player.model.Track
import java.io.File
import kotlin.math.absoluteValue

/**
 * Presents a cached Drive file as a playable [Track] backed by its local file and extracted metadata.
 * Uses a negative id so it never collides with MediaStore ids (which are positive).
 */
fun CachedTrack.toTrack(): Track = Track(
    id = -(driveFileId.hashCode().toLong().absoluteValue) - 1L,
    title = title,
    artist = artist,
    album = "Drive",
    albumId = -1L,
    duration = durationMs,
    uri = Uri.fromFile(File(localPath)),
    albumArtUri = artPath?.let { Uri.fromFile(File(it)) },
    dateAdded = downloadedAt / 1000,
    folderId = -1L,
    folderName = "Drive",
)
