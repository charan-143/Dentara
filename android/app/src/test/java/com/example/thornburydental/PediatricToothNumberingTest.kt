package com.example.thornburydental

import com.example.thornburydental.data.ToothCondition
import com.example.thornburydental.data.ToothNumberingSystem
import com.example.thornburydental.data.ToothRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PediatricToothNumberingTest {

    @Test
    fun testUniversalLetterToPrimaryFdiMapping() {
        assertEquals(55, ToothNumberingSystem.universalLetterToPrimaryFdi('A'))
        assertEquals(51, ToothNumberingSystem.universalLetterToPrimaryFdi('E'))
        assertEquals(61, ToothNumberingSystem.universalLetterToPrimaryFdi('F'))
        assertEquals(65, ToothNumberingSystem.universalLetterToPrimaryFdi('J'))
        assertEquals(75, ToothNumberingSystem.universalLetterToPrimaryFdi('K'))
        assertEquals(71, ToothNumberingSystem.universalLetterToPrimaryFdi('O'))
        assertEquals(81, ToothNumberingSystem.universalLetterToPrimaryFdi('P'))
        assertEquals(85, ToothNumberingSystem.universalLetterToPrimaryFdi('T'))
    }

    @Test
    fun testPrimaryFdiToUniversalLetterMapping() {
        assertEquals('A', ToothNumberingSystem.primaryFdiToUniversalLetter(55))
        assertEquals('E', ToothNumberingSystem.primaryFdiToUniversalLetter(51))
        assertEquals('J', ToothNumberingSystem.primaryFdiToUniversalLetter(65))
        assertEquals('T', ToothNumberingSystem.primaryFdiToUniversalLetter(85))
    }

    @Test
    fun testPrimarySuccessorCalculations() {
        // Upper right primary central incisor (51) succeeds to permanent central incisor (11)
        assertEquals(11, ToothNumberingSystem.getPermanentSuccessorFdi(51))
        // Upper right primary second molar (55) succeeds to permanent second premolar (15)
        assertEquals(15, ToothNumberingSystem.getPermanentSuccessorFdi(55))
        // Lower left primary canine (73) succeeds to permanent lower left canine (33)
        assertEquals(33, ToothNumberingSystem.getPermanentSuccessorFdi(73))
        // Lower right primary first molar (84) succeeds to permanent lower right first premolar (44)
        assertEquals(44, ToothNumberingSystem.getPermanentSuccessorFdi(84))
    }

    @Test
    fun testPrimaryAndPermanentValidation() {
        assertTrue(ToothNumberingSystem.isPrimaryFdi(55))
        assertTrue(ToothNumberingSystem.isPrimaryFdi(85))
        assertFalse(ToothNumberingSystem.isPrimaryFdi(16))

        assertTrue(ToothNumberingSystem.isPermanentFdi(11))
        assertTrue(ToothNumberingSystem.isPermanentFdi(48))
        assertFalse(ToothNumberingSystem.isPermanentFdi(55))
    }

    @Test
    fun testMixedDentitionToothRecord() {
        val primaryRecord = ToothRecord(
            number = 55,
            fdiNumber = 55,
            name = "Primary Maxillary Right Second Molar",
            arch = "Maxillary",
            condition = ToothCondition.SOUND,
            isPrimary = true,
            isExfoliated = false,
            permanentSuccessorFdi = 15
        )

        assertTrue(primaryRecord.isPrimary)
        assertFalse(primaryRecord.isExfoliated)
        assertEquals(15, primaryRecord.permanentSuccessorFdi)
    }
}
