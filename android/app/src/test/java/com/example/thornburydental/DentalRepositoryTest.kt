package com.example.thornburydental

import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.ToothCondition
import org.junit.Assert.*
import org.junit.Test

class DentalRepositoryTest {

    @Test
    fun testAllergyConflictDetection_penicillin() {
        val patient = DentalRepository.patients.value.first { it.id == "p1" } // Rosalind Achebe has Penicillin allergy
        val conflict = DentalRepository.checkAllergyConflict(patient, "Amoxicillin")
        assertNotNull("Should detect cross-reactivity between Penicillin allergy and Amoxicillin", conflict)
        assertTrue(conflict!!.contains("ALLERGY WARNING"))
    }

    @Test
    fun testAllergyConflictDetection_safeDrug() {
        val patient = DentalRepository.patients.value.first { it.id == "p1" }
        val conflict = DentalRepository.checkAllergyConflict(patient, "Clindamycin")
        assertNull("Clindamycin should not trigger Penicillin allergy conflict", conflict)
    }

    @Test
    fun testToothConditionUpdate() {
        val patient = DentalRepository.patients.value.first { it.id == "p1" }
        DentalRepository.updateToothCondition(patient.id, 8, ToothCondition.CROWN, "Monolithic zirconia crown placed")

        val updatedPatient = DentalRepository.patients.value.first { it.id == "p1" }
        assertEquals(ToothCondition.CROWN, updatedPatient.teeth[8]?.condition)
        assertEquals("Monolithic zirconia crown placed", updatedPatient.teeth[8]?.notes)
    }

    @Test
    fun testIssuePrescription() {
        val patient = DentalRepository.patients.value.first { it.id == "p3" }
        val initialCount = DentalRepository.prescriptions.value.size

        val rx = DentalRepository.issuePrescription(
            patient = patient,
            clinicianName = "Dr. Tomas Ferreira",
            drugName = "Ibuprofen",
            dosage = "600 mg",
            frequency = "TDS PRN",
            duration = "3 days",
            instructions = "Take after meals"
        )

        assertNotNull(rx.id)
        assertEquals(initialCount + 1, DentalRepository.prescriptions.value.size)
        assertEquals(rx.id, DentalRepository.prescriptions.value.first().id)
    }

    @Test
    fun testTogglePlanLock_generatesSha256Hash() {
        val plan = DentalRepository.treatmentPlans.value.first()
        assertTrue(plan.isLocked)
        assertTrue(plan.tamperHash.startsWith("sha256:"))

        DentalRepository.togglePlanLock(plan.id)
        val unlockedPlan = DentalRepository.treatmentPlans.value.first { it.id == plan.id }
        assertFalse(unlockedPlan.isLocked)

        DentalRepository.togglePlanLock(plan.id)
        val relockedPlan = DentalRepository.treatmentPlans.value.first { it.id == plan.id }
        assertTrue(relockedPlan.isLocked)
        assertTrue(relockedPlan.tamperHash.startsWith("sha256:"))
    }
}
