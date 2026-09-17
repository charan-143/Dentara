package com.example.thornburydental.data.db

import android.content.ContentValues
import android.database.Cursor
import net.sqlcipher.database.SQLiteDatabase
import com.example.thornburydental.data.Prescription

/**
 * Data Access Object for prescriptions in Thornbury Dental SQLite database.
 * Handles medication dispensing status, clinical issuance, and patient-specific queries.
 */
class PrescriptionDao(private val dbHelper: ThornburyDbHelper) {

    /**
     * Query all prescriptions ordered by issue date and insertion order (newest first).
     */
    fun getAllPrescriptions(): List<Prescription> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<Prescription>()
        db.query(
            ThornburyDbHelper.TABLE_PRESCRIPTIONS,
            null,
            null,
            null,
            null,
            null,
            "${ThornburyDbHelper.COL_RX_ISSUE_DATE} DESC, rowid DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursorToPrescription(cursor))
            }
        }
        return list
    }

    /**
     * Query all prescriptions for a specific patient ordered newest first.
     */
    fun getPrescriptionsForPatient(patientId: String): List<Prescription> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<Prescription>()
        db.query(
            ThornburyDbHelper.TABLE_PRESCRIPTIONS,
            null,
            "${ThornburyDbHelper.COL_RX_PATIENT_ID} = ?",
            arrayOf(patientId),
            null,
            null,
            "${ThornburyDbHelper.COL_RX_ISSUE_DATE} DESC, rowid DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursorToPrescription(cursor))
            }
        }
        return list
    }

    /**
     * Insert or update a prescription.
     */
    fun insertPrescription(rx: Prescription) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(ThornburyDbHelper.COL_RX_ID, rx.id)
            put(ThornburyDbHelper.COL_RX_PATIENT_ID, rx.patientId)
            put(ThornburyDbHelper.COL_RX_PATIENT_NAME, rx.patientName)
            put(ThornburyDbHelper.COL_RX_CLINICIAN_NAME, rx.clinicianName)
            put(ThornburyDbHelper.COL_RX_DRUG_NAME, rx.drugName)
            put(ThornburyDbHelper.COL_RX_DOSAGE, rx.dosage)
            put(ThornburyDbHelper.COL_RX_FREQUENCY, rx.frequency)
            put(ThornburyDbHelper.COL_RX_DURATION, rx.duration)
            put(ThornburyDbHelper.COL_RX_INSTRUCTIONS, rx.instructions)
            put(ThornburyDbHelper.COL_RX_ISSUE_DATE, rx.issueDate)
            put(ThornburyDbHelper.COL_RX_IS_DISPENSED, if (rx.isDispensed) 1 else 0)
        }
        db.insertWithOnConflict(
            ThornburyDbHelper.TABLE_PRESCRIPTIONS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    /**
     * Update the pharmacy dispense status for a prescription by ID.
     */
    fun updateDispenseStatus(rxId: String, isDispensed: Boolean) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(ThornburyDbHelper.COL_RX_IS_DISPENSED, if (isDispensed) 1 else 0)
        }
        db.update(
            ThornburyDbHelper.TABLE_PRESCRIPTIONS,
            values,
            "${ThornburyDbHelper.COL_RX_ID} = ?",
            arrayOf(rxId)
        )
    }

    private fun cursorToPrescription(cursor: Cursor): Prescription {
        val id = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_RX_ID))
        val patientId = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_RX_PATIENT_ID))
        val patientName = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_RX_PATIENT_NAME)) ?: ""
        val clinicianName = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_RX_CLINICIAN_NAME)) ?: ""
        val drugName = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_RX_DRUG_NAME)) ?: ""
        val dosage = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_RX_DOSAGE)) ?: ""
        val frequency = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_RX_FREQUENCY)) ?: ""
        val duration = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_RX_DURATION)) ?: ""
        val instructions = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_RX_INSTRUCTIONS)) ?: ""
        val issueDate = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_RX_ISSUE_DATE)) ?: ""
        val isDispensed = cursor.getInt(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_RX_IS_DISPENSED)) == 1

        return Prescription(
            id = id,
            patientId = patientId,
            patientName = patientName,
            clinicianName = clinicianName,
            drugName = drugName,
            dosage = dosage,
            frequency = frequency,
            duration = duration,
            instructions = instructions,
            issueDate = issueDate,
            isDispensed = isDispensed
        )
    }
}
