package com.melodix.player.repo.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.melodix.player.model.DriveFile
import com.melodix.player.model.Track
import com.melodix.player.worker.DownloadWorker
import com.melodix.player.worker.UploadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** App-scoped owner of the download/upload transfer queue and its live progress. */
class TransferCenter(context: Context) {

    private val workManager = WorkManager.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private data class Entry(val displayName: String, val direction: Direction, val input: Data)
    private val tracked = MutableStateFlow<Map<String, Entry>>(emptyMap())

    // Last-known status per unique work name. Mutated ONLY inside the single collector coroutine
    // below, so it needs no synchronization. Persisting it means a finished item does not regress
    // to QUEUED once WorkManager prunes its WorkInfo (which would otherwise resurrect the snackbar).
    private val statusCache = mutableMapOf<String, TransferReducer.WorkStatus>()

    private val _snapshot = MutableStateFlow(TransferSnapshot())
    val snapshot: StateFlow<TransferSnapshot> = _snapshot.asStateFlow()

    init {
        val downloads = workManager.getWorkInfosByTagFlow(TAG_DOWNLOAD)
        val uploads = workManager.getWorkInfosByTagFlow(TAG_UPLOAD)
        scope.launch {
            combine(tracked, downloads, uploads) { entries, dl, ul ->
                // A retried item can briefly expose both the old CANCELLED WorkInfo and the new one
                // under the same unique-name tag. Group by name and keep the most-live state so the
                // row doesn't show FAILED while the retry is actually running/queued.
                (dl + ul)
                    .mapNotNull { wi -> wi.toUniqueName()?.let { it to wi } }
                    .groupBy({ it.first }, { it.second })
                    .forEach { (name, infos) ->
                        val best = infos.maxByOrNull { liveness(it.state) } ?: return@forEach
                        statusCache[name] = best.toStatus()
                    }
                val trackedList = entries.map { (name, e) ->
                    TransferReducer.Tracked(name, e.displayName, e.direction)
                }
                TransferSnapshot(TransferReducer.reduce(trackedList, statusCache.toMap()))
            }.collect { snap -> _snapshot.update { snap } }
        }
    }

    fun enqueueDownloads(files: List<DriveFile>) {
        resetIfIdle()
        files.forEach { file ->
            val unique = "download_${file.id}"
            val input = workDataOf(
                DownloadWorker.KEY_FILE_ID to file.id,
                DownloadWorker.KEY_NAME to file.name,
                DownloadWorker.KEY_MIME to file.mimeType,
            )
            track(unique, Entry(file.name, Direction.DOWNLOAD, input))
            enqueue(unique, TAG_DOWNLOAD, OneTimeWorkRequestBuilder<DownloadWorker>().setInputData(input))
        }
    }

    fun enqueueUploads(tracks: List<Track>, folderId: String) {
        resetIfIdle()
        tracks.forEach { t ->
            val unique = "upload_${t.id}"
            val name = "${t.title}.mp3"
            val input = workDataOf(
                UploadWorker.KEY_URI to t.uri.toString(),
                UploadWorker.KEY_NAME to name,
                UploadWorker.KEY_MIME to "audio/mpeg",
                UploadWorker.KEY_FOLDER_ID to folderId,
                UploadWorker.KEY_SIZE to t.sizeBytes,
            )
            track(unique, Entry(name, Direction.UPLOAD, input))
            enqueue(unique, TAG_UPLOAD, OneTimeWorkRequestBuilder<UploadWorker>().setInputData(input))
        }
    }

    fun retry(uniqueName: String) {
        val entry = tracked.value[uniqueName] ?: return
        val tag = if (entry.direction == Direction.DOWNLOAD) TAG_DOWNLOAD else TAG_UPLOAD
        val builder = if (entry.direction == Direction.DOWNLOAD) {
            OneTimeWorkRequestBuilder<DownloadWorker>()
        } else {
            OneTimeWorkRequestBuilder<UploadWorker>()
        }.setInputData(entry.input)
        val request = builder.addTag(tag).addTag(uniqueName)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        workManager.enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, request)
    }

    /** When nothing is in flight, start each new user-triggered batch from a clean list. */
    private fun resetIfIdle() {
        if (!_snapshot.value.isActive) tracked.update { emptyMap() }
    }

    private fun track(unique: String, entry: Entry) {
        tracked.update { it + (unique to entry) }
    }

    private fun enqueue(unique: String, tag: String, builder: androidx.work.OneTimeWorkRequest.Builder) {
        val request = builder.addTag(tag).addTag(unique)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        workManager.enqueueUniqueWork(unique, ExistingWorkPolicy.KEEP, request)
    }

    private fun WorkInfo.toUniqueName(): String? =
        tags.firstOrNull { it.startsWith("download_") || it.startsWith("upload_") }

    private fun WorkInfo.toStatus(): TransferReducer.WorkStatus = when (state) {
        WorkInfo.State.RUNNING -> TransferReducer.WorkStatus(TransferItemState.RUNNING, progress.getInt("progress", 0))
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> TransferReducer.WorkStatus(TransferItemState.QUEUED, 0)
        WorkInfo.State.SUCCEEDED -> TransferReducer.WorkStatus(TransferItemState.DONE, 100)
        WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> TransferReducer.WorkStatus(TransferItemState.FAILED, 0)
    }

    private fun liveness(state: WorkInfo.State): Int = when (state) {
        WorkInfo.State.RUNNING -> 4
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> 3
        WorkInfo.State.SUCCEEDED -> 2
        WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> 1
    }

    companion object {
        const val TAG_DOWNLOAD = "melodix_download"
        const val TAG_UPLOAD = "melodix_upload"
    }
}
