package com.melodix.player.repo.local.hash

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

interface AudioHasher {
    /** Streams the content at [uri] and returns its lowercase hex MD5, or null if unreadable. */
    suspend fun md5(uri: Uri): String?
}

class ContentResolverAudioHasher(private val context: Context) : AudioHasher {
    override suspend fun md5(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val digest = MessageDigest.getInstance("MD5")
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(1 shl 16)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            } ?: return@runCatching null
            digest.digest().joinToString("") { "%02x".format(it) }
        }.getOrNull()
    }
}
