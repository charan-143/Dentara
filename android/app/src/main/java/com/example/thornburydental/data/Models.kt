package com.example.thornburydental.data

import kotlinx.serialization.Serializable

// =============================================================================
// Thornbury Dental Data Models
// =============================================================================

@Serializable
enum class UserRole {
    CLINICIAN,
    PATIENT,
    RECEPTIONIST
}

@Serializable
data class User(
    val id: String,
    val email: String,
    val name: String,
    val role: UserRole,
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class Clinician(
    val id: String,
    val name: String,
    val credentials: String,
    val specialty: String,
    val room: String,
    val bio: String,
    val phone: String = "+1 (503) 224-7700"
)

enum class ToothCondition(val label: String, val code: String) {
    SOUND("Sound", "S"),
    DECAY("Active Caries / Decay", "D"),
    FILLED("Restored / Filled", "F"),
    CROWN("Full Crown", "Cr"),
    MISSING("Missing", "M"),
    IMPLANT("Dental Implant", "Imp"),
    ROOT_CANAL("Endodontic / Root Canal", "RCT")
}

@Serializable
data class ToothRecord(
    val number: Int,             // Universal numbering 1-32
    val fdiNumber: Int,          // FDI numbering (11-48)
    val name: String,
    val arch: String,            // "Maxillary (Upper)" or "Mandibular (Lower)"
    val condition: ToothCondition = ToothCondition.SOUND,
    val notes: String = ""
)

@Serializable
data class Allergy(
    val allergen: String,
    val severity: String,        // "Severe / Anaphylaxis", "Moderate", "Mild"
    val reaction: String
)

enum class ToothAnatomyType {
    MOLAR,
    PREMOLAR,
    CANINE,
    INCISOR
}

fun getToothAnatomyType(toothNumber: Int): ToothAnatomyType {
    return when (toothNumber) {
        // Adult Permanent FDI Notation (11-48) & Primary Child FDI Notation (51-85)
        18, 17, 16, 26, 27, 28, 38, 37, 36, 46, 47, 48,
        55, 54, 64, 65, 75, 74, 84, 85 -> ToothAnatomyType.MOLAR

        15, 14, 24, 25, 34, 35, 44, 45 -> ToothAnatomyType.PREMOLAR

        13, 23, 33, 43, 53, 63, 73, 83 -> ToothAnatomyType.CANINE

        12, 11, 21, 22, 31, 32, 41, 42,
        51, 52, 61, 62, 71, 72, 81, 82 -> ToothAnatomyType.INCISOR

        // Universal 1-32 fallback
        1, 2, 3, 14, 15, 16, 17, 18, 19, 30, 31, 32 -> ToothAnatomyType.MOLAR
        4, 5, 12, 13, 20, 21, 28, 29 -> ToothAnatomyType.PREMOLAR
        6, 11, 22, 27 -> ToothAnatomyType.CANINE
        7, 8, 9, 10, 23, 24, 25, 26 -> ToothAnatomyType.INCISOR
        else -> ToothAnatomyType.MOLAR
    }
}

@Serializable
data class ExaminationAnswers(
    val chiefComplaints: List<String> = emptyList(),
    val chiefComplaintOther: String = "",
    val painSeverity: String = "",
    val sensitivityTriggers: List<String> = emptyList(),
    val periodontalBleeding: List<String> = emptyList(),
    val periodontalPockets: List<String> = emptyList(),
    val gingivalRecession: List<String> = emptyList(),
    val softTissue: List<String> = emptyList(),
    val stains: List<String> = emptyList(),
    val calculus: List<String> = emptyList(),
    val tmjAssessment: List<String> = emptyList(),
    val functionalHabits: List<String> = emptyList(),
    val brushingFrequency: String = "",
    val flossingFrequency: String = "",
    val cariesRisk: String = "",
    val otherDiagnosesConditions: List<String> = emptyList(),
    val otherDiagnosesNotes: String = "",
    val clinicianNotes: String = "",

    /**
     * Audit trail for values written by voice dictation, so a dictated reading can be told
     * apart from one a clinician typed and checked.
     *
     * Lives in this blob rather than its own table: it serialises into the existing
     * exam-answers JSON column, so it needs no schema migration.
     */
    val voiceEntries: List<VoiceChartEntry> = emptyList()
)

@Serializable
data class PatientDiagnosis(
    val primaryDiagnosis: String = "",
    val clinicalFindings: String = "",
    val prognosis: String = "Good",
    val systemicConsiderations: String = "",
    val dateRecorded: String = "",
    val lastUpdated: String = "",
    val clinicianName: String = ""
)

@Serializable
data class Patient(
    val id: String,
    val opNo: String,            // E.g. "OP-40182"
    val name: String,
    val dob: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val isChild: Boolean = false, // Pediatric flag: false = Adult (32 teeth), true = Child (20 primary teeth)
    val medicalHistory: String = "",
    val familyHistory: String = "",
    val pastDentalHistory: String = "",
    val lastVisit: String = "never",
    val medicalAlerts: List<String> = emptyList(),
    val allergies: List<Allergy> = emptyList(),
    val teeth: Map<Int, ToothRecord> = emptyMap(),
    val examAnswers: ExaminationAnswers? = null,
    val diagnosis: PatientDiagnosis? = null
)

@Serializable
data class ReportAttachment(
    val id: String,
    val name: String,
    val sizeStr: String,
    val mimeType: String,
    val uri: String? = null
)

@Serializable
data class DiagnosticReport(
    val id: String,
    val patientId: String,
    val clinicianName: String,
    val kind: String,            // "Radiograph", "Charting", "Chairside test", "CBCT Scan"
    val title: String,
    val summary: String,
    val takenAt: String,
    val releasedAt: String? = null,
    val image: String? = null,
    val attachments: List<ReportAttachment> = emptyList()
)

typealias DentalReport = DiagnosticReport

@Serializable
data class PlanStep(
    val id: String,
    val toothNumber: Int?,
    val toothNumbers: List<Int> = emptyList(),
    val procedure: String,
    val code: String,
    val fee: Double,
    val completed: Boolean = false
) {
    val procedureCode: String get() = code

    val effectiveToothNumbers: List<Int> get() {
        if (toothNumbers.isNotEmpty()) return toothNumbers
        return if (toothNumber != null) listOf(toothNumber) else emptyList()
    }

    val toothDisplayString: String get() {
        val teeth = effectiveToothNumbers
        return when {
            teeth.isEmpty() -> "General"
            teeth.size == 1 -> "Tooth ${teeth.first()}"
            else -> "Teeth ${teeth.joinToString(", ")}"
        }
    }
}

@Serializable
data class PlanAddendum(
    val id: String,
    val author: String,
    val text: String,
    val date: String
)

@Serializable
data class TreatmentPlan(
    val id: String,
    val patientId: String,
    val title: String = "Comprehensive Treatment Plan",
    val clinicianName: String,
    val diagnosis: String = "",
    val dateCreated: String,
    val isLocked: Boolean = true, // Immutable once published (tamper-evident)
    val tamperHash: String,
    val steps: List<PlanStep>,
    val addenda: List<PlanAddendum> = emptyList()
)

@Serializable
data class Prescription(
    val id: String,
    val patientId: String,
    val patientName: String,
    val clinicianName: String,
    val drugName: String,
    val dosage: String,
    val frequency: String,
    val duration: String,
    val instructions: String,
    val issueDate: String,
    val isDispensed: Boolean = false
)

@Serializable
data class Appointment(
    val id: String,
    val patientId: String,
    val patientName: String,
    val patientOpNo: String,
    val patientDob: String,
    val clinicianId: String,
    val clinicianName: String,
    val date: String = "",        // ISO "yyyy-MM-dd" — which calendar day this appointment falls on
    val time: String,             // E.g. "09:00"
    val durationMin: Int = 45,
    val room: String = "Surgery 1",
    val procedure: String,
    val allergyList: String? = null,
    val status: String = "confirmed", // "confirmed", "completed", "cancelled"
    val reminderEnabled: Boolean = false,
    val reminderLeadTimeMin: Int = 15
)

@Serializable
data class DraftPlan(
    val id: String,
    val patientId: String,
    val patientName: String,
    val procedure: String,
    val phase: String // "pre" or "post"
)

@Serializable
data class HeldResult(
    val id: String,
    val patientId: String,
    val patientName: String,
    val title: String,
    val kind: String // "Imaging", "Histology", "Periodontal Probe"
)

@Serializable
data class UserProfilePreferences(
    val id: String = "primary_profile",
    val fullName: String = "",
    val pronouns: String = "",
    val dob: String = "",
    val phone: String = "",
    val email: String = "",
    val dentalGoals: List<String> = emptyList(),
    val anxietyLevel: String = "Relaxed",
    val comfortAmenities: List<String> = emptyList(),
    val anesthesiaPreference: String = "Standard Local Anesthetic",
    val medicalAlerts: List<String> = emptyList(),
    val lastVisit: String = "Within 6 months",
    val schedulePreference: String = "Morning (8am - 12pm)",
    val contactChannel: String = "SMS / WhatsApp",
    val additionalNotes: String = "",
    val isOnboardingCompleted: Boolean = false,
    val morningReminderEnabled: Boolean = true,
    val morningReminderTime: String = "08:00",
    val chairsideReminderDefaultMin: Int = 15,
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class MedicationPreset(
    val id: String,
    val name: String,
    val dosage: String,
    val frequency: String,
    val duration: String,
    val instructions: String,
    val category: String = "General",
    val isCustom: Boolean = false
) {
    companion object {
        val defaultPresets = listOf(
            MedicationPreset(
                id = "preset-amoxicillin",
                name = "Amoxicillin",
                dosage = "500 mg capsules",
                frequency = "1 capsule every 8 hours",
                duration = "5 days",
                instructions = "Take with water. Complete the entire course.",
                category = "Antibiotics",
                isCustom = false
            ),
            MedicationPreset(
                id = "preset-clindamycin",
                name = "Clindamycin",
                dosage = "300 mg capsules",
                frequency = "1 capsule every 6 hours",
                duration = "7 days",
                instructions = "Penicillin-allergic option. Take with plenty of water.",
                category = "Antibiotics",
                isCustom = false
            ),
            MedicationPreset(
                id = "preset-metronidazole",
                name = "Metronidazole",
                dosage = "400 mg tablets",
                frequency = "1 tablet every 8 hours",
                duration = "5 days",
                instructions = "Avoid all alcohol during treatment and for 48 hours after.",
                category = "Antibiotics",
                isCustom = false
            ),
            MedicationPreset(
                id = "preset-ibuprofen",
                name = "Ibuprofen",
                dosage = "600 mg tablets",
                frequency = "1 tablet every 6 to 8 hours PRN",
                duration = "3 days",
                instructions = "Take strictly with food or milk. Max 2400mg in 24 hours.",
                category = "Analgesics",
                isCustom = false
            ),
            MedicationPreset(
                id = "preset-paracetamol",
                name = "Paracetamol",
                dosage = "500 mg tablets",
                frequency = "2 tablets every 6 hours PRN",
                duration = "3 days",
                instructions = "Max 4000mg in 24 hours. Do not take with other acetaminophen.",
                category = "Analgesics",
                isCustom = false
            ),
            MedicationPreset(
                id = "preset-chlorhexidine",
                name = "Chlorhexidine 0.2%",
                dosage = "300 mL rinse",
                frequency = "15 mL twice daily",
                duration = "7 days",
                instructions = "Rinse for 60 seconds after brushing. Do not swallow.",
                category = "Antiseptics",
                isCustom = false
            )
        )
    }
}

