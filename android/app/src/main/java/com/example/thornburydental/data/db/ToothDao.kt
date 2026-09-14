package com.example.thornburydental.data.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.thornburydental.data.ToothCondition
import com.example.thornburydental.data.ToothRecord

/**
 * Data Access Object for managing tooth records and conditions in SQLite database.
 * Supports Universal 1-32 / FDI tooth numbering, conditions, and clinical charting notes.
 */
class ToothDao(private val dbHelper: ThornburyDbHelper) {

    /**
     * Retrieve all tooth records for a given patient.
     * Returns a map keyed by Universal tooth number (1-32).
     */
    fun getTeethForPatient(patientId: String): Map<Int, ToothRecord> {
        val db = dbHelper.readableDatabase
        val teethMap = mutableMapOf<Int, ToothRecord>()
        val columns = arrayOf(
            ThornburyDbHelper.COL_TEETH_NUMBER,
            ThornburyDbHelper.COL_TEETH_FDI_NUMBER,
            ThornburyDbHelper.COL_TEETH_NAME,
            ThornburyDbHelper.COL_TEETH_ARCH,
            ThornburyDbHelper.COL_TEETH_CONDITION,
            ThornburyDbHelper.COL_TEETH_NOTES
        )
        val selection = "${ThornburyDbHelper.COL_TEETH_PATIENT_ID} = ?"
        val selectionArgs = arrayOf(patientId)

        db.query(
            ThornburyDbHelper.TABLE_TEETH,
            columns,
            selection,
            selectionArgs,
            null,
            null,
            "${ThornburyDbHelper.COL_TEETH_NUMBER} ASC"
        ).use { cursor ->
            val numIdx = cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_TEETH_NUMBER)
            val fdiIdx = cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_TEETH_FDI_NUMBER)
            val nameIdx = cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_TEETH_NAME)
            val archIdx = cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_TEETH_ARCH)
            val condIdx = cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_TEETH_CONDITION)
            val notesIdx = cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_TEETH_NOTES)

            while (cursor.moveToNext()) {
                val number = cursor.getInt(numIdx)
                val fdiNumber = cursor.getInt(fdiIdx)
                val name = cursor.getString(nameIdx) ?: ""
                val arch = cursor.getString(archIdx) ?: ""
                val conditionStr = cursor.getString(condIdx) ?: ""
                val condition = try {
                    ToothCondition.valueOf(conditionStr)
                } catch (_: Exception) {
                    ToothCondition.SOUND
                }
                val notes = cursor.getString(notesIdx) ?: ""

                teethMap[number] = ToothRecord(
                    number = number,
                    fdiNumber = fdiNumber,
                    name = name,
                    arch = arch,
                    condition = condition,
                    notes = notes
                )
            }
        }
        return teethMap
    }

    /**
     * Bulk insert or replace tooth records for a patient within a single SQLite transaction.
     */
    fun insertTeeth(patientId: String, teeth: Map<Int, ToothRecord>) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            for ((_, record) in teeth) {
                val values = ContentValues().apply {
                    put(ThornburyDbHelper.COL_TEETH_ID, "${patientId}_${record.number}")
                    put(ThornburyDbHelper.COL_TEETH_PATIENT_ID, patientId)
                    put(ThornburyDbHelper.COL_TEETH_NUMBER, record.number)
                    put(ThornburyDbHelper.COL_TEETH_FDI_NUMBER, record.fdiNumber)
                    put(ThornburyDbHelper.COL_TEETH_NAME, record.name)
                    put(ThornburyDbHelper.COL_TEETH_ARCH, record.arch)
                    put(ThornburyDbHelper.COL_TEETH_CONDITION, record.condition.name)
                    put(ThornburyDbHelper.COL_TEETH_NOTES, record.notes)
                }
                db.insertWithOnConflict(
                    ThornburyDbHelper.TABLE_TEETH,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Update tooth condition and clinical notes for a specific tooth of a patient.
     */
    fun updateToothCondition(
        patientId: String,
        toothNumber: Int,
        condition: ToothCondition,
        notes: String
    ) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(ThornburyDbHelper.COL_TEETH_CONDITION, condition.name)
            put(ThornburyDbHelper.COL_TEETH_NOTES, notes)
        }
        db.update(
            ThornburyDbHelper.TABLE_TEETH,
            values,
            "${ThornburyDbHelper.COL_TEETH_PATIENT_ID} = ? AND ${ThornburyDbHelper.COL_TEETH_NUMBER} = ?",
            arrayOf(patientId, toothNumber.toString())
        )
    }
}
