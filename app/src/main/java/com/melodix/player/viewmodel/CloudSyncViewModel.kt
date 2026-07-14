package com.melodix.player.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.melodix.player.model.DriveFile
import com.melodix.player.model.Track
import com.melodix.player.repo.AuthRepository
import com.melodix.player.repo.CacheRepository
import com.melodix.player.repo.DriveRepository
import com.melodix.player.repo.FavoritesRepository
import com.melodix.player.repo.MusicRepository
import com.melodix.player.repo.PlayHistoryRepository
import com.melodix.player.repo.PlaylistRepository
import com.melodix.player.repo.SettingsRepository
import com.melodix.player.repo.sync.SyncManager
import com.melodix.player.worker.DownloadWorker
import com.melodix.player.worker.UploadWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data class Synced(val at: Long) : SyncStatus
    data class Error(val message: String) : SyncStatus
}

/** Snapshot of a WorkManager queue (downloads or uploads). */
data class WorkQueue(
    val running: Int = 0,
    val queued: Int = 0,
    val done: Int = 0,
    val failed: Int = 0,
) {
    val total: Int get() = running + queued + done + failed
    val percent: Int get() = if (total == 0) 0 else done * 100 / total
    val active: Boolean get() = running > 0 || queued > 0
}

data class CloudSyncUiState(
    val signedInEmail: String? = null,
    val notConnected: Boolean = false,        // no Drive folder chosen yet
    val isLoading: Boolean = false,
    val driveFileCount: Int = 0,
    val cachedCount: Int = 0,
    val localOnlyCount: Int = 0,
    val favoritesCount: Int = 0,
    val historyCount: Int = 0,
    val playlistCount: Int = 0,
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val downloads: WorkQueue = WorkQueue(),
    val uploads: WorkQueue = WorkQueue(),
    val message: String? = null,
)

class CloudSyncViewModel(
    context: Context,
    private val authRepository: AuthRepository,
    private val driveRepository: DriveRepository,
    private val cacheRepository: CacheRepository,
    private val musicRepository: MusicRepository,
    private val settingsRepository: SettingsRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playHistoryRepository: PlayHistoryRepository,
    private val playlistRepository: PlaylistRepository,
    private val syncManager: SyncManager,
) : ViewModel() {

    private val workManager = WorkManager.getInstance(context)

    private val _uiState = MutableStateFlow(CloudSyncUiState())
    val uiState = _uiState.asStateFlow()

    private var driveFiles: List<DriveFile> = emptyList()
    private var localOnly: List<Track> = emptyList()
    private var folderId: String? = null

    init {
        observe(authRepository.currentUser) { u -> copy(signedInEmail = u?.email) }
        observe(favoritesRepository.getFavoriteIds()) { copy(favoritesCount = it.size) }
        observe(playHistoryRepository.getHistoryIds()) { copy(historyCount = it.size) }
        observe(playlistRepository.getPlaylists()) { copy(playlistCount = it.size) }
        observe(cacheRepository.observeCached()) { copy(cachedCount = it.size) }
        observe(workManager.getWorkInfosByTagFlow(TAG_DOWNLOAD)) { copy(downloads = it.toQueue()) }
        observe(workManager.getWorkInfosByTagFlow(TAG_UPLOAD)) { copy(uploads = it.toQueue()) }
    }

    /** Manual full sync of favorites/history/playlists; surfaces the real error on failure. */
    fun syncNow() {
        if (_uiState.value.syncStatus == SyncStatus.Syncing) return
        viewModelScope.launch {
            _uiState.update { it.copy(syncStatus = SyncStatus.Syncing, message = null) }
            val status = syncManager.syncNow().fold(
                onSuccess = { SyncStatus.Synced(System.currentTimeMillis()) },
                onFailure = { SyncStatus.Error(it.message ?: "Sync failed") },
            )
            _uiState.update { it.copy(syncStatus = status) }
        }
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
                    val driveNames = files.map { it.name.substringBeforeLast('.').lowercase() }.toSet()
                    localOnly = localTracks.filter { it.title.lowercase() !in driveNames }
                    _uiState.update {
                        it.copy(isLoading = false, driveFileCount = files.size, localOnlyCount = localOnly.size)
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, message = e.message ?: "Drive error") } }
        }
    }

    fun downloadAll() {
        viewModelScope.launch {
            val cached = cacheRepository.cachedIds()
            val toGet = driveFiles.filter { it.id !in cached }
            toGet.forEach { file ->
                enqueue(
                    "download_${file.id}", TAG_DOWNLOAD,
                    OneTimeWorkRequestBuilder<DownloadWorker>().setInputData(
                        workDataOf(
                            DownloadWorker.KEY_FILE_ID to file.id,
                            DownloadWorker.KEY_NAME to file.name,
                            DownloadWorker.KEY_MIME to file.mimeType,
                        ),
                    ),
                )
            }
            _uiState.update { it.copy(message = "Queued ${toGet.size} download(s).") }
        }
    }

    fun backupLocalOnly() {
        val id = folderId ?: return
        localOnly.forEach { track ->
            enqueue(
                "upload_${track.id}", TAG_UPLOAD,
                OneTimeWorkRequestBuilder<UploadWorker>().setInputData(
                    workDataOf(
                        UploadWorker.KEY_URI to track.uri.toString(),
                        UploadWorker.KEY_NAME to "${track.title}.mp3",
                        UploadWorker.KEY_MIME to "audio/mpeg",
                        UploadWorker.KEY_FOLDER_ID to id,
                    ),
                ),
            )
        }
        _uiState.update { it.copy(message = "Queued ${localOnly.size} upload(s).") }
    }

    private fun enqueue(uniqueName: String, tag: String, builder: OneTimeWorkRequest.Builder) {
        val request = builder
            .addTag(tag)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        workManager.enqueueUniqueWork(uniqueName, ExistingWorkPolicy.KEEP, request)
    }

    private fun <T> observe(flow: kotlinx.coroutines.flow.Flow<T>, reduce: CloudSyncUiState.(T) -> CloudSyncUiState) {
        viewModelScope.launch { flow.collect { value -> _uiState.update { it.reduce(value) } } }
    }

    private fun List<WorkInfo>.toQueue(): WorkQueue {
        var running = 0; var queued = 0; var done = 0; var failed = 0
        forEach {
            when (it.state) {
                WorkInfo.State.RUNNING -> running++
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> queued++
                WorkInfo.State.SUCCEEDED -> done++
                WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> failed++
            }
        }
        return WorkQueue(running, queued, done, failed)
    }

    private companion object {
        const val TAG_DOWNLOAD = "melodix_download"
        const val TAG_UPLOAD = "melodix_upload"
    }
}
