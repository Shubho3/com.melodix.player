package com.melodix.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodix.player.model.DriveFile
import com.melodix.player.model.Track
import com.melodix.player.repo.AuthRepository
import com.melodix.player.repo.CacheRepository
import com.melodix.player.repo.DriveRepository
import com.melodix.player.repo.MusicRepository
import com.melodix.player.repo.SettingsRepository
import com.melodix.player.repo.sync.SyncDiff
import com.melodix.player.repo.sync.TransferCenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CloudSyncUiState(
    val signedInEmail: String? = null,
    val notConnected: Boolean = false,        // no Drive folder chosen yet
    val isLoading: Boolean = false,
    val driveFileCount: Int = 0,
    val cachedCount: Int = 0,
    val localOnlyCount: Int = 0,
    val message: String? = null,
)

class CloudSyncViewModel(
    private val authRepository: AuthRepository,
    private val driveRepository: DriveRepository,
    private val cacheRepository: CacheRepository,
    private val musicRepository: MusicRepository,
    private val settingsRepository: SettingsRepository,
    private val transferCenter: TransferCenter,
    private val localHashRepository: com.melodix.player.repo.local.hash.LocalHashRepository,
    private val audioHasher: com.melodix.player.repo.local.hash.AudioHasher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CloudSyncUiState())
    val uiState = _uiState.asStateFlow()

    private var driveFiles: List<DriveFile> = emptyList()
    private var localOnly: List<Track> = emptyList()
    private var downloadableIds: Set<String> = emptySet()
    private var folderId: String? = null

    init {
        observe(authRepository.currentUser) { u -> copy(signedInEmail = u?.email) }
        observe(cacheRepository.observeCached()) { copy(cachedCount = it.size) }
    }

    fun refresh() {
        viewModelScope.launch {
            folderId = settingsRepository.getDriveFolderId().first()
            val id = folderId
            if (id == null) {
                _uiState.update { it.copy(notConnected = true) }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, notConnected = false, message = null) }
            driveRepository.listAudioFiles(id)
                .onSuccess { files ->
                    driveFiles = files
                    val localTracks = runCatching { musicRepository.getTracks().first() }.getOrDefault(emptyList())

                    // Size pre-filter: only hash local files whose exact byte-size matches a Drive file that has an md5.
                    val remoteSizes = files.mapNotNull { f -> f.md5Checksum?.let { f.sizeBytes } }.toSet()
                    var hashed = 0
                    val locals = localTracks.map { t ->
                        val md5 = if (t.sizeBytes in remoteSizes) {
                            hashed++
                            localHashRepository.md5For(t.id, t.sizeBytes, t.dateModified) { audioHasher.md5(t.uri) }
                        } else {
                            null
                        }
                        SyncDiff.Local(id = t.id, sizeBytes = t.sizeBytes, md5 = md5)
                    }
                    android.util.Log.d("Melodix", "SyncDiff: hashed $hashed of ${localTracks.size} local files")

                    val remotes = files.map { SyncDiff.Remote(id = it.id, sizeBytes = it.sizeBytes, md5 = it.md5Checksum) }
                    val cached = cacheRepository.cachedIds().toSet()
                    val diff = SyncDiff.compute(locals, remotes, cached)

                    localOnly = localTracks.filter { it.id in diff.localOnlyIds }
                    downloadableIds = diff.downloadableIds
                    _uiState.update {
                        it.copy(isLoading = false, driveFileCount = files.size, localOnlyCount = localOnly.size)
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, message = e.message ?: "Drive error") } }
        }
    }

    fun downloadAll() {
        val toGet = driveFiles.filter { it.id in downloadableIds }
        transferCenter.enqueueDownloads(toGet)
        _uiState.update { it.copy(message = "Queued ${toGet.size} download(s).") }
    }

    fun backupLocalOnly() {
        val id = folderId ?: return
        transferCenter.enqueueUploads(localOnly, id)
        _uiState.update { it.copy(message = "Queued ${localOnly.size} upload(s).") }
    }

    private fun <T> observe(flow: kotlinx.coroutines.flow.Flow<T>, reduce: CloudSyncUiState.(T) -> CloudSyncUiState) {
        viewModelScope.launch { flow.collect { value -> _uiState.update { it.reduce(value) } } }
    }
}
