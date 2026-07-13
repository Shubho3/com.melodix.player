package com.melodix.player.model

data class Playlist(
    val id: String,
    val name: String,
    val trackIds: List<Long> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)
