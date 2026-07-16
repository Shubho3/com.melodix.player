package com.melodix.player.repo.local

import com.melodix.player.repo.CacheCategory
import com.melodix.player.repo.CacheRepository
import com.melodix.player.repo.CacheType
import com.melodix.player.repo.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class StorageRepositoryImpl(
    private val context: android.content.Context,
    private val cacheRepository: CacheRepository,
) : StorageRepository {

    companion object {
        /** Coil disk cache lives at context.cacheDir/image_cache. */
        const val IMAGE_CACHE_DIR = "image_cache"
    }

    override suspend fun categories(): List<CacheCategory> = withContext(Dispatchers.IO) {
        listOf(
            CacheCategory(CacheType.DOWNLOADS, dirSize(cacheRepository.cacheDir())),
            CacheCategory(CacheType.IMAGES, dirSize(File(context.cacheDir, IMAGE_CACHE_DIR))),
            CacheCategory(CacheType.OTHER, otherBytes()),
        )
    }

    override suspend fun totalBytes(): Long = withContext(Dispatchers.IO) {
        dirSize(cacheRepository.cacheDir()) +
            dirSize(File(context.cacheDir, IMAGE_CACHE_DIR)) +
            otherBytes()
    }

    override suspend fun clear(type: CacheType): Unit = withContext(Dispatchers.IO) {
        when (type) {
            CacheType.DOWNLOADS -> cacheRepository.clear()
            CacheType.IMAGES -> {
                runCatching { coil3.SingletonImageLoader.get(context).diskCache?.clear() }
                File(context.cacheDir, IMAGE_CACHE_DIR).listFiles()?.forEach { it.deleteRecursively() }
            }
            CacheType.OTHER -> {
                context.cacheDir.listFiles()
                    ?.filter { it.name != IMAGE_CACHE_DIR }
                    ?.forEach { it.deleteRecursively() }
            }
        }
    }

    override suspend fun clearAll(): Unit = withContext(Dispatchers.IO) {
        clear(CacheType.DOWNLOADS)
        clear(CacheType.IMAGES)
        clear(CacheType.OTHER)
    }

    private fun otherBytes(): Long =
        context.cacheDir.listFiles()
            ?.filter { it.name != IMAGE_CACHE_DIR }
            ?.sumOf { dirSize(it) }
            ?: 0L

    private fun dirSize(file: File): Long {
        if (!file.exists()) return 0L
        return file.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
    }
}
