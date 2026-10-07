package com.example.thornburydental.speech

import android.content.Context
import android.util.Log
import com.example.thornburydental.ThornburyApplication
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.VoiceChartEntry
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

    data class Listening(
        val amplitude: Float = 0f,
        val targetMode: DictationTargetMode = DictationTargetMode.PERIODONTAL_CHARTING,
        val streamingPartialTranscript: String = ""
    ) : VoiceDictationState

    data class Transcribing(val partialTranscript: String = "") : VoiceDictationState

    data class FindingsExtracted(
        val rawTranscript: String,
        val findings: ClinicalFindings,
        val targetMode: DictationTargetMode = DictationTargetMode.PERIODONTAL_CHARTING
    ) : VoiceDictationState

    data class CommandParsed(
        val rawTranscript: String,
        val parsedCommand: ParsedVoiceCommand,
        val targetMode: DictationTargetMode
    ) : VoiceDictationState

    data class Success(val message: String) : VoiceDictationState

    /** A single dictation attempt failed. The clinician can retry. */
    data class Error(val message: String) : VoiceDictationState

    /**
     * Voice dictation cannot run right now - no speech model installed, no microphone access,
     * or the decoder failed to load.
     */
    data class Unavailable(val reason: String, val canProvision: Boolean = false) : VoiceDictationState
}

/**
 * Orchestrates full-utterance speech capture, transcription (Android SpeechRecognizer / Whisper),
 * AI clinical entity extraction, and updates to the patient record in DentalRepository.
 */
