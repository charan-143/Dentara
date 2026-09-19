package com.example.thornburydental
 
import com.example.thornburydental.speech.VoiceActivitySegmenter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.sin

class VoiceActivitySegmenterTest {

    private lateinit var segmenter: VoiceActivitySegmenter

    @Before
    fun setUp() {
        segmenter = VoiceActivitySegmenter()
    }

    private fun generateSineSpeech(durationMs: Int, amplitude: Int = 12000, freqHz: Double = 300.0): ShortArray {
        val samples = (VoiceActivitySegmenter.SAMPLE_RATE * durationMs) / 1000
        return ShortArray(samples) { i ->
            (sin(2.0 * Math.PI * freqHz * i / VoiceActivitySegmenter.SAMPLE_RATE) * amplitude).toInt().toShort()
        }
    }

    private fun generateSilence(durationMs: Int, noiseFloor: Int = 30): ShortArray {
        val samples = (VoiceActivitySegmenter.SAMPLE_RATE * durationMs) / 1000
        return ShortArray(samples) { i ->
            ((i % 5) - 2).toShort()
        }
    }

    @Test
    fun estimateSpeechProbability_returnsLowForSilence() {
        val silence = generateSilence(32) // 1 frame
        val prob = segmenter.estimateSpeechProbability(silence)
        assertTrue("Silence probability should be low (actual: $prob)", prob < 0.2f)
    }

    @Test
    fun estimateSpeechProbability_returnsHighForSpeech() {
        val speech = generateSineSpeech(32, amplitude = 14000) // 1 frame
        val prob = segmenter.estimateSpeechProbability(speech)
        assertTrue("Speech probability should exceed start threshold (actual: $prob)", prob >= VoiceActivitySegmenter.START_THRESHOLD)
    }

    @Test
    fun filterAndExtractSpeech_returnsNullForPureSilence() {
        val silence = generateSilence(2000) // 2s of silence
        val result = segmenter.filterAndExtractSpeech(silence)
        assertNull("Pure silence must be rejected by VAD to avoid useless whisper decodes", result)
    }

    @Test
    fun filterAndExtractSpeech_rejectsShortAcousticClick() {
        // 100ms click is shorter than MIN_SPEECH_FRAMES (250ms)
        val silenceBefore = generateSilence(500)
        val click = generateSineSpeech(100, amplitude = 15000)
        val silenceAfter = generateSilence(500)

        val combined = ShortArray(silenceBefore.size + click.size + silenceAfter.size)
        System.arraycopy(silenceBefore, 0, combined, 0, silenceBefore.size)
        System.arraycopy(click, 0, combined, silenceBefore.size, click.size)
        System.arraycopy(silenceAfter, 0, combined, silenceBefore.size + click.size, silenceAfter.size)

        val result = segmenter.filterAndExtractSpeech(combined)
        assertNull("Short clicks under 250ms must be rejected", result)
    }

    @Test
    fun filterAndExtractSpeech_extractsValidUtteranceWithPreRoll() {
        // 1000ms leading silence + 1000ms speech + 1000ms trailing silence
        val silenceBefore = generateSilence(1000)
        val speech = generateSineSpeech(1000, amplitude = 12000)
        val silenceAfter = generateSilence(1000)

        val totalLen = silenceBefore.size + speech.size + silenceAfter.size
        val stream = ShortArray(totalLen)
        System.arraycopy(silenceBefore, 0, stream, 0, silenceBefore.size)
        System.arraycopy(speech, 0, stream, silenceBefore.size, speech.size)
        System.arraycopy(silenceAfter, 0, stream, silenceBefore.size + speech.size, silenceAfter.size)

        val result = segmenter.filterAndExtractSpeech(stream)
        assertNotNull("Valid speech utterance must be extracted", result)
        assertTrue("Extracted speech must be non-empty", result!!.isNotEmpty())
        assertTrue("Extracted speech should be trimmed compared to original stream", result.size < stream.size)
    }

    @Test
    fun processFrame_detectsSpeechOnsetAndEndpointReachedWithTrailingSilence() {
        segmenter.reset()

        // 1. Feed leading silence frames (32ms each)
        for (i in 0 until 10) {
            val frame = generateSilence(32)
            val event = segmenter.processFrame(frame)
            assertEquals(VoiceActivitySegmenter.VadChunkEvent.Silence, event)
        }

        // 2. Feed speech frames (e.g. 15 frames ≈ 480ms speech)
        var sawSpeechStarted = false
        for (i in 0 until 15) {
            val frame = generateSineSpeech(32, amplitude = 12000)
            val event = segmenter.processFrame(frame)
            if (event is VoiceActivitySegmenter.VadChunkEvent.SpeechStarted) {
                sawSpeechStarted = true
            }
        }
        assertTrue("Expected to detect SpeechStarted event", sawSpeechStarted)

        // 3. Feed trailing silence frames (25 frames ≈ 800ms silence > MIN_SILENCE_FRAMES)
        var endpointEvent: VoiceActivitySegmenter.VadChunkEvent.EndpointReached? = null
        for (i in 0 until 25) {
            val frame = generateSilence(32)
            val event = segmenter.processFrame(frame)
            if (event is VoiceActivitySegmenter.VadChunkEvent.EndpointReached) {
                endpointEvent = event
                break
            }
        }

        assertNotNull("Expected real-time VAD endpoint to trigger after trailing silence", endpointEvent)
        assertTrue("Segmented PCM must contain samples", endpointEvent!!.speechPcm.isNotEmpty())
    }
}
