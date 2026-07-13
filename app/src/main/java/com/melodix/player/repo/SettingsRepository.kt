package com.melodix.player.repo

import com.melodix.player.core.theme.AppTheme
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getTheme(): Flow<AppTheme>
    suspend fun saveTheme(theme: AppTheme)
    fun isOnboardingComplete(): Flow<Boolean>
    suspend fun setOnboardingComplete()
    fun getLossless(): Flow<Boolean>
    suspend fun setLossless(enabled: Boolean)
}
