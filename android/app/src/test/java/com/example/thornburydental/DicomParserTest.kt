package com.example.thornburydental

import com.example.thornburydental.data.dicom.DicomMetadata
import com.example.thornburydental.data.dicom.DicomMetadataParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class DicomParserTest {

    @Test
    fun testInvalidStreamRejection() {
        val invalidBytes = ByteArray(50) { 0 }
        val metadata = DicomMetadataParser.parse(ByteArrayInputStream(invalidBytes))
        assertFalse(metadata.isValidDicom)
    }

    @Test
    fun testPhysicalDistanceCalculation() {
        // High-res intraoral sensor: 0.05 mm / pixel (50 microns)
        val metadata = DicomMetadata(
            pixelSpacingMmX = 0.05,
            pixelSpacingMmY = 0.05,
            isValidDicom = true
        )

        // 200 pixels vertically = 200 * 0.05 = 10.0 mm
        val distanceMm = metadata.calculatePhysicalDistanceMm(
            x1 = 100.0,
            y1 = 100.0,
            x2 = 100.0,
            y2 = 300.0
        )

        assertEquals(10.0, distanceMm, 0.001)
    }
}
