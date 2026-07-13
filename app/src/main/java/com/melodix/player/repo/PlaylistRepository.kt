package com.melodix.player.repo

import com.melodix.player.model.Playlist
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getPlaylists(): Flow<List<Playlist>>
    suspend fun getPlaylist(id: String): Playlist?
    suspend fun createPlaylist(name: String): Playlist
    suspend fun renamePlaylist(id: String, newName: String)
    suspend fun deletePlaylist(id: String)
    suspend fun addTrackToPlaylist(playlistId: String, trackId: Long)
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: Long)
}
