package com.melodix.player.repo.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.melodix.player.repo.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favoritesDataStore: DataStore<Preferences> by preferencesDataStore(name = "melodix_favorites")

class FavoritesRepositoryImpl(
    private val context: Context,
) : FavoritesRepository {

    override fun getFavoriteIds(): Flow<Set<Long>> =
        context.favoritesDataStore.data.map { prefs ->
            prefs[KEY_FAVORITES].orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
        }

    override suspend fun toggleFavorite(trackId: Long) {
        context.favoritesDataStore.edit { prefs ->
            val current = prefs[KEY_FAVORITES].orEmpty().toMutableSet()
            val id = trackId.toString()
            if (!current.add(id)) current.remove(id)
            prefs[KEY_FAVORITES] = current
        }
    }

    override suspend fun setFavorite(trackId: Long, favorite: Boolean) {
        context.favoritesDataStore.edit { prefs ->
            val current = prefs[KEY_FAVORITES].orEmpty().toMutableSet()
            val id = trackId.toString()
            if (favorite) current.add(id) else current.remove(id)
            prefs[KEY_FAVORITES] = current
        }
    }

    private companion object {
        val KEY_FAVORITES = stringSetPreferencesKey("favorite_track_ids")
    }
}
