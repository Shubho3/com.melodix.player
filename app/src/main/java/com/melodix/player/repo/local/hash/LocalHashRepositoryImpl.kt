package com.melodix.player.repo.local.hash

import com.melodix.player.repo.local.cache.LocalFileHashDao
import com.melodix.player.repo.local.cache.LocalFileHashEntity

class LocalHashRepositoryImpl(
    private val dao: LocalFileHashDao,
) : LocalHashRepository {

    override suspend fun md5For(
        mediaStoreId: Long,
        sizeBytes: Long,
        dateModified: Long,
        computeMd5: suspend () -> String?,
    ): String? {
        val cached = dao.getById(mediaStoreId)
        if (cached != null && cached.sizeBytes == sizeBytes && cached.dateModified == dateModified) {
            return cached.md5
        }
        val fresh = computeMd5() ?: return null
        dao.upsert(LocalFileHashEntity(mediaStoreId, sizeBytes, dateModified, fresh))
        return fresh
    }
}
