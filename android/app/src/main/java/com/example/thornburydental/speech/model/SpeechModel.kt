package com.example.thornburydental.speech.model

/**
 * Describes one offline speech recognition model: where to fetch it, how big it should be,
 * and the digest that proves the bytes on disk are the bytes we expected.
 *
 * [sha256] is mandatory and is checked before a model is ever handed to the decoder. A
 * truncated or substituted model does not fail loudly at load time; it produces confident
 * nonsense, which in a clinical chart is indistinguishable from a real measurement.
 */
data class SpeechModelSpec(
    val id: String,
    val displayName: String,
    val fileName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val sha256: String
) {
    init {
        require(sha256.length == 64) { "sha256 for $id must be a 64-character hex digest" }
        require(sizeBytes > 0) { "sizeBytes for $id must be positive" }
    }
}

object SpeechModels {

    /**
     * Whisper base.en, q5_1 quantised, in ggml format for whisper.cpp.
     *
     * Size and digest were measured against the file served by the canonical upstream
     * repository, not copied from documentation. Re-measure if the pinned URL changes.
     *
     * Consider mirroring this on infrastructure you control: a clinic device that cannot
     * reach huggingface.co cannot provision dictation at all.
     */
    val BASE_EN_Q5_1 = SpeechModelSpec(
        id = "ggml-base.en-q5_1",
        displayName = "English dictation (base, quantised)",
        fileName = "ggml-base.en-q5_1.bin",
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en-q5_1.bin",
        sizeBytes = 59_721_011L,
        sha256 = "4baf70dd0d7c4247ba2b81fafd9c01005ac77c2f9ef064e00dcf195d0e2fdd2f"
    )

    /** The model voice charting provisions on first use. */
    val DEFAULT: SpeechModelSpec = BASE_EN_Q5_1
}

/** Provisioning status of the offline speech model. */
sealed interface SpeechModelState {

    /** Not present on the device. Dictation cannot run. */
    object Absent : SpeechModelState

    data class Downloading(val bytesDownloaded: Long, val totalBytes: Long) : SpeechModelState {
        val fraction: Float
            get() = if (totalBytes <= 0L) 0f else (bytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
    }

    /** Bytes are on disk; the digest is being checked. Not yet usable. */
    object Verifying : SpeechModelState

    /** Present and digest-verified. Safe to load. */
    data class Ready(val path: String) : SpeechModelState

    /** Provisioning failed. [reason] is shown to the clinician verbatim. */
    data class Failed(val reason: String) : SpeechModelState
}
