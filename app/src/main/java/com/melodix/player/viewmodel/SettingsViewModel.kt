package com.melodix.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodix.player.core.theme.AppTheme
import com.melodix.player.model.CustomThemeColors
import com.melodix.player.model.MusicFolder
import com.melodix.player.repo.MusicRepository
import com.melodix.player.repo.SettingsRepository
import com.melodix.player.repo.StorageRepository
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
    val minDurationSec: Int = 0,
    val folders: List<MusicFolder> = emptyList(),
    val excludedFolderIds: Set<Long> = emptySet(),
    val customThemeColors: CustomThemeColors? = null,
    val driveFolderId: String? = null,
    val totalCacheBytes: Long = 0L,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val musicRepository: MusicRepository,
    private val storageRepository: StorageRepository,
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
        viewModelScope.launch {
            settingsRepository.getMinDurationSec().collect {
                _uiState.value = _uiState.value.copy(minDurationSec = it)
            }
        }
        viewModelScope.launch {
            settingsRepository.getExcludedFolderIds().collect {
                _uiState.value = _uiState.value.copy(excludedFolderIds = it)
            }
        }
        viewModelScope.launch {
            musicRepository.getFolders()
                .catch { }
                .collect { _uiState.value = _uiState.value.copy(folders = it) }
        }
        viewModelScope.launch {
            settingsRepository.getCustomThemeColors().collect {
                _uiState.value = _uiState.value.copy(customThemeColors = it)
            }
        }
        viewModelScope.launch {
            settingsRepository.getDriveFolderId().collect {
                _uiState.value = _uiState.value.copy(driveFolderId = it)
            }
        }
        refreshCacheSize()
    }

    /** Recomputes the total cache size shown on the Storage row (call again after clearing caches). */
    fun refreshCacheSize() {
        viewModelScope.launch {
            val total = storageRepository.totalBytes()
            _uiState.value = _uiState.value.copy(totalCacheBytes = total)
        }
    }

    fun setMinDuration(seconds: Int) {
        viewModelScope.launch { settingsRepository.setMinDurationSec(seconds) }
    }

    fun setFolderExcluded(folderId: Long, excluded: Boolean) {
        viewModelScope.launch { settingsRepository.setFolderExcluded(folderId, excluded) }
    }

    fun saveCustomTheme(colors: CustomThemeColors) {
        viewModelScope.launch {
            settingsRepository.setCustomThemeColors(colors)
            settingsRepository.saveTheme(AppTheme.CUSTOM)
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
