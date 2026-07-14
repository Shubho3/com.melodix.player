package com.melodix.player.repo

import com.melodix.player.core.theme.AppTheme
import com.melodix.player.model.CustomThemeColors
import com.melodix.player.model.SortSpec
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getTheme(): Flow<AppTheme>
    suspend fun saveTheme(theme: AppTheme)
    fun isOnboardingComplete(): Flow<Boolean>
    suspend fun setOnboardingComplete()
    fun getLossless(): Flow<Boolean>
    suspend fun setLossless(enabled: Boolean)

    /** Minimum song length in seconds; 0 = no minimum. */
    fun getMinDurationSec(): Flow<Int>
    suspend fun setMinDurationSec(seconds: Int)

    /** Folder (bucket) ids to hide from the library; empty = all folders shown. */
    fun getExcludedFolderIds(): Flow<Set<Long>>
    suspend fun setFolderExcluded(folderId: Long, excluded: Boolean)

    /** Persisted Library sort selection. */
    fun getSortSpec(): Flow<SortSpec>
    suspend fun setSortSpec(spec: SortSpec)

    /** User-defined custom theme colors; null until set. */
    fun getCustomThemeColors(): Flow<CustomThemeColors?>
    suspend fun setCustomThemeColors(colors: CustomThemeColors)
}
