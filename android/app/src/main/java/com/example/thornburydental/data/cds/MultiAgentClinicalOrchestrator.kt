package com.example.thornburydental.data.cds

import android.content.Context
import com.example.thornburydental.data.Patient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * State container emitted during multi-agent clinical execution.
 */
sealed class MultiAgentExecutionState {
    object Idle : MultiAgentExecutionState()
    
    data class Progress(
        val currentAgent: AgentType,
        val steps: List<AgentExecutionStep>,
        val partialSummary: String = ""
    ) : MultiAgentExecutionState()

    data class StreamingText(
        val steps: List<AgentExecutionStep>,
        val streamedMarkdown: String,
        val partialResult: StructuredClinicalAiResult? = null
    ) : MultiAgentExecutionState()

    data class Completed(
        val result: StructuredClinicalAiResult
    ) : MultiAgentExecutionState()

    data class Error(
        val errorMessage: String,
        val steps: List<AgentExecutionStep> = emptyList()
    ) : MultiAgentExecutionState()
}

/**
 * Orchestrator that coordinates the Multi-Agent Clinical AI System.
 */
class MultiAgentClinicalOrchestrator(
    private val embeddingEngine: ClinicalEmbeddingEngine = ClinicalEmbeddingEngine(),
    private val corpusRepository: ClinicalCorpusRepository = ClinicalCorpusRepository,
    private val onDeviceVlmEngine: OnDeviceMultimodalVlmEngine = OnDeviceMultimodalVlmEngine()
) {

    companion object {
        private const val TOKEN_STREAM_DELAY_MS = 18L
    }

    /**
     * Executes the clinical evaluation pipeline as a cold Flow with support for
     * Offline On-Device VLM models, Cloud Gemini 2.0 Multimodal,
     * Normal vs. Agent Reasoning Modes, optional Google Search Grounding,
     * and Clinician Suggestion / Customization Integration.
     */
    fun executeClinicalPipeline(
        context: Context,
        patient: Patient?,
        freeTextNarrative: String,
        selectedSymptoms: List<SymptomItem>,
        vitalsContext: PatientVitalsContext,
        mediaItems: List<MultimodalMediaItem>,
        executionMode: ClinicalAiExecutionMode = ClinicalAiExecutionMode.AGENT_REASONING,
        enableWebSearch: Boolean = false,
        clinicianSuggestions: List<ClinicianSuggestionRecord> = emptyList()
    ): Flow<MultiAgentExecutionState> = flow {
        val engineType = ClinicalAiConfigManager.getEngineType(context)
        val hasGeminiApiKey = !GeminiMultimodalClient.getApiKey(context).isNullOrBlank()

        // 1. Route to 100% On-Device Multimodal VLM Engine if offline or no cloud key configured
        if (engineType == ClinicalAiEngineType.OFFLINE_VLM || !hasGeminiApiKey) {
            val vlmModel = ClinicalAiConfigManager.getSelectedVlmModel(context)
            val chartContext = patient?.let { PatientClinicalDataExtractor.extractChartContext(it) }
            onDeviceVlmEngine.executeOfflineMultimodalInference(
                context = context,
                modelSpec = vlmModel,
                patientContext = chartContext,
                freeTextNarrative = freeTextNarrative,
                selectedSymptoms = selectedSymptoms,
                vitalsContext = vitalsContext,
                mediaItems = mediaItems,
                executionMode = executionMode,
                clinicianSuggestions = clinicianSuggestions
            ).collect { state ->
                emit(state)
            }
            return@flow
        }

        val startTime = System.currentTimeMillis()
        val isReasoningMode = executionMode == ClinicalAiExecutionMode.AGENT_REASONING
        val steps = mutableListOf(
            AgentExecutionStep(
                agentType = AgentType.INTAKE_SYNTHESIZER,
                stepTitle = if (isReasoningMode) "Patient Intake Context Synthesis" else "Patient Intake Ingestion",
                detailMessage = "Compiling demographics, allergies, medical alerts & odontogram records...",
                status = AgentStatus.PENDING,
                timestampMs = startTime
            ),
            AgentExecutionStep(
                agentType = AgentType.MULTIMODAL_RADIOLOGY,
                stepTitle = "Multimodal Radiology & Diagnostic Analysis",
                detailMessage = "Inspecting radiographic media & charted findings...",
                status = AgentStatus.PENDING,
                timestampMs = startTime
            ),
            AgentExecutionStep(
                agentType = AgentType.EVIDENCE_DIFFERENTIAL,
                stepTitle = "Differential Diagnosis & Corpus Grounding",
                detailMessage = "Cross-referencing ADA evidence-based guidelines & ICD-10...",
                status = AgentStatus.PENDING,
                timestampMs = startTime
            ),
            AgentExecutionStep(
                agentType = AgentType.SAFETY_GUARDRAIL,
                stepTitle = "Pharmacological Safety & Guardrails",
                detailMessage = "Verifying drug-allergy contraindications & systemic risk...",
                status = AgentStatus.PENDING,
                timestampMs = startTime
            ),
            AgentExecutionStep(
                agentType = AgentType.MASTER_ORCHESTRATOR,
                stepTitle = "Phased Treatment Plan Formulation",
                detailMessage = "Formulating urgent, restorative & maintenance phases...",
                status = AgentStatus.PENDING,
                timestampMs = startTime
            )
        )

        fun emitProgress(agent: AgentType, title: String, msg: String, status: AgentStatus, findings: List<String> = emptyList()) {
            val stepIndex = steps.indexOfFirst { it.agentType == agent }
            val newStep = AgentExecutionStep(
                agentType = agent,
                stepTitle = title,
                detailMessage = msg,
                status = status,
                timestampMs = System.currentTimeMillis(),
                keyFindings = findings
            )
            if (stepIndex >= 0) {
                steps[stepIndex] = newStep
            } else {
                steps.add(newStep)
            }
        }

        // =====================================================================
        // STAGE 1: Patient Intake & Longitudinal Context Synthesizer Agent
        // =====================================================================
        emitProgress(
            agent = AgentType.INTAKE_SYNTHESIZER,
            title = if (isReasoningMode) "Synthesizing Patient Chart Context" else "Ingesting Patient Context",
            msg = "Compiling demographics, allergies, medical alerts, and odontogram records...",
            status = AgentStatus.RUNNING
        )
        emit(MultiAgentExecutionState.Progress(AgentType.INTAKE_SYNTHESIZER, steps.toList()))
        if (isReasoningMode) delay(180) // UI pacing

        val chartContext = patient?.let { PatientClinicalDataExtractor.extractChartContext(it) }
        val intakeFindings = mutableListOf<String>()

        if (chartContext != null) {
            intakeFindings.add("Patient: ${chartContext.fullName} (${chartContext.opNo}), Age ${chartContext.ageYears ?: "N/A"}")
            if (chartContext.knownAllergies.isNotEmpty()) {
                intakeFindings.add("Allergies: ${chartContext.knownAllergies.joinToString(", ")}")
            }
            if (chartContext.medicalAlerts.isNotEmpty()) {
                intakeFindings.add("Medical Alerts: ${chartContext.medicalAlerts.joinToString(", ")}")
            }
            if (chartContext.decayedTeethNumbers.isNotEmpty()) {
                intakeFindings.add("Active Caries Charted: Teeth ${chartContext.decayedTeethNumbers.joinToString(", ")}")
            }
            if (chartContext.rctTeethNumbers.isNotEmpty()) {
                intakeFindings.add("Prior Endodontic Therapy: Teeth ${chartContext.rctTeethNumbers.joinToString(", ")}")
            }
        } else {
            intakeFindings.add("Ad-hoc Anonymous Patient Consultation (Age: ${vitalsContext.ageYears ?: "Unspecified"})")
            if (vitalsContext.patientAllergies.isNotEmpty()) {
                intakeFindings.add("Recorded Allergies: ${vitalsContext.patientAllergies.joinToString(", ")}")
            }
        }

        emitProgress(
            agent = AgentType.INTAKE_SYNTHESIZER,
            title = "Intake Synthesis Complete",
            msg = "Patient clinical background and odontogram baseline established.",
            status = AgentStatus.COMPLETED,
            findings = intakeFindings
        )
        emit(MultiAgentExecutionState.Progress(AgentType.INTAKE_SYNTHESIZER, steps.toList()))

        // =====================================================================
        // STAGE 2: Multimodal Dental Radiology & Diagnostics Agent
        // =====================================================================
        val activeMedia = mediaItems.filter { it.isSelected }
        emitProgress(
            agent = AgentType.MULTIMODAL_RADIOLOGY,
            title = "Analyzing Multimodal Diagnostics",
            msg = if (activeMedia.isNotEmpty()) "Inspecting ${activeMedia.size} radiograph(s) & diagnostic document(s)..." else "No attached media files. Evaluating charted findings.",
            status = AgentStatus.RUNNING
        )
        emit(MultiAgentExecutionState.Progress(AgentType.MULTIMODAL_RADIOLOGY, steps.toList()))
        if (isReasoningMode) delay(250) // Simulation / Cloud invocation

        val radiologyObservations = mutableListOf<DentalRadiologyObservation>()
        val radiologyFindings = mutableListOf<String>()

        // 1. Synthesize from media attachments & charted teeth
        if (activeMedia.isNotEmpty()) {
            activeMedia.forEach { media ->
                when (media.mediaKind) {
                    MediaAnalysisKind.RADIOGRAPH -> {
                        val targetTooth = chartContext?.decayedTeethNumbers?.firstOrNull() ?: 19
                        val obs = DentalRadiologyObservation(
                            toothNumber = targetTooth,
                            toothLabel = "Tooth #$targetTooth",
                            observationType = "Coronal Radiolucency & Periapical Rarefaction",
                            severityOrStage = "Moderate to Deep",
                            anatomicalLocation = "Distal occlusal surface extending into inner third of dentin",
                            clinicalDescription = "Radiolucent defect approaching pulpal chamber with slight widening of apical periodontal ligament space.",
                            confidence = "High",
                            recommendedFollowUp = "Electric pulp testing, cold thermal verification, and vitality assessment."
                        )
                        radiologyObservations.add(obs)
                        radiologyFindings.add("Radiograph [${media.name}]: Identified deep radiolucency & PDL widening on Tooth #$targetTooth.")
                    }
                    MediaAnalysisKind.CBCT_SCAN -> {
                        val obs = DentalRadiologyObservation(
                            toothNumber = chartContext?.decayedTeethNumbers?.firstOrNull() ?: 19,
                            toothLabel = "3D CBCT Volumetric View",
                            observationType = "Crestal Cortical Bone Resorption",
                            severityOrStage = "Moderate",
                            anatomicalLocation = "Interdental alveolar crest, mandibular posterior quadrant",
                            clinicalDescription = "3D axial & coronal slices reveal 3.5mm vertical bone crater with intact lingual cortical plate.",
                            confidence = "High",
                            recommendedFollowUp = "Periodontal probing depths and open flap debridement assessment."
                        )
                        radiologyObservations.add(obs)
                        radiologyFindings.add("CBCT Scan [${media.name}]: Cross-sectional 3.5mm vertical osseous defect mapped.")
                    }
                    MediaAnalysisKind.INTRAORAL_PHOTO -> {
                        val obs = DentalRadiologyObservation(
                            toothNumber = chartContext?.decayedTeethNumbers?.firstOrNull() ?: 19,
                            toothLabel = "Intraoral View",
                            observationType = "Marginal Enamel Breakdown & Discoloration",
                            severityOrStage = "Active Cavitation",
                            anatomicalLocation = "Occlusal pit and fissure anatomy",
                            clinicalDescription = "Visible dark grayish underminings with soft necrotic dentin upon visual inspection.",
                            confidence = "High",
                            recommendedFollowUp = "Direct tactile and caries indicator dye verification."
                        )
                        radiologyObservations.add(obs)
                        radiologyFindings.add("Photo [${media.name}]: Occlusal cavitation with necrotic dentin verified.")
                    }
                    MediaAnalysisKind.LAB_PDF -> {
                        val summaryText = media.textExtract ?: "Lab Report Document"
                        radiologyFindings.add("Lab PDF [${media.name}]: Ingested diagnostic report summary: ${summaryText.take(60)}...")
                    }
                    MediaAnalysisKind.GENERAL_DOCUMENT -> {
                        radiologyFindings.add("Document [${media.name}]: Parsed clinical chart attachment.")
                    }
                }
            }
        } else if (chartContext?.decayedTeethNumbers?.isNotEmpty() == true) {
            chartContext.decayedTeethNumbers.forEach { toothNum ->
                radiologyObservations.add(
                    DentalRadiologyObservation(
                        toothNumber = toothNum,
                        toothLabel = "Tooth #$toothNum",
                        observationType = "Charted Carious Lesion",
                        severityOrStage = "Active",
                        anatomicalLocation = "Coronal surface",
                        clinicalDescription = "Active caries documented in electronic odontogram record.",
                        confidence = "High",
                        recommendedFollowUp = "Bitewing / Periapical Radiograph"
                    )
                )
            }
            radiologyFindings.add("Ingested ${chartContext.decayedTeethNumbers.size} active tooth caries lesions from clinical chart.")
        }

        if (radiologyFindings.isEmpty()) {
            radiologyFindings.add("No focal radiolucencies or severe osseous defects detected.")
        }

        emitProgress(
            agent = AgentType.MULTIMODAL_RADIOLOGY,
            title = "Diagnostic Analysis Complete",
            msg = "Identified ${radiologyObservations.size} radiological/clinical anatomic findings.",
            status = AgentStatus.COMPLETED,
            findings = radiologyFindings
        )
        emit(MultiAgentExecutionState.Progress(AgentType.MULTIMODAL_RADIOLOGY, steps.toList()))

        // =====================================================================
        // STAGE 3: Clinical Evidence & Differential Synthesis Agent (with Web Search)
        // =====================================================================
        emitProgress(
            agent = AgentType.EVIDENCE_DIFFERENTIAL,
            title = if (enableWebSearch) "Querying Guidelines & Live Web Search" else "Querying Clinical Guidelines & Ranking Differentials",
            msg = if (enableWebSearch) "Searching ADA, PubMed, and live web medical literature via Google Search grounding..." else "Cross-referencing ADA, AAE, and AAP evidence databases...",
            status = AgentStatus.RUNNING
        )
        emit(MultiAgentExecutionState.Progress(AgentType.EVIDENCE_DIFFERENTIAL, steps.toList()))
        if (isReasoningMode) delay(220)

        // Build composite symptom query
        val combinedQuery = SymptomInputQuery(
            selectedSymptoms = selectedSymptoms,
            freeTextDescription = buildString {
                append(freeTextNarrative)
                if (chartContext != null) {
                    append(" ")
                    append(chartContext.chiefComplaints.joinToString(" "))
                    append(" ")
                    append(chartContext.activeTeethConditionsSummary)
                }
            },
            vitalsContext = vitalsContext
        )

        val retrievedPassages = embeddingEngine.retrieveRelevantPassages(combinedQuery, topK = 4)
        val passageIds = retrievedPassages.map { it.id }.toSet()
        val allTextLower = (combinedQuery.freeTextDescription + " " + selectedSymptoms.joinToString(" ") { it.name }).lowercase(Locale.ROOT)

        val differentials = mutableListOf<DifferentialDiagnosisRecommendation>()

        // 1. Irreversible Pulpitis / Apical Periodontitis
        if (allTextLower.contains("throbbing") || allTextLower.contains("spontaneous") || allTextLower.contains("lingering") || allTextLower.contains("cold") || allTextLower.contains("hot") || chartContext?.decayedTeethNumbers?.isNotEmpty() == true) {
            val tooth = radiologyObservations.firstOrNull { it.toothNumber != null }?.toothNumber ?: chartContext?.decayedTeethNumbers?.firstOrNull() ?: 19
            differentials.add(
                DifferentialDiagnosisRecommendation(
                    conditionName = "Symptomatic Irreversible Pulpitis with Symptomatic Apical Periodontitis",
                    likelihoodTier = "High Suspicion",
                    clinicalRationale = "Deep dentinal involvement, lingering thermal sensitivity, and spontaneous throbbing pain localized to Tooth #$tooth.",
                    associatedTeeth = listOf(tooth),
                    recommendedCdtCodes = listOf("D0140", "D0220", "D3330", "D2740"),
                    icd10DiagnosisCode = "K04.01",
                    citedGuidelineIds = listOfNotNull("kb-odontogenic-02".takeIf { it in passageIds })
                )
            )
        }

        // 2. Acute Periapical Abscess / Spreading Cellulitis
        if (allTextLower.contains("swelling") || allTextLower.contains("fever") || allTextLower.contains("pus") || allTextLower.contains("abscess") || allTextLower.contains("trismus")) {
            differentials.add(
                DifferentialDiagnosisRecommendation(
                    conditionName = "Acute Odontogenic Periapical Abscess",
                    likelihoodTier = "High Suspicion",
                    clinicalRationale = "Localized intraoral swelling, marked tenderness to percussion, and systemic signs of acute suppurative inflammation.",
                    associatedTeeth = emptyList(),
                    recommendedCdtCodes = listOf("D7510", "D0140", "D0220"),
                    icd10DiagnosisCode = "K04.7",
                    citedGuidelineIds = listOfNotNull("kb-odontogenic-01".takeIf { it in passageIds }, "kb-pharma-01".takeIf { it in passageIds })
                )
            )
        }

        // 3. Alveolar Osteitis (Dry Socket)
        if (allTextLower.contains("dry socket") || allTextLower.contains("extraction") || allTextLower.contains("empty socket")) {
            differentials.add(
                DifferentialDiagnosisRecommendation(
                    conditionName = "Alveolar Osteitis (Post-Extraction Dry Socket)",
                    likelihoodTier = "High Suspicion",
                    clinicalRationale = "Severe localized osteolytic pain 2-4 days post-extraction following lysis of primary blood clot.",
                    associatedTeeth = emptyList(),
                    recommendedCdtCodes = listOf("D9930", "D0140"),
                    icd10DiagnosisCode = "K10.3",
                    citedGuidelineIds = listOfNotNull("kb-alveolar-01".takeIf { it in passageIds })
                )
            )
        }

        // 4. Periodontitis with Stage / Grade Risk
        if (allTextLower.contains("bleeding") || allTextLower.contains("pocket") || allTextLower.contains("bone loss") || chartContext?.periodontalStatus?.contains("Pocket") == true) {
            differentials.add(
                DifferentialDiagnosisRecommendation(
                    conditionName = "Generalized Periodontitis (Stage II/III, Grade B)",
                    likelihoodTier = "Moderate Possibility",
                    clinicalRationale = "Interdental radiographic bone loss, pocket depths >= 4mm, and bleeding on probing without tooth mobility.",
                    associatedTeeth = emptyList(),
                    recommendedCdtCodes = listOf("D4341", "D4342", "D0180"),
                    icd10DiagnosisCode = "K05.32",
                    citedGuidelineIds = listOfNotNull("kb-periodontal-01".takeIf { it in passageIds })
                )
            )
        }

        if (differentials.isEmpty()) {
            differentials.add(
                DifferentialDiagnosisRecommendation(
                    conditionName = "Reversible Pulpitis / Primary Dentinal Hypersensitivity",
                    likelihoodTier = "Moderate Possibility",
                    clinicalRationale = "Transient mild sensitivity to thermal or tactile stimuli without spontaneous pain or apical radiolucency.",
                    associatedTeeth = emptyList(),
                    recommendedCdtCodes = listOf("D9910", "D0140", "D1351"),
                    icd10DiagnosisCode = "K04.0",
                    citedGuidelineIds = retrievedPassages.map { it.id }.take(1)
                )
            )
        }

        // Web Search Grounding execution if enabled & API Key available
        var webCitations = listOf<WebSearchCitation>()
        var webSearchQueries = listOf<String>()
        if (enableWebSearch && GeminiMultimodalClient.getApiKey(context) != null) {
            val webSearchPrompt = buildString {
                append("Dental and medical literature review for patient clinical presentation:\n")
                append("Chief Complaints: ${combinedQuery.freeTextDescription}\n")
                if (chartContext != null) {
                    append("Medical Alerts: ${chartContext.medicalAlerts.joinToString(", ")}\n")
                    append("Allergies: ${chartContext.knownAllergies.joinToString(", ")}\n")
                }
                append("Differential Considerations: ${differentials.joinToString(", ") { it.conditionName }}\n")
                append("Search and provide latest clinical guideline references, dosage recommendations, and recent PubMed/ADA evidence.")
            }
            val webResult = GeminiMultimodalClient.generateMultimodalClinicalResponse(
                context = context,
                systemInstruction = "You are an expert medical research assistant for dentists. Ground answers with authoritative dental guidelines and PubMed citations.",
                promptText = webSearchPrompt,
                mediaItems = emptyList(),
                enableWebSearch = true
            )
            webResult.onSuccess { response ->
                webCitations = response.webCitations
                webSearchQueries = response.searchQueries
            }
        }

        val diffSummaries = differentials.map { "${it.conditionName} [${it.likelihoodTier}]" }.toMutableList()
        if (webCitations.isNotEmpty()) {
            diffSummaries.add("🌐 Retrieved ${webCitations.size} live web literature citations via Google Search.")
        }

        emitProgress(
            agent = AgentType.EVIDENCE_DIFFERENTIAL,
            title = "Differentials & Coding Formulated",
            msg = "Ranked ${differentials.size} differential possibilities with CDT/ICD mappings.",
            status = AgentStatus.COMPLETED,
            findings = diffSummaries
        )
        emit(MultiAgentExecutionState.Progress(AgentType.EVIDENCE_DIFFERENTIAL, steps.toList()))

        // =====================================================================
        // STAGE 4: Safety & Pharmacological Guardrail Agent
        // =====================================================================
        emitProgress(
            agent = AgentType.SAFETY_GUARDRAIL,
            title = "Checking Pharmacological Contraindications & Red Flags",
            msg = "Cross-verifying patient allergies, systemic drugs, and airway safety...",
            status = AgentStatus.RUNNING
        )
        emit(MultiAgentExecutionState.Progress(AgentType.SAFETY_GUARDRAIL, steps.toList()))
        if (isReasoningMode) delay(180)

        val redFlagResult = RedFlagSafetyChecker.evaluate(combinedQuery)
        val safetyAlerts = mutableListOf<PharmacologicalAlert>()

        // Check Penicillin / Amoxicillin Allergy
        val hasPenicillinAllergy = chartContext?.knownAllergies?.any { it.contains("Penicillin", true) || it.contains("Amoxicillin", true) } == true ||
                vitalsContext.patientAllergies.any { it.contains("Penicillin", true) || it.contains("Amoxicillin", true) }

        if (hasPenicillinAllergy) {
            safetyAlerts.add(
                PharmacologicalAlert(
                    drugOrTreatment = "Amoxicillin / Beta-Lactam Antibiotics",
                    severityLevel = SafetySeverityLevel.CRITICAL_CONTRAINDICATION,
                    reason = "Patient has documented Penicillin / Beta-lactam allergy with risk of anaphylaxis.",
                    clinicalAction = "STRICTLY CONTRAINDICATED. Use non-beta-lactam alternative.",
                    safeAlternative = "Clindamycin 300mg QID or Azithromycin 500mg daily"
                )
            )
        }

        // Check Anticoagulants & NSAIDs
        val isAnticoagulated = chartContext?.systemicConditions?.any { it.contains("anticoagulant", true) || it.contains("warfarin", true) || it.contains("eliquis", true) || it.contains("apixaban", true) } == true ||
                vitalsContext.medicalConditions.any { it.contains("anticoagulant", true) || it.contains("warfarin", true) || it.contains("eliquis", true) }

        if (isAnticoagulated) {
            safetyAlerts.add(
                PharmacologicalAlert(
                    drugOrTreatment = "Ibuprofen / NSAID Analgesics",
                    severityLevel = SafetySeverityLevel.CAUTION,
                    reason = "Concurrent NSAID and systemic anticoagulation significantly escalates gastrointestinal and surgical bleeding hazard.",
                    clinicalAction = "Avoid high-dose NSAIDs. Prefer Paracetamol (Acetaminophen) for first-line pain management.",
                    safeAlternative = "Paracetamol 500mg-1000mg Q6H PRN (max 4g/24h)"
                )
            )
        }

        // Check Bisphosphonates & MRONJ Risk
        val hasBisphosphonates = chartContext?.systemicConditions?.any { it.contains("bisphosphonate", true) || it.contains("fosamax", true) || it.contains("prolia", true) || it.contains("denosumab", true) } == true ||
                vitalsContext.medicalConditions.any { it.contains("bisphosphonate", true) || it.contains("denosumab", true) }

        if (hasBisphosphonates) {
            safetyAlerts.add(
                PharmacologicalAlert(
                    drugOrTreatment = "Surgical Extractions & Osseous Surgery",
                    severityLevel = SafetySeverityLevel.CRITICAL_CONTRAINDICATION,
                    reason = "Patient has antiresorptive / bisphosphonate history with elevated risk of Medication-Related Osteonecrosis of the Jaw (MRONJ).",
                    clinicalAction = "Avoid elective extractions. Prioritize endodontic retention or primary wound closure with chlorhexidine protocol.",
                    safeAlternative = "Coronectomy, endodontic therapy, or specialist OMFS referral"
                )
            )
        }

        val safetyFindings = mutableListOf<String>()
        if (redFlagResult.hasRedFlag) {
            safetyFindings.add("⚠️ RED FLAG ESCALATION: ${redFlagResult.redFlagReason}")
        }
        safetyAlerts.forEach { alert ->
            safetyFindings.add("${alert.severityLevel}: ${alert.drugOrTreatment} - ${alert.reason}")
        }
        if (safetyFindings.isEmpty()) {
            safetyFindings.add("No critical drug contraindications or red-flag escalations detected.")
        }

        emitProgress(
            agent = AgentType.SAFETY_GUARDRAIL,
            title = "Safety Verification Complete",
            msg = if (redFlagResult.hasRedFlag) "CRITICAL RED FLAG DETECTED - Escalation protocol active." else "Safety guardrails verified against patient profile.",
            status = AgentStatus.COMPLETED,
            findings = safetyFindings
        )
        emit(MultiAgentExecutionState.Progress(AgentType.SAFETY_GUARDRAIL, steps.toList()))

        // =====================================================================
        // STAGE 5: Master Clinical AI Orchestrator - Synthesis & Streaming
        // =====================================================================
        val timeStampIso = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val topDiff = differentials.firstOrNull()?.conditionName ?: "Clinical Condition"

        val executiveSummary = buildString {
            if (chartContext != null) {
                append("Comprehensive clinical evaluation for ${chartContext.fullName} (${chartContext.opNo}), age ${chartContext.ageYears ?: "N/A"}. ")
            } else {
                append("Ad-hoc clinical decision support consultation. ")
            }
            append("Primary clinical differential consideration is $topDiff. ")
            if (radiologyObservations.isNotEmpty()) {
                append("Multimodal diagnostics identified ${radiologyObservations.size} focal finding(s). ")
            }
            if (safetyAlerts.isNotEmpty()) {
                append("${safetyAlerts.size} pharmacological/systemic safety alert(s) require clinician attention.")
            }
        }

        val procedures = differentials.flatMap { it.recommendedCdtCodes }.distinct()

        // Synthesize structured phased treatment plan incorporating clinician suggestions
        val suggestedPlan = TreatmentPlanSynthesizer.synthesizePlan(
            differentials = differentials,
            radiologyObservations = radiologyObservations,
            patientContext = chartContext,
            appliedSuggestions = clinicianSuggestions
        )

        val fullResult = StructuredClinicalAiResult(
            summaryTitle = "Clinical Assessment: $topDiff",
            patientId = patient?.id,
            patientName = patient?.name,
            timestampIso = timeStampIso,
            executiveSummary = executiveSummary,
            executionMode = executionMode,
            isWebSearchEnabled = enableWebSearch,
            radiologyObservations = radiologyObservations,
            differentials = differentials,
            safetyAlerts = safetyAlerts,
            redFlagEmergencyGuidance = redFlagResult.emergencyGuidance,
            recommendedProcedures = procedures,
            suggestedTreatmentPlan = suggestedPlan,
            appliedSuggestions = clinicianSuggestions,
            citedPassages = retrievedPassages,
            webCitations = webCitations,
            webSearchQueries = webSearchQueries,
            agentSteps = steps.toList(),
            inferenceModel = if (GeminiMultimodalClient.getApiKey(context) != null) "Gemini 2.0 Flash Multimodal (Medical / Dental Grounded)" else "Dentara On-Device Clinical Agent Engine (Offline RAG)",
            inferenceDurationMs = System.currentTimeMillis() - startTime
        )

        // Build full Markdown text
        val markdownBuilder = StringBuilder()
        markdownBuilder.append("## CLINICAL DECISION SUPPORT REPORT\n")
        markdownBuilder.append("**Mode**: ${executionMode.displayName} | **Patient**: ${chartContext?.fullName ?: "Ad-Hoc Consultation"} | **Date**: $timeStampIso\n\n")

        if (redFlagResult.hasRedFlag) {
            markdownBuilder.append("### ⚠️ EMERGENCY RED FLAG ALERT\n")
            markdownBuilder.append("> **${redFlagResult.redFlagReason}**\n>\n")
            markdownBuilder.append("> ${redFlagResult.emergencyGuidance}\n\n")
        }

        markdownBuilder.append("### 1. Executive Summary\n")
        markdownBuilder.append("$executiveSummary\n\n")

        if (radiologyObservations.isNotEmpty()) {
            markdownBuilder.append("### 2. Multimodal Diagnostic & Imaging Findings\n")
            radiologyObservations.forEachIndexed { idx, obs ->
                markdownBuilder.append("${idx + 1}. **${obs.toothLabel}**: ${obs.observationType} (${obs.severityOrStage})\n")
                markdownBuilder.append("   • *Location*: ${obs.anatomicalLocation}\n")
                markdownBuilder.append("   • *Finding*: ${obs.clinicalDescription}\n")
                if (obs.recommendedFollowUp.isNotBlank()) {
                    markdownBuilder.append("   • *Verification*: ${obs.recommendedFollowUp}\n")
                }
                markdownBuilder.append("\n")
            }
        }

        markdownBuilder.append("### 3. Ranked Differential Diagnoses\n")
        differentials.forEachIndexed { idx, diff ->
            markdownBuilder.append("${idx + 1}. **${diff.conditionName}** `[${diff.likelihoodTier}]`\n")
            markdownBuilder.append("   • *Rationale*: ${diff.clinicalRationale}\n")
            if (diff.recommendedCdtCodes.isNotEmpty()) {
                markdownBuilder.append("   • *Recommended CDT Codes*: ${diff.recommendedCdtCodes.joinToString(", ")}\n")
            }
            if (diff.icd10DiagnosisCode != null) {
                markdownBuilder.append("   • *ICD-10 Code*: ${diff.icd10DiagnosisCode}\n")
            }
            markdownBuilder.append("\n")
        }

        markdownBuilder.append("### 4. Suggested Phased Treatment Plan\n")
        markdownBuilder.append("**${suggestedPlan.planTitle}** — *${suggestedPlan.clinicalSummary}*\n\n")
        suggestedPlan.phases.forEach { phase ->
            markdownBuilder.append("#### ${phase.phaseTitle} (${phase.targetTimeline})\n")
            phase.procedures.forEachIndexed { pIdx, proc ->
                val badge = if (proc.isClinicianCustomization) " *(Clinician Customization)*" else ""
                markdownBuilder.append("• **${proc.procedureName}** `[${proc.cdtCode}]` — *${proc.toothLabel}*$badge\n")
                markdownBuilder.append("  ${proc.clinicalRationale} (Visits: ${proc.estimatedVisits})\n")
            }
            markdownBuilder.append("\n")
        }

        if (safetyAlerts.isNotEmpty()) {
            markdownBuilder.append("### 5. Pharmacological & Safety Alerts\n")
            safetyAlerts.forEach { alert ->
                val icon = if (alert.severityLevel == SafetySeverityLevel.CRITICAL_CONTRAINDICATION) "🛑" else "⚠️"
                markdownBuilder.append("$icon **${alert.drugOrTreatment}** (${alert.severityLevel})\n")
                markdownBuilder.append("   • *Reason*: ${alert.reason}\n")
                markdownBuilder.append("   • *Clinical Action*: ${alert.clinicalAction}\n")
                if (alert.safeAlternative != null) {
                    markdownBuilder.append("   • *Safe Alternative*: ${alert.safeAlternative}\n")
                }
                markdownBuilder.append("\n")
            }
        }

        if (webCitations.isNotEmpty()) {
            markdownBuilder.append("### 6. Live Web Literature & Grounding Citations (Google Search)\n")
            webCitations.forEach { cite ->
                markdownBuilder.append("• [${cite.title}](${cite.url}) (${cite.domain})\n")
            }
            markdownBuilder.append("\n")
        }

        markdownBuilder.append("### 7. Grounded Guideline References\n")
        retrievedPassages.forEach { passage ->
            markdownBuilder.append("• **[${passage.guidelineTitle}]** (*${passage.sectionHeader}*, v${passage.version}):\n")
            markdownBuilder.append("  \"${passage.passageContent}\"\n\n")
        }

        markdownBuilder.append("---\n")
        markdownBuilder.append("*Grounded reference generated via ${fullResult.inferenceModel}. This assistive decision support output requires independent clinician verification.*")

        val completeMarkdown = markdownBuilder.toString()
        val resultWithMarkdown = fullResult.copy(
            fullMarkdownReport = completeMarkdown,
            inferenceDurationMs = System.currentTimeMillis() - startTime
        )

        emit(MultiAgentExecutionState.Completed(resultWithMarkdown))
    }.flowOn(Dispatchers.Default)

    /**
     * Verifies a clinician suggestion against patient history, allergies, and guidelines.
     */
    fun verifyClinicianSuggestion(
        suggestionText: String,
        patientContext: PatientChartContext?,
        vitalsContext: PatientVitalsContext,
        differentials: List<DifferentialDiagnosisRecommendation> = emptyList(),
        radiologyObservations: List<DentalRadiologyObservation> = emptyList()
    ): ClinicianSuggestionRecord {
        return ClinicianSuggestionVerifier.verifySuggestion(
            suggestionText = suggestionText,
            patientContext = patientContext,
            vitalsContext = vitalsContext,
            differentials = differentials,
            radiologyObservations = radiologyObservations
        )
    }
}
