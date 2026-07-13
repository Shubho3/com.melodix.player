package com.melodix.player.repo.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.melodix.player.repo.PlayHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "melodix_history")

class PlayHistoryRepositoryImpl(
    private val context: Context,
) : PlayHistoryRepository {

    override fun getHistoryIds(): Flow<List<Long>> =
        context.historyDataStore.data.map { prefs ->
            prefs[KEY_HISTORY].orEmpty().parseIds()
        }

    override suspend fun recordPlay(trackId: Long) {
        context.historyDataStore.edit { prefs ->
            val current = prefs[KEY_HISTORY].orEmpty().parseIds().toMutableList()
            current.remove(trackId)          // de-dupe: move to front if already present
            current.add(0, trackId)          // most-recent first
            prefs[KEY_HISTORY] = current.take(MAX_HISTORY).joinToString(",")
        }
    }

    private fun String.parseIds(): List<Long> =
        if (isEmpty()) emptyList() else split(",").mapNotNull { it.toLongOrNull() }

    private companion object {
        const val MAX_HISTORY = 100
        val KEY_HISTORY = stringPreferencesKey("play_history_ids")
    }
}
