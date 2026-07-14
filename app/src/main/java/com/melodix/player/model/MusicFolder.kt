package com.melodix.player.model

/** A device folder (MediaStore bucket) that directly contains music, for the folder filter UI. */
data class MusicFolder(
    val id: Long,
    val name: String,
    val trackCount: Int,
)
