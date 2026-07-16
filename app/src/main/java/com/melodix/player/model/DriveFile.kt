package com.melodix.player.model

import kotlinx.serialization.Serializable

/** A file or folder returned by the Google Drive REST API. */
@Serializable
data class DriveFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: String? = null,          // Drive returns size as a string; null for folders
    val modifiedTime: String? = null,
    val md5Checksum: String? = null,   // Drive-computed MD5 of the file bytes; null for folders
) {
    val isFolder: Boolean get() = mimeType == "application/vnd.google-apps.folder"
    val sizeBytes: Long get() = size?.toLongOrNull() ?: 0L
}
