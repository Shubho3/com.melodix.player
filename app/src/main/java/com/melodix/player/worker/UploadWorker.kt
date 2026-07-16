package com.melodix.player.worker

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.melodix.player.repo.DriveRepository
import com.melodix.player.repo.drive.DriveApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Uploads one local audio file (by content URI) into the user's selected Drive folder. */
class UploadWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {

    private val driveRepository: DriveRepository by inject()
    private val driveApi: DriveApi by inject()

    override suspend fun doWork(): Result {
        val uriStr = inputData.getString(KEY_URI) ?: return Result.failure()
        val name = inputData.getString(KEY_NAME) ?: return Result.failure()
        val mimeType = inputData.getString(KEY_MIME) ?: "audio/mpeg"
        val folderId = inputData.getString(KEY_FOLDER_ID) ?: return Result.failure()
        return try {
            val token = driveRepository.accessToken().getOrThrow()
            val uri = Uri.parse(uriStr)
            val size = inputData.getLong(KEY_SIZE, -1L).let { if (it > 0) it else resolveSize(uri) }
            var lastPct = -1
            driveApi.uploadFile(
                accessToken = token,
                folderId = folderId,
                name = name,
                mimeType = mimeType,
                contentLength = size,
                openStream = { applicationContext.contentResolver.openInputStream(uri)!! },
            ) { uploaded, total ->
                val pct = if (total > 0) (uploaded * 100 / total).toInt() else 0
                if (pct != lastPct) {
                    lastPct = pct
                    setProgressAsync(androidx.work.workDataOf(KEY_PROGRESS to pct))
                }
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private fun resolveSize(uri: Uri): Long =
        runCatching {
            applicationContext.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
        }.getOrNull()?.takeIf { it > 0 } ?: -1L

    companion object {
        const val KEY_URI = "uri"
        const val KEY_NAME = "name"
        const val KEY_MIME = "mime"
        const val KEY_FOLDER_ID = "folder_id"
        const val KEY_SIZE = "size"
        const val KEY_PROGRESS = "progress"
        private const val MAX_ATTEMPTS = 3
    }
}
