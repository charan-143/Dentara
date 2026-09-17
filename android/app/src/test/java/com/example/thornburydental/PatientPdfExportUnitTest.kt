package com.example.thornburydental

import com.example.thornburydental.data.*
import com.example.thornburydental.export.PatientShareOptions
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Patient PDF Export options and clinical data preparation.
 */
class PatientPdfExportUnitTest {

    @Test
    fun testDefaultShareOptionsIncludeAllSections() {
        val options = PatientShareOptions()
        assertTrue(options.includeDemographics)
        assertTrue(options.includeMedicalHistory)
        assertTrue(options.includeExamination)
        assertTrue(options.includeDiagnosis)
        assertTrue(options.includeTreatmentPlans)
        assertTrue(options.includePrescriptions)
        assertTrue(options.includeReports)
        assertTrue(options.includeAppointments)
        assertTrue(options.hasAnySelected)
        assertEquals(8, options.selectedCount)
    }

    @Test
    fun testSelectiveOptionsToggle() {
        val options = PatientShareOptions(
            includeDemographics = true,
            includeMedicalHistory = false,
            includeExamination = false,
            includeDiagnosis = true,
            includeTreatmentPlans = true,
            includePrescriptions = false,
            includeReports = false,
            includeAppointments = true
        )
        assertTrue(options.includeDemographics)
        assertFalse(options.includeMedicalHistory)
        assertFalse(options.includeExamination)
        assertTrue(options.includeDiagnosis)
        assertTrue(options.includeTreatmentPlans)
        assertFalse(options.includePrescriptions)
        assertFalse(options.includeReports)
        assertTrue(options.includeAppointments)
        assertTrue(options.hasAnySelected)
        assertEquals(4, options.selectedCount)
    }

    @Test
    fun testDeselectAllOptions() {
        val options = PatientShareOptions(
            includeDemographics = false,
            includeMedicalHistory = false,
            includeExamination = false,
            includeDiagnosis = false,
            includeTreatmentPlans = false,
            includePrescriptions = false,
            includeReports = false,
            includeAppointments = false
        )
        assertFalse(options.hasAnySelected)
        assertEquals(0, options.selectedCount)
    }

    @Test
    fun testFileNameSanitization() {
        val rawOp = "OP-40192 / @Portland"
        val rawName = "Arthur Pendelton, Jr."
        val cleanOp = rawOp.replace("[^a-zA-Z0-9_-]".toRegex(), "")
        val cleanName = rawName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")

        val expectedFileName = "Dentara_EHR_${cleanOp}_$cleanName.pdf"
        assertEquals("Dentara_EHR_OP-40192Portland_Arthur_Pendelton__Jr_.pdf", expectedFileName)
        assertFalse(expectedFileName.contains("/"))
        assertFalse(expectedFileName.contains("@"))
        assertFalse(expectedFileName.contains(" "))
    }

    @Test
    fun testPatientClinicalDataFiltering() {
        val patient = Patient(
            id = "test_p1",
            opNo = "OP-10001",
            name = "Test Patient",
            dob = "1990-01-01"
        )

        val plans = listOf(
            TreatmentPlan(
                id = "tp1",
                patientId = "test_p1",
                title = "Implant Phase 1",
                clinicianName = "Dr. Ingrid Halvorsen",
                dateCreated = "2026-09-16",
                tamperHash = "hash1",
                steps = emptyList()
            ),
            TreatmentPlan(
                id = "tp2",
                patientId = "other_p2",
                title = "Other Plan",
                clinicianName = "Dr. Ingrid Halvorsen",
                dateCreated = "2026-09-16",
                tamperHash = "hash2",
                steps = emptyList()
            )
        )

        val prescriptions = listOf(
            Prescription(
                id = "rx1",
                patientId = "test_p1",
                patientName = "Test Patient",
                clinicianName = "Dr. Ingrid Halvorsen",
                drugName = "Amoxicillin 500mg",
                dosage = "500mg",
                frequency = "TID",
                duration = "7 days",
                instructions = "Take after meals",
                issueDate = "2026-09-16"
            ),
            Prescription(
                id = "rx2",
                patientId = "other_p2",
                patientName = "Other Patient",
                clinicianName = "Dr. Ingrid Halvorsen",
                drugName = "Ibuprofen 400mg",
                dosage = "400mg",
                frequency = "PRN",
                duration = "3 days",
                instructions = "For mild discomfort",
                issueDate = "2026-09-16"
            )
        )

        val reports = listOf(
            DiagnosticReport(
                id = "rep1",
                patientId = "test_p1",
                clinicianName = "Dr. Ingrid Halvorsen",
                kind = "Radiograph",
                title = "Periapical Radiograph",
                summary = "Normal bone density",
                takenAt = "2026-09-16"
            ),
            DiagnosticReport(
                id = "rep2",
                patientId = "other_p2",
                clinicianName = "Dr. Ingrid Halvorsen",
                kind = "CBCT Scan",
                title = "CBCT Scan",
                summary = "Full volume scan",
                takenAt = "2026-09-16"
            )
        )

        val appointments = listOf(
            Appointment(
                id = "app1",
                patientId = "test_p1",
                patientName = "Test Patient",
                patientOpNo = "OP-10001",
                patientDob = "1990-01-01",
                clinicianId = "c1",
                clinicianName = "Dr. Ingrid Halvorsen",
                date = "2026-09-20",
                time = "09:00",
                procedure = "Review"
            ),
            Appointment(
                id = "app2",
                patientId = "other_p2",
                patientName = "Other Patient",
                patientOpNo = "OP-10002",
                patientDob = "1985-05-05",
                clinicianId = "c1",
                clinicianName = "Dr. Ingrid Halvorsen",
                date = "2026-09-21",
                time = "10:00",
                procedure = "Cleaning"
            )
        )

        val filteredPlans = plans.filter { it.patientId == patient.id }
        val filteredRx = prescriptions.filter { it.patientId == patient.id }
        val filteredReports = reports.filter { it.patientId == patient.id }
        val filteredAppts = appointments.filter { it.patientId == patient.id }

        assertEquals(1, filteredPlans.size)
        assertEquals("tp1", filteredPlans[0].id)

        assertEquals(1, filteredRx.size)
        assertEquals("rx1", filteredRx[0].id)

        assertEquals(1, filteredReports.size)
        assertEquals("rep1", filteredReports[0].id)

        assertEquals(1, filteredAppts.size)
        assertEquals("app1", filteredAppts[0].id)
    }
}
