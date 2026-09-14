package com.example.thornburydental.data.db

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

    @Synchronized
    fun initialize(context: Context) {
        if (isInitialized) return
        val appContext = context.applicationContext
        dbHelper = ThornburyDbHelper.getInstance(appContext)
        toothDao = ToothDao(dbHelper)
        patientDao = PatientDao(dbHelper)
        appointmentDao = AppointmentDao(dbHelper)
        treatmentPlanDao = TreatmentPlanDao(dbHelper)
        prescriptionDao = PrescriptionDao(dbHelper)
        reportDao = ReportDao(dbHelper)
        userDao = UserDao(dbHelper)
        userPreferencesDao = UserPreferencesDao(dbHelper)
        isInitialized = true
        Log.d(TAG, "Thornbury local SQLite database initialized successfully.")
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
     * Seed initial demo records if the local database is fresh/empty.
     */
    suspend fun seedInitialDataIfEmpty(
        patients: List<Patient>,
        appointments: List<Appointment>,
        prescriptions: List<Prescription>,
        treatmentPlans: List<TreatmentPlan>,
        reports: List<DiagnosticReport>
    ) = withContext(Dispatchers.IO) {
        if (!isInitialized) return@withContext
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val existing = patientDao.getAllPatients()
            if (existing.isEmpty()) {
                Log.d(TAG, "Seeding initial local database records...")
                patients.forEach { patient ->
                    patientDao.insertPatient(patient)
                }
                appointments.forEach { appt ->
                    appointmentDao.insertAppointment(appt)
                }
                prescriptions.forEach { rx ->
                    prescriptionDao.insertPrescription(rx)
                }
                treatmentPlans.forEach { plan ->
                    treatmentPlanDao.insertTreatmentPlan(plan)
                }
                reports.forEach { report ->
                    reportDao.insertReport(report)
                }
                userDao.seedDefaultUsersIfEmpty()
                db.setTransactionSuccessful()
                Log.d(TAG, "Seeding completed successfully.")
            } else {
                userDao.seedDefaultUsersIfEmpty()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seed initial database data", e)
        } finally {
            db.endTransaction()
        }
    }
}
