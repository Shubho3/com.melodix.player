package com.melodix.player.worker

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.melodix.player.model.CachedTrack
import com.melodix.player.repo.CacheRepository
import com.melodix.player.repo.DriveRepository
import com.melodix.player.repo.drive.DriveApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

/** Downloads one Drive audio file to local storage and records its metadata in Room. */
class DownloadWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {

    private val driveRepository: DriveRepository by inject()
    private val driveApi: DriveApi by inject()
    private val cacheRepository: CacheRepository by inject()

    override suspend fun doWork(): Result {
        val fileId = inputData.getString(KEY_FILE_ID) ?: return Result.failure()
        val name = inputData.getString(KEY_NAME) ?: fileId
        val mimeType = inputData.getString(KEY_MIME) ?: "audio/mpeg"
        return try {
            val token = driveRepository.accessToken().getOrThrow()
            val dest = File(cacheRepository.cacheDir(), fileId)
            var lastPct = -1
            driveApi.downloadFile(token, fileId, dest) { downloaded, total ->
                val pct = if (total > 0) (downloaded * 100 / total).toInt() else 0
                if (pct != lastPct) {
                    lastPct = pct
                    setProgressAsync(androidx.work.workDataOf(KEY_PROGRESS to pct))
                }
            }
            val meta = extractMeta(dest, fileId)
            cacheRepository.save(
                CachedTrack(
                    driveFileId = fileId,
                    name = name,
                    title = meta.title ?: name.substringBeforeLast('.'),
                    artist = meta.artist ?: "Google Drive",
                    durationMs = meta.durationMs,
                    localPath = dest.absolutePath,
                    sizeBytes = dest.length(),
                    mimeType = mimeType,
                    downloadedAt = System.currentTimeMillis(),
                    artPath = meta.artPath,
                ),
            )
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private data class Meta(val title: String?, val artist: String?, val durationMs: Long, val artPath: String?)

    /** Extracts title/artist/duration and embedded cover art from the downloaded file in one pass. */
    private fun extractMeta(file: File, fileId: String): Meta {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.ifBlank { null }
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.ifBlank { null }
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val artPath = retriever.embeddedPicture?.let { picture ->
                File(cacheRepository.cacheDir(), "$fileId.jpg").apply { writeBytes(picture) }.absolutePath
            }
            Meta(title, artist, duration, artPath)
        } catch (e: Exception) {
            Meta(null, null, 0L, null)
        } finally {
            runCatching { retriever.release() }
        }
    }

    companion object {
        const val KEY_FILE_ID = "file_id"
        const val KEY_NAME = "name"
        const val KEY_MIME = "mime"
        const val KEY_PROGRESS = "progress"
        private const val MAX_ATTEMPTS = 3
    }
}
