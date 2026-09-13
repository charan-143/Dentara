package com.example.thornburydental.data

import kotlinx.serialization.Serializable

// =============================================================================
// Thornbury Dental Data Models
// =============================================================================

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
        // Universal 1-32 system
        // Upper: 1,2,3 (molar), 4,5 (premolar), 6 (canine), 7,8 (incisor), 9,10 (incisor), 11 (canine), 12,13 (premolar), 14,15,16 (molar)
        // Lower: 17,18,19 (molar), 20,21 (premolar), 22 (canine), 23,24 (incisor), 25,26 (incisor), 27 (canine), 28,29 (premolar), 30,31,32 (molar)
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
    val softTissue: List<String> = emptyList(),
    val functionalHabits: List<String> = emptyList(),
    val brushingFrequency: String = "",
    val flossingFrequency: String = "",
    val cariesRisk: String = "",
    val clinicianNotes: String = ""
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
    val medicalHistory: String = "",
    val familyHistory: String = "",
    val pastDentalHistory: String = "",
    val lastVisit: String = "never",
    val medicalAlerts: List<String> = emptyList(),
    val allergies: List<Allergy> = emptyList(),
    val teeth: Map<Int, ToothRecord> = emptyMap(),
    val examAnswers: ExaminationAnswers? = null
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
    val image: String? = null
)

@Serializable
data class PlanStep(
    val id: String,
    val toothNumber: Int?,
    val procedure: String,
    val code: String,
    val fee: Double,
    val completed: Boolean = false
)

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
    val clinicianName: String,
    val diagnosis: String,
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
    val time: String,             // E.g. "09:00"
    val durationMin: Int = 45,
    val room: String = "Surgery 1",
    val procedure: String,
    val allergyList: String? = null,
    val status: String = "confirmed" // "confirmed", "completed", "cancelled"
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
