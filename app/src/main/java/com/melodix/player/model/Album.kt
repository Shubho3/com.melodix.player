package com.melodix.player.model

import android.net.Uri

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val trackCount: Int,
    val albumArtUri: Uri?,
)
