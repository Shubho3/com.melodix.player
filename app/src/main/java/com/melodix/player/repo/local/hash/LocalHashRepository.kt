package com.melodix.player.repo.local.hash

interface LocalHashRepository {
    /**
     * Returns the MD5 for [mediaStoreId], using the cached value when [sizeBytes] and
     * [dateModified] are unchanged, otherwise calling [computeMd5] and caching the result.
     * Returns null if [computeMd5] returns null.
     */
    suspend fun md5For(
        mediaStoreId: Long,
        sizeBytes: Long,
        dateModified: Long,
        computeMd5: suspend () -> String?,
    ): String?
}
