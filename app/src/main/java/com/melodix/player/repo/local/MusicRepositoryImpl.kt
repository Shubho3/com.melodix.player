package com.melodix.player.repo.local

import android.content.Context
import android.content.ContentUris
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.melodix.player.model.Album
import com.melodix.player.model.Artist
import com.melodix.player.model.Track
import com.melodix.player.repo.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MusicRepositoryImpl(
    private val context: Context,
) : MusicRepository {

    private val contentResolver = context.contentResolver

    private val albumArtBaseUri: Uri =
        Uri.parse("content://media/external/audio/albumart")

    // Bumping this re-runs the MediaStore queries for every active collector.
    private val refreshTrigger = MutableStateFlow(0)

    override fun getTracks(): Flow<List<Track>> =
        refreshTrigger.map { queryTracks() }.flowOn(Dispatchers.IO)

    override fun getAlbums(): Flow<List<Album>> =
        refreshTrigger.map { queryAlbums() }.flowOn(Dispatchers.IO)

    override fun getArtists(): Flow<List<Artist>> =
        refreshTrigger.map { queryArtists() }.flowOn(Dispatchers.IO)

    override fun refresh() {
        refreshTrigger.value++
    }

    override suspend fun scanForNewFiles() {
        withContext(Dispatchers.IO) {
            // Best-effort: nudge the system to index files added outside the media scanner.
            runCatching {
                val paths = listOfNotNull(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)?.absolutePath,
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absolutePath,
                ).toTypedArray()
                if (paths.isNotEmpty()) {
                    MediaScannerConnection.scanFile(context, paths, null, null)
                }
            }
        }
        refresh()
    }

    override fun getTracksByAlbum(albumId: Long): Flow<List<Track>> = flow {
        emit(queryTracks().filter { it.albumId == albumId })
    }.flowOn(Dispatchers.IO)

    override fun getTracksByArtist(artistName: String): Flow<List<Track>> = flow {
        emit(queryTracks().filter { it.artist.equals(artistName, ignoreCase = true) })
    }.flowOn(Dispatchers.IO)

    override fun getTracksByIds(ids: List<Long>): Flow<List<Track>> = flow {
        val byId = queryTracks().associateBy { it.id }
        // Preserve the requested ordering; skip ids that no longer resolve.
        emit(ids.mapNotNull { byId[it] })
    }.flowOn(Dispatchers.IO)

    override fun getAlbum(albumId: Long): Flow<Album?> = flow {
        emit(queryAlbums().find { it.id == albumId })
    }.flowOn(Dispatchers.IO)

    override fun getArtist(artistId: Long): Flow<Artist?> = flow {
        emit(queryArtists().find { it.id == artistId })
    }.flowOn(Dispatchers.IO)

    override fun searchTracks(query: String): Flow<List<Track>> = flow {
        emit(queryTracks().filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
        })
    }.flowOn(Dispatchers.IO)

    override fun searchAlbums(query: String): Flow<List<Album>> = flow {
        emit(queryAlbums().filter {
            it.name.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true)
        })
    }.flowOn(Dispatchers.IO)

    override fun searchArtists(query: String): Flow<List<Artist>> = flow {
        emit(queryArtists().filter {
            it.name.contains(query, ignoreCase = true)
        })
    }.flowOn(Dispatchers.IO)

    private fun queryTracks(): List<Track> {
        val tracks = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val albumId = cursor.getLong(albumIdCol)
                tracks.add(
                    Track(
                        id = id,
                        title = cursor.getString(titleCol) ?: "Unknown",
                        artist = cursor.getString(artistCol) ?: "Unknown Artist",
                        album = cursor.getString(albumCol) ?: "Unknown Album",
                        albumId = albumId,
                        duration = cursor.getLong(durationCol),
                        uri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id,
                        ),
                        albumArtUri = ContentUris.withAppendedId(albumArtBaseUri, albumId),
                    ),
                )
            }
        }
        return tracks
    }

    private fun queryAlbums(): List<Album> {
        val albums = mutableListOf<Album>()
        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS,
        )
        val sortOrder = "${MediaStore.Audio.Albums.ALBUM} ASC"

        contentResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val countCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                albums.add(
                    Album(
                        id = id,
                        name = cursor.getString(nameCol) ?: "Unknown Album",
                        artist = cursor.getString(artistCol) ?: "Unknown Artist",
                        trackCount = cursor.getInt(countCol),
                        albumArtUri = ContentUris.withAppendedId(albumArtBaseUri, id),
                    ),
                )
            }
        }
        return albums
    }

    private fun queryArtists(): List<Artist> {
        val artists = mutableListOf<Artist>()
        val projection = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
        )
        val sortOrder = "${MediaStore.Audio.Artists.ARTIST} ASC"

        contentResolver.query(
            MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
            val countCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)

            while (cursor.moveToNext()) {
                artists.add(
                    Artist(
                        id = cursor.getLong(idCol),
                        name = cursor.getString(nameCol) ?: "Unknown Artist",
                        trackCount = cursor.getInt(countCol),
                    ),
                )
            }
        }
        return artists
    }
}
