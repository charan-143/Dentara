package com.example.thornburydental

import com.example.thornburydental.data.ToothNumberingSystem
import com.example.thornburydental.speech.DictationTargetMode
import com.example.thornburydental.speech.ParsedVoiceCommand
import com.example.thornburydental.speech.PerioSite
import com.example.thornburydental.speech.PocketDepthResult
import com.example.thornburydental.speech.VoiceCommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VoiceCommandParserTest {

    private lateinit var parser: VoiceCommandParser

    @Before
    fun setUp() {
        parser = VoiceCommandParser()
    }

    private fun perio(
        transcript: String,
        system: ToothNumberingSystem = ToothNumberingSystem.UNIVERSAL
    ): ParsedVoiceCommand = parser.parseTranscript(
        transcript,
        DictationTargetMode.PERIODONTAL_CHARTING,
        system
    )

    private fun entriesOf(command: ParsedVoiceCommand): List<PocketDepthResult> = when (command) {
        is ParsedVoiceCommand.SinglePocketDepth -> listOf(command.entry)
        is ParsedVoiceCommand.MultiplePocketDepths -> command.entries
        else -> emptyList()
    }

    // ---------------------------------------------------------------- basics

    @Test
    fun singleReadingWithBleeding() {
        val result = perio("Tooth 14 pocket depth 4 mm bleeding")

        assertTrue(result is ParsedVoiceCommand.SinglePocketDepth)
        val entry = (result as ParsedVoiceCommand.SinglePocketDepth).entry
        assertEquals(14, entry.toothNumber)
        assertEquals(4, entry.depthMm)
        assertTrue(entry.isBleeding)
        assertTrue(
            "Plain dictation should be confident",
            result.confidence >= VoiceCommandParser.AUTO_APPLY_CONFIDENCE
        )
    }

    @Test
    fun spokenNumberWordsAreUnderstood() {
        val result = perio("tooth fourteen pocket three millimetres")

        val entries = entriesOf(result)
        assertEquals(1, entries.size)
        assertEquals(14, entries[0].toothNumber)
        assertEquals(3, entries[0].depthMm)
    }

    @Test
    fun siteIsCapturedAsAStructuredValue() {
        val result = perio("Tooth 19 mesial pocket 5 mm")

        val entry = (result as ParsedVoiceCommand.SinglePocketDepth).entry
        assertEquals(19, entry.toothNumber)
        assertEquals(5, entry.depthMm)
        assertEquals(PerioSite.MESIAL, entry.site)
    }

    // ---------------------------------------------------------------- regressions

    /**
     * The old regex returned on its first match, so everything after the first reading was
     * silently discarded - a whole quadrant of probing could vanish without any error.
     */
    @Test
    fun multiToothDictationIsNotTruncated() {
        val result = perio("Tooth 14 pocket 3 tooth 15 pocket 4")

        assertTrue(result is ParsedVoiceCommand.MultiplePocketDepths)
        val entries = (result as ParsedVoiceCommand.MultiplePocketDepths).entries
        assertEquals(2, entries.size)
        assertEquals(14, entries[0].toothNumber)
        assertEquals(3, entries[0].depthMm)
        assertEquals(15, entries[1].toothNumber)
        assertEquals(4, entries[1].depthMm)
    }

    /**
     * The old regex split "14" across its two capture groups and read this as tooth 1 at 4 mm:
     * wrong tooth and wrong depth, with no error shown.
     */
    @Test
    fun depthStatedBeforeTheToothBindsToThatTooth() {
        val result = perio("pocket 3 on tooth 14")

        val entries = entriesOf(result)
        assertEquals(1, entries.size)
        assertEquals("Must be tooth 14, not tooth 1", 14, entries[0].toothNumber)
        assertEquals("Must be 3 mm, not 4 mm", 3, entries[0].depthMm)
    }

    /** Bleeding used to be detected anywhere in the utterance and applied to every reading. */
    @Test
    fun bleedingAttachesOnlyToTheReadingItFollows() {
        val result = perio("tooth 14 pocket 3 tooth 15 pocket 4 bleeding")

        val entries = entriesOf(result)
        assertEquals(2, entries.size)
        assertFalse("Tooth 14 was not reported as bleeding", entries[0].isBleeding)
        assertTrue("Tooth 15 was reported as bleeding", entries[1].isBleeding)
    }

    @Test
    fun negatedBleedingIsNotRecorded() {
        val result = perio("tooth 14 pocket 3 no bleeding")

        val entries = entriesOf(result)
        assertEquals(1, entries.size)
        assertFalse(entries[0].isBleeding)
    }

    /**
     * The most dangerous old behaviour: anything the parser could not read, if longer than five
     * characters, became a free-text clinical note. A misheard measurement was filed as prose in
     * the chart instead of being rejected.
     */
    @Test
    fun unparseablePeriodontalDictationIsRejectedNotFiledAsANote() {
        val result = perio("the patient seems a little anxious today")

        assertTrue("Expected Unrecognized, got $result", result is ParsedVoiceCommand.Unrecognized)
        assertEquals(0f, result.confidence, 0.001f)
    }

    @Test
    fun aToothWithNoDepthIsRejectedWithAReason() {
        val result = perio("tooth 14")

        assertTrue(result is ParsedVoiceCommand.Unrecognized)
        val reason = (result as ParsedVoiceCommand.Unrecognized).reason
        assertTrue("Reason should name the tooth, was: $reason", reason.contains("14"))
    }

    @Test
    fun aDepthWithNoToothIsRejectedWithAReason() {
        val result = perio("pocket 3 millimetres")

        assertTrue(result is ParsedVoiceCommand.Unrecognized)
        val reason = (result as ParsedVoiceCommand.Unrecognized).reason
        assertTrue("Reason should ask for a tooth, was: $reason", reason.contains("tooth"))
    }

    // ---------------------------------------------------------------- multi-site charting

    @Test
    fun eachStatedSiteRecordsItsOwnReading() {
        val result = perio("Tooth 3 buccal 4 mesial 3 distal 5")

        val entries = entriesOf(result)
        assertEquals(3, entries.size)
        assertEquals(PerioSite.BUCCAL, entries[0].site)
        assertEquals(4, entries[0].depthMm)
        assertEquals(PerioSite.MESIAL, entries[1].site)
        assertEquals(3, entries[1].depthMm)
        assertEquals(PerioSite.DISTAL, entries[2].site)
        assertEquals(5, entries[2].depthMm)
        entries.forEach { assertEquals(3, it.toothNumber) }
    }

    /** A stated site stays in force across a run of numbers, as in mesio-, mid- and disto-buccal. */
    @Test
    fun aSequenceOfDepthsKeepsTheStatedSite() {
        val result = perio("tooth 14 buccal 3 2 3")

        val entries = entriesOf(result)
        assertEquals(3, entries.size)
        assertEquals(listOf(3, 2, 3), entries.map { it.depthMm })
        entries.forEach { assertEquals(PerioSite.BUCCAL, it.site) }
    }

    // ---------------------------------------------------------------- numbering systems

    @Test
    fun fdiPrimaryToothIsValidUnderFdi() {
        val result = perio("tooth 55 pocket 3", ToothNumberingSystem.FDI)

        val entries = entriesOf(result)
        assertEquals(1, entries.size)
        assertEquals(55, entries[0].toothNumber)
    }

    /**
     * The same utterance must not quietly succeed under the other scheme. 55 is a primary molar
     * in FDI and simply does not exist in Universal.
     */
    @Test
    fun fdiPrimaryToothIsRejectedUnderUniversal() {
        val result = perio("tooth 55 pocket 3", ToothNumberingSystem.UNIVERSAL)

        assertTrue("Expected Unrecognized, got $result", result is ParsedVoiceCommand.Unrecognized)
    }

    @Test
    fun aNumberThatIsNotAValidFdiToothIsRejectedUnderFdi() {
        // 40 has quadrant 4 but position 0, which is not a tooth in FDI.
        val result = perio("tooth 40 pocket 3", ToothNumberingSystem.FDI)

        assertTrue("Expected Unrecognized, got $result", result is ParsedVoiceCommand.Unrecognized)
    }

    // ---------------------------------------------------------------- confidence

    @Test
    fun aToothTakenFromABareNumberIsFlaggedForReview() {
        // No spoken "tooth", so 14 is inferred - the likeliest route to the wrong tooth.
        val result = perio("14 pocket 3")

        val entries = entriesOf(result)
        assertEquals(1, entries.size)
        assertEquals(14, entries[0].toothNumber)
        assertTrue(
            "An inferred tooth must fall below the auto-apply threshold, was ${result.confidence}",
            result.confidence < VoiceCommandParser.AUTO_APPLY_CONFIDENCE
        )
    }

    @Test
    fun anImplausiblyDeepReadingIsFlaggedForReview() {
        val result = perio("tooth 14 pocket 14 mm")

        val entries = entriesOf(result)
        assertEquals(1, entries.size)
        assertEquals(14, entries[0].depthMm)
        assertTrue(
            "A 14 mm pocket must be reviewed, confidence was ${result.confidence}",
            result.confidence < VoiceCommandParser.AUTO_APPLY_CONFIDENCE
        )
        assertTrue("Expected a warning about the depth", result.warnings.isNotEmpty())
    }

    @Test
    fun plainDictationCarriesNoWarnings() {
        val result = perio("tooth 14 pocket 3 mm")

        assertTrue(result.warnings.isEmpty())
        assertEquals(1f, result.confidence, 0.001f)
    }

    // ---------------------------------------------------------------- clinical notes mode

    @Test
    fun clinicalNotesModeStillAcceptsNarrativeText() {
        val transcript = "Patient exhibits mild localized swelling around tooth 19. Scaling advised."
        val result = parser.parseTranscript(transcript, DictationTargetMode.CLINICAL_NOTES)

        assertTrue(result is ParsedVoiceCommand.ClinicalNote)
        val entry = (result as ParsedVoiceCommand.ClinicalNote).entry
        assertEquals("clinicianNotes", entry.targetSection)
        assertTrue(entry.noteText.contains("Patient exhibits mild localized swelling"))
    }

    @Test
    fun clinicalNotesModeRoutesADiagnosisHeadingToItsOwnSection() {
        val result = parser.parseTranscript(
            "Diagnosis: generalised chronic periodontitis",
            DictationTargetMode.CLINICAL_NOTES
        )

        val entry = (result as ParsedVoiceCommand.ClinicalNote).entry
        assertEquals("diagnosis", entry.targetSection)
        assertTrue(entry.noteText.startsWith("Generalised"))
    }

    @Test
    fun blankTranscriptIsRejected() {
        val result = parser.parseTranscript("   ", DictationTargetMode.PERIODONTAL_CHARTING)

        assertTrue(result is ParsedVoiceCommand.Unrecognized)
    }
}
