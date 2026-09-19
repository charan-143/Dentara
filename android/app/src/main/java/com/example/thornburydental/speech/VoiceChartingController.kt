package com.example.thornburydental.speech

import android.util.Log
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.speech.model.SpeechModelProvider
import com.example.thornburydental.speech.model.SpeechModelSpec
import com.example.thornburydental.speech.model.SpeechModelState
import com.example.thornburydental.speech.model.SpeechModelStore
import com.example.thornburydental.speech.model.SpeechModels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface VoiceDictationState {
    object Idle : VoiceDictationState
    data class Listening(val amplitude: Float, val targetMode: DictationTargetMode) : VoiceDictationState
    object Transcribing : VoiceDictationState
    data class CommandParsed(
        val rawTranscript: String,
        val parsedCommand: ParsedVoiceCommand,
        val targetMode: DictationTargetMode
    ) : VoiceDictationState

    /** A single dictation attempt failed. The clinician can retry. */
    data class Error(val message: String) : VoiceDictationState

    /**
     * Voice dictation cannot run right now - no speech model installed, no microphone access,
     * or the decoder failed to load. The UI must not offer to record.
     *
     * @param canProvision true when the blocker is a missing model this device could download.
     *        The UI offers a download action only in that case.
     */
    data class Unavailable(val reason: String, val canProvision: Boolean = false) : VoiceDictationState
}

/**
 * Orchestrates hands-free audio recording, offline transcription, dental command parsing,
 * and updating patient records in DentalRepository.
 *
 * Transcription runs entirely on the device via whisper.cpp; no audio or transcript is sent
 * anywhere. No part of this pipeline may invent, guess, or default a clinical value when
 * transcription fails - a failure surfaces to the clinician instead.
 */
