package com.example.thornburydental

import com.example.thornburydental.speech.DictationTargetMode
import com.example.thornburydental.speech.ParsedVoiceCommand
import com.example.thornburydental.speech.VoiceCommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VoiceCommandParserTest {

    private lateinit var parser: VoiceCommandParser

    @Before
    fun setUp() {
        parser = VoiceCommandParser()
    }

    @Test
    fun testParsePeriodontalDictation_singlePocketDepthWithBleeding() {
        val transcript = "Tooth 14 pocket depth 4 mm bleeding"
        val result = parser.parseTranscript(transcript, DictationTargetMode.PERIODONTAL_CHARTING)

        assertTrue(result is ParsedVoiceCommand.SinglePocketDepth)
        val entry = (result as ParsedVoiceCommand.SinglePocketDepth).entry

        assertEquals(14, entry.toothNumber)
        assertEquals(4, entry.depthMm)
        assertTrue(entry.isBleeding)
    }

    @Test
    fun testParsePeriodontalDictation_surfaceMeasurement() {
        val transcript = "Tooth 19 mesial pocket 5 mm"
        val result = parser.parseTranscript(transcript, DictationTargetMode.PERIODONTAL_CHARTING)

        assertTrue(result is ParsedVoiceCommand.SinglePocketDepth)
        val entry = (result as ParsedVoiceCommand.SinglePocketDepth).entry

        assertEquals(19, entry.toothNumber)
        assertEquals(5, entry.depthMm)
        assertEquals("mesial", entry.surface)
    }

    @Test
    fun testParsePeriodontalDictation_multipleTeethSequence() {
        val transcript = "Tooth 14 pocket 3 tooth 15 pocket 4"
        val result = parser.parseTranscript(transcript, DictationTargetMode.PERIODONTAL_CHARTING)

        assertTrue(result is ParsedVoiceCommand.MultiplePocketDepths)
        val entries = (result as ParsedVoiceCommand.MultiplePocketDepths).entries

        assertEquals(2, entries.size)
        assertEquals(14, entries[0].toothNumber)
        assertEquals(3, entries[0].depthMm)
        assertEquals(15, entries[1].toothNumber)
        assertEquals(4, entries[1].depthMm)
    }

    @Test
    fun testParseClinicalNotesDictation_narrativeText() {
        val transcript = "Patient exhibits mild localized swelling around tooth 19. Scaling and root planing advised."
        val result = parser.parseTranscript(transcript, DictationTargetMode.CLINICAL_NOTES)

        assertTrue(result is ParsedVoiceCommand.ClinicalNote)
        val entry = (result as ParsedVoiceCommand.ClinicalNote).entry

        assertEquals("clinicianNotes", entry.targetSection)
        assertTrue(entry.noteText.contains("Patient exhibits mild localized swelling"))
    }
}
