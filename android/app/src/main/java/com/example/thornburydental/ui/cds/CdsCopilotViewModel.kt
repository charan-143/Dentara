package com.example.thornburydental.ui.cds

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thornburydental.data.Patient
import com.example.thornburydental.data.cds.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Owns the clinical analysis lifecycle for the CDS Copilot screen. */
class CdsCopilotViewModel(
    private val orchestrator: MultiAgentClinicalOrchestrator = MultiAgentClinicalOrchestrator()
) : ViewModel() {
    private val _executionState = MutableStateFlow<MultiAgentExecutionState>(MultiAgentExecutionState.Idle)
    val executionState: StateFlow<MultiAgentExecutionState> = _executionState.asStateFlow()

    private var executionJob: Job? = null

    fun execute(
        context: Context,
        patient: Patient?,
        freeTextNarrative: String,
        selectedSymptoms: List<SymptomItem>,
        vitalsContext: PatientVitalsContext,
        executionMode: ClinicalAiExecutionMode,
        enableWebSearch: Boolean,
        clinicianSuggestions: List<String>
    ) {
        executionJob?.cancel()
        executionJob = viewModelScope.launch {
            _executionState.value = MultiAgentExecutionState.Idle
            try {
                orchestrator.executeClinicalPipeline(
                    context = context.applicationContext,
                    patient = patient,
                    freeTextNarrative = freeTextNarrative,
                    selectedSymptoms = selectedSymptoms,
                    vitalsContext = vitalsContext,
                    mediaItems = emptyList(),
                    executionMode = executionMode,
                    enableWebSearch = enableWebSearch,
                    clinicianSuggestions = clinicianSuggestions.map { suggestion ->
                        ClinicianSuggestionRecord(
                            id = java.util.UUID.randomUUID().toString(),
                            suggestionText = suggestion,
                            timestampIso = java.time.Instant.now().toString()
                        )
                    }
                ).collect { state -> _executionState.value = state }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _executionState.value = MultiAgentExecutionState.Error(
                    errorMessage = error.message ?: "Clinical analysis could not be completed."
                )
            }
        }
    }
}
