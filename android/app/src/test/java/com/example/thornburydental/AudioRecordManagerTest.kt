package com.example.thornburydental

import com.example.thornburydental.speech.AudioRecordManager
import com.example.thornburydental.speech.MicrophoneUnavailableException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioRecordManagerTest {

    private lateinit var manager: AudioRecordManager
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        manager = AudioRecordManager()
    }

    /**
     * There is no capture hardware under JVM unit tests. The manager must fail loudly
     * instead of substituting synthetic audio, which would let the dictation pipeline
     * produce clinical findings from sound the clinician never made.
     */
    @Test
    fun startRecording_throws_whenMicrophoneIsUnavailable() {
        try {
            manager.startRecording(testScope)
            fail("Expected MicrophoneUnavailableException when no microphone is present")
        } catch (expected: MicrophoneUnavailableException) {
            // Correct behaviour.
        }
    }

    @Test
    fun startRecording_leavesManagerIdle_afterAFailedStart() {
        try {
            manager.startRecording(testScope)
        } catch (expected: MicrophoneUnavailableException) {
            // Expected.
        }

        assertFalse("Must not report recording after a failed start", manager.isRecording())
        assertEquals(0f, manager.amplitudeFlow.value, 0.001f)
    }

    @Test
    fun stopRecording_returnsEmptyBuffer_whenNothingWasCaptured() {
        val pcmData = manager.stopRecording()

        assertEquals("No audio was captured, so no samples may be returned", 0, pcmData.size)
        assertFalse(manager.isRecording())
        assertEquals(0f, manager.amplitudeFlow.value, 0.001f)
    }
}
