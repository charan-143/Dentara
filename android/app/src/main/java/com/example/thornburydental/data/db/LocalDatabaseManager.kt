package com.example.thornburydental.data.db

import android.content.ContentValues
import android.content.Context
import android.util.Log
import com.example.thornburydental.data.Appointment
import com.example.thornburydental.data.DiagnosticReport
import com.example.thornburydental.data.Patient
import com.example.thornburydental.data.Prescription
import com.example.thornburydental.data.TreatmentPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Central manager coordinating all SQLite DAOs and lifecycle for Thornbury Dental local database.
 */
object LocalDatabaseManager {

    private const val TAG = "LocalDatabaseManager"

    @Volatile
    var isInitialized = false
        private set

    var appContext: Context? = null
        private set

    lateinit var dbHelper: ThornburyDbHelper
        private set

    lateinit var patientDao: PatientDao
        private set

    lateinit var toothDao: ToothDao
        private set

    lateinit var appointmentDao: AppointmentDao
        private set

    lateinit var treatmentPlanDao: TreatmentPlanDao
        private set

    lateinit var prescriptionDao: PrescriptionDao
        private set

    lateinit var reportDao: ReportDao
        private set

    lateinit var userDao: UserDao
        private set

    lateinit var userPreferencesDao: UserPreferencesDao
        private set

    lateinit var medicationPresetDao: MedicationPresetDao
        private set

    lateinit var voiceUndoDao: VoiceUndoDao
        private set

    @Synchronized
    fun initialize(context: Context) {
        if (isInitialized) return
        val appCtx = context.applicationContext
        appContext = appCtx
        dbHelper = ThornburyDbHelper.getInstance(appCtx)
        toothDao = ToothDao(dbHelper)
        patientDao = PatientDao(dbHelper)
        appointmentDao = AppointmentDao(dbHelper)
        treatmentPlanDao = TreatmentPlanDao(dbHelper)
        prescriptionDao = PrescriptionDao(dbHelper)
        reportDao = ReportDao(dbHelper)
        userDao = UserDao(dbHelper)
        userPreferencesDao = UserPreferencesDao(dbHelper)
        medicationPresetDao = MedicationPresetDao(dbHelper)
        voiceUndoDao = VoiceUndoDao(dbHelper)
        ensureMedicationPresetsTable()
        userDao.seedDefaultUsersIfEmpty()
        sanitizeClinicianNames()
        isInitialized = true
        Log.d(TAG, "Thornbury local SQLite database initialized successfully.")
    }

