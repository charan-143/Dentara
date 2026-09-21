package com.example.thornburydental.data.cds

import android.graphics.Bitmap
import kotlinx.serialization.Serializable

/**
 * Enumeration of specialized clinical agents in the Dentara Multi-Agent System.
 */
enum class AgentType(val displayName: String, val roleDescription: String) {
    INTAKE_SYNTHESIZER(
        displayName = "Patient Intake & Context Synthesizer",
        roleDescription = "Compiles demographics, medical history, systemic alerts, and tooth conditions."
    ),
    MULTIMODAL_RADIOLOGY(
        displayName = "Multimodal Dental Radiology Agent",
        roleDescription = "Inspects radiographs, intraoral photos, CBCT scans, and PDF lab results."
    ),
    EVIDENCE_DIFFERENTIAL(
        displayName = "Evidence & Differential Synthesis Agent",
        roleDescription = "Cross-references dental guidelines (ADA/AAE/AAP) to rank clinical differentials."
    ),
    SAFETY_GUARDRAIL(
        displayName = "Pharmacological & Red-Flag Safety Agent",
        roleDescription = "Evaluates contraindications, drug interactions, and emergency escalation criteria."
    ),
    MASTER_ORCHESTRATOR(
        displayName = "Clinical AI Master Orchestrator",
        roleDescription = "Coordinates parallel agent execution and produces unified clinical reports."
    )
}

/**
 * Execution state for individual sub-agents.
 */
enum class AgentStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    SKIPPED
}

/**
 * Progress tracking item for multi-agent visual timeline in Compose UI.
 */
@Serializable
data class AgentExecutionStep(
    val agentType: AgentType,
    val stepTitle: String,
    val detailMessage: String,
    val status: AgentStatus = AgentStatus.PENDING,
    val timestampMs: Long = System.currentTimeMillis(),
    val keyFindings: List<String> = emptyList()
)

/**
 * Ingested multimodal media item (radiograph, intraoral photo, CBCT slice, or PDF report).
 */
data class MultimodalMediaItem(
    val id: String,
    val reportId: String,
    val name: String,
    val mimeType: String,
    val localUri: String? = null,
    val mediaKind: MediaAnalysisKind = MediaAnalysisKind.GENERAL_DOCUMENT,
    val thumbnailBitmap: Bitmap? = null,
    val base64Data: String? = null,
    val isSelected: Boolean = true,
    val sizeBytes: Long = 0L,
    val textExtract: String? = null
)

enum class MediaAnalysisKind(val label: String) {
    RADIOGRAPH("Dental Radiograph (X-Ray / OPG)"),
    INTRAORAL_PHOTO("Intraoral Clinical Photography"),
    CBCT_SCAN("CBCT 3D / Axial Scan"),
    LAB_PDF("Diagnostic Lab Report / PDF"),
    GENERAL_DOCUMENT("Clinical Document / Chart Attachment")
}

/**
 * Specific radiological or image observation localized to dental anatomy.
 */
@Serializable
data class DentalRadiologyObservation(
    val toothNumber: Int? = null,
    val toothLabel: String = "General / Multi-quadrant",
    val observationType: String, // e.g. "Interproximal Caries", "Periapical Radiolucency", "Alveolar Crestal Bone Loss"
    val severityOrStage: String = "Mild", // "Mild", "Moderate", "Advanced / Severe", "Incipient"
    val anatomicalLocation: String = "", // e.g. "Mesial surface, coronal third", "Root apex"
    val clinicalDescription: String,
    val confidence: String = "High", // "High", "Moderate", "Preliminary"
    val recommendedFollowUp: String = ""
)

/**
 * Differential diagnosis recommendation item with clinical evidence and coding.
 */
@Serializable
data class DifferentialDiagnosisRecommendation(
    val conditionName: String,
    val likelihoodTier: String, // "High Suspicion", "Moderate Possibility", "Consider Rule-out"
    val clinicalRationale: String,
    val associatedTeeth: List<Int> = emptyList(),
    val recommendedCdtCodes: List<String> = emptyList(), // e.g. ["D0140", "D3330", "D2740"]
    val icd10DiagnosisCode: String? = null, // e.g. "K04.01", "K05.32"
    val citedGuidelineIds: List<String> = emptyList()
)

/**
 * Pharmacological alert or contraindication flagged by Safety Guardrail Agent.
 */
@Serializable
data class PharmacologicalAlert(
    val drugOrTreatment: String,
    val severityLevel: SafetySeverityLevel,
    val reason: String,
    val clinicalAction: String,
    val safeAlternative: String? = null
)

enum class SafetySeverityLevel {
    INFO,
    CAUTION,
    CRITICAL_CONTRAINDICATION,
    EMERGENCY_ESCALATION
}

/**
 * Complete patient chart snapshot ready for AI contextual ingestion.
 */
@Serializable
data class PatientChartContext(
    val patientId: String,
    val opNo: String,
    val fullName: String,
    val ageYears: Int?,
    val dob: String,
    val isChild: Boolean = false,
    val medicalAlerts: List<String> = emptyList(),
    val knownAllergies: List<String> = emptyList(),
    val systemicConditions: List<String> = emptyList(),
    val pastDentalHistory: String = "",
    val activeTeethConditionsSummary: String = "",
    val decayedTeethNumbers: List<Int> = emptyList(),
    val filledTeethNumbers: List<Int> = emptyList(),
    val missingTeethNumbers: List<Int> = emptyList(),
    val rctTeethNumbers: List<Int> = emptyList(),
    val crownTeethNumbers: List<Int> = emptyList(),
    val chiefComplaints: List<String> = emptyList(),
    val painScale: String = "",
    val cariesRisk: String = "",
    val periodontalStatus: String = "",
    val lastRecordedDiagnosis: String = ""
)

