package com.melodix.player.viewmodel

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodix.player.model.DriveFile
import com.melodix.player.repo.ConsentRequiredException
import com.melodix.player.repo.DriveRepository
import com.melodix.player.repo.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FolderPickerUiState(
    val path: List<DriveFile> = emptyList(),   // breadcrumb; empty = My Drive root
    val folders: List<DriveFile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val currentFolderName: String get() = path.lastOrNull()?.name ?: "My Drive"
    val isAtRoot: Boolean get() = path.isEmpty()
}

class DriveFolderPickerViewModel(
    private val driveRepository: DriveRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FolderPickerUiState())
    val uiState = _uiState.asStateFlow()

    /** Emits the consent intent the UI must launch when Drive access hasn't been granted yet. */
    private val _consentRequest = MutableSharedFlow<IntentSender>(extraBufferCapacity = 1)
    val consentRequest = _consentRequest.asSharedFlow()

    fun start() {
        if (_uiState.value.folders.isEmpty() && !_uiState.value.isLoading) open("root", replacePath = emptyList())
    }

    fun onConsentToken(token: String) {
        driveRepository.cacheToken(token)
        open("root", replacePath = emptyList())
    }

    fun onConsentCancelled() =
        _uiState.update { it.copy(isLoading = false, error = "Google Drive access was not granted.") }

    fun openFolder(folder: DriveFile) = open(folder.id, pushed = folder)

    fun goUp() {
        val path = _uiState.value.path.dropLast(1)
        open(path.lastOrNull()?.id ?: "root", replacePath = path)
    }

    fun selectCurrentFolder(onSelected: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.setDriveFolderId(_uiState.value.path.lastOrNull()?.id ?: "root")
            onSelected()
        }
    }

    private fun open(folderId: String, pushed: DriveFile? = null, replacePath: List<DriveFile>? = null) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            driveRepository.listFolders(folderId)
                .onSuccess { folders ->
                    _uiState.update {
                        it.copy(
                            folders = folders,
                            path = replacePath ?: (if (pushed != null) it.path + pushed else it.path),
                            isLoading = false,
                        )
                    }
                }
                .onFailure { e ->
                    if (e is ConsentRequiredException) {
                        _consentRequest.tryEmit(e.intentSender)
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = e.message ?: "Drive error") }
                    }
                }
        }
    }
}
