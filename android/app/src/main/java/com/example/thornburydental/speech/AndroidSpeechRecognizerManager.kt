package com.example.thornburydental.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Manages zero-download on-device Android SpeechRecognizer transcription.
 * Captures full speech utterances and delivers both live streaming updates
 * and final consolidated clinical dictation transcripts.
 */
class AndroidSpeechRecognizerManager(private val context: Context) {

    companion object {
        private const val TAG = "SpeechRecognizerManager"

        fun isAvailable(context: Context): Boolean {
            return SpeechRecognizer.isRecognitionAvailable(context)
        }
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _amplitudeFlow = MutableStateFlow(0f)
    val amplitudeFlow: StateFlow<Float> = _amplitudeFlow.asStateFlow()

    private var onPartialResultCallback: ((String) -> Unit)? = null
    private var onFinalResultCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null
    private var hasRetriedFallback = false

    private var isContinuous = false

    /**
     * Starts listening to clinician speech.
     */
    fun startListening(
        continuous: Boolean = false,
        onPartialResult: ((String) -> Unit)? = null,
        onFinalResult: ((String) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        this.isContinuous = continuous
        this.onPartialResultCallback = onPartialResult
        this.onFinalResultCallback = onFinalResult
        this.onErrorCallback = onError
        this.hasRetriedFallback = false

        startRecognitionInternal(useDefaultLanguage = false)
    }

    private fun startRecognitionInternal(useDefaultLanguage: Boolean) {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    if (!useDefaultLanguage) {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    }
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                }

                speechRecognizer?.startListening(intent)
                _isListening.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Error starting SpeechRecognizer", e)
                _isListening.value = false
                onErrorCallback?.invoke(e.message ?: "Failed to start speech recognition")
            }
        }
    }

    /**
     * Finishes speech recording and asks the recognizer to finalize transcription.
     */
    fun stopListening() {
        isContinuous = false
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping SpeechRecognizer", e)
            }
        }
    }

    /**
     * Cancels active recognition and frees resources.
     */
    fun cancel() {
        isContinuous = false
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
                _isListening.value = false
                _amplitudeFlow.value = 0f
            } catch (e: Exception) {
                Log.e(TAG, "Error canceling SpeechRecognizer", e)
            }
        }
    }

    fun destroy() {
        cancel()
    }

    private fun createListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
        }

        override fun onBeginningOfSpeech() {
            _isListening.value = true
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Convert rmsdB (-2 to 10 typical) to 0f..1f normalized amplitude
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _amplitudeFlow.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _isListening.value = false
        }

        override fun onError(error: Int) {
            _isListening.value = false
            _amplitudeFlow.value = 0f

            // Error 13 (ERROR_LANGUAGE_UNAVAILABLE) or Error 12 (ERROR_LANGUAGE_NOT_SUPPORTED):
            // Attempt automatic recovery by restarting with default system language model
            if ((error == 13 || error == 12 || error == 11) && !hasRetriedFallback) {
                hasRetriedFallback = true
                Log.i(TAG, "SpeechRecognizer language error ($error), retrying with system default recognizer")
                startRecognitionInternal(useDefaultLanguage = true)
                return
            }

            // In continuous hands-free mode, timeout / no-match is a soft silence event:
            // re-arm recognition silently so clinician can speak when ready
            if (isContinuous && (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                mainHandler.postDelayed({
                    if (isContinuous) {
                        startRecognitionInternal(useDefaultLanguage = false)
                    }
                }, 300)
                return
            }

            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please speak clearly."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy. Please try again."
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard"
                10 -> "Too many speech requests. Please wait a moment."
                11 -> "Speech recognition server disconnected"
                12 -> "Language not supported"
                13 -> "Language unavailable on device"
                14 -> "Cannot check language support"
                else -> "Speech recognition error ($error)"
            }
            Log.w(TAG, "SpeechRecognizer error: $errorMessage ($error)")
            if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                onErrorCallback?.invoke(errorMessage)
            } else {
                // If no match or timeout, send empty final result
                onFinalResultCallback?.invoke("")
            }
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            _amplitudeFlow.value = 0f
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val fullTranscript = matches?.firstOrNull().orEmpty().trim()
            onFinalResultCallback?.invoke(fullTranscript)

            // Re-arm recognition loop if continuous mode is active
            if (isContinuous) {
                mainHandler.postDelayed({
                    if (isContinuous) {
                        startRecognitionInternal(useDefaultLanguage = false)
                    }
                }, 400)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partialText = partial?.firstOrNull().orEmpty().trim()
            if (partialText.isNotBlank()) {
                onPartialResultCallback?.invoke(partialText)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
