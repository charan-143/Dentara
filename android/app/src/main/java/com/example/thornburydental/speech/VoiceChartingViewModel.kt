package com.example.thornburydental.speech

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thornburydental.speech.model.SpeechModelState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Single owner of voice dictation for the whole activity.
 *
 * Deliberately one instance. The dictation bar is hosted by the patient chart screen, and the
 * examination section nested inside it used to build a second controller of its own - so on the
 * examination tab two controllers were alive at once, each holding a microphone and, once a
 * real model was wired in, its own multi-megabyte decoder context.
 *
 * Holds no patient state: the patient id is supplied per call, so the same instance is correct
 * across every patient the clinician opens.
 */
class VoiceChartingViewModel internal constructor(
    private val controller: VoiceChartingController
) : ViewModel() {

    // Explicit no-arg constructor rather than a defaulted parameter: ViewModel instantiation
    // goes through reflection, so the zero-argument form has to exist unambiguously.
    constructor() : this(VoiceChartingController())

    /**
     * Teardown outlives [viewModelScope], which is already cancelled by the time [onCleared]
     * runs. Releasing the native decoder is a suspending call, so it needs a scope that is
     * still alive to finish.
     */
    private val teardownScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val uiState: StateFlow<VoiceDictationState> = controller.uiState
    val targetMode: StateFlow<DictationTargetMode> = controller.targetMode
    val modelState: StateFlow<SpeechModelState> = controller.modelState

    init {
        // Loads an already-installed model. Never downloads on its own.
        viewModelScope.launch { controller.initialize() }
    }

    fun setTargetMode(mode: DictationTargetMode) = controller.setTargetMode(mode)

    /**
     * Recording is scoped to the ViewModel, not to a composable, so switching tabs mid-dictation
     * does not silently truncate the recording.
     */
    fun startDictation(mode: DictationTargetMode) = controller.startDictation(viewModelScope, mode)

    fun stopDictationAndProcess() = controller.stopDictationAndProcess(viewModelScope)

    fun provisionModel() {
        viewModelScope.launch { controller.provisionModel() }
    }

    /** @param transcript what was dictated, stored as provenance beside the written value. */
    fun applyParsedCommand(
        patientId: String,
        command: ParsedVoiceCommand,
        transcript: String = ""
    ): Boolean = controller.applyParsedCommand(patientId, command, transcript)

    fun resetState() = controller.resetState()

    /**
     * Drops the microphone when the app stops. Android cuts background mic access anyway, and
     * holding an open capture stream while another app wants the mic is antisocial.
     */
    fun releaseMicrophone() = controller.resetState()

    override fun onCleared() {
        super.onCleared()
        // Free the microphone immediately; the native context can finish asynchronously.
        controller.resetState()
        teardownScope.launch {
            try {
                controller.shutdown()
            } finally {
                teardownScope.cancel()
            }
        }
    }
}
