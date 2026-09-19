package com.example.thornburydental

import com.example.thornburydental.data.cds.CdsResponseState
import com.example.thornburydental.data.cds.LlmGenerationController
import com.example.thornburydental.data.cds.LlmModels
import com.example.thornburydental.data.cds.OnDeviceLlmEngine
import com.example.thornburydental.data.cds.SymptomInputQuery
import com.example.thornburydental.data.cds.SymptomItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnDeviceLlmEngineTest {

    private lateinit var engine: OnDeviceLlmEngine
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        engine = OnDeviceLlmEngine(inferenceDispatcher = testDispatcher)
    }

    @Test
    fun testDetectDeviceTier_returnsValidTier() {
        val tier = engine.detectDeviceTier()
        assertNotNull(tier)
        assertTrue(tier.contains("Tier") || tier.contains("Llama") || tier.contains("Gemma"))
    }

    @Test
    fun testWarmupAndTrimMemory() {
        assertTrue(engine.warmup(LlmModels.LLAMA_3_2_1B_Q4))
        engine.onTrimMemory(80) // TRIM_MEMORY_COMPLETE
    }

    @Test
    fun testGenerateResponseStream_streamsTokensAndCompletes() = runTest(testDispatcher) {
        val query = SymptomInputQuery(
            selectedSymptoms = listOf(
                SymptomItem("s1", "Severe Spontaneous Toothache", "Maxillofacial & Dental")
            ),
            freeTextDescription = "Pain in molar."
        )

        val states = engine.generateResponseStream(query).toList()
        assertTrue("States list should not be empty", states.isNotEmpty())
        assertTrue("First state should be ProcessingRetrieval", states.first() is CdsResponseState.ProcessingRetrieval)

        val lastState = states.last()
        assertTrue("Final state should be Complete", lastState is CdsResponseState.Complete)

        val complete = lastState as CdsResponseState.Complete
        assertFalse(complete.fullText.isEmpty())
        assertFalse(complete.differentials.isEmpty())
        assertFalse(complete.citedPassages.isEmpty())
    }

    @Test
    fun testGenerateResponseStream_respectsBargeInAbort() = runTest(testDispatcher) {
        val controller = LlmGenerationController()
        controller.requestAbort() // Trigger barge-in before/during generation

        val query = SymptomInputQuery(
            selectedSymptoms = listOf(
                SymptomItem("s1", "Fever and swelling", "Infection")
            ),
            freeTextDescription = "Abscess under tooth"
        )

        val states = engine.generateResponseStream(query, controller).toList()
        assertTrue(states.isNotEmpty())
        val lastState = states.last()
        assertTrue("Generation completes cleanly after abort", lastState is CdsResponseState.Complete)
    }
}
