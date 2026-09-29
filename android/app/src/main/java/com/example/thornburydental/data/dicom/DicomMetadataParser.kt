package com.example.thornburydental.data.dicom

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.hypot

data class DicomMetadata(
    val patientId: String = "",
    val modality: String = "IO", // IO = Intraoral, DX = Digital Radiography, CT = CBCT
    val studyDescription: String = "",
    val rows: Int = 0,
    val columns: Int = 0,
    val bitsAllocated: Int = 16,
    val pixelSpacingMmX: Double = 0.05, // Default 50 microns / pixel for high-res intraoral
    val pixelSpacingMmY: Double = 0.05,
    val windowCenter: Double = 2048.0,
    val windowWidth: Double = 4096.0,
    val isValidDicom: Boolean = false
) {
    /**
     * Calculates physical distance in millimeters between two pixel points (e.g. root apex and crown cusp).
     */
    fun calculatePhysicalDistanceMm(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val dxMm = (x2 - x1) * pixelSpacingMmX
        val dyMm = (y2 - y1) * pixelSpacingMmY
        return hypot(dxMm, dyMm)
    }
}

/**
 * Lightweight, zero-dependency streaming DICOM parser for Android.
 * Extracts radiographic calibration tags, photometric windowing, and cephalometric metadata.
 */
object DicomMetadataParser {

    private val DICM_MAGIC = byteArrayOf(0x44, 0x49, 0x43, 0x4D) // "DICM"

    fun parse(inputStream: InputStream): DicomMetadata {
        val bytes = inputStream.readBytes()
        if (bytes.size < 132) {
            return DicomMetadata(isValidDicom = false)
        }

        // Check 128-byte preamble + "DICM" prefix
        val hasMagic = (0..3).all { bytes[128 + it] == DICM_MAGIC[it] }
        if (!hasMagic) {
            return DicomMetadata(isValidDicom = false)
        }

        var patientId = ""
        var modality = "IO"
        var studyDesc = ""
        var rows = 0
        var columns = 0
        var bitsAllocated = 16
        var pixelSpacingX = 0.05
        var pixelSpacingY = 0.05
        var windowCenter = 2048.0
        var windowWidth = 4096.0

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var offset = 132

        try {
            while (offset + 8 <= bytes.size) {
                val group = buffer.getShort(offset).toInt() and 0xFFFF
                val element = buffer.getShort(offset + 2).toInt() and 0xFFFF
                offset += 4

                // Read VR (Explicit VR has 2 chars)
                val vrBytes = byteArrayOf(bytes[offset], bytes[offset + 1])
                val vr = String(vrBytes, Charsets.US_ASCII)

                val length: Int
                if (vr in listOf("OB", "OW", "OF", "SQ", "UT", "UN")) {
                    offset += 4 // 2 bytes VR + 2 bytes reserved
                    length = buffer.getInt(offset)
                    offset += 4
                } else if (vr.all { it.isLetter() && it.isUpperCase() }) {
                    offset += 2
                    length = buffer.getShort(offset).toInt() and 0xFFFF
                    offset += 2
                } else {
                    // Implicit VR fallback
                    offset -= 2
                    length = buffer.getInt(offset)
                    offset += 4
                }

                if (length < 0 || offset + length > bytes.size) break

                val valueBytes = bytes.copyOfRange(offset, offset + length)
                val valueStr = String(valueBytes, Charsets.UTF_8).trim().replace("\u0000", "")

                when {
                    group == 0x0010 && element == 0x0020 -> patientId = valueStr
                    group == 0x0008 && element == 0x0060 -> modality = valueStr
                    group == 0x0008 && element == 0x1030 -> studyDesc = valueStr
                    group == 0x0028 && element == 0x0010 -> rows = valueStr.toIntOrNull() ?: if (length >= 2) buffer.getShort(offset).toInt() and 0xFFFF else 0
                    group == 0x0028 && element == 0x0011 -> columns = valueStr.toIntOrNull() ?: if (length >= 2) buffer.getShort(offset).toInt() and 0xFFFF else 0
                    group == 0x0028 && element == 0x0100 -> bitsAllocated = valueStr.toIntOrNull() ?: if (length >= 2) buffer.getShort(offset).toInt() and 0xFFFF else 16
                    group == 0x0028 && element == 0x0030 -> {
                        val parts = valueStr.split("\\")
                        if (parts.size >= 2) {
                            pixelSpacingY = parts[0].toDoubleOrNull() ?: 0.05
                            pixelSpacingX = parts[1].toDoubleOrNull() ?: 0.05
                        }
                    }
                    group == 0x0028 && element == 0x1050 -> windowCenter = valueStr.toDoubleOrNull() ?: 2048.0
                    group == 0x0028 && element == 0x1051 -> windowWidth = valueStr.toDoubleOrNull() ?: 4096.0
                }

                offset += length
            }
        } catch (_: Exception) {
            // Reached unparseable pixel data chunk
        }

        return DicomMetadata(
            patientId = patientId,
            modality = modality,
            studyDescription = studyDesc,
            rows = rows,
            columns = columns,
            bitsAllocated = bitsAllocated,
            pixelSpacingMmX = pixelSpacingX,
            pixelSpacingMmY = pixelSpacingY,
            windowCenter = windowCenter,
            windowWidth = windowWidth,
            isValidDicom = true
        )
    }
}
