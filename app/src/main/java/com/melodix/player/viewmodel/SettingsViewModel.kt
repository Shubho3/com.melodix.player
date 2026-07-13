package com.melodix.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodix.player.core.theme.AppTheme
import com.melodix.player.repo.MusicRepository
import com.melodix.player.repo.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class SettingsUiState(
    val selectedTheme: AppTheme = AppTheme.MONO,
    val losslessAudio: Boolean = true,
    val songCount: Int = 0,
    val isSyncing: Boolean = false,
    val didSync: Boolean = false,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val musicRepository: MusicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.getTheme().collect {
                _uiState.value = _uiState.value.copy(selectedTheme = it)
            }
        }
        viewModelScope.launch {
            settingsRepository.getLossless().collect {
                _uiState.value = _uiState.value.copy(losslessAudio = it)
            }
        }
        viewModelScope.launch {
            musicRepository.getTracks()
                .catch { }
                .collect { _uiState.value = _uiState.value.copy(songCount = it.size) }
        }
    }

    fun syncLibrary() {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, didSync = false)
            musicRepository.scanForNewFiles()
            _uiState.value = _uiState.value.copy(isSyncing = false, didSync = true)
        }
    }

    fun selectTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsRepository.saveTheme(theme)
        }
    }

    fun toggleLossless() {
        viewModelScope.launch {
            settingsRepository.setLossless(!_uiState.value.losslessAudio)
        }
    }
}
