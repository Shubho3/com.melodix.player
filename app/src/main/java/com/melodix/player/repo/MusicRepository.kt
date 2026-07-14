package com.melodix.player.repo

import com.melodix.player.model.Album
import com.melodix.player.model.Artist
import com.melodix.player.model.MusicFolder
import com.melodix.player.model.Track
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    fun getTracks(): Flow<List<Track>>
    fun getAlbums(): Flow<List<Album>>
    fun getArtists(): Flow<List<Artist>>

    /** All device folders containing music, unfiltered — for the folder-filter settings UI. */
    fun getFolders(): Flow<List<MusicFolder>>
    fun getTracksByAlbum(albumId: Long): Flow<List<Track>>
    fun getTracksByArtist(artistName: String): Flow<List<Track>>
    fun getTracksByIds(ids: List<Long>): Flow<List<Track>>
    fun getAlbum(albumId: Long): Flow<Album?>
    fun getArtist(artistId: Long): Flow<Artist?>
    fun searchTracks(query: String): Flow<List<Track>>
    fun searchAlbums(query: String): Flow<List<Album>>
    fun searchArtists(query: String): Flow<List<Artist>>

    /** Re-emit all library flows (getTracks/getAlbums/getArtists) from a fresh MediaStore query. */
    fun refresh()

    /** Ask the system to index newly-added files, then refresh the library. */
    suspend fun scanForNewFiles()
}
