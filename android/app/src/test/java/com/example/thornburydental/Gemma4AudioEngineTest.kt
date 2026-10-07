package com.example.thornburydental

import com.example.thornburydental.data.ToothCondition
import com.example.thornburydental.data.cds.LlmModels
import com.example.thornburydental.speech.ClinicalVoiceExtractor
import com.example.thornburydental.speech.Gemma4AudioEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.sin

class Gemma4AudioEngineTest {

    private lateinit var engine: Gemma4AudioEngine

    @Before
    fun setUp() {
        engine = Gemma4AudioEngine()
        assertTrue(engine.initialize())
    }

    @Test
    fun testModelSpecification() {
        val spec = Gemma4AudioEngine.MODEL_SPEC
        assertEquals("gemma-4-e2b-audio-it", spec.id)
        assertEquals("2B", spec.parameterCount)
        assertEquals("E2B-Q4_K", spec.quantization)
        assertTrue(spec.contextLength >= 2048)
    }

    @Test
    fun testConformer40msFrameChunking() {
        // 16000 Hz * 0.040s = 640 samples per frame
        assertEquals(640, Gemma4AudioEngine.SAMPLES_PER_FRAME)

        // 1 second of synthetic audio = 16000 samples -> exactly 25 frames
        val oneSecondAudio = ShortArray(16000) { (sin(it.toDouble() * 0.1) * 10000).toInt().toShort() }
        val frames = engine.chunkIntoConformerFrames(oneSecondAudio)

        assertEquals(25, frames.size)
        assertEquals(640, frames[0].size)

        // Verify normalized float values are bounded in [-1.0, 1.0]
        for (frame in frames) {
            for (sample in frame) {
                assertTrue("Sample $sample must be >= -1.0", sample >= -1.0f)
                assertTrue("Sample $sample must be <= 1.0", sample <= 1.0f)
            }
        }
    }

    @Test
    fun testRmsEnergyCalculation() {
        val silence = ShortArray(1600) { 0 }
        assertEquals(0f, engine.calculateRmsEnergy(silence), 0.001f)

        val tone = ShortArray(1600) { 5000 }
        val rms = engine.calculateRmsEnergy(tone)
        assertTrue("RMS should be ~5000", rms > 4900f && rms < 5100f)
    }

    @Test
    fun testWavHeaderGeneration() {
        val pcm = ShortArray(8000) { 1000 }
        val wavBytes = engine.pcmToWavBytes(pcm, sampleRate = 16000)

        // WAV header is 44 bytes + 16000 bytes PCM = 16044 bytes
        assertEquals(44 + 8000 * 2, wavBytes.size)

        // Verify RIFF and WAVE header magic bytes
        assertEquals('R'.code.toByte(), wavBytes[0])
        assertEquals('I'.code.toByte(), wavBytes[1])
        assertEquals('F'.code.toByte(), wavBytes[2])
        assertEquals('F'.code.toByte(), wavBytes[3])

        assertEquals('W'.code.toByte(), wavBytes[8])
        assertEquals('A'.code.toByte(), wavBytes[9])
        assertEquals('V'.code.toByte(), wavBytes[10])
        assertEquals('E'.code.toByte(), wavBytes[11])
    }

    @Test
    fun testProcessAudioUtteranceWithClinicalEntities() = runBlocking {
        val syntheticSpeechPcm = ShortArray(16000) { (sin(it.toDouble() * 0.05) * 4000).toInt().toShort() }
        val transcript = "Tooth 14 occlusal caries, tooth 3 mesiobuccal 4mm pocket with bleeding, plan composite restoration"

        val findings = engine.processAudioUtterance(
            pcmData = syntheticSpeechPcm,
            transcriptHint = transcript
        )

        assertTrue(findings.isAiExtracted)
        assertEquals(1, findings.toothConditions.size)
        assertEquals(14, findings.toothConditions[0].toothNumber)
        assertEquals(ToothCondition.DECAY, findings.toothConditions[0].condition)

        assertEquals(1, findings.perioMeasurements.size)
        assertEquals(3, findings.perioMeasurements[0].toothNumber)
        assertEquals(4, findings.perioMeasurements[0].depthMm)
        assertTrue(findings.perioMeasurements[0].isBleeding)

        assertEquals(1, findings.treatmentPlanItems.size)
    }

    @Test
    fun testShortAudioHandling() = runBlocking {
        val tinyAudio = ShortArray(500) { 100 }
        val findings = engine.processAudioUtterance(pcmData = tinyAudio)

        assertTrue(findings.warnings.any { it.contains("too short", ignoreCase = true) })
        assertEquals(0f, findings.confidence, 0.01f)
    }

    @Test
    fun testClinicalVoiceExtractorIntegration() = runBlocking {
        val audio = ShortArray(8000) { (sin(it.toDouble() * 0.08) * 3000).toInt().toShort() }
        val transcript = "Tooth 19 missing, tooth 30 crown, brushing twice daily"

        val findings = ClinicalVoiceExtractor.extractFromAudio(
            pcmAudio = audio,
            transcriptHint = transcript
        )

        assertEquals(2, findings.toothConditions.size)
        assertTrue(findings.toothConditions.any { it.toothNumber == 19 && it.condition == ToothCondition.MISSING })
        assertTrue(findings.toothConditions.any { it.toothNumber == 30 && it.condition == ToothCondition.CROWN })
        assertEquals("Twice daily (2x/day)", findings.examFindings.brushingFrequency)
    }
}
