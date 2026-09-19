package com.example.thornburydental

import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.Patient
import com.example.thornburydental.speech.ParsedVoiceCommand
import com.example.thornburydental.speech.PerioSite
import com.example.thornburydental.speech.PocketDepthResult
import com.example.thornburydental.speech.VoiceChartingController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Exercises the voice write path against DentalRepository.
 *
 * Each test registers its own patient. The previous version assumed a seeded patient "p1", but
 * _patients starts empty and is filled from the database, which is not initialised under JVM
 * unit tests - so all three of its tests failed with NoSuchElementException and the write path
 * had no coverage at all.
 */
class DentalRepositoryVoiceIntegrationTest {

    private lateinit var controller: VoiceChartingController
    private lateinit var patient: Patient

    @Before
    fun setUp() {
        controller = VoiceChartingController()
        patient = DentalRepository.registerPatient(
            name = "Test Patient",
            opNo = null,
            dob = "01/01/1980",
            phone = "",
            email = "",
            address = "",
            allergies = emptyList(),
            medicalAlerts = emptyList(),
            medicalHistory = ""
        )
    }

    private fun current(): Patient =
        DentalRepository.patients.value.first { it.id == patient.id }

    private fun pockets(): List<String> = current().examAnswers?.periodontalPockets.orEmpty()

    private fun toothNotes(toothNumber: Int): String =
        current().teeth.values
            .firstOrNull { it.number == toothNumber || it.fdiNumber == toothNumber }
            ?.notes
            .orEmpty()

    // ---------------------------------------------------------------- recording

    @Test
    fun recordsAReadingWithItsSite() {
        DentalRepository.applyVoicePeriodontalPocket(
            patientId = patient.id,
            toothNumber = 14,
            depthMm = 4,
            isBleeding = true,
            siteLabel = "Buccal"
        )

        assertTrue(
            "Site should be part of the recorded entry, was ${pockets()}",
            pockets().any { it.contains("Tooth 14 Buccal") && it.contains("4mm") }
        )
        assertTrue(current().examAnswers!!.periodontalBleeding.contains("Tooth 14 bleeding"))
    }

    /**
     * The regression this step exists for. updateExaminationAnswers used to be called from
     * inside the _patients.update lambda; StateFlow.update re-runs that lambda when its
     * compare-and-set loses, which the nested write guaranteed, so the non-idempotent tooth-note
     * append ran twice and produced "Pocket: 4mm [BOP] Pocket: 4mm [BOP]".
     */
    @Test
    fun aSingleReadingIsRecordedOnceInTheToothNotes() {
        DentalRepository.applyVoicePeriodontalPocket(
            patientId = patient.id,
            toothNumber = 14,
            depthMm = 4,
            isBleeding = true,
            siteLabel = "Buccal"
        )

        val notes = toothNotes(14)
        val occurrences = Regex("Pocket Buccal:").findAll(notes).count()
        assertEquals("Tooth note recorded $occurrences times in: $notes", 1, occurrences)
    }

    @Test
    fun twoSitesOnOneToothBothSurvive() {
        DentalRepository.applyVoicePeriodontalPocket(patient.id, 14, 4, false, "Buccal")
        DentalRepository.applyVoicePeriodontalPocket(patient.id, 14, 6, false, "Mesial")

        val recorded = pockets()
        assertTrue("Buccal missing from $recorded", recorded.any { it.startsWith("Tooth 14 Buccal:") })
        assertTrue("Mesial missing from $recorded", recorded.any { it.startsWith("Tooth 14 Mesial:") })
    }

    @Test
    fun reProbingTheSameSiteCorrectsTheReadingRatherThanAccumulating() {
        DentalRepository.applyVoicePeriodontalPocket(patient.id, 14, 4, false, "Buccal")
        DentalRepository.applyVoicePeriodontalPocket(patient.id, 14, 3, false, "Buccal")

        val buccal = pockets().filter { it.startsWith("Tooth 14 Buccal:") }
        assertEquals("Expected one buccal entry, got $buccal", 1, buccal.size)
        assertTrue("Should hold the corrected depth, was $buccal", buccal.first().contains("3mm"))
    }

    // ---------------------------------------------------------------- provenance

