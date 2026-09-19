package com.example.thornburydental

import com.example.thornburydental.data.cds.PatientVitalsContext
import com.example.thornburydental.data.cds.RedFlagSafetyChecker
import com.example.thornburydental.data.cds.SymptomItem
import com.example.thornburydental.data.cds.SymptomInputQuery
import org.junit.Assert.*
import org.junit.Test

class RedFlagSafetyCheckerTest {

    @Test
    fun testLudwigsAnginaAirwayRedFlag_detected() {
        val query = SymptomInputQuery(
            selectedSymptoms = listOf(
                SymptomItem("s3", "Facial Swelling / Submandibular Edema", "Maxillofacial", isRedFlagTrigger = true),
                SymptomItem("s9", "Dysphagia / Difficulty Swallowing", "Airway", isRedFlagTrigger = true)
            ),
            freeTextDescription = "Patient has severe submandibular swelling and floor of mouth elevation."
        )

        val result = RedFlagSafetyChecker.evaluate(query)
        assertTrue(result.hasRedFlag)
        assertNotNull(result.redFlagReason)
        assertTrue(result.redFlagReason!!.contains("Airway Compromise") || result.redFlagReason!!.contains("Ludwig"))
        assertTrue(result.recommendedActions.isNotEmpty())
    }

    @Test
    fun testHighFeverSepticemiaRedFlag_detected() {
        val query = SymptomInputQuery(
            selectedSymptoms = listOf(
                SymptomItem("s12", "Fever (≥ 38.5°C)", "Systemic", isRedFlagTrigger = true)
            ),
            vitalsContext = PatientVitalsContext(
                temperatureCelsius = 39.5f,
                heartRateBpm = 115
            )
        )

        val result = RedFlagSafetyChecker.evaluate(query)
        assertTrue(result.hasRedFlag)
        assertTrue(result.redFlagReason!!.contains("Systemic Toxicity") || result.redFlagReason!!.contains("Sepsis"))
    }

    @Test
    fun testAnaphylaxisRedFlag_detected() {
        val query = SymptomInputQuery(
            freeTextDescription = "Patient developed hives, lip angioedema, and urticaria and wheezing."
        )

        val result = RedFlagSafetyChecker.evaluate(query)
        assertTrue(result.hasRedFlag)
        assertTrue(result.redFlagReason!!.contains("Anaphylactic"))
        assertTrue(result.recommendedActions.any { it.contains("Epinephrine") })
    }

    @Test
    fun testNormalPulpitisSymptom_noRedFlag() {
        val query = SymptomInputQuery(
            selectedSymptoms = listOf(
                SymptomItem("s1", "Severe Spontaneous Toothache", "Maxillofacial & Dental")
            ),
            freeTextDescription = "Pain in lower right molar when drinking hot coffee."
        )

        val result = RedFlagSafetyChecker.evaluate(query)
        assertFalse(result.hasRedFlag)
        assertNull(result.redFlagReason)
    }

    @Test
    fun testNegationHandling_doesNotTriggerFalsePositive() {
        // NegEx negation: Clinician explicitly affirms patient DENIES shortness of breath, no fever, no airway compromise
        val query = SymptomInputQuery(
            selectedSymptoms = listOf(
                SymptomItem("s1", "Mild Tooth Discomfort", "Dental")
            ),
            freeTextDescription = "Localized cavity on tooth 14. Patient denies shortness of breath. No fever, without breathing difficulty, no airway compromise."
        )

        val result = RedFlagSafetyChecker.evaluate(query)
        assertFalse("Explicitly negated symptoms must not trigger false positive emergency alerts", result.hasRedFlag)
    }

    @Test
    fun testClinicalAcronyms_detectedCorrectly() {
        val query = SymptomInputQuery(
            freeTextDescription = "Severe SOB and FOM swelling with high fever."
        )

        val result = RedFlagSafetyChecker.evaluate(query)
        assertTrue(result.hasRedFlag)
        assertTrue(result.redFlagReason!!.contains("Airway Compromise"))
    }

    @Test
    fun testHemodynamicShockIndex_triggersRedFlag() {
        val query = SymptomInputQuery(
            freeTextDescription = "Persistent bleeding post-extraction.",
            vitalsContext = PatientVitalsContext(
                heartRateBpm = 125,
                bloodPressureSystolic = 85 // SI = 125 / 85 = 1.47 > 0.9 (Shock)
            )
        )

        val result = RedFlagSafetyChecker.evaluate(query)
        assertTrue(result.hasRedFlag)
        assertTrue(result.redFlagReason!!.contains("Hemorrhage") || result.redFlagReason!!.contains("Shock"))
    }

    @Test
    fun testPatientAllergyScreening_flagsPenicillinContraindication() {
        val query = SymptomInputQuery(
            selectedSymptoms = listOf(
                SymptomItem("s1", "Periapical Abscess", "Dental")
            ),
            freeTextDescription = "Large fluctuant swelling requiring antibiotic therapy.",
            vitalsContext = PatientVitalsContext(
                patientAllergies = listOf("Penicillin (Anaphylaxis)")
            )
        )

        val result = RedFlagSafetyChecker.evaluate(query)
        assertTrue(result.hasRedFlag)
        assertTrue(result.redFlagReason!!.contains("PENICILLIN ALLERGY"))
        assertTrue(result.recommendedActions.any { it.contains("Clindamycin") || it.contains("Azithromycin") })
    }
}
