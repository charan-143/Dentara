package com.example.thornburydental.ui.components.molar3d

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.max

object MolarObjParser {

    private var cachedModel: MolarModel? = null

    suspend fun loadModel(context: Context, assetName: String = "mandibular-first-molar.obj"): MolarModel =
        withContext(Dispatchers.IO) {
            cachedModel?.let { return@withContext it }

            context.assets.open(assetName).use { inputStream ->
                val model = parseObjStream(inputStream)
                cachedModel = model
                model
            }
        }

    fun parseObjStream(inputStream: InputStream): MolarModel {
        val reader = BufferedReader(InputStreamReader(inputStream), 32768)

        // Raw vertex positions and normals
        // Given ~16528 vertices, pre-size array for speed
        var posCapacity = 16600 * 3
        var normCapacity = 16600 * 3
        var rawPositions = FloatArray(posCapacity)
        var rawNormals = FloatArray(normCapacity)
        var vertexCount = 0
        var normalCount = 0

        // Material buckets for triangles: stores (posIdx, normIdx)
        // 0 = enamel, 1 = dentin, 2 = cementum
        class IntArrayList(initialCap: Int) {
            var data = IntArray(initialCap)
            var size = 0
            fun add(v: Int) {
                if (size == data.size) {
                    val next = IntArray(data.size * 2)
                    System.arraycopy(data, 0, next, 0, size)
                    data = next
                }
                data[size++] = v
            }
        }

        val enamelIndices = IntArrayList(45360 * 2)
        val dentinIndices = IntArrayList(4536 * 2)
        val cementumIndices = IntArrayList(43968 * 2)

        var currentBucket = enamelIndices
        var currentMaterial = "enamel"

        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        var line: String? = reader.readLine()
        while (line != null) {
            val len = line.length
            if (len >= 2) {
                val c0 = line[0]
                val c1 = line[1]

                if (c0 == 'v' && c1 == ' ') {
                    // Vertex: v x y z
                    val parts = line.split(" ").filter { it.isNotEmpty() }
                    if (parts.size >= 4) {
                        val x = parts[1].toFloat()
                        val y = parts[2].toFloat()
                        val z = parts[3].toFloat()

                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                        if (z < minZ) minZ = z
                        if (z > maxZ) maxZ = z

                        val idx = vertexCount * 3
                        if (idx + 2 >= rawPositions.size) {
                            val next = FloatArray(rawPositions.size * 2)
                            System.arraycopy(rawPositions, 0, next, 0, rawPositions.size)
                            rawPositions = next
                        }
                        rawPositions[idx] = x
                        rawPositions[idx + 1] = y
                        rawPositions[idx + 2] = z
                        vertexCount++
                    }
                } else if (c0 == 'v' && c1 == 'n') {
                    // Normal: vn nx ny nz
                    val parts = line.split(" ").filter { it.isNotEmpty() }
                    if (parts.size >= 4) {
                        val nx = parts[1].toFloat()
                        val ny = parts[2].toFloat()
                        val nz = parts[3].toFloat()

                        val idx = normalCount * 3
                        if (idx + 2 >= rawNormals.size) {
                            val next = FloatArray(rawNormals.size * 2)
                            System.arraycopy(rawNormals, 0, next, 0, rawNormals.size)
                            rawNormals = next
                        }
                        rawNormals[idx] = nx
                        rawNormals[idx + 1] = ny
                        rawNormals[idx + 2] = nz
                        normalCount++
                    }
                } else if (c0 == 'u' && line.startsWith("usemtl ")) {
                    val mtl = line.substring(7).trim()
                    currentMaterial = mtl
                    currentBucket = when (mtl) {
                        "enamel" -> enamelIndices
                        "dentin" -> dentinIndices
                        "cementum" -> cementumIndices
                        else -> enamelIndices
                    }
                } else if (c0 == 'f' && c1 == ' ') {
                    // Face: f v1//vn1 v2//vn2 v3//vn3
                    var start = 2
                    for (i in 0 until 3) {
                        while (start < len && line[start] == ' ') start++
                        var end = start
                        while (end < len && line[end] != ' ') end++
                        if (start < end) {
                            val token = line.substring(start, end)
                            val sepIdx = token.indexOf("//")
                            if (sepIdx != -1) {
                                val vIdx = token.substring(0, sepIdx).toInt() - 1
                                val vnIdx = token.substring(sepIdx + 2).toInt() - 1
                                currentBucket.add(vIdx)
                                currentBucket.add(vnIdx)
                            }
                            start = end + 1
                        }
                    }
                }
            }
            line = reader.readLine()
        }

        // Normalization calculation: Center and scale model so it spans approx -1.0 to +1.0
        val cx = (minX + maxX) / 2f
        val cy = (minY + maxY) / 2f
        val cz = (minZ + maxZ) / 2f
        val maxExtent = max(maxX - minX, max(maxY - minY, maxZ - minZ))
        val scale = if (maxExtent > 0f) 2.0f / maxExtent else 1.0f

        fun buildSubMesh(name: String, material: String, indices: IntArrayList): MolarSubMesh {
            val totalPairs = indices.size / 2 // each vertex is 2 ints: pos, norm
            val bufferSize = totalPairs * 6 // 3 pos + 3 norm floats
            val byteBuffer = ByteBuffer.allocateDirect(bufferSize * 4).order(ByteOrder.nativeOrder())
            val floatBuffer: FloatBuffer = byteBuffer.asFloatBuffer()

            var i = 0
            while (i < indices.size) {
                val vIdx = indices.data[i] * 3
                val vnIdx = indices.data[i + 1] * 3

                val x = (rawPositions[vIdx] - cx) * scale
                val y = (rawPositions[vIdx + 1] - cy) * scale
                val z = (rawPositions[vIdx + 2] - cz) * scale

                val nx = rawNormals[vnIdx]
                val ny = rawNormals[vnIdx + 1]
                val nz = rawNormals[vnIdx + 2]

                floatBuffer.put(x)
                floatBuffer.put(y)
                floatBuffer.put(z)
                floatBuffer.put(nx)
                floatBuffer.put(ny)
                floatBuffer.put(nz)

                i += 2
            }
            floatBuffer.position(0)
            return MolarSubMesh(
                name = name,
                material = material,
                vertexBuffer = floatBuffer,
                vertexCount = totalPairs
            )
        }

        val enamelMesh = buildSubMesh("Crown Enamel & Occlusal Table", "enamel", enamelIndices)
        val dentinMesh = buildSubMesh("Dentin Core & Pulp Chamber Walls", "dentin", dentinIndices)
        val cementumMesh = buildSubMesh("Bifurcated Roots & Cementum", "cementum", cementumIndices)

        val totalTriangles = (enamelMesh.vertexCount + dentinMesh.vertexCount + cementumMesh.vertexCount) / 3

        return MolarModel(
            subMeshes = listOf(enamelMesh, dentinMesh, cementumMesh),
            totalVertices = vertexCount,
            totalTriangles = totalTriangles,
            boundsMin = floatArrayOf(minX, minY, minZ),
            boundsMax = floatArrayOf(maxX, maxY, maxZ)
        )
    }
}