class VoiceChartingController(
    private val whisperEngine: WhisperEngine = WhisperEngine(),
    private val audioRecordManager: AudioRecordManager = AudioRecordManager(),
    private val commandParser: VoiceCommandParser = VoiceCommandParser(),
    private val modelStore: SpeechModelStore? = SpeechModelProvider.storeOrNull(),
    private val modelSpec: SpeechModelSpec = SpeechModels.DEFAULT,
    private val context: Context? = try { ThornburyApplication.instance } catch (_: Throwable) { null }
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
    private var lastParsedCommand: ParsedVoiceCommand? = null
    private var lastFindings: ClinicalFindings? = null
    private val gemma4AudioEngine: Gemma4AudioEngine = Gemma4AudioEngine().apply { initialize() }
    private var speechRecognizerManager: AndroidSpeechRecognizerManager? = null

    init {
        context?.let { ctx ->
            if (AndroidSpeechRecognizerManager.isAvailable(ctx)) {
                speechRecognizerManager = AndroidSpeechRecognizerManager(ctx)
            }
        }
    }

    /**
     * Loads an already-installed Whisper model if available.
     */
    suspend fun initialize(): Boolean {
        val store = modelStore
        if (store == null) {
            if (speechRecognizerManager != null) {
                _uiState.value = VoiceDictationState.Idle
                return true
            }
            _uiState.value = VoiceDictationState.Unavailable(STORE_UNAVAILABLE)
            return false
        }

        store.refreshState(modelSpec)
        val installed = store.installedModel(modelSpec)
        if (installed == null) {
            if (speechRecognizerManager != null) {
                _uiState.value = VoiceDictationState.Idle
                return true
            }
            _uiState.value = VoiceDictationState.Unavailable(MODEL_NOT_INSTALLED, canProvision = true)
            return false
        }

        val loaded = whisperEngine.initialize(installed)
        if (!loaded && speechRecognizerManager == null) {
            _uiState.value = VoiceDictationState.Unavailable(ENGINE_LOAD_FAILED)
            return false
        }

        _uiState.value = VoiceDictationState.Idle
        return true
    }

    /**
     * Downloads the offline model if needed, verifies it, and loads the decoder.
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
     * Starts listening to clinician speech (capturing the entire utterance).
     */
    fun startDictation(
        scope: CoroutineScope,
        mode: DictationTargetMode = _targetMode.value,
        continuousHandsFree: Boolean = true,
        patientId: String? = null
    ) {
        activeMode = mode
        _targetMode.value = mode

        // Check if Android SpeechRecognizer is available on this device
        val recognizer = speechRecognizerManager
        if (recognizer != null) {
            _uiState.value = VoiceDictationState.Listening(0f, mode)
            recognizer.startListening(
                continuous = continuousHandsFree,
                onPartialResult = { partial ->
                    val current = _uiState.value
                    if (current is VoiceDictationState.Listening) {
                        _uiState.value = current.copy(streamingPartialTranscript = partial)
                    }
                },
                onFinalResult = { fullTranscript ->
                    if (fullTranscript.isNotBlank()) {
                        processFullUtterance(scope, fullTranscript, patientId)
                    } else {
                        if (_uiState.value is VoiceDictationState.Listening) {
                            _uiState.value = VoiceDictationState.Idle
                        }
                    }
                },
                onError = { err ->
                    safeLogE("SpeechRecognizer error: $err")
                    _uiState.value = VoiceDictationState.Error(err)
                }
            )

            listeningScopeJob?.cancel()
            listeningScopeJob = scope.launch {
                recognizer.amplitudeFlow.collect { amp ->
                    val current = _uiState.value
                    if (current is VoiceDictationState.Listening) {
                        _uiState.value = current.copy(amplitude = amp)
                    }
                }
            }
            return
        }

        // Fallback to raw AudioRecord + WhisperEngine
        if (!whisperEngine.isReady()) {
            _uiState.value = VoiceDictationState.Unavailable(
                MODEL_NOT_INSTALLED,
                canProvision = modelStore != null
            )
            return
        }

        try {
            audioRecordManager.startRecording(
                scope = scope,
                onEndpointDetected = if (continuousHandsFree) {
                    { speechPcm -> processPcmSpeechUtterance(scope, speechPcm, patientId) }
                } else null
            )
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
                val current = _uiState.value
                if (current is VoiceDictationState.Listening) {
                    _uiState.value = current.copy(amplitude = amplitude, targetMode = activeMode)
                }
            }
        }
    }

    private var lastRawTranscript: String = ""

    /**
     * Processes PCM audio detected from offline microphone capture using Gemma 4 E2B Multimodal Native Audio.
     */
    private fun processPcmSpeechUtterance(
        scope: CoroutineScope,
        speechPcm: ShortArray,
        patientId: String?
    ) {
        if (speechPcm.size < WhisperEngine.MIN_SAMPLES) return

        scope.launch(Dispatchers.Default) {
            try {
                val currentListening = _uiState.value as? VoiceDictationState.Listening
                _uiState.value = VoiceDictationState.Transcribing(
                    partialTranscript = currentListening?.streamingPartialTranscript ?: ""
                )

                val transcript = whisperEngine.transcribeAudio(speechPcm, activeMode)
                // Gemma 4 E2B Native Audio Model Extraction
                val gemmaFindings = gemma4AudioEngine.processAudioUtterance(
                    pcmData = speechPcm,
                    context = context,
                    transcriptHint = transcript
                )

                val effectiveTranscript = transcript.ifBlank { gemmaFindings.rawTranscript }
                if (effectiveTranscript.isBlank() && gemmaFindings.isEmpty) {
                    if (audioRecordManager.isRecording()) {
                        _uiState.value = VoiceDictationState.Listening(0f, activeMode)
                    }
                    return@launch
                }

                processFullUtterance(scope, effectiveTranscript, patientId, gemmaFindings)
            } catch (e: Exception) {
                safeLogE("Error transcribing PCM utterance with Gemma 4 E2B", e)
                if (audioRecordManager.isRecording()) {
                    _uiState.value = VoiceDictationState.Listening(0f, activeMode)
                }
            }
        }
    }

    /**
     * Complete full-utterance processor:
     * 1. Uses Gemma 4 E2B AI / deterministic clinical extractor to parse multi-entity findings.
     * 2. Checks for undo / confirmation commands.
     * 3. Sets state to FindingsExtracted / CommandParsed.
     * 4. Auto-applies if enabled.
     */
    fun processFullUtterance(
        scope: CoroutineScope,
        transcript: String,
        patientId: String?,
        precomputedFindings: ClinicalFindings? = null
    ) {
        scope.launch(Dispatchers.Default) {
            try {
                _uiState.value = VoiceDictationState.Transcribing(partialTranscript = transcript)

                // 1. Spoken Undo / Spoken Confirmation check
                val parsedCommand = commandParser.parseTranscript(
                    transcript = transcript,
                    mode = activeMode,
                    numberingSystem = DentalRepository.toothNumberingSystem.value
                )

                when (parsedCommand) {
                    is ParsedVoiceCommand.SpokenUndo -> {
                        val undone = DentalRepository.undoLastVoiceEntry(patientId)
                        _uiState.value = VoiceDictationState.CommandParsed(
                            rawTranscript = transcript,
                            parsedCommand = parsedCommand,
                            targetMode = activeMode
                        )
                        return@launch
                    }
                    is ParsedVoiceCommand.SpokenConfirmation -> {
                        if (parsedCommand.confirmed && patientId != null) {
                            lastFindings?.let { findings ->
                                DentalRepository.applyClinicalFindings(patientId, findings)
                                lastFindings = null
                            }
                            lastParsedCommand?.let { cmd ->
                                applyParsedCommand(patientId, cmd, lastRawTranscript)
                                lastParsedCommand = null
                            }
                        }
                        _uiState.value = VoiceDictationState.CommandParsed(
                            rawTranscript = transcript,
                            parsedCommand = parsedCommand,
                            targetMode = activeMode
                        )
                        return@launch
                    }
                    else -> {}
                }

                // 2. AI (Gemma 4 E2B / Gemini) & Rule-based multi-entity clinical extraction
                val findings = if (precomputedFindings != null && !precomputedFindings.isEmpty) {
                    precomputedFindings
                } else {
                    ClinicalVoiceExtractor.extract(transcript, context)
                }
                lastFindings = findings
                lastParsedCommand = parsedCommand
                lastRawTranscript = transcript

                if (findings.isEmpty && parsedCommand is ParsedVoiceCommand.Unrecognized) {
                    _uiState.value = VoiceDictationState.CommandParsed(
                        rawTranscript = transcript,
                        parsedCommand = parsedCommand,
                        targetMode = activeMode
                    )
                    return@launch
                }

                // Check auto-apply
                val autoApply = DentalRepository.isVoiceAutoApplyEnabled.value
                if (autoApply && !findings.requiresReview && patientId != null && !findings.isEmpty) {
                    DentalRepository.applyClinicalFindings(patientId, findings)
                    _uiState.value = VoiceDictationState.Success("Applied ${findings.totalCount} findings to patient chart")
                } else {
                    _uiState.value = VoiceDictationState.FindingsExtracted(
                        rawTranscript = transcript,
                        findings = findings,
                        targetMode = activeMode
                    )
                }
            } catch (e: Exception) {
                safeLogE("Error processing full speech utterance", e)
                _uiState.value = VoiceDictationState.Error(e.message ?: "Failed to process dictation")
            }
        }
    }

    /**
     * Stops audio recording and triggers transcription for manual capture.
     */
    fun stopDictationAndProcess(scope: CoroutineScope, patientId: String? = null) {
        val recognizer = speechRecognizerManager
        if (recognizer != null && recognizer.isListening.value) {
            _uiState.value = VoiceDictationState.Transcribing()
            recognizer.stopListening()
            return
        }

        if (!audioRecordManager.isRecording() && _uiState.value !is VoiceDictationState.Listening) {
            return
        }

        _uiState.value = VoiceDictationState.Transcribing()

        scope.launch(Dispatchers.Default) {
            try {
                val pcmData = audioRecordManager.stopRecording()
                listeningScopeJob?.cancel()

                val transcript = whisperEngine.transcribeAudio(pcmData, activeMode)
                val gemmaFindings = gemma4AudioEngine.processAudioUtterance(
                    pcmData = pcmData,
                    context = context,
                    transcriptHint = transcript
                )

                val effectiveTranscript = transcript.ifBlank { gemmaFindings.rawTranscript }
                if (effectiveTranscript.isBlank() && gemmaFindings.isEmpty) {
                    _uiState.value = VoiceDictationState.Error("No spoken clinical dictation detected")
                    return@launch
                }

                processFullUtterance(scope, effectiveTranscript, patientId, gemmaFindings)
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
     * Applies extracted clinical findings directly to the patient chart in DentalRepository.
     */
    fun applyClinicalFindings(
        patientId: String,
        findings: ClinicalFindings
    ): Boolean {
        return DentalRepository.applyClinicalFindings(patientId, findings)
    }

    /**
     * Applies the parsed voice command to the patient record in DentalRepository.
     */
    fun applyParsedCommand(
        patientId: String,
        command: ParsedVoiceCommand,
        transcript: String = ""
    ): Boolean {
        DentalRepository.captureVoiceUndoPoint(patientId, undoDescriptionFor(command))

        val hasImplausible = when (command) {
            is ParsedVoiceCommand.SinglePocketDepth -> command.entry.depthMm > VoiceCommandParser.MAX_ORDINARY_DEPTH_MM
            is ParsedVoiceCommand.MultiplePocketDepths -> command.entries.any { it.depthMm > VoiceCommandParser.MAX_ORDINARY_DEPTH_MM }
            else -> false
        }

        val provenance = VoiceChartEntry(
            clinicianName = DentalRepository.clinicianDisplayName.value,
            transcript = transcript,
            confidence = command.confidence,
            reviewRequired = hasImplausible || command.confidence < VoiceCommandParser.AUTO_APPLY_CONFIDENCE
        )

        return try {
            when (command) {
                is ParsedVoiceCommand.SinglePocketDepth -> {
                    applyPocket(patientId, command.entry, provenance)
                    true
                }
                is ParsedVoiceCommand.MultiplePocketDepths -> {
                    for (entry in command.entries) {
                        applyPocket(patientId, entry, provenance)
                    }
                    true
                }
                is ParsedVoiceCommand.ClinicalNote -> {
                    DentalRepository.appendVoiceClinicalNote(
                        patientId = patientId,
                        noteText = command.entry.noteText,
                        targetSection = command.entry.targetSection,
                        provenance = provenance
                    )
                    true
                }
                is ParsedVoiceCommand.SpokenConfirmation -> true
                is ParsedVoiceCommand.SpokenUndo -> true
                is ParsedVoiceCommand.Unrecognized -> false
            }
        } catch (e: Exception) {
            safeLogE("Failed to apply parsed command to DentalRepository", e)
            false
        }
    }

    /**
     * Reverts the most recent dictated entry.
     */
    fun undoLastVoiceEntry(patientId: String? = null): String? = DentalRepository.undoLastVoiceEntry(patientId)

    private fun undoDescriptionFor(command: ParsedVoiceCommand): String = when (command) {
        is ParsedVoiceCommand.SinglePocketDepth ->
            "Tooth " + command.entry.toothNumber + ", " + command.entry.depthMm + "mm"
        is ParsedVoiceCommand.MultiplePocketDepths ->
            command.entries.size.toString() + " readings on teeth " +
                command.entries.map { it.toothNumber }.distinct().joinToString(", ")
        is ParsedVoiceCommand.ClinicalNote ->
            if (command.entry.targetSection == "diagnosis") "Dictated diagnosis" else "Dictated note"
        is ParsedVoiceCommand.SpokenConfirmation -> "Confirmed action"
        is ParsedVoiceCommand.SpokenUndo -> "Undo action"
        is ParsedVoiceCommand.Unrecognized -> "Nothing"
    }

    private fun applyPocket(
        patientId: String,
        entry: PocketDepthResult,
        provenance: VoiceChartEntry
    ) {
        DentalRepository.applyVoicePeriodontalPocket(
            patientId = patientId,
            toothNumber = entry.toothNumber,
            depthMm = entry.depthMm,
            isBleeding = entry.isBleeding,
            siteLabel = if (entry.site == PerioSite.UNSPECIFIED) "" else entry.site.label,
            provenance = provenance
        )
    }

    /**
     * Returns to idle after a dictation attempt.
     */
    fun resetState() {
        try {
            speechRecognizerManager?.cancel()
            audioRecordManager.stopRecording()
        } catch (e: Exception) {
            safeLogE("Error stopping recording during reset", e)
        }
        listeningScopeJob?.cancel()
        if (_uiState.value !is VoiceDictationState.Unavailable) {
            _uiState.value = VoiceDictationState.Idle
        }
    }

    /** Releases the microphone and the native decoder. */
    suspend fun shutdown() {
        resetState()
        whisperEngine.release()
    }

    private fun safeLogE(msg: String, t: Throwable? = null) {
        try {
            Log.e(TAG, msg, t)
        } catch (_: Throwable) {
            println("[$TAG] $msg ${t?.message ?: ""}")
        }
    }
}
