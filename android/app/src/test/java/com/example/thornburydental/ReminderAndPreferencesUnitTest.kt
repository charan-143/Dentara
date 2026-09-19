package com.example.thornburydental

import com.example.thornburydental.data.Appointment
import com.example.thornburydental.data.AuthRepository
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.UserProfilePreferences
import com.example.thornburydental.data.db.ThornburyDbHelper
import com.example.thornburydental.reminder.ReminderManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReminderAndPreferencesUnitTest {

    // =========================================================================
    // 1. ReminderManager 12-Hour Formatting Tests
    // =========================================================================

    @Test
    fun testFormatTime12Hour_morningHour() {
        // "08:00" -> "8:00 AM"
        assertEquals("8:00 AM", ReminderManager.formatTime12Hour("08:00"))
    }

    @Test
    fun testFormatTime12Hour_midnight() {
        // "00:00" -> "12:00 AM"
        assertEquals("12:00 AM", ReminderManager.formatTime12Hour("00:00"))
    }

    @Test
    fun testFormatTime12Hour_noon() {
        // "12:00" -> "12:00 PM"
        assertEquals("12:00 PM", ReminderManager.formatTime12Hour("12:00"))
    }

    @Test
    fun testFormatTime12Hour_afternoon() {
        // "14:30" -> "2:30 PM"
        assertEquals("2:30 PM", ReminderManager.formatTime12Hour("14:30"))
    }

    @Test
    fun testFormatTime12Hour_lateNight() {
        // "23:59" -> "11:59 PM"
        assertEquals("11:59 PM", ReminderManager.formatTime12Hour("23:59"))
    }

    @Test
    fun testFormatTime12Hour_invalidFormats_returnsRawStringSafely() {
        assertEquals("invalid_time", ReminderManager.formatTime12Hour("invalid_time"))
        assertEquals("25:00", ReminderManager.formatTime12Hour("25:00"))
        assertEquals("99:99", ReminderManager.formatTime12Hour("99:99"))
        assertEquals("not-a-time", ReminderManager.formatTime12Hour("not-a-time"))
        assertEquals("", ReminderManager.formatTime12Hour(""))
    }

    @Test
    fun testFormatTime12Hour_already12HourFormatted() {
        assertEquals("8:00 AM", ReminderManager.formatTime12Hour("8:00 AM"))
        assertEquals("2:30 PM", ReminderManager.formatTime12Hour("02:30 PM"))
        assertEquals("11:45 PM", ReminderManager.formatTime12Hour("11:45 pm"))
    }

    // =========================================================================
    // 2. Reminder Calculation Logic
    // =========================================================================

    @Test
    fun testComputeAppointmentReminderTime_calculation() {
        val dateIso = "2026-10-15"
        val timeStr = "09:30 AM"
        val leadMinutes = 15

        val computedMillis = ReminderManager.computeAppointmentReminderTime(dateIso, timeStr, leadMinutes)

        val cal = Calendar.getInstance().apply {
            timeInMillis = computedMillis
        }

        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, cal.get(Calendar.MONTH))
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(15, cal.get(Calendar.MINUTE)) // 9:30 AM - 15 min = 9:15 AM
        assertEquals(0, cal.get(Calendar.SECOND))
        assertEquals(0, cal.get(Calendar.MILLISECOND))
    }

    @Test
    fun testComputeAppointmentReminderTime_24HourFormat() {
        val dateIso = "2026-11-20"
        val timeStr = "14:00"
        val leadMinutes = 30

        val computedMillis = ReminderManager.computeAppointmentReminderTime(dateIso, timeStr, leadMinutes)

        val cal = Calendar.getInstance().apply {
            timeInMillis = computedMillis
        }

        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, cal.get(Calendar.MONTH))
        assertEquals(20, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(13, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE)) // 14:00 - 30 min = 13:30
    }

    // =========================================================================
    // 3. Appointment Model Reminder Defaults & Custom Fields
    // =========================================================================

    @Test
    fun testAppointment_defaultReminderFields() {
        val appt = Appointment(
            id = "appt-test-1",
            patientId = "p1",
            patientName = "Test Patient",
            patientOpNo = "OP-1000",
            patientDob = "1990-01-01",
            clinicianId = "c1",
            clinicianName = "Dr. Ingrid Halvorsen",
            date = "2026-09-17",
            time = "10:00 AM",
            durationMin = 45,
            procedure = "Dental Prophylaxis"
        )

        assertFalse("Default reminderEnabled must be false", appt.reminderEnabled)
        assertEquals("Default reminderLeadTimeMin must be 15", 15, appt.reminderLeadTimeMin)
    }

    @Test
    fun testAppointment_customReminderFields() {
        val appt = Appointment(
            id = "appt-test-2",
            patientId = "p1",
            patientName = "Test Patient",
            patientOpNo = "OP-1000",
            patientDob = "1990-01-01",
            clinicianId = "c1",
            clinicianName = "Dr. Ingrid Halvorsen",
            date = "2026-09-17",
            time = "11:00 AM",
            durationMin = 60,
            procedure = "Root Canal Therapy",
            reminderEnabled = true,
            reminderLeadTimeMin = 30
        )

        assertTrue("reminderEnabled should be true", appt.reminderEnabled)
        assertEquals("reminderLeadTimeMin should be 30", 30, appt.reminderLeadTimeMin)
    }

    // =========================================================================
    // 4. UserProfilePreferences Model Reminder Defaults & Custom Fields
    // =========================================================================

    @Test
    fun testUserProfilePreferences_defaultReminderFields() {
        val prefs = UserProfilePreferences()

        assertTrue("Default morningReminderEnabled must be true", prefs.morningReminderEnabled)
        assertEquals("Default morningReminderTime must be '08:00'", "08:00", prefs.morningReminderTime)
        assertEquals("Default chairsideReminderDefaultMin must be 15", 15, prefs.chairsideReminderDefaultMin)
    }

    @Test
    fun testUserProfilePreferences_customReminderFields() {
        val prefs = UserProfilePreferences(
            fullName = "Dr. Test Clinician",
            morningReminderEnabled = false,
            morningReminderTime = "07:30",
            chairsideReminderDefaultMin = 45
        )

        assertFalse("morningReminderEnabled should be false", prefs.morningReminderEnabled)
        assertEquals("morningReminderTime should be '07:30'", "07:30", prefs.morningReminderTime)
        assertEquals("chairsideReminderDefaultMin should be 45", 45, prefs.chairsideReminderDefaultMin)
    }

    // =========================================================================
    // 5. Database Schema Migration Constants Integrity
    // =========================================================================

    @Test
    fun testDatabaseSchema_v5MigrationConstants() {
        assertEquals("Database version must be upgraded to 7", 7, ThornburyDbHelper.DATABASE_VERSION)
        assertEquals("reminder_enabled", ThornburyDbHelper.COL_APPTS_REMINDER_ENABLED)
        assertEquals("reminder_lead_min", ThornburyDbHelper.COL_APPTS_REMINDER_LEAD_MIN)
        assertEquals("morning_reminder_enabled", ThornburyDbHelper.COL_PREF_MORNING_REMINDER_ENABLED)
        assertEquals("morning_reminder_time", ThornburyDbHelper.COL_PREF_MORNING_REMINDER_TIME)
        assertEquals("chairside_reminder_default_min", ThornburyDbHelper.COL_PREF_CHAIRSIDE_REMINDER_DEFAULT_MIN)
    }

    // =========================================================================
    // 6. DentalRepository Clinician Display Name & Reminder Sync
    // =========================================================================

    @Test
    fun testDentalRepository_clinicianDisplayNameUpdate() {
        val originalName = DentalRepository.clinicianDisplayName.value

        try {
            DentalRepository.updateClinicianName("Dr. Marcus Vance")
            assertEquals("Dr. Marcus Vance", DentalRepository.clinicianDisplayName.value)
            assertEquals("Dr. Marcus Vance", AuthRepository.currentUser.value?.name)
            assertEquals("Dr. Marcus Vance", DentalRepository.userPreferences.value?.fullName)

            // Test fallback on empty name
            DentalRepository.updateClinicianName("   ")
            assertEquals("Dr. Ingrid Halvorsen", DentalRepository.clinicianDisplayName.value)
        } finally {
            DentalRepository.updateClinicianName(originalName)
        }
    }

    @Test
    fun testDentalRepository_morningReminderSettingsUpdate() {
        val originalEnabled = DentalRepository.morningReminderEnabled.value
        val originalTime = DentalRepository.morningReminderTime.value

        try {
            DentalRepository.updateMorningReminderSettings(enabled = false, timeString = "07:15")
            assertFalse(DentalRepository.morningReminderEnabled.value)
            assertEquals("07:15", DentalRepository.morningReminderTime.value)
            assertEquals(false, DentalRepository.userPreferences.value?.morningReminderEnabled)
            assertEquals("07:15", DentalRepository.userPreferences.value?.morningReminderTime)
        } finally {
            DentalRepository.updateMorningReminderSettings(originalEnabled, originalTime)
        }
    }

    @Test
    fun testDentalRepository_chairsideReminderDefaultUpdate() {
        val originalLead = DentalRepository.chairsideReminderDefaultMin.value

        try {
            DentalRepository.updateChairsideReminderDefault(30)
            assertEquals(30, DentalRepository.chairsideReminderDefaultMin.value)
            assertEquals(30, DentalRepository.userPreferences.value?.chairsideReminderDefaultMin)
        } finally {
            DentalRepository.updateChairsideReminderDefault(originalLead)
        }
    }

    @Test
    fun testDentalRepository_updateAppointmentReminder() {
        val patient = DentalRepository.patients.value.first()
        val appt = DentalRepository.scheduleAppointment(
            patient = patient,
            clinicianId = "c1",
            clinicianName = "Dr. Ingrid Halvorsen",
            date = "2026-09-25",
            time = "10:30 AM",
            durationMin = 30,
            room = "Surgery 1",
            procedure = "Periodic Oral Evaluation",
            reminderEnabled = false,
            reminderLeadTimeMin = 15
        )

        assertNotNull(appt.id)
        assertFalse(appt.reminderEnabled)

        // Enable reminder with 45 min lead time
        DentalRepository.updateAppointmentReminder(appt.id, enabled = true, leadTimeMin = 45)

        val updated = DentalRepository.appointments.value.firstOrNull { it.id == appt.id }
        assertNotNull("Updated appointment must exist in StateFlow", updated)
        assertTrue(updated!!.reminderEnabled)
        assertEquals(45, updated.reminderLeadTimeMin)
    }

    @Test
    fun testDentalRepository_bookAppointmentWithReminders() {
        val patient = DentalRepository.patients.value.first()
        val booked = DentalRepository.bookAppointment(
            patient = patient,
            clinicianId = "c1",
            clinicianName = "Dr. Ingrid Halvorsen",
            date = "2026-09-26",
            time = "02:00 PM",
            durationMin = 60,
            room = "Surgery 2",
            procedure = "Composite Restoration #14",
            reminderEnabled = true,
            reminderLeadTimeMin = 30
        )

        assertNotNull(booked.id)
        assertTrue(booked.reminderEnabled)
        assertEquals(30, booked.reminderLeadTimeMin)

        val stored = DentalRepository.appointments.value.firstOrNull { it.id == booked.id }
        assertNotNull(stored)
        assertTrue(stored!!.reminderEnabled)
        assertEquals(30, stored.reminderLeadTimeMin)
    }
}
