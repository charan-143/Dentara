package com.example.thornburydental

import com.example.thornburydental.data.ToothAnatomyType
import com.example.thornburydental.data.ToothCondition
import com.example.thornburydental.data.ToothRecord
import com.example.thornburydental.ui.components.getToothAnatomyType
import com.example.thornburydental.ui.components.isMaxillaryTooth
import org.junit.Assert.*
import org.junit.Test

class AnatomicalToothClassificationTest {

    @Test
    fun testMaxillaryToothIdentification_FdiNumbering() {
        // FDI Quadrant 1 (UR 11-18) & Quadrant 2 (UL 21-28) -> Maxillary
        val upperPermanentFdi = listOf(18, 17, 16, 15, 14, 13, 12, 11, 21, 22, 23, 24, 25, 26, 27, 28)
        for (fdi in upperPermanentFdi) {
            assertTrue("FDI Tooth #$fdi must be Maxillary (Upper)", isMaxillaryTooth(fdi))
        }

        // FDI Quadrant 5 (UR Primary 51-55) & Quadrant 6 (UL Primary 61-65) -> Maxillary
        val upperPrimaryFdi = listOf(55, 54, 53, 52, 51, 61, 62, 63, 64, 65)
        for (fdi in upperPrimaryFdi) {
            assertTrue("Primary FDI Tooth #$fdi must be Maxillary (Upper)", isMaxillaryTooth(fdi))
        }

        // FDI Quadrant 3 (LL 31-38) & Quadrant 4 (LR 41-48) -> Mandibular
        val lowerPermanentFdi = listOf(31, 32, 33, 34, 35, 36, 37, 38, 41, 42, 43, 44, 45, 46, 47, 48)
        for (fdi in lowerPermanentFdi) {
            assertFalse("FDI Tooth #$fdi must be Mandibular (Lower)", isMaxillaryTooth(fdi))
        }

        // FDI Quadrant 7 (LL Primary 71-75) & Quadrant 8 (LR Primary 81-85) -> Mandibular
        val lowerPrimaryFdi = listOf(71, 72, 73, 74, 75, 81, 82, 83, 84, 85)
        for (fdi in lowerPrimaryFdi) {
            assertFalse("Primary FDI Tooth #$fdi must be Mandibular (Lower)", isMaxillaryTooth(fdi))
        }
    }

    @Test
    fun testToothRecordMaxillaryResolution() {
        val upperTooth = ToothRecord(
            number = 21,
            fdiNumber = 21,
            name = "Maxillary Left Central Incisor",
            arch = "Maxillary (Upper Arch)",
            condition = ToothCondition.SOUND
        )
        assertTrue(isMaxillaryTooth(upperTooth))

        val lowerTooth = ToothRecord(
            number = 36,
            fdiNumber = 36,
            name = "Mandibular Left First Molar",
            arch = "Mandibular (Lower Arch)",
            condition = ToothCondition.SOUND
        )
        assertFalse(isMaxillaryTooth(lowerTooth))

        val childUpper = ToothRecord(
            number = 55,
            fdiNumber = 55,
            name = "Primary Maxillary Right Second Molar",
            arch = "Maxillary (Upper Primary Arch)",
            condition = ToothCondition.SOUND
        )
        assertTrue(isMaxillaryTooth(childUpper))

        val childLower = ToothRecord(
            number = 75,
            fdiNumber = 75,
            name = "Primary Mandibular Left Second Molar",
            arch = "Mandibular (Lower Primary Arch)",
            condition = ToothCondition.SOUND
        )
        assertFalse(isMaxillaryTooth(childLower))
    }

    @Test
    fun testToothAnatomyClassification_Fdi() {
        // Upper Incisors
        assertEquals(ToothAnatomyType.INCISOR, getToothAnatomyType(11))
        assertEquals(ToothAnatomyType.INCISOR, getToothAnatomyType(21))
        assertEquals(ToothAnatomyType.INCISOR, getToothAnatomyType(12))
        assertEquals(ToothAnatomyType.INCISOR, getToothAnatomyType(22))

        // Upper Canines
        assertEquals(ToothAnatomyType.CANINE, getToothAnatomyType(13))
        assertEquals(ToothAnatomyType.CANINE, getToothAnatomyType(23))

        // Upper Premolars
        assertEquals(ToothAnatomyType.PREMOLAR, getToothAnatomyType(14))
        assertEquals(ToothAnatomyType.PREMOLAR, getToothAnatomyType(15))
        assertEquals(ToothAnatomyType.PREMOLAR, getToothAnatomyType(24))
        assertEquals(ToothAnatomyType.PREMOLAR, getToothAnatomyType(25))

        // Upper Molars
        assertEquals(ToothAnatomyType.MOLAR, getToothAnatomyType(16))
        assertEquals(ToothAnatomyType.MOLAR, getToothAnatomyType(17))
        assertEquals(ToothAnatomyType.MOLAR, getToothAnatomyType(18))
        assertEquals(ToothAnatomyType.MOLAR, getToothAnatomyType(26))
        assertEquals(ToothAnatomyType.MOLAR, getToothAnatomyType(27))
        assertEquals(ToothAnatomyType.MOLAR, getToothAnatomyType(28))
    }

    @Test
    fun testQuadrantOrderingStartsFromMidlineToMesial() {
        // Upper Right (UR Q1) starts with Central Incisor (11) and ends with 3rd Molar (18)
        val urPermanent = listOf(11, 12, 13, 14, 15, 16, 17, 18)
        assertEquals(11, urPermanent.first()) // Midline (Central Incisor)
        assertEquals(18, urPermanent.last())  // Distal (Wisdom Molar)

        // Upper Left (UL Q2) starts with Central Incisor (21) and ends with 3rd Molar (28)
        val ulPermanent = listOf(21, 22, 23, 24, 25, 26, 27, 28)
        assertEquals(21, ulPermanent.first()) // Midline (Central Incisor)
        assertEquals(28, ulPermanent.last())  // Distal (Wisdom Molar)

        // Lower Left (LL Q3) starts with Central Incisor (31) and ends with 3rd Molar (38)
        val llPermanent = listOf(31, 32, 33, 34, 35, 36, 37, 38)
        assertEquals(31, llPermanent.first()) // Midline (Central Incisor)
        assertEquals(38, llPermanent.last())  // Distal (Wisdom Molar)

        // Lower Right (LR Q4) starts with Central Incisor (41) and ends with 3rd Molar (48)
        val lrPermanent = listOf(41, 42, 43, 44, 45, 46, 47, 48)
        assertEquals(41, lrPermanent.first()) // Midline (Central Incisor)
        assertEquals(48, lrPermanent.last())  // Distal (Wisdom Molar)

        // Primary Teeth:
        // UR Primary (Q5): 51 -> 55
        assertEquals(51, listOf(51, 52, 53, 54, 55).first())
        // UL Primary (Q6): 61 -> 65
        assertEquals(61, listOf(61, 62, 63, 64, 65).first())
        // LL Primary (Q7): 71 -> 75
        assertEquals(71, listOf(71, 72, 73, 74, 75).first())
        // LR Primary (Q8): 81 -> 85
        assertEquals(81, listOf(81, 82, 83, 84, 85).first())
    }
}
