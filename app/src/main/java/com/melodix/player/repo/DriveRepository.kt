package com.melodix.player.repo

import android.content.IntentSender
import com.melodix.player.model.DriveFile

/** Thrown when Drive access needs the user to grant consent; carries the intent to launch. */
class ConsentRequiredException(val intentSender: IntentSender) : Exception("Drive consent required")

interface DriveRepository {
    /** Returns a valid access token, or fails with [ConsentRequiredException] when consent is needed. */
    suspend fun accessToken(): Result<String>

    /** Records a token obtained after the user completed the consent screen. */
    fun cacheToken(token: String)

    suspend fun listFolders(parentId: String): Result<List<DriveFile>>   // "root" for the top level
    suspend fun listAudioFiles(folderId: String): Result<List<DriveFile>>
}
