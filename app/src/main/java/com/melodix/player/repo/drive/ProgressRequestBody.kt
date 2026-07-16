package com.melodix.player.repo.drive

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.InputStream

/** Streams [openStream] into the request, reporting cumulative bytes via [onProgress]. */
class ProgressRequestBody(
    private val contentType: MediaType?,
    private val contentLength: Long,
    private val openStream: () -> InputStream,
    private val onProgress: (uploaded: Long, total: Long) -> Unit,
) : RequestBody() {
    override fun contentType(): MediaType? = contentType
    override fun contentLength(): Long = contentLength
    override fun writeTo(sink: BufferedSink) {
        openStream().use { input ->
            val buffer = ByteArray(1 shl 16)
            var uploaded = 0L
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                sink.write(buffer, 0, read)
                uploaded += read
                onProgress(uploaded, contentLength)
            }
        }
    }
}
