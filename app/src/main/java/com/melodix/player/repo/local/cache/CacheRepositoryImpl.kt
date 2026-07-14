package com.melodix.player.repo.local.cache

import android.content.Context
import com.melodix.player.model.CachedTrack
import com.melodix.player.repo.CacheRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class CacheRepositoryImpl(
    context: Context,
    private val dao: CachedTrackDao,
) : CacheRepository {

    private val dir = File(context.filesDir, "drive_cache").apply { mkdirs() }

    override fun cacheDir(): File = dir

    override fun observeCached(): Flow<List<CachedTrack>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun cachedIds(): Set<String> = dao.cachedIds().toSet()

    override suspend fun isCached(driveFileId: String): Boolean {
        val row = dao.getById(driveFileId) ?: return false
        return File(row.localPath).exists()
    }

    override suspend fun save(track: CachedTrack) = dao.upsert(track.toEntity())

    override suspend fun remove(driveFileId: String) {
        dao.getById(driveFileId)?.let { File(it.localPath).delete() }
        dao.delete(driveFileId)
    }
}

private fun CachedTrackEntity.toDomain() = CachedTrack(
    driveFileId = driveFileId,
    name = name,
    title = title,
    artist = artist,
    durationMs = durationMs,
    localPath = localPath,
    sizeBytes = sizeBytes,
    mimeType = mimeType,
    downloadedAt = downloadedAt,
    artPath = artPath,
)

private fun CachedTrack.toEntity() = CachedTrackEntity(
    driveFileId = driveFileId,
    name = name,
    title = title,
    artist = artist,
    durationMs = durationMs,
    localPath = localPath,
    sizeBytes = sizeBytes,
    mimeType = mimeType,
    downloadedAt = downloadedAt,
    artPath = artPath,
)
