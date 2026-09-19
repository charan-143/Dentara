package com.example.thornburydental.data.db

import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import com.example.thornburydental.data.VoiceUndoPoint
import java.util.UUID

/**
 * SQLite DAO providing persistent storage for voice charting undo snapshots.
 *
 * Persisting undo points to encrypted SQLite ensures that if the app is backgrounded,
 * recreated, or the process restarts mid-session, recent voice measurements can still be
 * safely reverted without leaving invalid readings in the patient chart.
 */
class VoiceUndoDao(private val dbHelper: ThornburyDbHelper) {

    companion object {
        private const val TAG = "VoiceUndoDao"
        private const val MAX_PERSISTED_UNDO_PER_PATIENT = 20
    }

    /**
     * Inserts a new voice undo snapshot for a patient.
     */
    fun insertUndoPoint(point: VoiceUndoPoint): String {
        val id = UUID.randomUUID().toString()
        try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put(ThornburyDbHelper.COL_UNDO_ID, id)
                put(ThornburyDbHelper.COL_UNDO_PATIENT_ID, point.patientId)
                put(ThornburyDbHelper.COL_UNDO_DESCRIPTION, point.description)
                put(ThornburyDbHelper.COL_UNDO_CAPTURED_AT, point.capturedAtEpochMs)
                put(ThornburyDbHelper.COL_UNDO_EXAM_ANSWERS_JSON, point.examAnswers.toDbJson())
                put(ThornburyDbHelper.COL_UNDO_TEETH_JSON, point.teeth.toTeethMapDbJson())
            }
            db.insert(ThornburyDbHelper.TABLE_VOICE_UNDO_POINTS, null, values)

            // Prune excess undo points beyond limit for this patient
            pruneExcessUndoPoints(point.patientId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert voice undo point", e)
        }
        return id
    }

    /**
     * Retrieves all persistent undo points for a patient, ordered chronologically (oldest to newest).
     */
    fun getUndoPointsForPatient(patientId: String): List<VoiceUndoPoint> {
        val points = mutableListOf<VoiceUndoPoint>()
        var cursor: Cursor? = null
        try {
            val db = dbHelper.readableDatabase
            cursor = db.query(
                ThornburyDbHelper.TABLE_VOICE_UNDO_POINTS,
                null,
                "${ThornburyDbHelper.COL_UNDO_PATIENT_ID} = ?",
                arrayOf(patientId),
                null,
                null,
                "${ThornburyDbHelper.COL_UNDO_CAPTURED_AT} ASC"
            )
            while (cursor.moveToNext()) {
                points.add(cursorToVoiceUndoPoint(cursor))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query voice undo points for patient: $patientId", e)
        } finally {
            cursor?.close()
        }
        return points
    }

    /**
     * Pops and returns the most recent undo point for a patient, removing it from the database.
     */
    fun popLatestUndoPoint(patientId: String): VoiceUndoPoint? {
        var cursor: Cursor? = null
        try {
            val db = dbHelper.writableDatabase
            cursor = db.query(
                ThornburyDbHelper.TABLE_VOICE_UNDO_POINTS,
                null,
                "${ThornburyDbHelper.COL_UNDO_PATIENT_ID} = ?",
                arrayOf(patientId),
                null,
                null,
                "${ThornburyDbHelper.COL_UNDO_CAPTURED_AT} DESC",
                "1"
            )
            if (cursor.moveToFirst()) {
                val point = cursorToVoiceUndoPoint(cursor)
                val id = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_UNDO_ID))
                db.delete(
                    ThornburyDbHelper.TABLE_VOICE_UNDO_POINTS,
                    "${ThornburyDbHelper.COL_UNDO_ID} = ?",
                    arrayOf(id)
                )
                return point
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pop latest voice undo point for patient: $patientId", e)
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * Clears all undo points for a patient (e.g. when finishing charting).
     */
    fun clearUndoPointsForPatient(patientId: String) {
        try {
            val db = dbHelper.writableDatabase
            db.delete(
                ThornburyDbHelper.TABLE_VOICE_UNDO_POINTS,
                "${ThornburyDbHelper.COL_UNDO_PATIENT_ID} = ?",
                arrayOf(patientId)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear voice undo points for patient: $patientId", e)
        }
    }

    /**
     * Keeps the most recent [MAX_PERSISTED_UNDO_PER_PATIENT] entries.
     */
    private fun pruneExcessUndoPoints(patientId: String) {
        try {
            val db = dbHelper.writableDatabase
            db.execSQL(
                """
                DELETE FROM ${ThornburyDbHelper.TABLE_VOICE_UNDO_POINTS}
                WHERE ${ThornburyDbHelper.COL_UNDO_PATIENT_ID} = '$patientId'
                AND ${ThornburyDbHelper.COL_UNDO_ID} NOT IN (
                    SELECT ${ThornburyDbHelper.COL_UNDO_ID}
                    FROM ${ThornburyDbHelper.TABLE_VOICE_UNDO_POINTS}
                    WHERE ${ThornburyDbHelper.COL_UNDO_PATIENT_ID} = '$patientId'
                    ORDER BY ${ThornburyDbHelper.COL_UNDO_CAPTURED_AT} DESC
                    LIMIT $MAX_PERSISTED_UNDO_PER_PATIENT
                )
                """.trimIndent()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prune excess voice undo points", e)
        }
    }

    private fun cursorToVoiceUndoPoint(cursor: Cursor): VoiceUndoPoint {
        val patientId = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_UNDO_PATIENT_ID))
        val description = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_UNDO_DESCRIPTION))
        val capturedAt = cursor.getLong(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_UNDO_CAPTURED_AT))
        val examAnswersJson = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_UNDO_EXAM_ANSWERS_JSON))
        val teethJson = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_UNDO_TEETH_JSON))

        return VoiceUndoPoint(
            patientId = patientId,
            description = description,
            capturedAtEpochMs = capturedAt,
            examAnswers = examAnswersJson.toExamAnswers(),
            teeth = teethJson.toTeethMap()
        )
    }
}