/**
 * Execution mode for the Clinical AI system.
 */
enum class ClinicalAiExecutionMode(val displayName: String, val shortBadge: String, val description: String) {
    NORMAL(
        displayName = "Normal Mode",
        shortBadge = "⚡ FAST SYNTHESIS",
        description = "Single-pass rapid clinical diagnostic synthesis & guideline matching."
    ),
    AGENT_REASONING(
        displayName = "Agent Reasoning Mode",
        shortBadge = "🧠 MULTI-AGENT REASONING",
        description = "Deep multi-agent chain-of-thought orchestration with specialized clinical agents."
    )
}

/**
 * Web search grounding citation retrieved from online literature/databases via Gemini Grounding.
 */
@Serializable
data class WebSearchCitation(
    val title: String,
    val url: String,
    val snippet: String? = null,
    val domain: String = ""
)

/**
 * Individual clinical procedure within an AI-suggested treatment plan.
 */
@Serializable
data class SuggestedTreatmentProcedure(
    val procedureName: String,
    val toothNumber: Int? = null,
    val toothLabel: String = "General",
    val cdtCode: String = "", // e.g. "D3330", "D2740", "D4341"
    val priority: String = "High", // "Urgent / Emergency", "High", "Moderate", "Preventive / Maintenance"
    val clinicalRationale: String = "",
    val estimatedVisits: Int = 1,
    val isClinicianCustomization: Boolean = false
)

/**
 * Phased appointment group within an AI-suggested treatment plan.
 */
@Serializable
data class SuggestedTreatmentPhase(
    val phaseNumber: Int,
    val phaseTitle: String, // e.g. "Phase 1: Emergency & Stabilization", "Phase 2: Definitive Restorative & Endodontics", "Phase 3: Prevention & Maintenance"
    val targetTimeline: String = "Immediate", // e.g. "Immediate (Visit 1)", "1-2 Weeks Post-Op", "3-6 Month Recall"
    val procedures: List<SuggestedTreatmentProcedure> = emptyList()
)

/**
 * Complete structured treatment plan suggested by Clinical AI.
 */
@Serializable
data class SuggestedTreatmentPlan(
    val planTitle: String,
    val clinicalSummary: String,
    val primaryDiagnosis: String,
    val phases: List<SuggestedTreatmentPhase> = emptyList(),
    val totalEstimatedProcedures: Int = 0,
    val clinicianCustomizations: List<String> = emptyList()
)

/**
 * Status of clinician suggestion verification against allergies, medical history, and clinical guidelines.
 */
enum class SuggestionVerificationStatus(val label: String, val badgeColorHex: Long) {
    VERIFIED_SAFE("Verified Safe & Guideline-Compliant", 0xFF2E7D32),
    ACCEPTED_WITH_CAUTION("Accepted with Clinical Caution", 0xFFE65100),
    CONTRAINDICATED("Contraindicated / Safety Hazard", 0xFFC62828),
    REQUIRES_MODIFICATION("Requires Clinical Modification", 0xFF1565C0)
}

/**
 * Clinician input suggestion with verification metadata and CDT mapping.
 */
@Serializable
data class ClinicianSuggestionRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val suggestionText: String,
    val timestampIso: String = "",
    val verificationStatus: SuggestionVerificationStatus = SuggestionVerificationStatus.VERIFIED_SAFE,
    val feedbackMessage: String = "",
    val detectedCdtCodes: List<String> = emptyList(),
    val affectedTeeth: List<Int> = emptyList(),
    val riskWarnings: List<String> = emptyList(),
    val isApplied: Boolean = true
)

/**
 * Structured output generated by the Multi-Agent Clinical AI System.
 */
@Serializable
data class StructuredClinicalAiResult(
    val summaryTitle: String,
    val patientId: String?,
    val patientName: String?,
    val timestampIso: String,
    val executiveSummary: String,
    val executionMode: ClinicalAiExecutionMode = ClinicalAiExecutionMode.AGENT_REASONING,
    val isWebSearchEnabled: Boolean = false,
    val radiologyObservations: List<DentalRadiologyObservation> = emptyList(),
    val differentials: List<DifferentialDiagnosisRecommendation> = emptyList(),
    val safetyAlerts: List<PharmacologicalAlert> = emptyList(),
    val redFlagEmergencyGuidance: String? = null,
    val recommendedProcedures: List<String> = emptyList(),
    val suggestedTreatmentPlan: SuggestedTreatmentPlan? = null,
    val appliedSuggestions: List<ClinicianSuggestionRecord> = emptyList(),
    val citedPassages: List<RetrievedPassage> = emptyList(),
    val webCitations: List<WebSearchCitation> = emptyList(),
    val webSearchQueries: List<String> = emptyList(),
    val agentSteps: List<AgentExecutionStep> = emptyList(),
    val inferenceModel: String = "Gemini 2.0 Flash (Multimodal Medical / Dental)",
    val inferenceDurationMs: Long = 0L,
    val fullMarkdownReport: String = ""
)


