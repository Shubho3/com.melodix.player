package com.melodix.player.repo

enum class CacheType { DOWNLOADS, IMAGES, OTHER }
data class CacheCategory(val type: CacheType, val sizeBytes: Long)

interface StorageRepository {
    suspend fun categories(): List<CacheCategory>  // order: DOWNLOADS, IMAGES, OTHER; include zero-size entries
    suspend fun totalBytes(): Long
    suspend fun clear(type: CacheType)
    suspend fun clearAll()
}
