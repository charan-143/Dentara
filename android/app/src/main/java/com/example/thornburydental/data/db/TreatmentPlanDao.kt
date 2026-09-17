package com.example.thornburydental.data.db

import android.content.ContentValues
import android.database.Cursor
import net.sqlcipher.database.SQLiteDatabase
import com.example.thornburydental.data.PlanAddendum
import com.example.thornburydental.data.TreatmentPlan

/**
 * Data Access Object for Treatment Plans in Thornbury Dental SQLite database.
 * Supports multi-step dental care pathways, addenda logging, tamper-evident SHA-256 hash updates,
 * and individual step completion tracking.
 */
class TreatmentPlanDao(private val dbHelper: ThornburyDbHelper) {

    /**
     * Retrieves all treatment plans from the database, deserializing steps and addenda JSON.
     */
    fun getAllTreatmentPlans(): List<TreatmentPlan> {
        val db = dbHelper.readableDatabase
        val plans = mutableListOf<TreatmentPlan>()
        db.query(
            ThornburyDbHelper.TABLE_TREATMENT_PLANS,
            null,
            null,
            null,
            null,
            null,
            "${ThornburyDbHelper.COL_PLANS_DATE_CREATED} DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                plans.add(cursorToTreatmentPlan(cursor))
            }
        }
        return plans
    }

    /**
     * Retrieves all treatment plans for a specific patient.
     */
    fun getTreatmentPlansForPatient(patientId: String): List<TreatmentPlan> {
        val db = dbHelper.readableDatabase
        val plans = mutableListOf<TreatmentPlan>()
        db.query(
            ThornburyDbHelper.TABLE_TREATMENT_PLANS,
            null,
            "${ThornburyDbHelper.COL_PLANS_PATIENT_ID} = ?",
            arrayOf(patientId),
            null,
            null,
            "${ThornburyDbHelper.COL_PLANS_DATE_CREATED} DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                plans.add(cursorToTreatmentPlan(cursor))
            }
        }
        return plans
    }

    /**
     * Retrieves a single treatment plan by ID.
     */
    fun getTreatmentPlanById(planId: String): TreatmentPlan? {
        val db = dbHelper.readableDatabase
        db.query(
            ThornburyDbHelper.TABLE_TREATMENT_PLANS,
            null,
            "${ThornburyDbHelper.COL_PLANS_ID} = ?",
            arrayOf(planId),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursorToTreatmentPlan(cursor)
            }
        }
        return null
    }

    /**
     * Inserts or replaces a treatment plan in the database.
     */
    fun insertTreatmentPlan(plan: TreatmentPlan) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(ThornburyDbHelper.COL_PLANS_ID, plan.id)
            put(ThornburyDbHelper.COL_PLANS_PATIENT_ID, plan.patientId)
            put(ThornburyDbHelper.COL_PLANS_TITLE, plan.title)
            put(ThornburyDbHelper.COL_PLANS_CLINICIAN_NAME, plan.clinicianName)
            put(ThornburyDbHelper.COL_PLANS_DIAGNOSIS, plan.diagnosis)
            put(ThornburyDbHelper.COL_PLANS_DATE_CREATED, plan.dateCreated)
            put(ThornburyDbHelper.COL_PLANS_IS_LOCKED, if (plan.isLocked) 1 else 0)
            put(ThornburyDbHelper.COL_PLANS_TAMPER_HASH, plan.tamperHash)
            put(ThornburyDbHelper.COL_PLANS_STEPS_JSON, DbConverters.planStepsToJson(plan.steps))
            put(ThornburyDbHelper.COL_PLANS_ADDENDA_JSON, DbConverters.planAddendaToJson(plan.addenda))
        }
        db.insertWithOnConflict(
            ThornburyDbHelper.TABLE_TREATMENT_PLANS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    /**
     * Updates the plan lock status and associates a new tamper verification hash.
     */
    fun togglePlanLock(planId: String, newLockState: Boolean, newTamperHash: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(ThornburyDbHelper.COL_PLANS_IS_LOCKED, if (newLockState) 1 else 0)
            put(ThornburyDbHelper.COL_PLANS_TAMPER_HASH, newTamperHash)
        }
        db.update(
            ThornburyDbHelper.TABLE_TREATMENT_PLANS,
            values,
            "${ThornburyDbHelper.COL_PLANS_ID} = ?",
            arrayOf(planId)
        )
    }

    /**
     * Toggles the completion status of a specific step within a treatment plan,
     * updating the serialized steps_json column.
     */
    fun toggleStepCompletion(planId: String, stepId: String) {
        val plan = getTreatmentPlanById(planId) ?: return
        val updatedSteps = plan.steps.map { step ->
            if (step.id == stepId) step.copy(completed = !step.completed) else step
        }
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(ThornburyDbHelper.COL_PLANS_STEPS_JSON, DbConverters.planStepsToJson(updatedSteps))
        }
        db.update(
            ThornburyDbHelper.TABLE_TREATMENT_PLANS,
            values,
            "${ThornburyDbHelper.COL_PLANS_ID} = ?",
            arrayOf(planId)
        )
    }

    /**
     * Appends a new addendum to the treatment plan, updating the serialized addenda_json column.
     */
    fun addPlanAddendum(planId: String, addendum: PlanAddendum) {
        val plan = getTreatmentPlanById(planId) ?: return
        val updatedAddenda = plan.addenda + addendum
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(ThornburyDbHelper.COL_PLANS_ADDENDA_JSON, DbConverters.planAddendaToJson(updatedAddenda))
        }
        db.update(
            ThornburyDbHelper.TABLE_TREATMENT_PLANS,
            values,
            "${ThornburyDbHelper.COL_PLANS_ID} = ?",
            arrayOf(planId)
        )
    }

    /**
     * Deletes a treatment plan by ID.
     */
    fun deleteTreatmentPlan(planId: String) {
        val db = dbHelper.writableDatabase
        db.delete(
            ThornburyDbHelper.TABLE_TREATMENT_PLANS,
            "${ThornburyDbHelper.COL_PLANS_ID} = ?",
            arrayOf(planId)
        )
    }

    private fun cursorToTreatmentPlan(cursor: Cursor): TreatmentPlan {
        val stepsJson = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PLANS_STEPS_JSON))
        val addendaJson = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PLANS_ADDENDA_JSON))
        val isLockedInt = cursor.getInt(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PLANS_IS_LOCKED))
        val titleIdx = cursor.getColumnIndex(ThornburyDbHelper.COL_PLANS_TITLE)
        val title = if (titleIdx >= 0) cursor.getString(titleIdx) ?: "Treatment Plan" else "Treatment Plan"

        return TreatmentPlan(
            id = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PLANS_ID)),
            patientId = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PLANS_PATIENT_ID)),
            title = title,
            clinicianName = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PLANS_CLINICIAN_NAME)),
            diagnosis = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PLANS_DIAGNOSIS)),
            dateCreated = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PLANS_DATE_CREATED)),
            isLocked = isLockedInt == 1,
            tamperHash = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PLANS_TAMPER_HASH)),
            steps = DbConverters.jsonToPlanSteps(stepsJson),
            addenda = DbConverters.jsonToPlanAddenda(addendaJson)
        )
    }
}
