package com.example.thornburydental

import com.example.thornburydental.speech.DictationTargetMode
import com.example.thornburydental.speech.SpeechEngineUnavailableException
import com.example.thornburydental.speech.WhisperEngine
import com.example.thornburydental.speech.WhisperNativeBridge
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class WhisperEngineTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    /**
     * Stands in for libdentara_whisper.so, which does not exist on the JVM. Lets the engine
     * lifecycle and failure handling be tested without a device.
     */
    private class FakeBridge(
        private val initResult: Long = 1L,
        private val transcript: String? = "tooth fourteen pocket depth three millimeters bleeding",
        private val throwOnTranscribe: Boolean = false
    ) : WhisperNativeBridge {
        var released = 0
        var lastPrompt: String? = null
        var transcribeCalls = 0

        override fun nativeInit(modelPath: String): Long = initResult

        override fun nativeRelease(contextPtr: Long) {
            released++
        }

        override fun nativeTranscribe(
            contextPtr: Long,
            pcm: ShortArray,
            threads: Int,
            language: String?,
            prompt: String?
        ): String? {
            transcribeCalls++
            lastPrompt = prompt
            if (throwOnTranscribe) throw RuntimeException("decoder exploded")
            return transcript
        }
    }

    private fun modelFile(): File = temporaryFolder.newFile("ggml-test-" + System.nanoTime() + ".bin")

    private fun speech(seconds: Int = 3): ShortArray =
        ShortArray(WhisperEngine.SAMPLE_RATE * seconds) { i ->
            (Math.sin(i.toDouble() / 8.0) * 8000).toInt().toShort()
        }

    @Test
    fun engineIsNotReadyBeforeInitialize() {
        assertFalse(WhisperEngine(bridge = FakeBridge()).isReady())
    }

    @Test
    fun initialize_failsWhenTheModelFileIsMissing() = runBlocking {
        val engine = WhisperEngine(bridge = FakeBridge())

        val loaded = engine.initialize(File(temporaryFolder.root, "does-not-exist.bin"))

        assertFalse("A missing model must not report success", loaded)
        assertFalse(engine.isReady())
    }

    @Test
    fun initialize_failsWhenTheNativeLibraryIsUnavailable() = runBlocking {
        val engine = WhisperEngine(bridge = null)

        assertFalse(engine.initialize(modelFile()))
        assertFalse(engine.isReady())
    }

    @Test
    fun initialize_failsWhenTheDecoderRefusesTheModel() = runBlocking {
        val engine = WhisperEngine(bridge = FakeBridge(initResult = 0L))

        assertFalse("A null context pointer means the model did not load", engine.initialize(modelFile()))
        assertFalse(engine.isReady())
    }

    @Test
    fun initialize_succeedsWithAModelAndAWorkingBridge() = runBlocking {
        val engine = WhisperEngine(bridge = FakeBridge())

        assertTrue(engine.initialize(modelFile()))
        assertTrue(engine.isReady())
    }

    /**
     * The critical guarantee carried over from the stub removal: when transcription cannot
     * happen, the caller gets an error, never a plausible-looking clinical measurement.
     */
    @Test(expected = SpeechEngineUnavailableException::class)
    fun transcribeAudio_throwsBeforeAnyModelIsLoaded() = runBlocking {
        WhisperEngine(bridge = FakeBridge()).transcribeAudio(speech())
        Unit
    }

    @Test(expected = SpeechEngineUnavailableException::class)
    fun transcribeAudio_throwsWhenTheDecoderReturnsNothing() = runBlocking {
        val engine = WhisperEngine(bridge = FakeBridge(transcript = null))
        engine.initialize(modelFile())

        engine.transcribeAudio(speech())
        Unit
    }

    @Test(expected = SpeechEngineUnavailableException::class)
    fun transcribeAudio_throwsWhenTheDecoderCrashes() = runBlocking {
        val engine = WhisperEngine(bridge = FakeBridge(throwOnTranscribe = true))
        engine.initialize(modelFile())

        engine.transcribeAudio(speech())
        Unit
    }

    @Test
    fun transcribeAudio_normalisesTheDecodedTranscript() = runBlocking {
        val engine = WhisperEngine(bridge = FakeBridge())
        engine.initialize(modelFile())

        val result = engine.transcribeAudio(speech(), DictationTargetMode.PERIODONTAL_CHARTING)

        assertTrue("Spoken numbers should become digits, was: $result", result.contains("Tooth 14"))
        assertTrue(result.contains("mm"))
        assertTrue(result.contains("bleeding"))
    }

    /**
     * Audio shorter than whisper can work with is reported as nothing recognised, rather than
     * decoded into whatever the model makes of a fragment.
     */
    @Test
    fun transcribeAudio_returnsEmptyForAudioTooShortToDecode() = runBlocking {
        val bridge = FakeBridge()
        val engine = WhisperEngine(bridge = bridge)
        engine.initialize(modelFile())

        val result = engine.transcribeAudio(ShortArray(WhisperEngine.MIN_SAMPLES - 1))

        assertEquals("", result)
        assertEquals("The decoder should not have been asked", 0, bridge.transcribeCalls)
    }

    @Test
    fun transcribeAudio_primesTheDecoderWithModeSpecificVocabulary() = runBlocking {
        val bridge = FakeBridge()
        val engine = WhisperEngine(bridge = bridge)
        engine.initialize(modelFile())

        engine.transcribeAudio(speech(), DictationTargetMode.PERIODONTAL_CHARTING)
        val periodontalPrompt = bridge.lastPrompt

        engine.transcribeAudio(speech(), DictationTargetMode.CLINICAL_NOTES)
        val notesPrompt = bridge.lastPrompt

        assertTrue("Perio prompt should mention probing", periodontalPrompt!!.contains("probing"))
        assertTrue("Notes prompt should mention examination terms", notesPrompt!!.contains("Caries"))
        assertTrue("Prompts must differ by mode", periodontalPrompt != notesPrompt)
    }

    @Test
    fun release_freesTheContextAndLeavesTheEngineNotReady() = runBlocking {
        val bridge = FakeBridge()
        val engine = WhisperEngine(bridge = bridge)
        engine.initialize(modelFile())

        engine.release()

        assertFalse(engine.isReady())
        assertEquals(1, bridge.released)
    }

    @Test
    fun release_isSafeToCallTwice() = runBlocking {
        val bridge = FakeBridge()
        val engine = WhisperEngine(bridge = bridge)
        engine.initialize(modelFile())

        engine.release()
        engine.release()

        assertEquals("Second release must not free the same pointer again", 1, bridge.released)
    }

    @Test
    fun formatDentalTranscript_replacesNumberWordsAndMedicalTerms() {
        val engine = WhisperEngine(bridge = FakeBridge())
        val raw = "tooth fourteen pocket depth three millimeters bleeding"

        val formatted = engine.formatDentalTranscript(raw, DictationTargetMode.PERIODONTAL_CHARTING)

        assertTrue(formatted.contains("Tooth 14"))
        assertTrue(formatted.contains("pocket"))
        assertTrue(formatted.contains("3"))
        assertTrue(formatted.contains("mm"))
        assertTrue(formatted.contains("bleeding"))
    }

    @Test
    fun formatDentalTranscript_returnsEmptyForBlankInput() {
        val engine = WhisperEngine(bridge = FakeBridge())

        assertEquals("", engine.formatDentalTranscript("   ", DictationTargetMode.CLINICAL_NOTES))
    }
}
