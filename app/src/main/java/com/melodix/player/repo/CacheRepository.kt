package com.melodix.player.repo

import com.melodix.player.model.CachedTrack
import kotlinx.coroutines.flow.Flow
import java.io.File

/** Offline cache of Drive audio: metadata in Room, bytes in app storage. */
interface CacheRepository {
    fun observeCached(): Flow<List<CachedTrack>>
    suspend fun cachedIds(): Set<String>
    suspend fun isCached(driveFileId: String): Boolean
    suspend fun save(track: CachedTrack)
    suspend fun remove(driveFileId: String)
    /** Deletes every cached track: removes all downloaded files and clears the metadata table. */
    suspend fun clear()
    /** Directory downloaded audio is written to. */
    fun cacheDir(): File
}
