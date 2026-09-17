package com.example.thornburydental.data.db

import android.content.ContentValues
import android.database.Cursor
import net.sqlcipher.database.SQLiteDatabase
import com.example.thornburydental.data.Allergy
import com.example.thornburydental.data.ExaminationAnswers
import com.example.thornburydental.data.Patient
import com.example.thornburydental.data.PatientDiagnosis
import com.example.thornburydental.data.ToothRecord

/**
 * Data Access Object for patient demographic and clinical records in SQLite database.
 * Manages demographics, medical alerts, allergies, examination answers, and odontogram tooth association.
 */
class PatientDao(
    private val dbHelper: ThornburyDbHelper,
    private val toothDao: ToothDao = ToothDao(dbHelper)
) {

    /**
     * Query all patients, deserializing JSON fields and loading odontogram teeth.
     */
    fun getAllPatients(): List<Patient> {
        val db = dbHelper.readableDatabase
        val rawPatients = mutableListOf<Patient>()

        db.query(
            ThornburyDbHelper.TABLE_PATIENTS,
            null,
            null,
            null,
            null,
            null,
            "${ThornburyDbHelper.COL_PATIENTS_NAME} ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                rawPatients.add(cursorToPatient(cursor))
            }
        }

        return rawPatients.map { patient ->
            val teeth = toothDao.getTeethForPatient(patient.id)
            if (teeth.isNotEmpty()) patient.copy(teeth = teeth) else patient
        }
    }

    /**
     * Retrieve a patient by unique ID, including odontogram teeth records.
     */
    fun getPatientById(id: String): Patient? {
        val db = dbHelper.readableDatabase
        var patient: Patient? = null

        db.query(
            ThornburyDbHelper.TABLE_PATIENTS,
            null,
            "${ThornburyDbHelper.COL_PATIENTS_ID} = ?",
            arrayOf(id),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                patient = cursorToPatient(cursor)
            }
        }

        return patient?.let {
            val teeth = toothDao.getTeethForPatient(it.id)
            if (teeth.isNotEmpty()) it.copy(teeth = teeth) else it
        }
    }

    /**
     * Insert or update a patient and their odontogram teeth (if present) in a transaction.
     */
    fun insertPatient(patient: Patient) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(ThornburyDbHelper.COL_PATIENTS_ID, patient.id)
                put(ThornburyDbHelper.COL_PATIENTS_OP_NO, patient.opNo)
                put(ThornburyDbHelper.COL_PATIENTS_NAME, patient.name)
                put(ThornburyDbHelper.COL_PATIENTS_DOB, patient.dob)
                put(ThornburyDbHelper.COL_PATIENTS_PHONE, patient.phone)
                put(ThornburyDbHelper.COL_PATIENTS_EMAIL, patient.email)
                put(ThornburyDbHelper.COL_PATIENTS_ADDRESS, patient.address)
                put(ThornburyDbHelper.COL_PATIENTS_IS_CHILD, if (patient.isChild) 1 else 0)
                put(ThornburyDbHelper.COL_PATIENTS_MEDICAL_HISTORY, patient.medicalHistory)
                put(ThornburyDbHelper.COL_PATIENTS_FAMILY_HISTORY, patient.familyHistory)
                put(ThornburyDbHelper.COL_PATIENTS_PAST_DENTAL_HISTORY, patient.pastDentalHistory)
                put(ThornburyDbHelper.COL_PATIENTS_LAST_VISIT, patient.lastVisit)
                put(ThornburyDbHelper.COL_PATIENTS_MEDICAL_ALERTS_JSON, patient.medicalAlerts.toStringListDbJson())
                put(ThornburyDbHelper.COL_PATIENTS_ALLERGIES_JSON, patient.allergies.toDbJson())
                put(ThornburyDbHelper.COL_PATIENTS_EXAM_ANSWERS_JSON, patient.examAnswers.toDbJson())
                put(ThornburyDbHelper.COL_PATIENTS_DIAGNOSIS_JSON, patient.diagnosis.toPatientDiagnosisDbJson())
                put(ThornburyDbHelper.COL_PATIENTS_CREATED_AT, System.currentTimeMillis())
            }
            db.insertWithOnConflict(
                ThornburyDbHelper.TABLE_PATIENTS,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )

            if (patient.teeth.isNotEmpty()) {
                toothDao.insertTeeth(patient.id, patient.teeth)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Update or record one-time persistent clinical diagnosis for a patient.
     */
    fun updateDiagnosis(patientId: String, diagnosis: PatientDiagnosis) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(ThornburyDbHelper.COL_PATIENTS_DIAGNOSIS_JSON, diagnosis.toPatientDiagnosisDbJson())
        }
        db.update(
            ThornburyDbHelper.TABLE_PATIENTS,
            values,
            "${ThornburyDbHelper.COL_PATIENTS_ID} = ?",
            arrayOf(patientId)
        )
    }

    /**
     * Update patient demographics and medical history fields.
     */
    fun updatePatientDemographics(
        patientId: String,
        opNo: String?,
        name: String,
        phone: String,
        email: String,
        address: String,
        medicalHistory: String,
        familyHistory: String,
        pastDentalHistory: String,
        dob: String?,
        isChild: Boolean = false
    ) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            opNo?.let { put(ThornburyDbHelper.COL_PATIENTS_OP_NO, it) }
            put(ThornburyDbHelper.COL_PATIENTS_NAME, name)
            dob?.let { put(ThornburyDbHelper.COL_PATIENTS_DOB, it) }
            put(ThornburyDbHelper.COL_PATIENTS_PHONE, phone)
            put(ThornburyDbHelper.COL_PATIENTS_EMAIL, email)
            put(ThornburyDbHelper.COL_PATIENTS_ADDRESS, address)
            put(ThornburyDbHelper.COL_PATIENTS_IS_CHILD, if (isChild) 1 else 0)
            put(ThornburyDbHelper.COL_PATIENTS_MEDICAL_HISTORY, medicalHistory)
            put(ThornburyDbHelper.COL_PATIENTS_FAMILY_HISTORY, familyHistory)
            put(ThornburyDbHelper.COL_PATIENTS_PAST_DENTAL_HISTORY, pastDentalHistory)
        }
        db.update(
            ThornburyDbHelper.TABLE_PATIENTS,
            values,
            "${ThornburyDbHelper.COL_PATIENTS_ID} = ?",
            arrayOf(patientId)
        )
    }

    /**
     * Update clinical examination answers for a patient.
     */
    fun updateExaminationAnswers(patientId: String, answers: ExaminationAnswers) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(ThornburyDbHelper.COL_PATIENTS_EXAM_ANSWERS_JSON, answers.toDbJson())
        }
        db.update(
            ThornburyDbHelper.TABLE_PATIENTS,
            values,
            "${ThornburyDbHelper.COL_PATIENTS_ID} = ?",
            arrayOf(patientId)
        )
    }

    /**
     * Add or update an allergy for a patient.
     */
    fun addAllergy(patientId: String, allergy: Allergy) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            var currentAllergies: List<Allergy>? = null
            db.query(
                ThornburyDbHelper.TABLE_PATIENTS,
                arrayOf(ThornburyDbHelper.COL_PATIENTS_ALLERGIES_JSON),
                "${ThornburyDbHelper.COL_PATIENTS_ID} = ?",
                arrayOf(patientId),
                null,
                null,
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    currentAllergies = cursor.getString(0).toAllergies()
                }
            }

            currentAllergies?.let { list ->
                val updated = list.filterNot { it.allergen.equals(allergy.allergen, ignoreCase = true) } + allergy
                val values = ContentValues().apply {
                    put(ThornburyDbHelper.COL_PATIENTS_ALLERGIES_JSON, updated.toDbJson())
                }
                db.update(
                    ThornburyDbHelper.TABLE_PATIENTS,
                    values,
                    "${ThornburyDbHelper.COL_PATIENTS_ID} = ?",
                    arrayOf(patientId)
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Remove an allergy from a patient's allergy list by allergen name.
     */
    fun removeAllergy(patientId: String, allergen: String) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            var currentAllergies: List<Allergy>? = null
            db.query(
                ThornburyDbHelper.TABLE_PATIENTS,
                arrayOf(ThornburyDbHelper.COL_PATIENTS_ALLERGIES_JSON),
                "${ThornburyDbHelper.COL_PATIENTS_ID} = ?",
                arrayOf(patientId),
                null,
                null,
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    currentAllergies = cursor.getString(0).toAllergies()
                }
            }

            currentAllergies?.let { list ->
                val updated = list.filterNot { it.allergen.equals(allergen, ignoreCase = true) }
                val values = ContentValues().apply {
                    put(ThornburyDbHelper.COL_PATIENTS_ALLERGIES_JSON, updated.toDbJson())
                }
                db.update(
                    ThornburyDbHelper.TABLE_PATIENTS,
                    values,
                    "${ThornburyDbHelper.COL_PATIENTS_ID} = ?",
                    arrayOf(patientId)
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Add a medical alert to a patient's record if not already present.
     */
    fun addMedicalAlert(patientId: String, alert: String) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            var currentAlerts: List<String>? = null
            db.query(
                ThornburyDbHelper.TABLE_PATIENTS,
                arrayOf(ThornburyDbHelper.COL_PATIENTS_MEDICAL_ALERTS_JSON),
                "${ThornburyDbHelper.COL_PATIENTS_ID} = ?",
                arrayOf(patientId),
                null,
                null,
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    currentAlerts = cursor.getString(0).toStringList()
                }
            }

            currentAlerts?.let { list ->
                if (!list.any { it.equals(alert, ignoreCase = true) }) {
                    val updated = list + alert
                    val values = ContentValues().apply {
                        put(ThornburyDbHelper.COL_PATIENTS_MEDICAL_ALERTS_JSON, updated.toStringListDbJson())
                    }
                    db.update(
                        ThornburyDbHelper.TABLE_PATIENTS,
                        values,
                        "${ThornburyDbHelper.COL_PATIENTS_ID} = ?",
                        arrayOf(patientId)
                    )
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Remove a medical alert from a patient's record.
     */
    fun removeMedicalAlert(patientId: String, alert: String) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            var currentAlerts: List<String>? = null
            db.query(
                ThornburyDbHelper.TABLE_PATIENTS,
                arrayOf(ThornburyDbHelper.COL_PATIENTS_MEDICAL_ALERTS_JSON),
                "${ThornburyDbHelper.COL_PATIENTS_ID} = ?",
                arrayOf(patientId),
                null,
                null,
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    currentAlerts = cursor.getString(0).toStringList()
                }
            }

            currentAlerts?.let { list ->
                val updated = list.filterNot { it.equals(alert, ignoreCase = true) }
                val values = ContentValues().apply {
                    put(ThornburyDbHelper.COL_PATIENTS_MEDICAL_ALERTS_JSON, updated.toStringListDbJson())
                }
                db.update(
                    ThornburyDbHelper.TABLE_PATIENTS,
                    values,
                    "${ThornburyDbHelper.COL_PATIENTS_ID} = ?",
                    arrayOf(patientId)
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Delete a patient and their teeth records.
     */
    fun deletePatient(patientId: String) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(
                ThornburyDbHelper.TABLE_TEETH,
                "${ThornburyDbHelper.COL_TEETH_PATIENT_ID} = ?",
                arrayOf(patientId)
            )
            db.delete(
                ThornburyDbHelper.TABLE_PATIENTS,
                "${ThornburyDbHelper.COL_PATIENTS_ID} = ?",
                arrayOf(patientId)
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun cursorToPatient(cursor: Cursor, teeth: Map<Int, ToothRecord> = emptyMap()): Patient {
        val id = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PATIENTS_ID))
        val opNo = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PATIENTS_OP_NO)) ?: ""
        val name = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PATIENTS_NAME)) ?: ""
        val dob = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PATIENTS_DOB)) ?: ""
        val phone = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PATIENTS_PHONE)) ?: ""
        val email = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PATIENTS_EMAIL)) ?: ""
        val address = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PATIENTS_ADDRESS)) ?: ""
        val medicalHistory = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PATIENTS_MEDICAL_HISTORY)) ?: ""
        val familyHistory = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PATIENTS_FAMILY_HISTORY)) ?: ""
        val pastDentalHistory = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PATIENTS_PAST_DENTAL_HISTORY)) ?: ""
        val lastVisit = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PATIENTS_LAST_VISIT)) ?: "never"

        val medicalAlertsJson = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PATIENTS_MEDICAL_ALERTS_JSON))
        val allergiesJson = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PATIENTS_ALLERGIES_JSON))
        val examAnswersJson = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PATIENTS_EXAM_ANSWERS_JSON))
        val diagnosisJson = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PATIENTS_DIAGNOSIS_JSON))
        val isChildIndex = cursor.getColumnIndex(ThornburyDbHelper.COL_PATIENTS_IS_CHILD)
        val isChild = if (isChildIndex >= 0) cursor.getInt(isChildIndex) == 1 else false

        return Patient(
            id = id,
            opNo = opNo,
            name = name,
            dob = dob,
            phone = phone,
            email = email,
            address = address,
            isChild = isChild,
            medicalHistory = medicalHistory,
            familyHistory = familyHistory,
            pastDentalHistory = pastDentalHistory,
            lastVisit = lastVisit,
            medicalAlerts = medicalAlertsJson.toStringList(),
            allergies = allergiesJson.toAllergies(),
            teeth = teeth,
            examAnswers = examAnswersJson.toExamAnswers(),
            diagnosis = diagnosisJson.toPatientDiagnosis()
        )
    }
}