class VoiceChartingController(
    private val whisperEngine: WhisperEngine = WhisperEngine(),
    private val audioRecordManager: AudioRecordManager = AudioRecordManager(),
    private val commandParser: VoiceCommandParser = VoiceCommandParser(),
    private val modelStore: SpeechModelStore? = SpeechModelProvider.storeOrNull(),
    private val modelSpec: SpeechModelSpec = SpeechModels.DEFAULT
) {
    companion object {
        private const val TAG = "VoiceChartingController"

        private const val MODEL_NOT_INSTALLED =
            "Voice dictation needs a one-time offline language download before it can be used."

        private const val ENGINE_LOAD_FAILED =
            "The offline dictation model could not be loaded on this device."

        private const val STORE_UNAVAILABLE =
            "Voice dictation is not set up on this device."
    }

    private val _uiState = MutableStateFlow<VoiceDictationState>(VoiceDictationState.Idle)
    val uiState: StateFlow<VoiceDictationState> = _uiState.asStateFlow()

    private val _targetMode = MutableStateFlow(DictationTargetMode.PERIODONTAL_CHARTING)
    val targetMode: StateFlow<DictationTargetMode> = _targetMode.asStateFlow()

    private val fallbackModelState = MutableStateFlow<SpeechModelState>(SpeechModelState.Absent)

    /** Provisioning status of the offline model, for download progress in the UI. */
    val modelState: StateFlow<SpeechModelState> = modelStore?.state ?: fallbackModelState.asStateFlow()

    private var activeMode = DictationTargetMode.PERIODONTAL_CHARTING
    private var listeningScopeJob: Job? = null

    /**
     * Loads an already-installed model, if there is one.
     *
     * Deliberately does not download: the model is tens of megabytes and a clinic tablet may be
     * on a metered or restricted connection. A missing model reports
     * [VoiceDictationState.Unavailable] with canProvision set, and the clinician chooses.
     */
    suspend fun initialize(): Boolean {
        val store = modelStore
        if (store == null) {
            _uiState.value = VoiceDictationState.Unavailable(STORE_UNAVAILABLE)
            return false
        }

        store.refreshState(modelSpec)
        val installed = store.installedModel(modelSpec)
        if (installed == null) {
            _uiState.value = VoiceDictationState.Unavailable(MODEL_NOT_INSTALLED, canProvision = true)
            return false
        }

        val loaded = whisperEngine.initialize(installed)
        if (!loaded) {
            _uiState.value = VoiceDictationState.Unavailable(ENGINE_LOAD_FAILED)
            return false
        }

        _uiState.value = VoiceDictationState.Idle
        return true
    }

    /**
     * Downloads the offline model if needed, verifies it, and loads the decoder.
     * Progress is published on [modelState].
     *
     * @return true when dictation is ready to use.
     */
    suspend fun provisionModel(): Boolean {
        val store = modelStore
        if (store == null) {
            _uiState.value = VoiceDictationState.Unavailable(STORE_UNAVAILABLE)
            return false
        }

        val file = store.ensureAvailable(modelSpec)
        if (file == null) {
            val reason = (store.state.value as? SpeechModelState.Failed)?.reason
                ?: "The dictation model could not be downloaded."
            _uiState.value = VoiceDictationState.Unavailable(reason, canProvision = true)
            return false
        }

        val loaded = whisperEngine.initialize(file)
        if (!loaded) {
            _uiState.value = VoiceDictationState.Unavailable(ENGINE_LOAD_FAILED)
            return false
        }

        _uiState.value = VoiceDictationState.Idle
        return true
    }

    fun setTargetMode(mode: DictationTargetMode) {
        _targetMode.value = mode
        activeMode = mode
    }

    /**
     * Starts hands-free audio recording, provided the decoder and microphone are both usable.
     * Refuses and reports the reason otherwise.
     */
    fun startDictation(scope: CoroutineScope, mode: DictationTargetMode = _targetMode.value) {
        if (!whisperEngine.isReady()) {
            _uiState.value = VoiceDictationState.Unavailable(
                MODEL_NOT_INSTALLED,
                canProvision = modelStore != null
            )
            return
        }

        activeMode = mode
        _targetMode.value = mode

        try {
            audioRecordManager.startRecording(scope)
        } catch (e: MicrophoneUnavailableException) {
            safeLogE("Microphone unavailable", e)
            _uiState.value = VoiceDictationState.Unavailable(e.message ?: "Microphone unavailable.")
            return
        } catch (e: Exception) {
            safeLogE("Failed to start dictation", e)
            _uiState.value = VoiceDictationState.Error(e.message ?: "Could not start recording.")
            return
        }

        _uiState.value = VoiceDictationState.Listening(0f, mode)

        listeningScopeJob?.cancel()
        listeningScopeJob = scope.launch {
            audioRecordManager.amplitudeFlow.collect { amplitude ->
                if (_uiState.value is VoiceDictationState.Listening) {
                    _uiState.value = VoiceDictationState.Listening(amplitude, activeMode)
                }
            }
        }
    }

    /**
     * Stops audio recording and triggers offline transcription and command parsing.
     */
    fun stopDictationAndProcess(scope: CoroutineScope) {
        if (!audioRecordManager.isRecording() && _uiState.value !is VoiceDictationState.Listening) {
            return
        }

        _uiState.value = VoiceDictationState.Transcribing

        scope.launch(Dispatchers.Default) {
            try {
                val pcmData = audioRecordManager.stopRecording()
                listeningScopeJob?.cancel()

                val transcript = whisperEngine.transcribeAudio(pcmData, activeMode)

                if (transcript.isBlank()) {
                    _uiState.value = VoiceDictationState.Error("No spoken clinical dictation detected")
                    return@launch
                }

                val parsedCommand = commandParser.parseTranscript(
                    transcript = transcript,
                    mode = activeMode,
                    // Read at parse time: the clinician can change the scheme in settings and
                    // the very next dictation has to honour it.
                    numberingSystem = DentalRepository.toothNumberingSystem.value
                )

                _uiState.value = VoiceDictationState.CommandParsed(
                    rawTranscript = transcript,
                    parsedCommand = parsedCommand,
                    targetMode = activeMode
                )
            } catch (e: SpeechEngineUnavailableException) {
                safeLogE("Transcription unavailable", e)
                _uiState.value = VoiceDictationState.Unavailable(e.message ?: ENGINE_LOAD_FAILED)
            } catch (e: Exception) {
                safeLogE("Error in processing dictation", e)
                _uiState.value = VoiceDictationState.Error(e.message ?: "Transcription processing error")
            }
        }
    }

    /**
     * Applies the parsed voice command to the patient record in DentalRepository.
     */
    fun applyParsedCommand(patientId: String, command: ParsedVoiceCommand): Boolean {
        return try {
            when (command) {
                is ParsedVoiceCommand.SinglePocketDepth -> {
                    val entry = command.entry
                    DentalRepository.applyVoicePeriodontalPocket(
                        patientId = patientId,
                        toothNumber = entry.toothNumber,
                        depthMm = entry.depthMm,
                        isBleeding = entry.isBleeding
                    )
                    true
                }
                is ParsedVoiceCommand.MultiplePocketDepths -> {
                    for (entry in command.entries) {
                        DentalRepository.applyVoicePeriodontalPocket(
                            patientId = patientId,
                            toothNumber = entry.toothNumber,
                            depthMm = entry.depthMm,
                            isBleeding = entry.isBleeding
                        )
                    }
                    true
                }
                is ParsedVoiceCommand.ClinicalNote -> {
                    DentalRepository.appendVoiceClinicalNote(
                        patientId = patientId,
                        noteText = command.entry.noteText
                    )
                    true
                }
                is ParsedVoiceCommand.Unrecognized -> false
            }
        } catch (e: Exception) {
            safeLogE("Failed to apply parsed command to DentalRepository", e)
            false
        }
    }

    /**
     * Returns to idle after a dictation attempt. An [VoiceDictationState.Unavailable] state is
     * preserved - the underlying cause has not gone away, so the UI must keep dictation blocked.
     */
    fun resetState() {
        try {
            audioRecordManager.stopRecording()
        } catch (e: Exception) {
            safeLogE("Error stopping recording during reset", e)
        }
        listeningScopeJob?.cancel()
        if (_uiState.value !is VoiceDictationState.Unavailable) {
            _uiState.value = VoiceDictationState.Idle
        }
    }

    /** Releases the microphone and the native decoder. Call when the screen goes away. */
    suspend fun shutdown() {
        resetState()
        whisperEngine.release()
    }

    /** Log is stubbed out under JVM unit tests; fall back to stdout there. */
    private fun safeLogE(msg: String, t: Throwable? = null) {
        try {
            Log.e(TAG, msg, t)
        } catch (_: Throwable) {
            println("[$TAG] $msg ${t?.message ?: ""}")
        }
    }
}
