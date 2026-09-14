package com.example.thornburydental.ui.components.molar3d

import java.nio.FloatBuffer

enum class MolarLayer(
    val displayName: String,
    val description: String,
    val materialName: String,
    val defaultRgba: FloatArray,
    val highlightedRgba: FloatArray
) {
    ALL(
        displayName = "Full Anatomy",
        description = "Complete crown and bifurcated root anatomy",
        materialName = "all",
        defaultRgba = floatArrayOf(0.95f, 0.93f, 0.88f, 1.0f),
        highlightedRgba = floatArrayOf(0.95f, 0.93f, 0.88f, 1.0f)
    ),
    ENAMEL(
        displayName = "Enamel",
        description = "Mineralized crown shell & occlusal table",
        materialName = "enamel",
        defaultRgba = floatArrayOf(0.96f, 0.94f, 0.90f, 1.0f),
        highlightedRgba = floatArrayOf(1.0f, 0.98f, 0.94f, 1.0f)
    ),
    DENTIN(
        displayName = "Dentin Core",
        description = "Vital core cushion communicating with root canal",
        materialName = "dentin",
        defaultRgba = floatArrayOf(0.91f, 0.78f, 0.52f, 1.0f),
        highlightedRgba = floatArrayOf(0.98f, 0.82f, 0.45f, 1.0f)
    ),
    ROOTS(
        displayName = "Roots (Cementum)",
        description = "Mesial & distal roots anchored in alveolar bone",
        materialName = "cementum",
        defaultRgba = floatArrayOf(0.78f, 0.62f, 0.48f, 1.0f),
        highlightedRgba = floatArrayOf(0.85f, 0.55f, 0.38f, 1.0f)
    )
}

data class MolarSubMesh(
    val name: String,
    val material: String,
    val vertexBuffer: FloatBuffer, // Interleaved: position (3) + normal (3)
    val vertexCount: Int
)

data class MolarModel(
    val subMeshes: List<MolarSubMesh>,
    val totalVertices: Int,
    val totalTriangles: Int,
    val boundsMin: FloatArray,
    val boundsMax: FloatArray
)
