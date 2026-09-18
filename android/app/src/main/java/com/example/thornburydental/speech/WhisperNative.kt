package com.example.thornburydental.speech

/**
 * Seam over the whisper.cpp JNI entry points, so [WhisperEngine] can be unit tested on the
 * JVM where no native library exists.
 */
interface WhisperNativeBridge {
    /** @return an opaque context handle, or 0 if the model could not be loaded. */
    fun nativeInit(modelPath: String): Long

    fun nativeRelease(contextPtr: Long)

    /** @return the transcript, or null if decoding failed. Null is not an empty transcript. */
    fun nativeTranscribe(
        contextPtr: Long,
        pcm: ShortArray,
        threads: Int,
        language: String?,
        prompt: String?
    ): String?
}

/**
 * The real bridge, backed by libdentara_whisper.so.
 *
 * [libraryAvailable] is false when the native library is absent, which is the normal case
 * under JVM unit tests and on an ABI we did not build for. Callers must treat that as
 * "dictation unavailable" rather than retrying.
 */
object WhisperNative : WhisperNativeBridge {

    val libraryAvailable: Boolean = try {
        System.loadLibrary("dentara_whisper")
        true
    } catch (_: Throwable) {
        false
    }

    external override fun nativeInit(modelPath: String): Long

    external override fun nativeRelease(contextPtr: Long)

    external override fun nativeTranscribe(
        contextPtr: Long,
        pcm: ShortArray,
        threads: Int,
        language: String?,
        prompt: String?
    ): String?
}
