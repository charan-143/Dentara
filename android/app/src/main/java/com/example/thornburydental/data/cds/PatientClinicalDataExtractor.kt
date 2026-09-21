package com.example.thornburydental.data.cds

import com.example.thornburydental.data.Patient
import com.example.thornburydental.data.ToothCondition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Extracts and formats longitudinal clinical information, demographics,
 * examination answers, and tooth record statuses from a Patient domain object
 * into structured AI agent contexts.
 */
object PatientClinicalDataExtractor {

    /**
     * Converts a Patient domain entity into a structured PatientChartContext.
     */
    fun extractChartContext(patient: Patient): PatientChartContext {
        val age = calculateAge(patient.dob)
        
        val decayed = mutableListOf<Int>()
        val filled = mutableListOf<Int>()
        val missing = mutableListOf<Int>()
        val rct = mutableListOf<Int>()
        val crown = mutableListOf<Int>()

        patient.teeth.values.forEach { tooth ->
            when (tooth.condition) {
                ToothCondition.DECAY -> decayed.add(tooth.number)
                ToothCondition.FILLED -> filled.add(tooth.number)
                ToothCondition.MISSING -> missing.add(tooth.number)
                ToothCondition.ROOT_CANAL -> rct.add(tooth.number)
                ToothCondition.CROWN -> crown.add(tooth.number)
                ToothCondition.IMPLANT -> filled.add(tooth.number)
                ToothCondition.SOUND -> {}
            }
        }

        val teethSummary = buildString {
            if (decayed.isNotEmpty()) append("Active Decay (Teeth: ${decayed.joinToString(", ")}); ")
            if (rct.isNotEmpty()) append("Root Canal Treated (Teeth: ${rct.joinToString(", ")}); ")
            if (crown.isNotEmpty()) append("Prosthetic Crowns (Teeth: ${crown.joinToString(", ")}); ")
            if (filled.isNotEmpty()) append("Restored/Filled (Teeth: ${filled.joinToString(", ")}); ")
            if (missing.isNotEmpty()) append("Missing/Edentulous (Teeth: ${missing.joinToString(", ")})")
        }.ifEmpty { "All charted teeth sound" }

        val exam = patient.examAnswers
        val complaints = exam?.chiefComplaints?.toMutableList() ?: mutableListOf()
        if (exam != null && exam.chiefComplaintOther.isNotBlank()) {
            complaints.add(exam.chiefComplaintOther)
        }

        val allergies = patient.allergies.map { "${it.allergen} (${it.severity}: ${it.reaction})" }

        val perioStatus = buildString {
            if (exam != null) {
                if (exam.periodontalBleeding.isNotEmpty()) {
                    append("Bleeding: ${exam.periodontalBleeding.joinToString(", ")}; ")
                }
                if (exam.periodontalPockets.isNotEmpty()) {
                    append("Pocket Depth: ${exam.periodontalPockets.joinToString(", ")}; ")
                }
                if (exam.gingivalRecession.isNotEmpty()) {
                    append("Recession: ${exam.gingivalRecession.joinToString(", ")}")
                }
            }
        }.ifEmpty { "No severe periodontal breakdown noted" }

        return PatientChartContext(
            patientId = patient.id,
            opNo = patient.opNo,
            fullName = patient.name,
            ageYears = age,
            dob = patient.dob,
            isChild = patient.isChild,
            medicalAlerts = patient.medicalAlerts,
            knownAllergies = allergies,
            systemicConditions = if (patient.medicalHistory.isNotBlank()) listOf(patient.medicalHistory) else emptyList(),
            pastDentalHistory = patient.pastDentalHistory,
            activeTeethConditionsSummary = teethSummary,
            decayedTeethNumbers = decayed,
            filledTeethNumbers = filled,
            missingTeethNumbers = missing,
            rctTeethNumbers = rct,
            crownTeethNumbers = crown,
            chiefComplaints = complaints,
            painScale = exam?.painSeverity.orEmpty(),
            cariesRisk = exam?.cariesRisk.orEmpty(),
            periodontalStatus = perioStatus,
            lastRecordedDiagnosis = patient.diagnosis?.primaryDiagnosis.orEmpty()
        )
    }

    /**
     * Builds a comprehensive text prompt suitable for clinical LLM reasoning.
     */
    fun buildAgentContextPrompt(context: PatientChartContext): String {
        return buildString {
            appendLine("=== PATIENT CLINICAL DOSSIER ===")
            appendLine("Patient ID: ${context.opNo} | Name: ${context.fullName}")
            appendLine("Demographics: Age ${context.ageYears ?: "Unknown"} years (DOB: ${context.dob}) | Dentition Mode: ${if (context.isChild) "Pediatric / Primary" else "Permanent / Adult"}")
            
            if (context.medicalAlerts.isNotEmpty()) {
                appendLine("⚠️ CRITICAL MEDICAL ALERTS: ${context.medicalAlerts.joinToString(", ")}")
            }
            if (context.knownAllergies.isNotEmpty()) {
                appendLine("ALLERGIES: ${context.knownAllergies.joinToString("; ")}")
            } else {
                appendLine("ALLERGIES: No known drug or contact allergies recorded (NKDA)")
            }

            if (context.systemicConditions.isNotEmpty()) {
                appendLine("SYSTEMIC MEDICAL HISTORY: ${context.systemicConditions.joinToString("; ")}")
            }

            if (context.pastDentalHistory.isNotBlank()) {
                appendLine("PAST DENTAL HISTORY: ${context.pastDentalHistory}")
            }

            appendLine("ODONTOGRAM STATUS: ${context.activeTeethConditionsSummary}")

            if (context.chiefComplaints.isNotEmpty()) {
                appendLine("CHIEF COMPLAINTS: ${context.chiefComplaints.joinToString(", ")}")
            }
            if (context.painScale.isNotBlank()) {
                appendLine("PAIN SEVERITY: ${context.painScale}")
            }
            if (context.cariesRisk.isNotBlank()) {
                appendLine("CARIES RISK PROFILE: ${context.cariesRisk}")
            }
            if (context.periodontalStatus.isNotBlank()) {
                appendLine("PERIODONTAL ASSESSMENT: ${context.periodontalStatus}")
            }
            if (context.lastRecordedDiagnosis.isNotBlank()) {
                appendLine("PREVIOUS DIAGNOSIS: ${context.lastRecordedDiagnosis}")
            }
        }
    }

    private fun calculateAge(dobString: String): Int? {
        if (dobString.isBlank()) return null
        val formats = listOf("yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy", "dd MMM yyyy", "yyyy")
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                val birthDate = sdf.parse(dobString) ?: continue
                val dobCal = Calendar.getInstance().apply { time = birthDate }
                val todayCal = Calendar.getInstance()

                var age = todayCal.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR)
                if (todayCal.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
                    age--
                }
                if (age in 0..125) return age
            } catch (_: Exception) {}
        }
        return null
    }
}
