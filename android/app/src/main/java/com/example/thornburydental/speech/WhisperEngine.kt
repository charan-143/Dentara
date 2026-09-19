package com.example.thornburydental.speech

import android.util.Log
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Raised when dictation is attempted without a working offline speech recognition model.
 *
 * This is deliberately a hard failure. Clinical measurements must never be inferred,
 * guessed, or synthesised - if the engine cannot transcribe the audio it was given,
 * the caller has to surface that to the clinician rather than write anything to a chart.
 */
class SpeechEngineUnavailableException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

/**
 * Offline speech-to-text for clinical dental dictation, backed by whisper.cpp running
 * entirely on the device. No audio and no transcript leaves the handset.
 *
 * Lifecycle: [initialize] with a verified model file, then [transcribeAudio] as often as
 * needed, then [release]. Decoding is serialised by a mutex because one whisper_context
 * cannot service two concurrent decodes.
 */
class WhisperEngine(
    private val bridge: WhisperNativeBridge? = if (WhisperNative.libraryAvailable) WhisperNative else null,
    private val threads: Int = defaultThreadCount(),
    private val decodeDispatcher: CoroutineDispatcher = defaultDecodeDispatcher
) {

    companion object {
        private const val TAG = "WhisperEngine"

        /**
         * Dedicated single-threaded dispatcher for native decoding to avoid competing with
         * the UI thread or coroutine pool on big cores.
         */
        val defaultDecodeDispatcher: CoroutineDispatcher by lazy {
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "dentara-whisper-decoder").apply {
                    priority = Thread.NORM_PRIORITY + 1
                }
            }.asCoroutineDispatcher()
        }

        /** Audio contract the decoder consumes: 16 kHz, 16-bit, mono. */
        const val SAMPLE_RATE = 16000

        /** whisper needs at least a second of audio to produce anything meaningful. */
        const val MIN_SAMPLES = SAMPLE_RATE

        private const val LANGUAGE = "en"

        private const val UNAVAILABLE_MESSAGE =
            "Speech recognition is not available. No offline dictation model is loaded on this device."

        /**
         * Decoder priming text. Whisper conditions on this, which improves recall of domain
         * vocabulary a general model otherwise mangles - "mesial" as "medial", "buccal" as
         * "buckle". It biases the decoder; it does not constrain it, and it cannot put words
         * into the transcript that were not spoken.
         */
        private const val PERIODONTAL_PROMPT =
            "Periodontal charting dictation. Tooth numbers, probing pocket depths in millimetres, " +
                "bleeding on probing, mesial, distal, buccal, lingual, palatal, furcation, recession, mobility."

        private const val CLINICAL_NOTES_PROMPT =
            "Dental clinical examination notes. Caries, gingivitis, periodontitis, calculus, plaque, " +
                "scaling, root planing, prophylaxis, restoration, extraction, endodontic, occlusion."

        /**
         * Clinical vocabulary normalisation applied after decoding. Maps spoken forms to
         * the canonical spelling used in the chart.
         */
        private val DENTAL_VOCABULARY = mapOf(
            "tooth" to "Tooth",
            "pocket" to "pocket",
            "depth" to "depth",
            "bleeding" to "bleeding",
            "probing" to "probing",
            "mm" to "mm",
            "millimeter" to "mm",
            "millimeters" to "mm",
            "millimetre" to "mm",
            "millimetres" to "mm",
            "mesial" to "mesial",
            "distal" to "distal",
            "buccal" to "buccal",
            "lingual" to "lingual",
            "facial" to "facial",
            "palatal" to "palatal",
            "caries" to "caries",
            "decay" to "decay",
            "gingival" to "gingival",
            "recession" to "recession",
            "periodontal" to "periodontal",
            "scaling" to "scaling",
            "root planing" to "root planing",
            "prophylaxis" to "prophylaxis"
        )

        /**
         * Mobile SoCs pair a few fast cores with several slow ones. Spreading a decode across
         * every core schedules work onto little cores and is routinely slower than using a
         * handful, so this is capped rather than set to the core count.
         */
        private fun defaultThreadCount(): Int =
            Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
    }

    private val decodeMutex = Mutex()

    @Volatile
    private var contextPtr: Long = 0L

    /** True once a model is loaded and decoding can actually happen. */
    fun isReady(): Boolean = contextPtr != 0L

    /**
     * Loads a verified model file into a native decoder context.
     *
     * @param modelFile a file whose digest has already been checked by
     *        [com.example.thornburydental.speech.model.SpeechModelStore]. Loading an
     *        unverified model risks a decoder that emits confident nonsense.
     * @return true when the engine is ready to transcribe.
     */
    suspend fun initialize(modelFile: File): Boolean = withContext(decodeDispatcher) {
        if (bridge == null) {
            safeLogW("Native whisper library is not available on this ABI; dictation disabled.")
            return@withContext false
        }
        if (!modelFile.isFile) {
            safeLogW("Model file missing at ${modelFile.absolutePath}; dictation disabled.")
            return@withContext false
        }

        decodeMutex.withLock {
            if (contextPtr != 0L) {
                bridge.nativeRelease(contextPtr)
                contextPtr = 0L
            }
            contextPtr = try {
                bridge.nativeInit(modelFile.absolutePath)
            } catch (t: Throwable) {
                safeLogE("whisper model load threw", t)
                0L
            }
        }

        if (contextPtr == 0L) {
            safeLogW("whisper failed to load model at ${modelFile.absolutePath}")
            false
        } else {
            true
        }
    }

    /**
     * Transcribes 16 kHz 16-bit mono PCM audio into clinical text.
     *
     * @return the transcript, or an empty string when nothing intelligible was said. An empty
     *         result means "nothing recognised", which the caller must distinguish from failure.
     * @throws SpeechEngineUnavailableException when no model is loaded, or decoding failed.
     */
    suspend fun transcribeAudio(
        pcmData: ShortArray,
        targetMode: DictationTargetMode = DictationTargetMode.PERIODONTAL_CHARTING
    ): String = withContext(decodeDispatcher) {
        val activeBridge = bridge
        if (activeBridge == null || contextPtr == 0L) {
            throw SpeechEngineUnavailableException(UNAVAILABLE_MESSAGE)
        }
        if (pcmData.size < MIN_SAMPLES) {
            // Shorter than whisper can work with. Reporting nothing beats decoding noise.
            return@withContext ""
        }

        val raw = decodeMutex.withLock {
            if (contextPtr == 0L) {
                throw SpeechEngineUnavailableException(UNAVAILABLE_MESSAGE)
            }
            try {
                activeBridge.nativeTranscribe(
                    contextPtr = contextPtr,
                    pcm = pcmData,
                    threads = threads,
                    language = LANGUAGE,
                    prompt = promptFor(targetMode)
                )
            } catch (t: Throwable) {
                throw SpeechEngineUnavailableException("Speech decoding failed on this device.", t)
            }
        } ?: throw SpeechEngineUnavailableException("Speech decoding failed. Please dictate again.")

        formatDentalTranscript(raw, targetMode)
    }

    /** Releases the native decoder context. Safe to call more than once. */
    suspend fun release() {
        val activeBridge = bridge ?: return
        decodeMutex.withLock {
            if (contextPtr != 0L) {
                try {
                    activeBridge.nativeRelease(contextPtr)
                } catch (t: Throwable) {
                    safeLogE("Error releasing whisper context", t)
                }
                contextPtr = 0L
            }
        }
    }

    private fun promptFor(mode: DictationTargetMode): String = when (mode) {
        DictationTargetMode.PERIODONTAL_CHARTING -> PERIODONTAL_PROMPT
        DictationTargetMode.CLINICAL_NOTES -> CLINICAL_NOTES_PROMPT
    }

    /**
     * Normalises a decoded transcript into standard dental notation: spoken numbers become
     * digits and clinical terms are mapped to their canonical spelling.
     */
    fun formatDentalTranscript(rawText: String, mode: DictationTargetMode): String {
        if (rawText.isBlank()) return ""

        var formatted = rawText.trim()

        val numberWords = mapOf(
            "one" to "1", "two" to "2", "three" to "3", "four" to "4", "five" to "5",
            "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9", "ten" to "10",
            "eleven" to "11", "twelve" to "12", "thirteen" to "13", "fourteen" to "14",
            "fifteen" to "15", "sixteen" to "16", "seventeen" to "17", "eighteen" to "18",
            "nineteen" to "19", "twenty" to "20", "thirty" to "30", "forty" to "40"
        )

        for ((word, digit) in numberWords) {
            formatted = formatted.replace(Regex("(?i)\\b$word\\b"), digit)
        }

        for ((key, value) in DENTAL_VOCABULARY) {
            formatted = formatted.replace(Regex("(?i)\\b$key\\b"), value)
        }

        return formatted
    }

    /** Log is stubbed out under JVM unit tests; fall back to stdout there. */
    private fun safeLogW(msg: String, t: Throwable? = null) {
        try {
            Log.w(TAG, msg, t)
        } catch (_: Throwable) {
            println("[$TAG] $msg ${t?.message ?: ""}")
        }
    }

    private fun safeLogE(msg: String, t: Throwable? = null) {
        try {
            Log.e(TAG, msg, t)
        } catch (_: Throwable) {
            println("[$TAG] $msg ${t?.message ?: ""}")
        }
    }
}

enum class DictationTargetMode(val title: String, val description: String) {
    PERIODONTAL_CHARTING("Periodontal Depths", "Dictate tooth pocket depths and bleeding flags (for example, Tooth 14 pocket 3mm bleeding)"),
    CLINICAL_NOTES("Clinical Notes", "Dictate general examination notes, diagnoses, and procedure summaries")
}
