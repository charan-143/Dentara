package com.example.thornburydental.data.cds

import android.app.ActivityManager
import android.content.Context
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * On-device LLM runtime configuration, memory policies, and model specifications.
 */
data class LlmModelSpec(
    val id: String,
    val displayName: String,
    val fileName: String,
    val parameterCount: String,
    val quantization: String,
    val minRamMb: Long,
    val contextLength: Int = 2048
)

object LlmModels {
    val LLAMA_3_2_1B_Q4 = LlmModelSpec(
        id = "llama-3.2-1b-instruct-q4_0",
        displayName = "Llama-3.2-1B-Instruct (Q4_0, ~0.8 GB)",
        fileName = "llama-3.2-1b-instruct-q4_0.gguf",
        parameterCount = "1B",
        quantization = "Q4_0",
        minRamMb = 1800L,
        contextLength = 2048
    )

    val LLAMA_3_2_3B_Q4 = LlmModelSpec(
        id = "llama-3.2-3b-instruct-q4_0",
        displayName = "Llama-3.2-3B-Instruct (Q4_0, ~2.0 GB)",
        fileName = "llama-3.2-3b-instruct-q4_0.gguf",
        parameterCount = "3B",
        quantization = "Q4_0",
        minRamMb = 3800L,
        contextLength = 2048
    )

    val GEMMA_2_2B_Q4 = LlmModelSpec(
        id = "gemma-2-2b-it-q4_0",
        displayName = "Gemma-2-2B-IT (Q4_0, ~1.4 GB)",
        fileName = "gemma-2-2b-it-q4_0.gguf",
        parameterCount = "2B",
        quantization = "Q4_0",
        minRamMb = 2800L,
        contextLength = 2048
    )

    /**
     * Inspects system available RAM via ActivityManager.MemoryInfo to choose
     * the optimal model tier (1B vs 3B) without risking OOM termination.
     */
    fun selectModelForDevice(context: Context?): LlmModelSpec {
        if (context == null) return LLAMA_3_2_1B_Q4

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val availMemMb = memoryInfo.availMem / (1024 * 1024)
        val totalMemMb = memoryInfo.totalMem / (1024 * 1024)

        return when {
            totalMemMb >= 6000L && availMemMb >= 2500L -> LLAMA_3_2_3B_Q4
            totalMemMb >= 4000L && availMemMb >= 1600L -> GEMMA_2_2B_Q4
            else -> LLAMA_3_2_1B_Q4
        }
    }
}

/**
 * Controller for managing prompt prefix caching and barge-in / abort tokens.
 */
class LlmGenerationController {
    val isAborted = AtomicBoolean(false)

    fun requestAbort() {
        isAborted.set(true)
    }

    fun reset() {
        isAborted.set(false)
    }
}
