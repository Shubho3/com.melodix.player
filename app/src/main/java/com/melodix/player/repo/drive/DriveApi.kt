package com.melodix.player.repo.drive

import com.melodix.player.model.DriveFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class DriveHttpException(val code: Int, message: String) : Exception("Drive HTTP $code: $message")

@Serializable
private data class FileListResponse(
    val files: List<DriveFile> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
private data class FileMetadata(val name: String, val parents: List<String>)

/** Thin Google Drive REST v3 client over OkHttp. All calls take a bearer access token. */
class DriveApi(
    private val client: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    /** Runs files.list for [query], following pagination. Throws [DriveHttpException] (e.g. 401) on error. */
    suspend fun listFiles(accessToken: String, query: String): List<DriveFile> =
        withContext(Dispatchers.IO) {
            val all = mutableListOf<DriveFile>()
            var pageToken: String? = null
            do {
                val url = FILES_URL.toHttpUrl().newBuilder()
                    .addQueryParameter("q", query)
                    .addQueryParameter("fields", "nextPageToken,files(id,name,mimeType,size,modifiedTime,md5Checksum)")
                    .addQueryParameter("orderBy", "folder,name")
                    .addQueryParameter("pageSize", "200")
                    .addQueryParameter("spaces", "drive")
                    .apply { pageToken?.let { addQueryParameter("pageToken", it) } }
                    .build()
                val request = Request.Builder().url(url).bearer(accessToken).get().build()
                client.newCall(request).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) throw DriveHttpException(resp.code, body)
                    val parsed = json.decodeFromString<FileListResponse>(body)
                    all += parsed.files
                    pageToken = parsed.nextPageToken
                }
            } while (pageToken != null)
            all
        }

    /** Streams a Drive file's bytes to [dest]. */
    suspend fun downloadFile(
        accessToken: String,
        fileId: String,
        dest: File,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> },
    ) = withContext(Dispatchers.IO) {
        val url = "$FILES_URL/$fileId".toHttpUrl().newBuilder()
            .addQueryParameter("alt", "media")
            .build()
        val request = Request.Builder().url(url).bearer(accessToken).get().build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw DriveHttpException(resp.code, resp.message)
            val total = resp.body!!.contentLength()
            dest.outputStream().use { out ->
                resp.body!!.byteStream().use { input ->
                    val buffer = ByteArray(1 shl 16)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                }
            }
        }
        Unit
    }

    /** Uploads a new file named [name] into [folderId], streaming from [openStream]. Requires a write scope. */
    suspend fun uploadFile(
        accessToken: String,
        folderId: String,
        name: String,
        mimeType: String,
        contentLength: Long,
        openStream: () -> java.io.InputStream,
        onProgress: (uploaded: Long, total: Long) -> Unit = { _, _ -> },
    ): DriveFile = withContext(Dispatchers.IO) {
        val metadata = json.encodeToString(FileMetadata(name = name, parents = listOf(folderId)))
        val body = MultipartBody.Builder().setType("multipart/related".toMediaType())
            .addPart(metadata.toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .addPart(ProgressRequestBody(mimeType.toMediaType(), contentLength, openStream, onProgress))
            .build()
        val url = UPLOAD_URL.toHttpUrl().newBuilder()
            .addQueryParameter("uploadType", "multipart")
            .addQueryParameter("fields", "id,name,mimeType,size")
            .build()
        val request = Request.Builder().url(url).bearer(accessToken).post(body).build()
        client.newCall(request).execute().use { resp ->
            val respBody = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw DriveHttpException(resp.code, respBody)
            json.decodeFromString<DriveFile>(respBody)
        }
    }

    private fun Request.Builder.bearer(token: String) = header("Authorization", "Bearer $token")

    companion object {
        const val FILES_URL = "https://www.googleapis.com/drive/v3/files"
        const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
    }
}
