package com.example.thornburydental.data.cds

import android.content.Context

enum class ClinicalAiEngineType {
    ON_DEVICE_LLM,
    OFFLINE_VLM,
    CLOUD_GEMINI
}

data class MultimodalVlmModelSpec(
    val id: String,
    val displayName: String,
    val fileName: String,
    val visionProjector: String,
    val architecture: String
)

object ClinicalAiConfigManager {
    fun getEngineType(context: Context): ClinicalAiEngineType = ClinicalAiEngineType.OFFLINE_VLM
    fun getSelectedVlmModel(context: Context): MultimodalVlmModelSpec = MultimodalVlmModelSpec(
        id = "llava-1.5-7b-q4",
        displayName = "Llava 1.5 7B",
        fileName = "llava-1.5-7b-q4.gguf",
        visionProjector = "mmproj-model-f16.gguf",
        architecture = "llava"
    )
}
