package com.example.thornburydental.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLite database helper for Thornbury Dental application.
 * Manages schema creation, table definitions, foreign key constraints, indexes, and migrations.
 */
class ThornburyDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "thornbury_dental.db"
        const val DATABASE_VERSION = 4

        // Table Names
        const val TABLE_PATIENTS = "patients"
        const val TABLE_TEETH = "teeth"
        const val TABLE_APPOINTMENTS = "appointments"
        const val TABLE_TREATMENT_PLANS = "treatment_plans"
        const val TABLE_PRESCRIPTIONS = "prescriptions"
        const val TABLE_DIAGNOSTIC_REPORTS = "diagnostic_reports"
        const val TABLE_USERS = "users"
        const val TABLE_USER_PREFERENCES = "user_preferences"

        // User Preferences columns
        const val COL_PREF_ID = "id"
        const val COL_PREF_FULL_NAME = "full_name"
        const val COL_PREF_PRONOUNS = "pronouns"
        const val COL_PREF_DOB = "dob"
        const val COL_PREF_PHONE = "phone"
        const val COL_PREF_EMAIL = "email"
        const val COL_PREF_DENTAL_GOALS_JSON = "dental_goals_json"
        const val COL_PREF_ANXIETY_LEVEL = "anxiety_level"
        const val COL_PREF_COMFORT_AMENITIES_JSON = "comfort_amenities_json"
        const val COL_PREF_ANESTHESIA_PREF = "anesthesia_preference"
        const val COL_PREF_MEDICAL_ALERTS_JSON = "medical_alerts_json"
        const val COL_PREF_LAST_VISIT = "last_visit"
        const val COL_PREF_SCHEDULE_PREF = "schedule_preference"
        const val COL_PREF_CONTACT_CHANNEL = "contact_channel"
        const val COL_PREF_ADDITIONAL_NOTES = "additional_notes"
        const val COL_PREF_IS_COMPLETED = "is_onboarding_completed"
        const val COL_PREF_UPDATED_AT = "updated_at"

        // Users columns
        const val COL_USERS_ID = "id"
        const val COL_USERS_EMAIL = "email"
        const val COL_USERS_PASSWORD_HASH = "password_hash"
        const val COL_USERS_NAME = "name"
        const val COL_USERS_ROLE = "role"
        const val COL_USERS_PHONE = "phone"
        const val COL_USERS_CREATED_AT = "created_at"

        // Patients columns
        const val COL_PATIENTS_ID = "id"
        const val COL_PATIENTS_OP_NO = "op_no"
        const val COL_PATIENTS_NAME = "name"
        const val COL_PATIENTS_DOB = "dob"
        const val COL_PATIENTS_PHONE = "phone"
        const val COL_PATIENTS_EMAIL = "email"
        const val COL_PATIENTS_ADDRESS = "address"
        const val COL_PATIENTS_MEDICAL_HISTORY = "medical_history"
        const val COL_PATIENTS_FAMILY_HISTORY = "family_history"
        const val COL_PATIENTS_PAST_DENTAL_HISTORY = "past_dental_history"
        const val COL_PATIENTS_LAST_VISIT = "last_visit"
        const val COL_PATIENTS_MEDICAL_ALERTS_JSON = "medical_alerts_json"
        const val COL_PATIENTS_ALLERGIES_JSON = "allergies_json"
        const val COL_PATIENTS_EXAM_ANSWERS_JSON = "exam_answers_json"
        const val COL_PATIENTS_DIAGNOSIS_JSON = "diagnosis_json"
        const val COL_PATIENTS_CREATED_AT = "created_at"

        // Teeth columns
        const val COL_TEETH_ID = "id"
        const val COL_TEETH_PATIENT_ID = "patient_id"
        const val COL_TEETH_NUMBER = "number"
        const val COL_TEETH_FDI_NUMBER = "fdi_number"
        const val COL_TEETH_NAME = "name"
        const val COL_TEETH_ARCH = "arch"
        const val COL_TEETH_CONDITION = "condition"
        const val COL_TEETH_NOTES = "notes"

        // Appointments columns
        const val COL_APPTS_ID = "id"
        const val COL_APPTS_PATIENT_ID = "patient_id"
        const val COL_APPTS_PATIENT_NAME = "patient_name"
        const val COL_APPTS_PATIENT_OP_NO = "patient_op_no"
        const val COL_APPTS_PATIENT_DOB = "patient_dob"
        const val COL_APPTS_CLINICIAN_ID = "clinician_id"
        const val COL_APPTS_CLINICIAN_NAME = "clinician_name"
        const val COL_APPTS_DATE = "date"
        const val COL_APPTS_TIME = "time"
        const val COL_APPTS_DURATION_MIN = "duration_min"
        const val COL_APPTS_ROOM = "room"
        const val COL_APPTS_PROCEDURE = "procedure"
        const val COL_APPTS_ALLERGY_LIST = "allergy_list"
        const val COL_APPTS_STATUS = "status"
        const val COL_APPTS_CREATED_AT = "created_at"

        // Treatment Plans columns
        const val COL_PLANS_ID = "id"
        const val COL_PLANS_PATIENT_ID = "patient_id"
        const val COL_PLANS_TITLE = "title"
        const val COL_PLANS_CLINICIAN_NAME = "clinician_name"
        const val COL_PLANS_DIAGNOSIS = "diagnosis"
        const val COL_PLANS_DATE_CREATED = "date_created"
        const val COL_PLANS_IS_LOCKED = "is_locked"
        const val COL_PLANS_TAMPER_HASH = "tamper_hash"
        const val COL_PLANS_STEPS_JSON = "steps_json"
        const val COL_PLANS_ADDENDA_JSON = "addenda_json"

        // Prescriptions columns
        const val COL_RX_ID = "id"
        const val COL_RX_PATIENT_ID = "patient_id"
        const val COL_RX_PATIENT_NAME = "patient_name"
        const val COL_RX_CLINICIAN_NAME = "clinician_name"
        const val COL_RX_DRUG_NAME = "drug_name"
        const val COL_RX_DOSAGE = "dosage"
        const val COL_RX_FREQUENCY = "frequency"
        const val COL_RX_DURATION = "duration"
        const val COL_RX_INSTRUCTIONS = "instructions"
        const val COL_RX_ISSUE_DATE = "issue_date"
        const val COL_RX_IS_DISPENSED = "is_dispensed"

        // Diagnostic Reports columns
        const val COL_REPORTS_ID = "id"
        const val COL_REPORTS_PATIENT_ID = "patient_id"
        const val COL_REPORTS_CLINICIAN_NAME = "clinician_name"
        const val COL_REPORTS_KIND = "kind"
        const val COL_REPORTS_TITLE = "title"
        const val COL_REPORTS_SUMMARY = "summary"
        const val COL_REPORTS_TAKEN_AT = "taken_at"
        const val COL_REPORTS_RELEASED_AT = "released_at"
        const val COL_REPORTS_IMAGE = "image"
        const val COL_REPORTS_ATTACHMENTS_JSON = "attachments_json"

        @Volatile
        private var INSTANCE: ThornburyDbHelper? = null

        fun getInstance(context: Context): ThornburyDbHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThornburyDbHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Patients Table
        db.execSQL(
            """
            CREATE TABLE $TABLE_PATIENTS (
                $COL_PATIENTS_ID TEXT PRIMARY KEY,
                $COL_PATIENTS_OP_NO TEXT NOT NULL,
                $COL_PATIENTS_NAME TEXT NOT NULL,
                $COL_PATIENTS_DOB TEXT NOT NULL,
                $COL_PATIENTS_PHONE TEXT,
                $COL_PATIENTS_EMAIL TEXT,
                $COL_PATIENTS_ADDRESS TEXT,
                $COL_PATIENTS_MEDICAL_HISTORY TEXT,
                $COL_PATIENTS_FAMILY_HISTORY TEXT,
                $COL_PATIENTS_PAST_DENTAL_HISTORY TEXT,
                $COL_PATIENTS_LAST_VISIT TEXT,
                $COL_PATIENTS_MEDICAL_ALERTS_JSON TEXT,
                $COL_PATIENTS_ALLERGIES_JSON TEXT,
                $COL_PATIENTS_EXAM_ANSWERS_JSON TEXT,
                $COL_PATIENTS_DIAGNOSIS_JSON TEXT,
                $COL_PATIENTS_CREATED_AT INTEGER
            );
            """.trimIndent()
        )

        // 2. Teeth Table
        db.execSQL(
            """
            CREATE TABLE $TABLE_TEETH (
                $COL_TEETH_ID TEXT PRIMARY KEY,
                $COL_TEETH_PATIENT_ID TEXT NOT NULL,
                $COL_TEETH_NUMBER INTEGER NOT NULL,
                $COL_TEETH_FDI_NUMBER INTEGER NOT NULL,
                $COL_TEETH_NAME TEXT NOT NULL,
                $COL_TEETH_ARCH TEXT NOT NULL,
                $COL_TEETH_CONDITION TEXT NOT NULL,
                $COL_TEETH_NOTES TEXT,
                FOREIGN KEY ($COL_TEETH_PATIENT_ID) REFERENCES $TABLE_PATIENTS ($COL_PATIENTS_ID) ON DELETE CASCADE
            );
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_teeth_patient ON $TABLE_TEETH ($COL_TEETH_PATIENT_ID);")

        // 3. Appointments Table
        db.execSQL(
            """
            CREATE TABLE $TABLE_APPOINTMENTS (
                $COL_APPTS_ID TEXT PRIMARY KEY,
                $COL_APPTS_PATIENT_ID TEXT NOT NULL,
                $COL_APPTS_PATIENT_NAME TEXT NOT NULL,
                $COL_APPTS_PATIENT_OP_NO TEXT NOT NULL,
                $COL_APPTS_PATIENT_DOB TEXT NOT NULL,
                $COL_APPTS_CLINICIAN_ID TEXT NOT NULL,
                $COL_APPTS_CLINICIAN_NAME TEXT NOT NULL,
                $COL_APPTS_DATE TEXT NOT NULL DEFAULT '',
                $COL_APPTS_TIME TEXT NOT NULL,
                $COL_APPTS_DURATION_MIN INTEGER NOT NULL,
                $COL_APPTS_ROOM TEXT NOT NULL,
                $COL_APPTS_PROCEDURE TEXT NOT NULL,
                $COL_APPTS_ALLERGY_LIST TEXT,
                $COL_APPTS_STATUS TEXT NOT NULL,
                $COL_APPTS_CREATED_AT INTEGER
            );
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_appts_patient ON $TABLE_APPOINTMENTS ($COL_APPTS_PATIENT_ID);")

        // 4. Treatment Plans Table
        db.execSQL(
            """
            CREATE TABLE $TABLE_TREATMENT_PLANS (
                $COL_PLANS_ID TEXT PRIMARY KEY,
                $COL_PLANS_PATIENT_ID TEXT NOT NULL,
                $COL_PLANS_TITLE TEXT NOT NULL DEFAULT 'Comprehensive Treatment Plan',
                $COL_PLANS_CLINICIAN_NAME TEXT NOT NULL,
                $COL_PLANS_DIAGNOSIS TEXT NOT NULL,
                $COL_PLANS_DATE_CREATED TEXT NOT NULL,
                $COL_PLANS_IS_LOCKED INTEGER NOT NULL DEFAULT 1,
                $COL_PLANS_TAMPER_HASH TEXT NOT NULL,
                $COL_PLANS_STEPS_JSON TEXT NOT NULL,
                $COL_PLANS_ADDENDA_JSON TEXT NOT NULL
            );
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_plans_patient ON $TABLE_TREATMENT_PLANS ($COL_PLANS_PATIENT_ID);")

        // 5. Prescriptions Table
        db.execSQL(
            """
            CREATE TABLE $TABLE_PRESCRIPTIONS (
                $COL_RX_ID TEXT PRIMARY KEY,
                $COL_RX_PATIENT_ID TEXT NOT NULL,
                $COL_RX_PATIENT_NAME TEXT NOT NULL,
                $COL_RX_CLINICIAN_NAME TEXT NOT NULL,
                $COL_RX_DRUG_NAME TEXT NOT NULL,
                $COL_RX_DOSAGE TEXT NOT NULL,
                $COL_RX_FREQUENCY TEXT NOT NULL,
                $COL_RX_DURATION TEXT NOT NULL,
                $COL_RX_INSTRUCTIONS TEXT NOT NULL,
                $COL_RX_ISSUE_DATE TEXT NOT NULL,
                $COL_RX_IS_DISPENSED INTEGER NOT NULL DEFAULT 0
            );
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_rx_patient ON $TABLE_PRESCRIPTIONS ($COL_RX_PATIENT_ID);")

        // 6. Diagnostic Reports Table
        db.execSQL(
            """
            CREATE TABLE $TABLE_DIAGNOSTIC_REPORTS (
                $COL_REPORTS_ID TEXT PRIMARY KEY,
                $COL_REPORTS_PATIENT_ID TEXT NOT NULL,
                $COL_REPORTS_CLINICIAN_NAME TEXT NOT NULL,
                $COL_REPORTS_KIND TEXT NOT NULL,
                $COL_REPORTS_TITLE TEXT NOT NULL,
                $COL_REPORTS_SUMMARY TEXT NOT NULL,
                $COL_REPORTS_TAKEN_AT TEXT NOT NULL,
                $COL_REPORTS_RELEASED_AT TEXT,
                $COL_REPORTS_IMAGE TEXT,
                $COL_REPORTS_ATTACHMENTS_JSON TEXT NOT NULL
            );
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_reports_patient ON $TABLE_DIAGNOSTIC_REPORTS ($COL_REPORTS_PATIENT_ID);")

        // 7. Users Table
        db.execSQL(
            """
            CREATE TABLE $TABLE_USERS (
                $COL_USERS_ID TEXT PRIMARY KEY,
                $COL_USERS_EMAIL TEXT UNIQUE NOT NULL,
                $COL_USERS_PASSWORD_HASH TEXT NOT NULL,
                $COL_USERS_NAME TEXT NOT NULL,
                $COL_USERS_ROLE TEXT NOT NULL,
                $COL_USERS_PHONE TEXT,
                $COL_USERS_CREATED_AT INTEGER
            );
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON $TABLE_USERS ($COL_USERS_EMAIL);")

        // 8. User Preferences Table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_USER_PREFERENCES (
                $COL_PREF_ID TEXT PRIMARY KEY,
                $COL_PREF_FULL_NAME TEXT NOT NULL,
                $COL_PREF_PRONOUNS TEXT,
                $COL_PREF_DOB TEXT,
                $COL_PREF_PHONE TEXT,
                $COL_PREF_EMAIL TEXT,
                $COL_PREF_DENTAL_GOALS_JSON TEXT NOT NULL,
                $COL_PREF_ANXIETY_LEVEL TEXT NOT NULL,
                $COL_PREF_COMFORT_AMENITIES_JSON TEXT NOT NULL,
                $COL_PREF_ANESTHESIA_PREF TEXT NOT NULL,
                $COL_PREF_MEDICAL_ALERTS_JSON TEXT NOT NULL,
                $COL_PREF_LAST_VISIT TEXT NOT NULL,
                $COL_PREF_SCHEDULE_PREF TEXT NOT NULL,
                $COL_PREF_CONTACT_CHANNEL TEXT NOT NULL,
                $COL_PREF_ADDITIONAL_NOTES TEXT,
                $COL_PREF_IS_COMPLETED INTEGER NOT NULL DEFAULT 0,
                $COL_PREF_UPDATED_AT INTEGER NOT NULL
            );
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop existing tables in reverse dependency order
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USER_PREFERENCES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DIAGNOSTIC_REPORTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PRESCRIPTIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TREATMENT_PLANS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_APPOINTMENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TEETH")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PATIENTS")
        onCreate(db)
    }
}
