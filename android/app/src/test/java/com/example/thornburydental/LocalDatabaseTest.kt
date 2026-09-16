package com.example.thornburydental

import com.example.thornburydental.data.Allergy
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.ExaminationAnswers
import com.example.thornburydental.data.PlanAddendum
import com.example.thornburydental.data.PlanStep
import com.example.thornburydental.data.ReportAttachment
import com.example.thornburydental.data.ToothCondition
import com.example.thornburydental.data.UserProfilePreferences
import com.example.thornburydental.data.db.DbConverters
import com.example.thornburydental.data.db.ThornburyDbHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDatabaseTest {

    // =========================================================================
    // 1. Database Converters Serialization / Deserialization
    // =========================================================================

    @Test
    fun testAllergiesConverter_roundTrip() {
        val allergies = listOf(
            Allergy(allergen = "Penicillin", severity = "Severe / Anaphylaxis", reaction = "Hives, swelling"),
            Allergy(allergen = "Latex", severity = "Moderate", reaction = "Contact dermatitis")
        )
        val json = DbConverters.allergiesToJson(allergies)
        val restored = DbConverters.jsonToAllergies(json)

        assertEquals(allergies.size, restored.size)
        assertEquals("Penicillin", restored[0].allergen)
        assertEquals("Severe / Anaphylaxis", restored[0].severity)
        assertEquals("Latex", restored[1].allergen)
    }

    @Test
    fun testStringListConverter_roundTrip() {
        val alerts = listOf("Bleeding risk: Apixaban", "High caries index", "Latex precaution")
        val json = DbConverters.stringListToJson(alerts)
        val restored = DbConverters.jsonToStringList(json)

        assertEquals(alerts, restored)
    }

    @Test
    fun testExaminationAnswersConverter_roundTrip() {
        val answers = ExaminationAnswers(
            chiefComplaints = listOf("Sensitivity", "Cosmetic concern"),
            chiefComplaintOther = "Upper right tooth sensitive to ice",
            painSeverity = "Moderate",
            sensitivityTriggers = listOf("Cold", "Acidic"),
            periodontalBleeding = listOf("Bleeding on probing"),
            softTissue = listOf("Healthy"),
            functionalHabits = listOf("Bruxism / Clenching"),
            brushingFrequency = "2x/day",
            flossingFrequency = "Daily",
            cariesRisk = "Moderate Risk",
            clinicianNotes = "Nightguard recommended"
        )
        val json = DbConverters.examAnswersToJson(answers)
        val restored = DbConverters.jsonToExamAnswers(json)

        assertNotNull(restored)
        assertEquals(answers.chiefComplaints, restored?.chiefComplaints)
        assertEquals(answers.painSeverity, restored?.painSeverity)
        assertEquals(answers.clinicianNotes, restored?.clinicianNotes)
    }

    @Test
    fun testPlanStepsConverter_roundTrip() {
        val steps = listOf(
            PlanStep(id = "s1", toothNumber = 19, procedure = "Endodontic retreatment", code = "D3348", fee = 1250.0, completed = true),
            PlanStep(id = "s2", toothNumber = 19, procedure = "Full zirconia crown", code = "D2740", fee = 1100.0, completed = false)
        )
        val json = DbConverters.planStepsToJson(steps)
        val restored = DbConverters.jsonToPlanSteps(json)

        assertEquals(steps.size, restored.size)
        assertEquals(steps[0].code, restored[0].code)
        assertEquals(steps[0].fee, restored[0].fee, 0.001)
        assertTrue(restored[0].completed)
        assertFalse(restored[1].completed)
    }

    @Test
    fun testPlanAddendaConverter_roundTrip() {
        val addenda = listOf(
            PlanAddendum(id = "ad1", author = "Dr. Halvorsen", text = "Added post-op review", date = "2026-09-14 10:00")
        )
        val json = DbConverters.planAddendaToJson(addenda)
        val restored = DbConverters.jsonToPlanAddenda(json)

        assertEquals(addenda.size, restored.size)
        assertEquals("Dr. Halvorsen", restored[0].author)
        assertEquals("Added post-op review", restored[0].text)
    }

    @Test
    fun testReportAttachmentsConverter_roundTrip() {
        val attachments = listOf(
            ReportAttachment(id = "att1", name = "panoramic_opg.dcm", sizeStr = "14.2 MB", mimeType = "application/dicom", uri = "content://dcm/1")
        )
        val json = DbConverters.reportAttachmentsToJson(attachments)
        val restored = DbConverters.jsonToReportAttachments(json)

        assertEquals(attachments.size, restored.size)
        assertEquals("panoramic_opg.dcm", restored[0].name)
        assertEquals("14.2 MB", restored[0].sizeStr)
    }

    // =========================================================================
    // 2. Database Schema & Table Definitions Integrity
    // =========================================================================

    @Test
    fun testDatabaseSchemaConstants() {
        assertEquals("thornbury_dental.db", ThornburyDbHelper.DATABASE_NAME)
        assertEquals(5, ThornburyDbHelper.DATABASE_VERSION)
        assertEquals("patients", ThornburyDbHelper.TABLE_PATIENTS)
        assertEquals("teeth", ThornburyDbHelper.TABLE_TEETH)
        assertEquals("appointments", ThornburyDbHelper.TABLE_APPOINTMENTS)
        assertEquals("treatment_plans", ThornburyDbHelper.TABLE_TREATMENT_PLANS)
        assertEquals("prescriptions", ThornburyDbHelper.TABLE_PRESCRIPTIONS)
        assertEquals("diagnostic_reports", ThornburyDbHelper.TABLE_DIAGNOSTIC_REPORTS)
        assertEquals("users", ThornburyDbHelper.TABLE_USERS)
        assertEquals("user_preferences", ThornburyDbHelper.TABLE_USER_PREFERENCES)
    }

    // =========================================================================
    // 3. Clinical Workflow & Repository Operations
    // =========================================================================

    @Test
    fun testPatientRegistrationWithTeethAndDemographics() {
        val initialCount = DentalRepository.patients.value.size

        val newPatient = DentalRepository.registerPatient(
            name = "Alistair Finch",
            opNo = "OP-99001",
            dob = "1990-05-15",
            phone = "+1 (503) 555-0199",
            email = "a.finch@example.org",
            address = "500 Elm Street, Portland, OR",
            allergies = listOf(Allergy("Amoxicillin", "Moderate", "Rash")),
            medicalAlerts = listOf("Asthma"),
            medicalHistory = "Mild exercise-induced asthma."
        )

        assertNotNull(newPatient.id)
        assertEquals("Alistair Finch", newPatient.name)
        assertEquals("OP-99001", newPatient.opNo)
        assertEquals(32, newPatient.teeth.size)
        assertEquals(initialCount + 1, DentalRepository.patients.value.size)

        // Verify tooth condition default
        assertEquals(ToothCondition.SOUND, newPatient.teeth[1]?.condition)

        // Test demographics update
        DentalRepository.updatePatientDemographics(
            patientId = newPatient.id,
            opNo = "OP-99001",
            name = "Alistair B. Finch",
            phone = "+1 (503) 555-0199",
            email = "alistair.finch@example.org",
            address = "502 Elm Street, Portland, OR",
            medicalHistory = "Updated medical history.",
            familyHistory = "None.",
            pastDentalHistory = "Orthodontics."
        )

        val updated = DentalRepository.patients.value.first { it.id == newPatient.id }
        assertEquals("Alistair B. Finch", updated.name)
        assertEquals("alistair.finch@example.org", updated.email)
    }

    @Test
    fun testDiagnosticReportsWorkflow() {
        val patient = DentalRepository.patients.value.first()
        val initialReportsCount = DentalRepository.reports.value.size

        val report = DentalRepository.addReport(
            patientId = patient.id,
            kind = "Radiograph",
            title = "Periapical Radiograph Tooth #30",
            summary = "Deep carious involvement approaching pulp chamber.",
            clinicianName = "Dr. Ingrid Halvorsen",
            attachments = listOf(
                ReportAttachment(id = "att-99", name = "pa_tooth_30.jpg", sizeStr = "2.4 MB", mimeType = "image/jpeg")
            )
        )

        assertNotNull(report.id)
        assertNull(report.releasedAt)
        assertEquals(initialReportsCount + 1, DentalRepository.reports.value.size)

        // Toggle release to patient
        DentalRepository.toggleReportRelease(report.id)
        val releasedReport = DentalRepository.reports.value.first { it.id == report.id }
        assertNotNull(releasedReport.releasedAt)
    }

    // =========================================================================
    // 4. User Profile & Preferences Intake Workflow
    // =========================================================================

    @Test
    fun testUserProfilePreferencesWorkflow() {
        val testPrefs = UserProfilePreferences(
            id = "primary_profile",
            fullName = "Genevieve Vance",
            pronouns = "She/Her",
            dob = "1994-11-12",
            phone = "+1 (503) 555-0199",
            email = "genevieve.vance@example.com",
            dentalGoals = listOf("Routine Hygiene & Examination", "Teeth Whitening & Aesthetics"),
            anxietyLevel = "Mild Apprehension",
            comfortAmenities = listOf("Noise-cancelling headphones", "Weighted blanket"),
            anesthesiaPreference = "Topical Pre-numbing Gel",
            medicalAlerts = listOf("Latex Allergy"),
            lastVisit = "6 - 12 months",
            schedulePreference = "Morning (8am - 12pm)",
            contactChannel = "SMS / WhatsApp",
            additionalNotes = "Cold sensitivity on upper right incisor."
        )

        DentalRepository.saveUserPreferences(testPrefs)

        val stored = DentalRepository.userPreferences.value
        assertNotNull(stored)
        assertEquals("Genevieve Vance", stored!!.fullName)
        assertEquals(2, stored.dentalGoals.size)
        assertEquals("Mild Apprehension", stored.anxietyLevel)
        assertEquals("Latex Allergy", stored.medicalAlerts.first())

        // Verify clinician display name update
        assertEquals("Genevieve Vance", DentalRepository.clinicianDisplayName.value)
    }
}
