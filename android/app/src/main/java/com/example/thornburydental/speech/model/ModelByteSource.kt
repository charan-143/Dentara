package com.example.thornburydental.speech.model

import java.io.Closeable
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * An open transfer: a stream of model bytes plus what the server said about the whole resource.
 *
 * @param stream bytes starting at the requested offset.
 * @param totalBytes total size of the complete resource, or -1 when the server did not say.
 * @param resumedAt byte offset the stream actually starts at. When a resume was requested but
 *        the server ignored it, this is 0 and the caller must restart from the beginning.
 */
class ModelByteStream(
    val stream: InputStream,
    val totalBytes: Long,
    val resumedAt: Long,
    private val onClose: () -> Unit = {}
) : Closeable {
    override fun close() {
        try {
            stream.close()
        } finally {
            onClose()
        }
    }
}

/**
 * Where model bytes come from. Abstracted so the downloader can be exercised in unit tests
 * without a network, and so a clinic can point provisioning at its own mirror.
 */
interface ModelByteSource {
    /**
     * @param startByte offset to resume from; 0 for a fresh transfer.
     * @throws java.io.IOException if the transfer cannot be started.
     */
    fun open(url: String, startByte: Long): ModelByteStream
}

/** Default [ModelByteSource] over HTTPS, with byte-range resume when the server supports it. */
class HttpModelByteSource(
    private val connectTimeoutMs: Int = 20_000,
    private val readTimeoutMs: Int = 60_000
) : ModelByteSource {

    override fun open(url: String, startByte: Long): ModelByteStream {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            instanceFollowRedirects = true
            if (startByte > 0L) {
                setRequestProperty("Range", "bytes=$startByte-")
            }
        }

        val code = connection.responseCode
        if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
            connection.disconnect()
            throw java.io.IOException("Model download failed with HTTP $code")
        }

        // HTTP 206 honours the range; plain 200 means the server sent the whole file and any
        // bytes already on disk must be discarded rather than appended to.
        val resumedAt = if (code == HttpURLConnection.HTTP_PARTIAL) startByte else 0L
        val contentLength = connection.contentLengthLong
        val totalBytes = if (contentLength < 0L) -1L else contentLength + resumedAt

        return ModelByteStream(
            stream = connection.inputStream,
            totalBytes = totalBytes,
            resumedAt = resumedAt,
            onClose = { connection.disconnect() }
        )
    }
}
