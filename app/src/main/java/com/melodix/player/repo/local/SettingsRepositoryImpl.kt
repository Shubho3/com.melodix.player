package com.melodix.player.repo.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.melodix.player.core.theme.AppTheme
import com.melodix.player.repo.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "melodix_settings")

class SettingsRepositoryImpl(
    private val context: Context,
) : SettingsRepository {

    override fun getTheme(): Flow<AppTheme> =
        context.dataStore.data.map { prefs ->
            val name = prefs[KEY_THEME] ?: AppTheme.MONO.name
            AppTheme.entries.find { it.name == name } ?: AppTheme.MONO
        }

    override suspend fun saveTheme(theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = theme.name
        }
    }

    override fun isOnboardingComplete(): Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_ONBOARDING_COMPLETE] ?: false
        }

    override suspend fun setOnboardingComplete() {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETE] = true
        }
    }

    override fun getLossless(): Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[KEY_LOSSLESS] ?: true }

    override suspend fun setLossless(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LOSSLESS] = enabled
        }
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("selected_theme")
        val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val KEY_LOSSLESS = booleanPreferencesKey("lossless_audio")
    }
}
