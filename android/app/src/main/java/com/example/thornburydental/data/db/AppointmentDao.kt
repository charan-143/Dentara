package com.example.thornburydental.data.db

import android.content.ContentValues
import android.database.Cursor
import net.sqlcipher.database.SQLiteDatabase
import com.example.thornburydental.data.Appointment
import com.example.thornburydental.util.parseTimeToMinutes

/**
 * Data Access Object for Appointments in Thornbury Dental SQLite database.
 * Manages clinical scheduling, patient appointments, status progression, and queue ordering.
 */
class AppointmentDao(private val dbHelper: ThornburyDbHelper) {

    /**
     * Retrieves all appointments ordered chronologically by scheduled time.
     */
    fun getAllAppointments(): List<Appointment> {
        val db = dbHelper.readableDatabase
        val appointments = mutableListOf<Appointment>()
        db.query(
            ThornburyDbHelper.TABLE_APPOINTMENTS,
            null,
            null,
            null,
            null,
            null,
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                appointments.add(cursorToAppointment(cursor))
            }
        }
        // Sort chronologically by (date, parsed time-of-day) rather than a
        // lexicographic SQL ORDER BY over the free-form "hh:mm AM/PM" TEXT
        // column, which sorts incorrectly on its own (e.g. "02:00 PM" would
        // sort before "11:30 AM" as text) and says nothing about which day an
        // appointment is on. `date` is ISO "yyyy-MM-dd", so plain string
        // comparison is already chronologically correct for it.
        return appointments.sortedWith(compareBy({ it.date }, { parseTimeToMinutes(it.time) }))
    }

    /**
     * Retrieves all appointments for a specific patient, ordered by time.
     */
    fun getAppointmentsForPatient(patientId: String): List<Appointment> {
        val db = dbHelper.readableDatabase
        val appointments = mutableListOf<Appointment>()
        db.query(
            ThornburyDbHelper.TABLE_APPOINTMENTS,
            null,
            "${ThornburyDbHelper.COL_APPTS_PATIENT_ID} = ?",
            arrayOf(patientId),
            null,
            null,
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                appointments.add(cursorToAppointment(cursor))
            }
        }
        // See getAllAppointments() — chronological (date, then time) sort.
        return appointments.sortedWith(compareBy({ it.date }, { parseTimeToMinutes(it.time) }))
    }

    /**
     * Retrieves a single appointment by its unique identifier.
     */
    fun getAppointmentById(appointmentId: String): Appointment? {
        val db = dbHelper.readableDatabase
        db.query(
            ThornburyDbHelper.TABLE_APPOINTMENTS,
            null,
            "${ThornburyDbHelper.COL_APPTS_ID} = ?",
            arrayOf(appointmentId),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursorToAppointment(cursor)
            }
        }
        return null
    }

    /**
     * Inserts an appointment into the database using ContentValues and column constants.
     * Uses CONFLICT_REPLACE to handle updates cleanly.
     */
    fun insertAppointment(appointment: Appointment) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(ThornburyDbHelper.COL_APPTS_ID, appointment.id)
            put(ThornburyDbHelper.COL_APPTS_PATIENT_ID, appointment.patientId)
            put(ThornburyDbHelper.COL_APPTS_PATIENT_NAME, appointment.patientName)
            put(ThornburyDbHelper.COL_APPTS_PATIENT_OP_NO, appointment.patientOpNo)
            put(ThornburyDbHelper.COL_APPTS_PATIENT_DOB, appointment.patientDob)
            put(ThornburyDbHelper.COL_APPTS_CLINICIAN_ID, appointment.clinicianId)
            put(ThornburyDbHelper.COL_APPTS_CLINICIAN_NAME, appointment.clinicianName)
            put(ThornburyDbHelper.COL_APPTS_DATE, appointment.date)
            put(ThornburyDbHelper.COL_APPTS_TIME, appointment.time)
            put(ThornburyDbHelper.COL_APPTS_DURATION_MIN, appointment.durationMin)
            put(ThornburyDbHelper.COL_APPTS_ROOM, appointment.room)
            put(ThornburyDbHelper.COL_APPTS_PROCEDURE, appointment.procedure)
            if (appointment.allergyList != null) {
                put(ThornburyDbHelper.COL_APPTS_ALLERGY_LIST, appointment.allergyList)
            } else {
                putNull(ThornburyDbHelper.COL_APPTS_ALLERGY_LIST)
            }
            put(ThornburyDbHelper.COL_APPTS_STATUS, appointment.status)
            put(ThornburyDbHelper.COL_APPTS_REMINDER_ENABLED, if (appointment.reminderEnabled) 1 else 0)
            put(ThornburyDbHelper.COL_APPTS_REMINDER_LEAD_MIN, appointment.reminderLeadTimeMin)
            put(ThornburyDbHelper.COL_APPTS_CREATED_AT, System.currentTimeMillis())
        }
        db.insertWithOnConflict(
            ThornburyDbHelper.TABLE_APPOINTMENTS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    /**
     * Updates the status of an existing appointment (e.g. confirmed, completed, cancelled).
     */
    fun updateAppointmentStatus(appointmentId: String, newStatus: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(ThornburyDbHelper.COL_APPTS_STATUS, newStatus)
        }
        db.update(
            ThornburyDbHelper.TABLE_APPOINTMENTS,
            values,
            "${ThornburyDbHelper.COL_APPTS_ID} = ?",
            arrayOf(appointmentId)
        )
    }

    /**
     * Updates reminder settings for an appointment.
     */
    fun updateAppointmentReminder(appointmentId: String, enabled: Boolean, leadMin: Int) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(ThornburyDbHelper.COL_APPTS_REMINDER_ENABLED, if (enabled) 1 else 0)
            put(ThornburyDbHelper.COL_APPTS_REMINDER_LEAD_MIN, leadMin)
        }
        db.update(
            ThornburyDbHelper.TABLE_APPOINTMENTS,
            values,
            "${ThornburyDbHelper.COL_APPTS_ID} = ?",
            arrayOf(appointmentId)
        )
    }

    /**
     * Retrieves all appointments for a specific calendar day (ISO "yyyy-MM-dd").
     */
    fun getAppointmentsForDate(date: String): List<Appointment> {
        val db = dbHelper.readableDatabase
        val appointments = mutableListOf<Appointment>()
        db.query(
            ThornburyDbHelper.TABLE_APPOINTMENTS,
            null,
            "${ThornburyDbHelper.COL_APPTS_DATE} = ?",
            arrayOf(date),
            null,
            null,
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                appointments.add(cursorToAppointment(cursor))
            }
        }
        return appointments.sortedWith(compareBy({ it.date }, { parseTimeToMinutes(it.time) }))
    }

    /**
     * Retrieves all appointments that currently have chairside arrival reminder enabled.
     */
    fun getAppointmentsWithActiveReminders(): List<Appointment> {
        val db = dbHelper.readableDatabase
        val appointments = mutableListOf<Appointment>()
        db.query(
            ThornburyDbHelper.TABLE_APPOINTMENTS,
            null,
            "${ThornburyDbHelper.COL_APPTS_REMINDER_ENABLED} = 1",
            null,
            null,
            null,
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                appointments.add(cursorToAppointment(cursor))
            }
        }
        return appointments.sortedWith(compareBy({ it.date }, { parseTimeToMinutes(it.time) }))
    }

    /**
     * Deletes an appointment by its ID.
     */
    fun deleteAppointment(appointmentId: String) {
        val db = dbHelper.writableDatabase
        db.delete(
            ThornburyDbHelper.TABLE_APPOINTMENTS,
            "${ThornburyDbHelper.COL_APPTS_ID} = ?",
            arrayOf(appointmentId)
        )
    }

    private fun cursorToAppointment(cursor: Cursor): Appointment {
        val allergyColIndex = cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_APPTS_ALLERGY_LIST)
        val allergyList = if (cursor.isNull(allergyColIndex)) null else cursor.getString(allergyColIndex)

        val reminderEnabledIndex = cursor.getColumnIndex(ThornburyDbHelper.COL_APPTS_REMINDER_ENABLED)
        val reminderEnabled = if (reminderEnabledIndex != -1) cursor.getInt(reminderEnabledIndex) == 1 else false

        val reminderLeadIndex = cursor.getColumnIndex(ThornburyDbHelper.COL_APPTS_REMINDER_LEAD_MIN)
        val reminderLeadMin = if (reminderLeadIndex != -1) cursor.getInt(reminderLeadIndex) else 15

        return Appointment(
            id = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_APPTS_ID)),
            patientId = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_APPTS_PATIENT_ID)),
            patientName = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_APPTS_PATIENT_NAME)),
            patientOpNo = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_APPTS_PATIENT_OP_NO)),
            patientDob = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_APPTS_PATIENT_DOB)),
            clinicianId = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_APPTS_CLINICIAN_ID)),
            clinicianName = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_APPTS_CLINICIAN_NAME)),
            date = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_APPTS_DATE)) ?: "",
            time = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_APPTS_TIME)),
            durationMin = cursor.getInt(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_APPTS_DURATION_MIN)),
            room = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_APPTS_ROOM)),
            procedure = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_APPTS_PROCEDURE)),
            allergyList = allergyList,
            status = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_APPTS_STATUS)),
            reminderEnabled = reminderEnabled,
            reminderLeadTimeMin = reminderLeadMin
        )
    }
}
