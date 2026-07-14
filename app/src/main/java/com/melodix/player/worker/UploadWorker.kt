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
            // NOTE: reads the whole file into memory. Fine for typical tracks; stream for very large files.
            val bytes = applicationContext.contentResolver.openInputStream(Uri.parse(uriStr))
                ?.use { it.readBytes() } ?: return Result.failure()
            driveApi.uploadFile(token, folderId, name, mimeType, bytes)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_URI = "uri"
        const val KEY_NAME = "name"
        const val KEY_MIME = "mime"
        const val KEY_FOLDER_ID = "folder_id"
        private const val MAX_ATTEMPTS = 3
    }
}
