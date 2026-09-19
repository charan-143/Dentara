package com.example.thornburydental.speech.model

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/**
 * Owns the on-disk offline speech models: where they live, whether the bytes are trustworthy,
 * and fetching them on first use.
 *
 * The central rule is that [ensureAvailable] only ever returns a file whose SHA-256 matches its
 * [SpeechModelSpec]. Partial downloads live under a .part suffix and are promoted to the real
 * filename only after verification, so a killed download or a truncated transfer can never be
 * mistaken for an installed model.
 */
class SpeechModelStore(
    private val modelsDir: File,
    private val downloader: SpeechModelDownloader = SpeechModelDownloader(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val _state = MutableStateFlow<SpeechModelState>(SpeechModelState.Absent)
    val state: StateFlow<SpeechModelState> = _state.asStateFlow()

    fun modelFile(spec: SpeechModelSpec): File = File(modelsDir, spec.fileName)

    private fun partFile(spec: SpeechModelSpec): File = File(modelsDir, spec.fileName + ".part")

    /**
     * Returns the installed model file, or null when it is absent or fails verification.
     * A file that fails verification is deleted, because keeping it guarantees the same
     * failure on every later attempt.
     */
    suspend fun installedModel(spec: SpeechModelSpec): File? = withContext(ioDispatcher) {
        val file = modelFile(spec)
        if (!file.isFile) return@withContext null

        if (file.length() != spec.sizeBytes) {
            file.delete()
            return@withContext null
        }
        if (!digestMatches(file, spec.sha256)) {
            file.delete()
            return@withContext null
        }
        file
    }

    /** Refreshes [state] from disk without downloading anything. */
    suspend fun refreshState(spec: SpeechModelSpec = SpeechModels.DEFAULT): SpeechModelState {
        _state.value = SpeechModelState.Verifying
        val installed = installedModel(spec)
        val next = if (installed != null) {
            SpeechModelState.Ready(installed.absolutePath)
        } else {
            SpeechModelState.Absent
        }
        _state.value = next
        return next
    }

    /**
     * Returns a verified model file, downloading it first if needed.
     *
     * @return the verified file, or null when provisioning failed. On failure [state] carries
     *         a reason suitable for display.
     */
    suspend fun ensureAvailable(spec: SpeechModelSpec = SpeechModels.DEFAULT): File? {
        installedModel(spec)?.let {
            _state.value = SpeechModelState.Ready(it.absolutePath)
            return it
        }

        val part = partFile(spec)
        return try {
            withContext(ioDispatcher) {
                if (!modelsDir.exists() && !modelsDir.mkdirs()) {
                    throw IOException("Could not create model directory at ${modelsDir.absolutePath}")
                }
            }

            downloader.download(spec, part) { downloaded, total ->
                _state.value = SpeechModelState.Downloading(downloaded, total)
            }

            _state.value = SpeechModelState.Verifying
            coroutineContext.ensureActive()

            val verified = withContext(ioDispatcher) {
                part.length() == spec.sizeBytes && digestMatches(part, spec.sha256)
            }

            if (!verified) {
                withContext(ioDispatcher) { part.delete() }
                _state.value = SpeechModelState.Failed(
                    "Downloaded dictation model failed its integrity check and was discarded."
                )
                return null
            }

            val destination = modelFile(spec)
            val promoted = withContext(ioDispatcher) {
                destination.delete()
                part.renameTo(destination)
            }
            if (!promoted) {
                withContext(ioDispatcher) { part.delete() }
                _state.value = SpeechModelState.Failed("Could not install the dictation model on this device.")
                return null
            }

            _state.value = SpeechModelState.Ready(destination.absolutePath)
            destination
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Leave the .part file in place so a later attempt can resume it.
            _state.value = SpeechModelState.Absent
            throw e
        } catch (e: Exception) {
            _state.value = SpeechModelState.Failed(
                e.message ?: "Could not download the dictation model."
            )
            null
        }
    }

    /** Removes the installed model and any partial download. */
    suspend fun delete(spec: SpeechModelSpec = SpeechModels.DEFAULT) = withContext(ioDispatcher) {
        modelFile(spec).delete()
        partFile(spec).delete()
        _state.value = SpeechModelState.Absent
    }

    private fun digestMatches(file: File, expectedSha256: String): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1 shl 16)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        return actual.equals(expectedSha256, ignoreCase = true)
    }
}
