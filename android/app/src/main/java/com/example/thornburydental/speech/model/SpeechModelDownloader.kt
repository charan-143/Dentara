package com.example.thornburydental.speech.model

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.coroutines.coroutineContext

/**
 * Fetches a model file, resuming a previous partial transfer when the server allows it.
 *
 * Writes only to the caller supplied destination, which is expected to be a .part file.
 * Verification is the callers job: this class makes no claim that what it downloaded is
 * the right file, only that it transferred bytes without an I/O error.
 */
class SpeechModelDownloader(
    private val source: ModelByteSource = HttpModelByteSource(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * @param onProgress invoked with (bytesDownloaded, totalBytes) as the transfer advances.
     *        totalBytes falls back to the expected size from [spec] when the server is silent.
     * @throws IOException if the transfer fails.
     */
    suspend fun download(
        spec: SpeechModelSpec,
        destination: File,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): File = withContext(ioDispatcher) {
        val alreadyHave = if (destination.isFile) destination.length() else 0L

        // A .part longer than the finished file means it is stale or corrupt, not resumable.
        val resumeFrom = if (alreadyHave in 1 until spec.sizeBytes) alreadyHave else 0L
        if (resumeFrom == 0L && destination.exists()) {
            destination.delete()
        }

        source.open(spec.downloadUrl, resumeFrom).use { transfer ->
            val startOffset = transfer.resumedAt
            val total = if (transfer.totalBytes > 0L) transfer.totalBytes else spec.sizeBytes

            // The server may have ignored our Range header, in which case we start over.
            val append = startOffset > 0L
            if (!append && destination.exists()) {
                destination.delete()
            }

            var written = startOffset
            onProgress(written, total)

            FileOutputStream(destination, append).use { output ->
                val buffer = ByteArray(1 shl 16)
                var lastReported = written
                while (true) {
                    coroutineContext.ensureActive()

                    val read = transfer.stream.read(buffer)
                    if (read < 0) break

                    output.write(buffer, 0, read)
                    written += read

                    // Report roughly every 512 KiB rather than every chunk, to keep the UI
                    // from recomposing on every socket read.
                    if (written - lastReported >= (1L shl 19)) {
                        lastReported = written
                        onProgress(written, total)
                    }
                }
                output.flush()
                output.fd.sync()
            }

            onProgress(written, total)

            if (written < spec.sizeBytes) {
                throw IOException(
                    "Dictation model download ended early: got $written of ${spec.sizeBytes} bytes."
                )
            }
        }

        destination
    }
}
