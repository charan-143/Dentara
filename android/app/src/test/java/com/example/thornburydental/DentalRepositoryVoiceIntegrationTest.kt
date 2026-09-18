package com.example.thornburydental

import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.ToothCondition
import com.example.thornburydental.speech.ClinicalNoteResult
import com.example.thornburydental.speech.ParsedVoiceCommand
import com.example.thornburydental.speech.PocketDepthResult
import com.example.thornburydental.speech.VoiceChartingController
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DentalRepositoryVoiceIntegrationTest {

    private lateinit var controller: VoiceChartingController
    private val testPatientId = "p1"

    @Before
    fun setUp() {
        controller = VoiceChartingController()
    }

    @Test
    fun testApplyVoicePeriodontalPocket_updatesExamAnswersAndTeeth() {
        DentalRepository.applyVoicePeriodontalPocket(
            patientId = testPatientId,
            toothNumber = 14,
            depthMm = 4,
            isBleeding = true
        )

        val updatedPatient = DentalRepository.patients.value.first { it.id == testPatientId }
        val exam = updatedPatient.examAnswers

        assertNotNull(exam)
        assertTrue(exam!!.periodontalPockets.any { it.contains("Tooth 14: 4mm (Bleeding)") })
        assertTrue(exam.periodontalBleeding.contains("Tooth 14 bleeding"))

        val tooth14 = updatedPatient.teeth.values.firstOrNull { it.number == 14 || it.fdiNumber == 14 }
        assertNotNull(tooth14)
        assertTrue(tooth14!!.notes.contains("Pocket: 4mm [BOP]"))
    }

    @Test
    fun testAppendVoiceClinicalNote_appendsToClinicianNotes() {
        val initialNotes = DentalRepository.patients.value.first { it.id == testPatientId }.examAnswers?.clinicianNotes ?: ""

        DentalRepository.appendVoiceClinicalNote(
            patientId = testPatientId,
            noteText = "Voice dictation note: Patient tolerates periodontal probing well."
        )

        val updatedPatient = DentalRepository.patients.value.first { it.id == testPatientId }
        val updatedNotes = updatedPatient.examAnswers?.clinicianNotes ?: ""

        assertTrue(updatedNotes.contains("Patient tolerates periodontal probing well."))
    }

    @Test
    fun testVoiceChartingController_applyParsedCommandIntegration() {
        val singleCmd = ParsedVoiceCommand.SinglePocketDepth(
            PocketDepthResult(toothNumber = 19, depthMm = 5, isBleeding = true)
        )

        val success = controller.applyParsedCommand(testPatientId, singleCmd)
        assertTrue(success)

        val patient = DentalRepository.patients.value.first { it.id == testPatientId }
        assertTrue(patient.examAnswers?.periodontalPockets?.any { it.contains("Tooth 19: 5mm") } == true)
    }
}