    private fun ensureMedicationPresetsTable() {
        try {
            val db = dbHelper.writableDatabase
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS ${ThornburyDbHelper.TABLE_MEDICATION_PRESETS} (
                    ${ThornburyDbHelper.COL_PRESET_ID} TEXT PRIMARY KEY,
                    ${ThornburyDbHelper.COL_PRESET_NAME} TEXT NOT NULL,
                    ${ThornburyDbHelper.COL_PRESET_DOSAGE} TEXT NOT NULL,
                    ${ThornburyDbHelper.COL_PRESET_FREQUENCY} TEXT NOT NULL,
                    ${ThornburyDbHelper.COL_PRESET_DURATION} TEXT NOT NULL,
                    ${ThornburyDbHelper.COL_PRESET_INSTRUCTIONS} TEXT NOT NULL,
                    ${ThornburyDbHelper.COL_PRESET_CATEGORY} TEXT NOT NULL DEFAULT 'General',
                    ${ThornburyDbHelper.COL_PRESET_IS_CUSTOM} INTEGER NOT NULL DEFAULT 0
                );
                """.trimIndent()
            )
            medicationPresetDao.seedDefaultsIfEmpty(com.example.thornburydental.data.MedicationPreset.defaultPresets)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ensure medication_presets table", e)
        }
    }

    /**
     * Check if the database has been seeded with data.
     */
    fun isDatabaseEmpty(): Boolean {
        if (!isInitialized) return true
        return try {
            patientDao.getAllPatients().isEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if database is empty", e)
            true
        }
    }

    /**
     * Deprecated development mock data seeder. Retained as a no-op for backward compatibility.
     */
    suspend fun seedInitialDataIfEmpty(
        patients: List<Patient> = emptyList(),
        appointments: List<Appointment> = emptyList(),
        prescriptions: List<Prescription> = emptyList(),
        treatmentPlans: List<TreatmentPlan> = emptyList(),
        reports: List<DiagnosticReport> = emptyList()
    ) = withContext(Dispatchers.IO) {
        // No-op in production: development mock data seeding has been purged.
    }

    /**
     * Purges legacy development mock patients (p1..p7) and their associated records
     * (appointments, reports, prescriptions, treatment plans, teeth) from SQLite
     * on existing devices that ran earlier development builds.
     * Preserves clinical formulary presets and user preferences.
     */
    suspend fun purgeDevelopmentSeedData() = withContext(Dispatchers.IO) {
        if (!isInitialized) return@withContext
        try {
            val db = dbHelper.writableDatabase
            val mockIds = listOf("p1", "p2", "p3", "p4", "p5", "p6", "p7")
            val placeholders = mockIds.joinToString(",") { "?" }
            val args = mockIds.toTypedArray()

            db.beginTransaction()
            try {
                db.delete(ThornburyDbHelper.TABLE_APPOINTMENTS, "${ThornburyDbHelper.COL_APPTS_PATIENT_ID} IN ($placeholders)", args)
                db.delete(ThornburyDbHelper.TABLE_DIAGNOSTIC_REPORTS, "${ThornburyDbHelper.COL_REPORTS_PATIENT_ID} IN ($placeholders)", args)
                db.delete(ThornburyDbHelper.TABLE_PRESCRIPTIONS, "${ThornburyDbHelper.COL_RX_PATIENT_ID} IN ($placeholders)", args)
                db.delete(ThornburyDbHelper.TABLE_TREATMENT_PLANS, "${ThornburyDbHelper.COL_PLANS_PATIENT_ID} IN ($placeholders)", args)
                db.delete(ThornburyDbHelper.TABLE_TEETH, "${ThornburyDbHelper.COL_TEETH_PATIENT_ID} IN ($placeholders)", args)
                db.delete(ThornburyDbHelper.TABLE_PATIENTS, "${ThornburyDbHelper.COL_PATIENTS_ID} IN ($placeholders)", args)
                db.setTransactionSuccessful()
                Log.d(TAG, "Development mock seed data purged successfully.")
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to purge development seed data", e)
        }
    }

    /**
     * Sanitize all stored records in local SQLite so that only the user clinician
     * (Dr. Ingrid Halvorsen) is referenced, cleansing any legacy multi-clinician mock data.
     */
    fun sanitizeClinicianNames() {
        try {
            val db = dbHelper.writableDatabase
            val singleClinician = "Dr. Ingrid Halvorsen"

            val apptValues = ContentValues().apply {
                put(ThornburyDbHelper.COL_APPTS_CLINICIAN_NAME, singleClinician)
                put(ThornburyDbHelper.COL_APPTS_CLINICIAN_ID, "c1")
            }
            db.update(ThornburyDbHelper.TABLE_APPOINTMENTS, apptValues, null, null)

            val rxValues = ContentValues().apply {
                put(ThornburyDbHelper.COL_RX_CLINICIAN_NAME, singleClinician)
            }
            db.update(ThornburyDbHelper.TABLE_PRESCRIPTIONS, rxValues, null, null)

            val planValues = ContentValues().apply {
                put(ThornburyDbHelper.COL_PLANS_CLINICIAN_NAME, singleClinician)
            }
            db.update(ThornburyDbHelper.TABLE_TREATMENT_PLANS, planValues, null, null)

            val reportValues = ContentValues().apply {
                put(ThornburyDbHelper.COL_REPORTS_CLINICIAN_NAME, singleClinician)
            }
            db.update(ThornburyDbHelper.TABLE_DIAGNOSTIC_REPORTS, reportValues, null, null)

            val patients = patientDao.getAllPatients()
            patients.forEach { p ->
                if (p.diagnosis != null && p.diagnosis.clinicianName != singleClinician) {
                    val updatedDiag = p.diagnosis.copy(clinicianName = singleClinician)
                    patientDao.updateDiagnosis(p.id, updatedDiag)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sanitize clinician names in database", e)
        }
    }
}
