package com.example.thornburydental

import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.PlanStep
import com.example.thornburydental.data.ToothCondition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DentalRepositoryTest {

    // =========================================================================
    // 1. Allergy Conflict Detection & Clinical Override
    // =========================================================================

    @Test
    fun testAllergyConflictDetection_penicillinCrossReactivityWithAmoxicillin() {
        val patient = DentalRepository.patients.value.first { it.id == "p1" }.copy(
            allergies = listOf(com.example.thornburydental.data.Allergy("Penicillin", "Severe / Anaphylaxis", "Urticaria, bronchospasm"))
        )
        val conflict = DentalRepository.checkAllergyConflict(patient, "Amoxicillin")
        assertNotNull("Should detect cross-reactivity between Penicillin allergy and Amoxicillin", conflict)
        assertTrue("Conflict warning should indicate allergy alert", conflict!!.contains("ALLERGY WARNING"))
        assertTrue("Conflict message should reference Penicillin", conflict.contains("Penicillin", ignoreCase = true))
    }

    @Test
    fun testAllergyConflictDetection_penicillinCrossReactivityWithCoAmoxiclav() {
        val patient = DentalRepository.patients.value.first { it.id == "p1" }.copy(
            allergies = listOf(com.example.thornburydental.data.Allergy("Penicillin", "Severe / Anaphylaxis", "Urticaria, bronchospasm"))
        )
        val conflict = DentalRepository.checkAllergyConflict(patient, "Co-Amoxiclav")
        assertNotNull("Should detect cross-reactivity between Penicillin allergy and Co-Amoxiclav", conflict)
        assertTrue("Conflict message should contain allergy warning", conflict!!.contains("ALLERGY WARNING"))
        assertTrue("Conflict message should reference Penicillin", conflict.contains("Penicillin", ignoreCase = true))
    }

    @Test
    fun testAllergyConflictDetection_safeDrugClindamycin() {
        val patient = DentalRepository.patients.value.first { it.id == "p1" }
        val conflict = DentalRepository.checkAllergyConflict(patient, "Clindamycin")
        assertNull("Clindamycin should not trigger Penicillin allergy conflict", conflict)
    }

    @Test
    fun testIssuePrescription_withClinicalOverride_appendsOverrideToInstructions() {
        val patient = DentalRepository.patients.value.first { it.id == "p1" }
        val overrideReason = "Severe localized infection unresponsive to alternative macrolides; monitored inpatient"
        val baseInstructions = "Take 1 capsule every 8 hours with a full glass of water"

        val rx = DentalRepository.issuePrescription(
            patient = patient,
            clinicianName = "Dr. Ingrid Halvorsen",
            drugName = "Amoxicillin",
            dosage = "500 mg",
            frequency = "TDS",
            duration = "5 days",
            instructions = baseInstructions,
            overrideReason = overrideReason
        )

        assertNotNull(rx.id)
        assertTrue(
            "Prescription instructions must include clinical override bracket",
            rx.instructions.contains("[CLINICAL OVERRIDE: $overrideReason]")
        )
        assertTrue(
            "Prescription instructions must retain original base instructions",
            rx.instructions.startsWith(baseInstructions)
        )

        // Verify stored in StateFlow
        val storedRx = DentalRepository.prescriptions.value.first { it.id == rx.id }
        assertEquals(rx.instructions, storedRx.instructions)
    }

    @Test
    fun testIssuePrescription_withoutOverrideReason_leavesInstructionsClean() {
        val patient = DentalRepository.patients.value.first { it.id == "p3" }
        val cleanInstructions = "Take 1 tablet after food every 6 to 8 hours PRN"

        val rx = DentalRepository.issuePrescription(
            patient = patient,
            clinicianName = "Dr. Ingrid Halvorsen",
            drugName = "Ibuprofen",
            dosage = "400 mg",
            frequency = "TDS PRN",
            duration = "3 days",
            instructions = cleanInstructions,
            overrideReason = null
        )

        assertNotNull(rx.id)
        assertEquals("Instructions should be clean without override tag", cleanInstructions, rx.instructions)
        assertFalse("Instructions should not have CLINICAL OVERRIDE prefix", rx.instructions.contains("[CLINICAL OVERRIDE"))
    }

    // =========================================================================
    // 2. Canonical SHA-256 Treatment Plan Hashing
    // =========================================================================

    private val sampleSteps = listOf(
        PlanStep("s1", null, "Full mouth periodontal charting and OHI", "D0180", 120.0, true),
        PlanStep("s2", 3, "Quadrant scaling and root planing (Upper Right)", "D4341", 280.0, true),
        PlanStep("s3", 30, "Quadrant scaling and root planing (Lower Right)", "D4341", 280.0, false)
    )

    @Test
    fun testComputeCanonicalPlanHash_validSha256DigestFormatAndLength() {
        val hash = DentalRepository.computeCanonicalPlanHash(
            planId = "plan-canonical-1",
            patientId = "p1",
            diagnosis = "Generalized Periodontitis Stage III",
            steps = sampleSteps
        )

        assertTrue("Hash must start with sha256: prefix", hash.startsWith("sha256:"))
        assertEquals("Total length of sha256: + 64 hex digits must be exactly 71", 71, hash.length)

        val hexPart = hash.removePrefix("sha256:")
        assertEquals("Hex part must be 64 characters long", 64, hexPart.length)
        assertTrue("Hex part must contain only valid lowercase hexadecimal digits", hexPart.matches(Regex("^[0-9a-f]{64}$")))
    }

    @Test
    fun testComputeCanonicalPlanHash_determinism() {
        val hash1 = DentalRepository.computeCanonicalPlanHash(
            planId = "plan-canonical-1",
            patientId = "p1",
            diagnosis = "Generalized Periodontitis Stage III",
            steps = sampleSteps
        )

        val hash2 = DentalRepository.computeCanonicalPlanHash(
            planId = "plan-canonical-1",
            patientId = "p1",
            diagnosis = "Generalized Periodontitis Stage III",
            steps = sampleSteps
        )

        assertEquals("Hashes from identical plan inputs must be strictly equal", hash1, hash2)

        // Verify step ordering invariance (canonical hashing sorts steps by id)
        val hashReordered = DentalRepository.computeCanonicalPlanHash(
            planId = "plan-canonical-1",
            patientId = "p1",
            diagnosis = "Generalized Periodontitis Stage III",
            steps = sampleSteps.reversed()
        )
        assertEquals("Canonical hash must be independent of step list insertion order", hash1, hashReordered)
    }

    @Test
    fun testComputeCanonicalPlanHash_tamperSensitivity_stepFee() {
        val baseHash = DentalRepository.computeCanonicalPlanHash(
            planId = "plan-canonical-1",
            patientId = "p1",
            diagnosis = "Generalized Periodontitis Stage III",
            steps = sampleSteps
        )

        val tamperedSteps = sampleSteps.map { step ->
            if (step.id == "s2") step.copy(fee = 350.0) else step
        }
        val tamperedHash = DentalRepository.computeCanonicalPlanHash(
            planId = "plan-canonical-1",
            patientId = "p1",
            diagnosis = "Generalized Periodontitis Stage III",
            steps = tamperedSteps
        )

        assertNotEquals("Altering step fee must change the canonical tamper-proof hash", baseHash, tamperedHash)
    }

    @Test
    fun testComputeCanonicalPlanHash_tamperSensitivity_stepToothNumber() {
        val baseHash = DentalRepository.computeCanonicalPlanHash(
            planId = "plan-canonical-1",
            patientId = "p1",
            diagnosis = "Generalized Periodontitis Stage III",
            steps = sampleSteps
        )

        val tamperedSteps = sampleSteps.map { step ->
            if (step.id == "s2") step.copy(toothNumber = 4) else step
        }
        val tamperedHash = DentalRepository.computeCanonicalPlanHash(
            planId = "plan-canonical-1",
            patientId = "p1",
            diagnosis = "Generalized Periodontitis Stage III",
            steps = tamperedSteps
        )

        assertNotEquals("Altering step tooth number must change the canonical tamper-proof hash", baseHash, tamperedHash)
    }

    @Test
    fun testComputeCanonicalPlanHash_tamperSensitivity_stepProcedureCode() {
        val baseHash = DentalRepository.computeCanonicalPlanHash(
            planId = "plan-canonical-1",
            patientId = "p1",
            diagnosis = "Generalized Periodontitis Stage III",
            steps = sampleSteps
        )

        val tamperedSteps = sampleSteps.map { step ->
            if (step.id == "s3") step.copy(code = "D4342") else step
        }
        val tamperedHash = DentalRepository.computeCanonicalPlanHash(
            planId = "plan-canonical-1",
            patientId = "p1",
            diagnosis = "Generalized Periodontitis Stage III",
            steps = tamperedSteps
        )

        assertNotEquals("Altering procedure code must change the canonical tamper-proof hash", baseHash, tamperedHash)
    }

    @Test
    fun testComputeCanonicalPlanHash_tamperSensitivity_stepCompletion() {
        val baseHash = DentalRepository.computeCanonicalPlanHash(
            planId = "plan-canonical-1",
            patientId = "p1",
            diagnosis = "Generalized Periodontitis Stage III",
            steps = sampleSteps
        )

        val tamperedSteps = sampleSteps.map { step ->
            if (step.id == "s3") step.copy(completed = true) else step
        }
        val tamperedHash = DentalRepository.computeCanonicalPlanHash(
            planId = "plan-canonical-1",
            patientId = "p1",
            diagnosis = "Generalized Periodontitis Stage III",
            steps = tamperedSteps
        )

        assertNotEquals("Altering step completion status must change the canonical tamper-proof hash", baseHash, tamperedHash)
    }

    @Test
    fun testComputeCanonicalPlanHash_tamperSensitivity_diagnosis() {
        val baseHash = DentalRepository.computeCanonicalPlanHash(
            planId = "plan-canonical-1",
            patientId = "p1",
            diagnosis = "Generalized Periodontitis Stage III",
            steps = sampleSteps
        )

        val alteredDiagnosisHash = DentalRepository.computeCanonicalPlanHash(
            planId = "plan-canonical-1",
            patientId = "p1",
            diagnosis = "Localized aggressive periodontitis #3, #30",
            steps = sampleSteps
        )

        assertNotEquals("Altering treatment plan diagnosis must change the canonical tamper-proof hash", baseHash, alteredDiagnosisHash)
    }

    // =========================================================================
    // 3. Concurrency & StateFlow Updates
    // =========================================================================

    @Test
    fun testUpdateAppointmentStatus_atomicStateFlowUpdate() {
        val apptId = "a1"
        val originalAppt = DentalRepository.appointments.value.first { it.id == apptId }
        val originalStatus = originalAppt.status

        try {
            DentalRepository.updateAppointmentStatus(apptId, "cancelled")
            val cancelledAppt = DentalRepository.appointments.value.first { it.id == apptId }
            assertEquals("Appointment status should be atomically updated to 'cancelled'", "cancelled", cancelledAppt.status)

            DentalRepository.updateAppointmentStatus(apptId, "completed")
            val completedAppt = DentalRepository.appointments.value.first { it.id == apptId }
            assertEquals("Appointment status should be atomically updated to 'completed'", "completed", completedAppt.status)
        } finally {
            // Restore original state
            DentalRepository.updateAppointmentStatus(apptId, originalStatus)
        }
    }

    @Test
    fun testUpdateAppointmentStatus_concurrentAtomicUpdates() = runTest {
        val apptId = "a2"
        val originalAppt = DentalRepository.appointments.value.first { it.id == apptId }
        val originalStatus = originalAppt.status

        try {
            val statuses = listOf("in_chair", "confirmed", "completed", "cancelled", "confirmed")
            val deferreds = statuses.mapIndexed { index, status ->
                async(Dispatchers.Default) {
                    DentalRepository.updateAppointmentStatus(apptId, "$status-$index")
                }
            }
            deferreds.awaitAll()

            val currentAppt = DentalRepository.appointments.value.first { it.id == apptId }
            assertTrue(
                "Appointment status must be one of the applied concurrent values",
                statuses.indices.any { currentAppt.status == "${statuses[it]}-$it" }
            )
            // Ensure no duplicate IDs or data corruption
            val allAppts = DentalRepository.appointments.value
            assertEquals(1, allAppts.count { it.id == apptId })
        } finally {
            DentalRepository.updateAppointmentStatus(apptId, originalStatus)
        }
    }

    @Test
    fun testTogglePlanStepCompletion_stateFlowUpdate() {
        val planId = "plan-901"
        val stepId = "s3"
        val originalPlan = DentalRepository.treatmentPlans.value.first { it.id == planId }
        val originalStep = originalPlan.steps.first { it.id == stepId }
        val initialCompletion = originalStep.completed

        try {
            DentalRepository.togglePlanStepCompletion(planId, stepId)
            val updatedStep1 = DentalRepository.treatmentPlans.value
                .first { it.id == planId }.steps.first { it.id == stepId }
            assertEquals("Step completion status should be toggled to opposite", !initialCompletion, updatedStep1.completed)

            DentalRepository.togglePlanStepCompletion(planId, stepId)
            val updatedStep2 = DentalRepository.treatmentPlans.value
                .first { it.id == planId }.steps.first { it.id == stepId }
            assertEquals("Step completion status should toggle back to initial", initialCompletion, updatedStep2.completed)
        } finally {
            // Ensure restored to initial
            val currentPlan = DentalRepository.treatmentPlans.value.first { it.id == planId }
            if (currentPlan.steps.first { it.id == stepId }.completed != initialCompletion) {
                DentalRepository.togglePlanStepCompletion(planId, stepId)
            }
        }
    }

    // =========================================================================
    // 4. Additional Seed & Workflow Coverage
    // =========================================================================

    @Test
    fun testToothConditionUpdate() {
        val patient = DentalRepository.patients.value.first { it.id == "p1" }
        DentalRepository.updateToothCondition(patient.id, 8, ToothCondition.CROWN, "Monolithic zirconia crown placed")

        val updatedPatient = DentalRepository.patients.value.first { it.id == "p1" }
        assertEquals(ToothCondition.CROWN, updatedPatient.teeth[8]?.condition)
        assertEquals("Monolithic zirconia crown placed", updatedPatient.teeth[8]?.notes)
    }

    @Test
    fun testIssuePrescription_basicIssuance() {
        val patient = DentalRepository.patients.value.first { it.id == "p3" }
        val initialCount = DentalRepository.prescriptions.value.size

        val rx = DentalRepository.issuePrescription(
            patient = patient,
            clinicianName = "Dr. Ingrid Halvorsen",
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

    @Test
    fun testCreateTreatmentPlan_withToothNumbers_andZeroFees() {
        val steps = listOf(
            PlanStep(id = "step-1", toothNumber = 19, procedure = "Microscope guided root canal therapy", code = "D3330", fee = 0.0, completed = false),
            PlanStep(id = "step-2", toothNumber = 30, procedure = "Composite resin restoration", code = "D2392", fee = 0.0, completed = false),
            PlanStep(id = "step-3", toothNumber = null, procedure = "Full mouth prophylaxis", code = "D1110", fee = 0.0, completed = false)
        )
        val created = DentalRepository.createTreatmentPlan(
            patientId = "p1",
            title = "Endo & Restorative Care",
            clinicianName = "Dr. Ingrid Halvorsen",
            diagnosis = "Deep caries #19, #30",
            steps = steps
        )

        assertNotNull(created.id)
        assertEquals(3, created.steps.size)
        assertEquals(19, created.steps[0].toothNumber)
        assertEquals(30, created.steps[1].toothNumber)
        assertNull(created.steps[2].toothNumber)
        assertEquals(0.0, created.steps.sumOf { it.fee }, 0.001)

        val retrieved = DentalRepository.treatmentPlans.value.first { it.id == created.id }
        assertEquals(19, retrieved.steps[0].toothNumber)
        assertEquals(30, retrieved.steps[1].toothNumber)
        assertNull(retrieved.steps[2].toothNumber)
    }

    // =========================================================================
    // 7. Medication Presets & Custom Prescribing
    // =========================================================================

    @Test
    fun testMedicationPresets_initialDefaults() {
        val presets = DentalRepository.medicationPresets.value
        assertTrue("Medication presets must contain initial dental formulary", presets.isNotEmpty())
        assertTrue("Formulary should contain Amoxicillin", presets.any { it.name.contains("Amoxicillin", ignoreCase = true) })
        assertTrue("Formulary should contain Ibuprofen", presets.any { it.name.contains("Ibuprofen", ignoreCase = true) })
        assertTrue("Formulary should contain Paracetamol", presets.any { it.name.contains("Paracetamol", ignoreCase = true) })
    }

    @Test
    fun testAddMedicationPreset_customPreset() {
        val customPreset = DentalRepository.addMedicationPreset(
            name = "Augmentin",
            dosage = "625 mg tablets",
            frequency = "1 tablet every 12 hours",
            duration = "7 days",
            instructions = "Take with food to minimize GI discomfort.",
            category = "Antibiotics"
        )

        assertNotNull(customPreset.id)
        assertTrue(customPreset.isCustom)
        assertEquals("Augmentin", customPreset.name)
        assertEquals("625 mg tablets", customPreset.dosage)

        val retrieved = DentalRepository.medicationPresets.value.firstOrNull { it.id == customPreset.id }
        assertNotNull("Newly added custom preset must be in StateFlow", retrieved)
        assertEquals("Augmentin", retrieved?.name)
    }

    @Test
    fun testUpdateMedicationPreset() {
        val preset = DentalRepository.addMedicationPreset(
            name = "Azithromycin",
            dosage = "250 mg tablets",
            frequency = "1 tablet once daily",
            duration = "3 days",
            instructions = "Take 1 hour before food.",
            category = "Antibiotics"
        )

        val updated = preset.copy(
            dosage = "500 mg tablets",
            instructions = "Take 500mg on day 1, then 250mg daily."
        )
        DentalRepository.updateMedicationPreset(updated)

        val retrieved = DentalRepository.medicationPresets.value.first { it.id == preset.id }
        assertEquals("500 mg tablets", retrieved.dosage)
        assertEquals("Take 500mg on day 1, then 250mg daily.", retrieved.instructions)
    }

    @Test
    fun testDeleteMedicationPreset() {
        val preset = DentalRepository.addMedicationPreset(
            name = "Temporary Drug",
            dosage = "10 mg",
            frequency = "Once daily",
            duration = "1 day",
            instructions = "Temporary",
            category = "Custom"
        )
        assertTrue(DentalRepository.medicationPresets.value.any { it.id == preset.id })

        DentalRepository.deleteMedicationPreset(preset.id)
        assertFalse(DentalRepository.medicationPresets.value.any { it.id == preset.id })
    }

    @Test
    fun testResetMedicationPresetsToDefaults() {
        DentalRepository.addMedicationPreset(
            name = "Extra Drug",
            dosage = "20 mg",
            frequency = "TDS",
            duration = "5 days",
            instructions = "Test",
            category = "Custom"
        )
        DentalRepository.resetMedicationPresetsToDefaults()

        val presets = DentalRepository.medicationPresets.value
        assertEquals(com.example.thornburydental.data.MedicationPreset.defaultPresets.size, presets.size)
        assertFalse(presets.any { it.name == "Extra Drug" })
    }

    @Test
    fun testIssuePrescription_customMedication_withSaveAsPreset() {
        val patient = DentalRepository.patients.value.first { it.id == "p2" }
        val customDrug = "Doxycycline-${System.currentTimeMillis()}"

        val rx = DentalRepository.issuePrescription(
            patient = patient,
            clinicianName = "Dr. Ingrid Halvorsen",
            drugName = customDrug,
            dosage = "100 mg capsules",
            frequency = "1 capsule twice daily",
            duration = "7 days",
            instructions = "Take with a full glass of water. Avoid lying down for 30 minutes.",
            saveAsPreset = true,
            presetCategory = "Antibiotics"
        )

        assertNotNull(rx.id)
        assertEquals(customDrug, rx.drugName)

        // Verify prescription is in repository prescriptions StateFlow
        val storedRx = DentalRepository.prescriptions.value.firstOrNull { it.id == rx.id }
        assertNotNull(storedRx)
        assertEquals(customDrug, storedRx?.drugName)

        // Verify preset was created in medicationPresets StateFlow
        val storedPreset = DentalRepository.medicationPresets.value.firstOrNull { it.name == customDrug }
        assertNotNull("Custom medication should be automatically saved as a preset", storedPreset)
        assertEquals("100 mg capsules", storedPreset?.dosage)
        assertEquals("Antibiotics", storedPreset?.category)
    }
}
