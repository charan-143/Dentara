package com.example.thornburydental

import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.Patient
import com.example.thornburydental.data.ToothCondition
import com.example.thornburydental.speech.ClinicalExamFindings
import com.example.thornburydental.speech.ClinicalFindings
import com.example.thornburydental.speech.ClinicalVoiceExtractor
import com.example.thornburydental.speech.PerioMeasurementFinding
import com.example.thornburydental.speech.ToothConditionFinding
import com.example.thornburydental.speech.TreatmentPlanFinding
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ClinicalVoiceExtractorTest {

    private lateinit var patient: Patient

    @Before
    fun setUp() {
        patient = DentalRepository.registerPatient(
            name = "John Doe",
            opNo = "OP-1001",
            dob = "01/01/1985",
            phone = "555-0100",
            email = "john@example.com",
            address = "123 Main St",
            allergies = emptyList(),
            medicalAlerts = emptyList(),
            medicalHistory = ""
        )
    }

    private fun current(): Patient =
        DentalRepository.patients.value.first { it.id == patient.id }

    @Test
    fun extractToothConditionsCorrectly() {
        val transcript = "Tooth 14 occlusal caries and tooth 19 missing and tooth 30 root canal"
        val findings = ClinicalVoiceExtractor.extractDeterministic(transcript)

        assertFalse(findings.isEmpty)
        assertEquals(3, findings.toothConditions.size)

        val t14 = findings.toothConditions.first { it.toothNumber == 14 }
        assertEquals(ToothCondition.DECAY, t14.condition)

        val t19 = findings.toothConditions.first { it.toothNumber == 19 }
        assertEquals(ToothCondition.MISSING, t19.condition)

        val t30 = findings.toothConditions.first { it.toothNumber == 30 }
        assertEquals(ToothCondition.ROOT_CANAL, t30.condition)
    }

    @Test
    fun extractPeriodontalPocketsWithBleeding() {
        val transcript = "Tooth 3 mesiobuccal 4mm with bleeding and tooth 14 pocket 5mm"
        val findings = ClinicalVoiceExtractor.extractDeterministic(transcript)

        assertFalse(findings.isEmpty)
        assertTrue(findings.perioMeasurements.isNotEmpty())

        val t3 = findings.perioMeasurements.first { it.toothNumber == 3 }
        assertEquals(4, t3.depthMm)
        assertTrue(t3.isBleeding)
        assertEquals("MB", t3.site)

        val t14 = findings.perioMeasurements.first { it.toothNumber == 14 }
        assertEquals(5, t14.depthMm)
    }

    @Test
    fun extractTreatmentPlanAndClinicalNotes() {
        val transcript = "Patient reports sensitivity to cold. Recommend composite filling for tooth 14"
        val findings = ClinicalVoiceExtractor.extractDeterministic(transcript)

        assertTrue(findings.examFindings.sensitivityTriggers.contains("Cold"))
        assertTrue(findings.treatmentPlanItems.any { it.procedure.contains("composite filling", ignoreCase = true) && it.toothNumber == 14 })
    }

    @Test
    fun applyClinicalFindingsLogsIntoSeparateDedicatedFields() {
        val findings = ClinicalFindings(
            rawTranscript = "Tooth 14 occlusal decay. Tooth 3 buccal 4mm bleeding. Chief complaint pain on chewing. Cold sensitivity. High caries risk. Diagnosis moderate periodontitis. Patient prefers morning visits.",
            toothConditions = listOf(
                ToothConditionFinding(14, ToothCondition.DECAY, surface = "MOD", notes = "Deep active decay")
            ),
            perioMeasurements = listOf(
                PerioMeasurementFinding(3, site = "Buccal", depthMm = 4, isBleeding = true)
            ),
            treatmentPlanItems = listOf(
                TreatmentPlanFinding(toothNumber = 14, procedure = "Composite Resin Restoration", estimatedCost = 150.0)
            ),
            examFindings = ClinicalExamFindings(
                chiefComplaints = listOf("Pain on chewing"),
                painSeverity = "Moderate (5/10)",
                sensitivityTriggers = listOf("Cold"),
                cariesRisk = "High",
                diagnosis = "Moderate Periodontitis",
                extraNotes = listOf("Patient prefers morning visits")
            )
        )

        val applied = DentalRepository.applyClinicalFindings(patient.id, findings, "Dr. Smith")
        assertTrue(applied)

        val updated = current()

        // 1. Verify Odontogram separate field
        val tooth14 = updated.teeth.values.first { it.number == 14 || it.fdiNumber == 14 }
        assertEquals(ToothCondition.DECAY, tooth14.condition)
        assertTrue(tooth14.notes.contains("MOD"))

        // 2. Verify Periodontal chart separate field
        val exam = updated.examAnswers
        assertNotNull(exam)
        assertTrue(exam!!.periodontalPockets.any { it.contains("Tooth 3 Buccal: 4mm") })
        assertTrue(exam.periodontalBleeding.contains("Tooth 3 bleeding"))

        // 3. Verify Treatment Plan separate field
        val plans = DentalRepository.treatmentPlans.value.filter { it.patientId == patient.id }
        assertTrue(plans.isNotEmpty())
        assertTrue(plans.any { plan -> plan.steps.any { it.procedure.contains("Composite Resin") && it.toothNumber == 14 } })

        // 4. Verify discrete Examination fields
        assertTrue(exam.chiefComplaints.contains("Pain on chewing"))
        assertEquals("Moderate (5/10)", exam.painSeverity)
        assertTrue(exam.sensitivityTriggers.contains("Cold"))
        assertEquals("High", exam.cariesRisk)

        // 5. Verify Patient Diagnosis separate field
        assertNotNull(updated.diagnosis)
        assertEquals("Moderate Periodontitis", updated.diagnosis!!.primaryDiagnosis)

        // 6. Verify ONLY extra unclassified notes are logged in clinicianNotes
        assertTrue(exam.clinicianNotes.contains("Patient prefers morning visits"))
        assertFalse("Odontogram conditions should not be dumped into clinicalNotes", exam.clinicianNotes.contains("MOD"))
        assertFalse("Treatment plans should not be dumped into clinicalNotes", exam.clinicianNotes.contains("Composite Resin Restoration"))

        // 7. Verify Undo point captured
        val undone = DentalRepository.undoLastVoiceEntry(patient.id)
        assertNotNull(undone)
    }

    @Test
    fun extractFunctionalHabitsAndHygieneDiscreteFields() {
        val transcript = "Patient reports bruxism and night clenching. Brushes twice daily. Flosses daily. Mild aphthous ulcer on right mucosa. Extrinsic tea stains. Clicking in TMJ on right side. Caries risk high."
        val findings = ClinicalVoiceExtractor.extractDeterministic(transcript)

        assertTrue(findings.examFindings.functionalHabits.contains("Bruxism / Night Grinding"))
        assertTrue(findings.examFindings.functionalHabits.contains("Clenching"))
        assertEquals("Twice daily (2x/day)", findings.examFindings.brushingFrequency)
        assertEquals("Daily", findings.examFindings.flossingFrequency)
        assertTrue(findings.examFindings.softTissue.isNotEmpty())
        assertTrue(findings.examFindings.stains.isNotEmpty())
        assertTrue(findings.examFindings.tmjAssessment.isNotEmpty())
        assertEquals("High", findings.examFindings.cariesRisk)

        // Apply findings to patient
        DentalRepository.applyClinicalFindings(patient.id, findings)
        val updated = current()
        val exam = updated.examAnswers
        assertNotNull(exam)
        assertTrue(exam!!.functionalHabits.contains("Bruxism / Night Grinding"))
        assertEquals("Twice daily (2x/day)", exam.brushingFrequency)
        assertEquals("Daily", exam.flossingFrequency)
        assertTrue(exam.softTissue.any { it.contains("ulcer", ignoreCase = true) })
        assertTrue(exam.stains.any { it.contains("stain", ignoreCase = true) })
        assertTrue(exam.tmjAssessment.any { it.contains("clicking", ignoreCase = true) })
    }

    @Test
    fun extractSpokenHomophonesAndAcronyms() {
        val transcript = "Tooth fourteen occlusal decay MOD. Tooth for missing. Tooth two full crown. Tooth three mesiobuccal 4mm bop."
        val findings = ClinicalVoiceExtractor.extractDeterministic(transcript)

        assertEquals(3, findings.toothConditions.size)
        val t14 = findings.toothConditions.first { it.toothNumber == 14 }
        assertEquals(ToothCondition.DECAY, t14.condition)
        assertEquals("MOD", t14.surface)

        val t4 = findings.toothConditions.first { it.toothNumber == 4 }
        assertEquals(ToothCondition.MISSING, t4.condition)

        val t2 = findings.toothConditions.first { it.toothNumber == 2 }
        assertEquals(ToothCondition.CROWN, t2.condition)

        val t3 = findings.perioMeasurements.first { it.toothNumber == 3 }
        assertEquals(4, t3.depthMm)
        assertTrue(t3.isBleeding)

        // Verify TTS spoken summary
        val summary = com.example.thornburydental.speech.ChartReadBack.spokenSummary(findings)
        assertTrue(summary.contains("Tooth 14 active caries / decay surface MOD"))
        assertTrue(summary.contains("Tooth 4 missing"))
    }
}
