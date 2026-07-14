package com.melodix.player.repo.drive

import com.melodix.player.core.auth.AuthorizeOutcome
import com.melodix.player.core.auth.DriveAuthManager
import com.melodix.player.model.DriveFile
import com.melodix.player.repo.ConsentRequiredException
import com.melodix.player.repo.DriveRepository

class DriveRepositoryImpl(
    private val authManager: DriveAuthManager,
    private val api: DriveApi,
) : DriveRepository {

    @Volatile private var token: String? = null

    override fun cacheToken(token: String) { this.token = token }

    override suspend fun accessToken(): Result<String> {
        token?.let { return Result.success(it) }
        return authManager.authorize().mapCatching { outcome ->
            when (outcome) {
                is AuthorizeOutcome.Authorized -> outcome.accessToken.also { token = it }
                is AuthorizeOutcome.NeedsConsent -> throw ConsentRequiredException(outcome.intentSender)
            }
        }
    }

    override suspend fun listFolders(parentId: String): Result<List<DriveFile>> = withToken { t ->
        api.listFiles(
            t,
            "'${parentId.esc()}' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false",
        )
    }

    override suspend fun listAudioFiles(folderId: String): Result<List<DriveFile>> = withToken { t ->
        api.listFiles(
            t,
            "'${folderId.esc()}' in parents and trashed = false and " +
                "(mimeType contains 'audio/' or name contains '.mp3' or name contains '.m4a')",
        )
    }

    /** Runs [block] with a token; on a 401 it drops the cached token, re-authorizes silently, and retries once. */
    private suspend fun <T> withToken(block: suspend (String) -> T): Result<T> = runCatching {
        val t = accessToken().getOrThrow()
        try {
            block(t)
        } catch (e: DriveHttpException) {
            if (e.code == 401) {
                token = null
                block(accessToken().getOrThrow())
            } else {
                throw e
            }
        }
    }

    private fun String.esc() = replace("\\", "\\\\").replace("'", "\\'")
}
