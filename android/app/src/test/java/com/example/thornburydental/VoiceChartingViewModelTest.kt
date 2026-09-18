package com.example.thornburydental

import androidx.lifecycle.ViewModelStore
import com.example.thornburydental.speech.DictationTargetMode
import com.example.thornburydental.speech.VoiceChartingController
import com.example.thornburydental.speech.VoiceChartingViewModel
import com.example.thornburydental.speech.VoiceDictationState
import com.example.thornburydental.speech.WhisperEngine
import com.example.thornburydental.speech.WhisperNativeBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceChartingViewModelTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private class FakeBridge : WhisperNativeBridge {
        var released = 0
        override fun nativeInit(modelPath: String): Long = 1L
        override fun nativeRelease(contextPtr: Long) {
            released++
        }

        override fun nativeTranscribe(
            contextPtr: Long,
            pcm: ShortArray,
            threads: Int,
            language: String?,
            prompt: String?
        ): String? = ""
    }

    @Before
    fun setUp() {
        // viewModelScope dispatches on Main, which does not exist under JVM unit tests.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun modelFile(): File = temporaryFolder.newFile("ggml-test-" + System.nanoTime() + ".bin")

    private fun controllerWith(engine: WhisperEngine) =
        VoiceChartingController(whisperEngine = engine, modelStore = null)

    @Test
    fun exposesTheControllerStateRatherThanACopy() {
        val controller = controllerWith(WhisperEngine(bridge = FakeBridge()))
        val viewModel = VoiceChartingViewModel(controller)

        assertSame(controller.uiState, viewModel.uiState)
        assertSame(controller.targetMode, viewModel.targetMode)
        assertSame(controller.modelState, viewModel.modelState)
    }

    @Test
    fun setTargetMode_delegatesToTheController() {
        val controller = controllerWith(WhisperEngine(bridge = FakeBridge()))
        val viewModel = VoiceChartingViewModel(controller)

        viewModel.setTargetMode(DictationTargetMode.CLINICAL_NOTES)

        assertEquals(DictationTargetMode.CLINICAL_NOTES, controller.targetMode.value)
    }

    @Test
    fun releaseMicrophone_isSafeAtAnyTimeAndLeavesNothingListening() {
        val controller = controllerWith(WhisperEngine(bridge = FakeBridge()))
        val viewModel = VoiceChartingViewModel(controller)

        viewModel.releaseMicrophone()

        assertFalse(controller.uiState.value is VoiceDictationState.Listening)
    }

    /**
     * onCleared runs after viewModelScope is already cancelled, so releasing the native decoder
     * is handed to a separate scope. If that scope were tied to the ViewModel the decoder would
     * leak a multi-megabyte context every time the clinician left the chart screen.
     */
    @Test
    fun clearingTheViewModel_releasesTheNativeDecoder() = runBlocking {
        val bridge = FakeBridge()
        val engine = WhisperEngine(bridge = bridge)
        assertTrue("engine should load before the test starts", engine.initialize(modelFile()))

        val viewModel = VoiceChartingViewModel(controllerWith(engine))
        val store = ViewModelStore()
        store.put("voice", viewModel)

        store.clear()

        val released = withTimeoutOrNull(5_000) {
            while (engine.isReady()) {
                delay(10)
            }
            true
        }

        assertTrue("Native decoder was not released after the ViewModel was cleared", released == true)
        assertEquals("Decoder context should be freed exactly once", 1, bridge.released)
    }
}
