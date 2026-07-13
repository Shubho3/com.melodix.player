package com.melodix.player.repo.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.melodix.player.model.Playlist
import com.melodix.player.repo.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.playlistStore: DataStore<Preferences> by preferencesDataStore("playlists")

private val KEY_PLAYLIST_DATA = stringPreferencesKey("playlist_data")

class PlaylistRepositoryImpl(
    private val context: Context,
) : PlaylistRepository {

    override fun getPlaylists(): Flow<List<Playlist>> =
        context.playlistStore.data.map { prefs ->
            deserialize(prefs[KEY_PLAYLIST_DATA])
        }

    override suspend fun getPlaylist(id: String): Playlist? {
        val all = context.playlistStore.data.first()
        return deserialize(all[KEY_PLAYLIST_DATA]).find { it.id == id }
    }

    override suspend fun createPlaylist(name: String): Playlist {
        val playlist = Playlist(id = UUID.randomUUID().toString(), name = name)
        updateAll { it + playlist }
        return playlist
    }

    override suspend fun renamePlaylist(id: String, newName: String) {
        updateAll { list -> list.map { if (it.id == id) it.copy(name = newName) else it } }
    }

    override suspend fun deletePlaylist(id: String) {
        updateAll { list -> list.filter { it.id != id } }
    }

    override suspend fun addTrackToPlaylist(playlistId: String, trackId: Long) {
        updateAll { list ->
            list.map {
                if (it.id == playlistId && trackId !in it.trackIds) {
                    it.copy(trackIds = it.trackIds + trackId)
                } else it
            }
        }
    }

    override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: Long) {
        updateAll { list ->
            list.map {
                if (it.id == playlistId) it.copy(trackIds = it.trackIds - trackId) else it
            }
        }
    }

    private suspend fun updateAll(transform: (List<Playlist>) -> List<Playlist>) {
        context.playlistStore.edit { prefs ->
            val current = deserialize(prefs[KEY_PLAYLIST_DATA])
            prefs[KEY_PLAYLIST_DATA] = serialize(transform(current))
        }
    }

    private fun serialize(playlists: List<Playlist>): String =
        playlists.joinToString(PLAYLIST_SEP) { p ->
            "${p.id}${FIELD_SEP}${p.name}${FIELD_SEP}${p.trackIds.joinToString(",")}${FIELD_SEP}${p.createdAt}"
        }

    private fun deserialize(raw: String?): List<Playlist> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(PLAYLIST_SEP).mapNotNull { entry ->
            val parts = entry.split(FIELD_SEP)
            if (parts.size < 4) return@mapNotNull null
            Playlist(
                id = parts[0],
                name = parts[1],
                trackIds = parts[2].split(",").filter { it.isNotBlank() }.mapNotNull { it.toLongOrNull() },
                createdAt = parts[3].toLongOrNull() ?: 0L,
            )
        }
    }

    companion object {
        private const val PLAYLIST_SEP = "|||"
        private const val FIELD_SEP = ":::"
    }
}
