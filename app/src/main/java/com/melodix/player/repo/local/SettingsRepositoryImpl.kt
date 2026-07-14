package com.melodix.player.repo.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.melodix.player.core.theme.AppTheme
import com.melodix.player.model.CustomThemeColors
import com.melodix.player.model.SortKey
import com.melodix.player.model.SortSpec
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

    override fun getMinDurationSec(): Flow<Int> =
        context.dataStore.data.map { prefs -> prefs[KEY_MIN_DURATION] ?: 0 }

    override suspend fun setMinDurationSec(seconds: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_MIN_DURATION] = seconds }
    }

    override fun getExcludedFolderIds(): Flow<Set<Long>> =
        context.dataStore.data.map { prefs ->
            (prefs[KEY_EXCLUDED_FOLDERS] ?: emptySet()).mapNotNull { it.toLongOrNull() }.toSet()
        }

    override suspend fun setFolderExcluded(folderId: Long, excluded: Boolean) {
        context.dataStore.edit { prefs ->
            val current = (prefs[KEY_EXCLUDED_FOLDERS] ?: emptySet()).toMutableSet()
            if (excluded) current.add(folderId.toString()) else current.remove(folderId.toString())
            prefs[KEY_EXCLUDED_FOLDERS] = current
        }
    }

    override fun getSortSpec(): Flow<SortSpec> =
        context.dataStore.data.map { prefs ->
            val key = prefs[KEY_SORT_KEY]
                ?.let { name -> runCatching { SortKey.valueOf(name) }.getOrNull() }
                ?: SortKey.DATE_ADDED
            SortSpec(key = key, ascending = prefs[KEY_SORT_ASC] ?: false)
        }

    override suspend fun setSortSpec(spec: SortSpec) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SORT_KEY] = spec.key.name
            prefs[KEY_SORT_ASC] = spec.ascending
        }
    }

    override fun getCustomThemeColors(): Flow<CustomThemeColors?> =
        context.dataStore.data.map { prefs ->
            val accent = prefs[KEY_CUSTOM_ACCENT]
            val background = prefs[KEY_CUSTOM_BG]
            val text = prefs[KEY_CUSTOM_TEXT]
            if (accent != null && background != null && text != null) {
                CustomThemeColors(accent, background, text)
            } else {
                null
            }
        }

    override suspend fun setCustomThemeColors(colors: CustomThemeColors) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CUSTOM_ACCENT] = colors.accent
            prefs[KEY_CUSTOM_BG] = colors.background
            prefs[KEY_CUSTOM_TEXT] = colors.text
        }
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("selected_theme")
        val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val KEY_LOSSLESS = booleanPreferencesKey("lossless_audio")
        val KEY_MIN_DURATION = intPreferencesKey("min_duration_sec")
        val KEY_EXCLUDED_FOLDERS = stringSetPreferencesKey("excluded_folder_ids")
        val KEY_SORT_KEY = stringPreferencesKey("sort_key")
        val KEY_SORT_ASC = booleanPreferencesKey("sort_ascending")
        val KEY_CUSTOM_ACCENT = intPreferencesKey("custom_accent")
        val KEY_CUSTOM_BG = intPreferencesKey("custom_background")
        val KEY_CUSTOM_TEXT = intPreferencesKey("custom_text")
    }
}