    @Test
    fun aVoiceWrittenReadingCarriesProvenance() {
        controller.applyParsedCommand(
            patientId = patient.id,
            command = ParsedVoiceCommand.SinglePocketDepth(
                entry = PocketDepthResult(
                    toothNumber = 19,
                    depthMm = 5,
                    isBleeding = true,
                    site = PerioSite.LINGUAL
                ),
                confidence = 1f
            ),
            transcript = "tooth 19 lingual pocket 5 millimetres bleeding"
        )

        val entries = current().examAnswers?.voiceEntries.orEmpty()
        assertEquals(1, entries.size)
        val entry = entries.first()
        assertEquals("tooth 19 lingual pocket 5 millimetres bleeding", entry.transcript)
        assertEquals(1f, entry.confidence, 0.001f)
        assertFalse("A confident reading needed no review", entry.reviewRequired)
        assertTrue(
            "applied should describe what was written, was ${entry.applied}",
            entry.applied.contains("Tooth 19")
        )
        assertTrue(entry.recordedAtEpochMs > 0L)
    }

    @Test
    fun aLowConfidenceReadingIsMarkedAsHavingNeededReview() {
        controller.applyParsedCommand(
            patientId = patient.id,
            command = ParsedVoiceCommand.SinglePocketDepth(
                entry = PocketDepthResult(toothNumber = 19, depthMm = 5),
                confidence = 0.4f
            ),
            transcript = "19 pocket 5"
        )

        val entry = current().examAnswers!!.voiceEntries.first()
        assertTrue("Below the auto-apply threshold, so review was required", entry.reviewRequired)
    }

    @Test
    fun theSiteIsCarriedFromTheParsedCommandIntoTheRecord() {
        controller.applyParsedCommand(
            patientId = patient.id,
            command = ParsedVoiceCommand.SinglePocketDepth(
                entry = PocketDepthResult(toothNumber = 19, depthMm = 5, site = PerioSite.MESIAL)
            ),
            transcript = "tooth 19 mesial pocket 5"
        )

        assertTrue(
            "Site should reach the record, was ${pockets()}",
            pockets().any { it.startsWith("Tooth 19 Mesial:") }
        )
    }

    // ---------------------------------------------------------------- notes

    @Test
    fun aDictatedNoteLandsInClinicianNotes() {
        DentalRepository.appendVoiceClinicalNote(
            patientId = patient.id,
            noteText = "Patient tolerates periodontal probing well."
        )

        assertTrue(current().examAnswers!!.clinicianNotes.contains("tolerates periodontal probing"))
    }

    /**
     * The parser has always computed a target section for a spoken "Diagnosis:" heading, and the
     * repository has always ignored it, so diagnoses were filed as ordinary clinical notes.
     */
    @Test
    fun aDictatedDiagnosisLandsInTheDiagnosisSectionNotClinicianNotes() {
        DentalRepository.appendVoiceClinicalNote(
            patientId = patient.id,
            noteText = "Generalised chronic periodontitis",
            targetSection = "diagnosis"
        )

        val exam = current().examAnswers!!
        assertTrue(
            "Diagnosis text missing from diagnosis notes, was: ${exam.otherDiagnosesNotes}",
            exam.otherDiagnosesNotes.contains("Generalised chronic periodontitis")
        )
        assertFalse(
            "Diagnosis should not also be filed as a clinical note",
            exam.clinicianNotes.contains("Generalised chronic periodontitis")
        )
    }

    @Test
    fun successiveNotesAreKeptRatherThanOverwritten() {
        DentalRepository.appendVoiceClinicalNote(patient.id, "First observation")
        DentalRepository.appendVoiceClinicalNote(patient.id, "Second observation")

        val notes = current().examAnswers!!.clinicianNotes
        assertTrue(notes.contains("First observation"))
        assertTrue(notes.contains("Second observation"))
    }

    @Test
    fun anUnrecognizedCommandWritesNothing() {
        val applied = controller.applyParsedCommand(
            patientId = patient.id,
            command = ParsedVoiceCommand.Unrecognized("mumble", "nothing recognised"),
            transcript = "mumble"
        )

        assertFalse(applied)
        assertTrue(pockets().isEmpty())
        assertTrue(current().examAnswers?.voiceEntries.orEmpty().isEmpty())
    }

    @Test
    fun registeredPatientHasTeethToRecordAgainst() {
        // Guards the fixture itself: without teeth, the tooth-note assertions above would pass
        // vacuously.
        assertNotNull(current().teeth)
        assertTrue(
            "Expected a dentition, got ${current().teeth.size} teeth",
            current().teeth.isNotEmpty()
        )
    }
}
