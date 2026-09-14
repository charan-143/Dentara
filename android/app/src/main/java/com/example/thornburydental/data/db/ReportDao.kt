package com.example.thornburydental.data.db

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.thornburydental.data.DiagnosticReport

/**
 * Data Access Object for Diagnostic Reports in Thornbury Dental SQLite database.
 * Manages radiographs, charting results, clinical test records, attachments, and patient release controls.
 */
class ReportDao(private val dbHelper: ThornburyDbHelper) {

    /**
     * Retrieves all diagnostic reports, deserializing attachments JSON.
     */
    fun getAllReports(): List<DiagnosticReport> {
        val db = dbHelper.readableDatabase
        val reports = mutableListOf<DiagnosticReport>()
        db.query(
            ThornburyDbHelper.TABLE_DIAGNOSTIC_REPORTS,
            null,
            null,
            null,
            null,
            null,
            "${ThornburyDbHelper.COL_REPORTS_TAKEN_AT} DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                reports.add(cursorToReport(cursor))
            }
        }
        return reports
    }

    /**
     * Retrieves all diagnostic reports for a specific patient.
     */
    fun getReportsForPatient(patientId: String): List<DiagnosticReport> {
        val db = dbHelper.readableDatabase
        val reports = mutableListOf<DiagnosticReport>()
        db.query(
            ThornburyDbHelper.TABLE_DIAGNOSTIC_REPORTS,
            null,
            "${ThornburyDbHelper.COL_REPORTS_PATIENT_ID} = ?",
            arrayOf(patientId),
            null,
            null,
            "${ThornburyDbHelper.COL_REPORTS_TAKEN_AT} DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                reports.add(cursorToReport(cursor))
            }
        }
        return reports
    }

    /**
     * Retrieves a single diagnostic report by ID.
     */
    fun getReportById(reportId: String): DiagnosticReport? {
        val db = dbHelper.readableDatabase
        db.query(
            ThornburyDbHelper.TABLE_DIAGNOSTIC_REPORTS,
            null,
            "${ThornburyDbHelper.COL_REPORTS_ID} = ?",
            arrayOf(reportId),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursorToReport(cursor)
            }
        }
        return null
    }

    /**
     * Inserts or replaces a diagnostic report in the database using ContentValues and column constants.
     */
    fun insertReport(report: DiagnosticReport) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(ThornburyDbHelper.COL_REPORTS_ID, report.id)
            put(ThornburyDbHelper.COL_REPORTS_PATIENT_ID, report.patientId)
            put(ThornburyDbHelper.COL_REPORTS_CLINICIAN_NAME, report.clinicianName)
            put(ThornburyDbHelper.COL_REPORTS_KIND, report.kind)
            put(ThornburyDbHelper.COL_REPORTS_TITLE, report.title)
            put(ThornburyDbHelper.COL_REPORTS_SUMMARY, report.summary)
            put(ThornburyDbHelper.COL_REPORTS_TAKEN_AT, report.takenAt)
            if (report.releasedAt != null) {
                put(ThornburyDbHelper.COL_REPORTS_RELEASED_AT, report.releasedAt)
            } else {
                putNull(ThornburyDbHelper.COL_REPORTS_RELEASED_AT)
            }
            if (report.image != null) {
                put(ThornburyDbHelper.COL_REPORTS_IMAGE, report.image)
            } else {
                putNull(ThornburyDbHelper.COL_REPORTS_IMAGE)
            }
            put(ThornburyDbHelper.COL_REPORTS_ATTACHMENTS_JSON, DbConverters.reportAttachmentsToJson(report.attachments))
        }
        db.insertWithOnConflict(
            ThornburyDbHelper.TABLE_DIAGNOSTIC_REPORTS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    /**
     * Updates the released status and release timestamp of a report.
     * When [releasedAt] is provided, the report is released to the patient portal.
     * When [releasedAt] is null, the release is revoked.
     */
    fun toggleReportRelease(reportId: String, releasedAt: String?) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            if (releasedAt != null) {
                put(ThornburyDbHelper.COL_REPORTS_RELEASED_AT, releasedAt)
            } else {
                putNull(ThornburyDbHelper.COL_REPORTS_RELEASED_AT)
            }
        }
        db.update(
            ThornburyDbHelper.TABLE_DIAGNOSTIC_REPORTS,
            values,
            "${ThornburyDbHelper.COL_REPORTS_ID} = ?",
            arrayOf(reportId)
        )
    }

    /**
     * Overload to toggle release state based on current release status.
     * If currently unreleased, releases with the default timestamp "Today, Just now".
     */
    fun toggleReportRelease(reportId: String) {
        val report = getReportById(reportId) ?: return
        val newReleasedAt = if (report.releasedAt == null) "Today, Just now" else null
        toggleReportRelease(reportId, newReleasedAt)
    }

    /**
     * Deletes a diagnostic report by ID.
     */
    fun deleteReport(reportId: String) {
        val db = dbHelper.writableDatabase
        db.delete(
            ThornburyDbHelper.TABLE_DIAGNOSTIC_REPORTS,
            "${ThornburyDbHelper.COL_REPORTS_ID} = ?",
            arrayOf(reportId)
        )
    }

    private fun cursorToReport(cursor: Cursor): DiagnosticReport {
        val releasedAtCol = cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_REPORTS_RELEASED_AT)
        val releasedAt = if (cursor.isNull(releasedAtCol)) null else cursor.getString(releasedAtCol)

        val imageCol = cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_REPORTS_IMAGE)
        val image = if (cursor.isNull(imageCol)) null else cursor.getString(imageCol)

        val attachmentsJson = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_REPORTS_ATTACHMENTS_JSON))

        return DiagnosticReport(
            id = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_REPORTS_ID)),
            patientId = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_REPORTS_PATIENT_ID)),
            clinicianName = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_REPORTS_CLINICIAN_NAME)),
            kind = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_REPORTS_KIND)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_REPORTS_TITLE)),
            summary = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_REPORTS_SUMMARY)),
            takenAt = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_REPORTS_TAKEN_AT)),
            releasedAt = releasedAt,
            image = image,
            attachments = DbConverters.jsonToReportAttachments(attachmentsJson)
        )
    }
}
