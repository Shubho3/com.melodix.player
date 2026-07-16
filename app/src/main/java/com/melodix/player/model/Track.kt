package com.melodix.player.model

import android.net.Uri

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val uri: Uri,
    val albumArtUri: Uri?,
    val dateAdded: Long = 0L,      // MediaStore DATE_ADDED (epoch seconds)
    val folderId: Long = -1L,      // MediaStore BUCKET_ID; -1 = unknown
    val folderName: String = "",   // MediaStore BUCKET_DISPLAY_NAME
    val sizeBytes: Long = 0L,      // MediaStore SIZE (bytes)
    val dateModified: Long = 0L,   // MediaStore DATE_MODIFIED (epoch seconds)
)
