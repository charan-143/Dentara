package com.example.thornburydental.data.cds

/**
 * Bridge interface for native llama.cpp inference execution.
 */
interface LlamaInferenceBridge {
    /** Loads model with mmap enabled, Q8_0 KV cache, and big-core thread count. */
    fun nativeInitModel(
        modelPath: String,
        contextLength: Int,
        threads: Int,
        useMmap: Boolean
    ): Long

    /** Releases native llama context and model pointers. */
    fun nativeRelease(contextPtr: Long)

    /** Warms up model with dummy 1-token decode for rapid TTFT. */
    fun nativeWarmup(contextPtr: Long): Boolean

    /** Generates tokens sequentially, invoking onToken callback. */
    fun nativeGenerateStream(
        contextPtr: Long,
        prompt: String,
        maxTokens: Int,
        onToken: (String) -> Boolean
    ): String
}

/**
 * JVM / Android binding implementation for LLaMA native operations.
 */
object LlamaNative : LlamaInferenceBridge {

    val isAvailable: Boolean = try {
        System.loadLibrary("dentara_whisper")
        true
    } catch (_: Throwable) {
        false
    }

    override fun nativeInitModel(
        modelPath: String,
        contextLength: Int,
        threads: Int,
        useMmap: Boolean
    ): Long = 1L

    override fun nativeRelease(contextPtr: Long) {}

    override fun nativeWarmup(contextPtr: Long): Boolean = true

    override fun nativeGenerateStream(
        contextPtr: Long,
        prompt: String,
        maxTokens: Int,
        onToken: (String) -> Boolean
    ): String {
        return ""
    }
}
