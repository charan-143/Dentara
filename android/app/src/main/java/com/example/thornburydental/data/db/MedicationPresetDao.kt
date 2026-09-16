package com.example.thornburydental.data.db

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.thornburydental.data.MedicationPreset

/**
 * Data Access Object for medication presets in Thornbury Dental SQLite database.
 * Supports clinical formulary management, custom medication presets, and default recovery.
 */
class MedicationPresetDao(private val dbHelper: ThornburyDbHelper) {

    /**
     * Query all presets ordered by custom status (defaults first, then user-created) and name.
     */
    fun getAllPresets(): List<MedicationPreset> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<MedicationPreset>()
        db.query(
            ThornburyDbHelper.TABLE_MEDICATION_PRESETS,
            null,
            null,
            null,
            null,
            null,
            "${ThornburyDbHelper.COL_PRESET_IS_CUSTOM} ASC, ${ThornburyDbHelper.COL_PRESET_NAME} ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursorToPreset(cursor))
            }
        }
        return list
    }

    /**
     * Insert or replace a medication preset.
     */
    fun insertPreset(preset: MedicationPreset) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(ThornburyDbHelper.COL_PRESET_ID, preset.id)
            put(ThornburyDbHelper.COL_PRESET_NAME, preset.name)
            put(ThornburyDbHelper.COL_PRESET_DOSAGE, preset.dosage)
            put(ThornburyDbHelper.COL_PRESET_FREQUENCY, preset.frequency)
            put(ThornburyDbHelper.COL_PRESET_DURATION, preset.duration)
            put(ThornburyDbHelper.COL_PRESET_INSTRUCTIONS, preset.instructions)
            put(ThornburyDbHelper.COL_PRESET_CATEGORY, preset.category)
            put(ThornburyDbHelper.COL_PRESET_IS_CUSTOM, if (preset.isCustom) 1 else 0)
        }
        db.insertWithOnConflict(
            ThornburyDbHelper.TABLE_MEDICATION_PRESETS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    /**
     * Update an existing medication preset.
     */
    fun updatePreset(preset: MedicationPreset) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(ThornburyDbHelper.COL_PRESET_NAME, preset.name)
            put(ThornburyDbHelper.COL_PRESET_DOSAGE, preset.dosage)
            put(ThornburyDbHelper.COL_PRESET_FREQUENCY, preset.frequency)
            put(ThornburyDbHelper.COL_PRESET_DURATION, preset.duration)
            put(ThornburyDbHelper.COL_PRESET_INSTRUCTIONS, preset.instructions)
            put(ThornburyDbHelper.COL_PRESET_CATEGORY, preset.category)
            put(ThornburyDbHelper.COL_PRESET_IS_CUSTOM, if (preset.isCustom) 1 else 0)
        }
        db.update(
            ThornburyDbHelper.TABLE_MEDICATION_PRESETS,
            values,
            "${ThornburyDbHelper.COL_PRESET_ID} = ?",
            arrayOf(preset.id)
        )
    }

    /**
     * Delete a medication preset by ID.
     */
    fun deletePreset(presetId: String): Int {
        val db = dbHelper.writableDatabase
        return db.delete(
            ThornburyDbHelper.TABLE_MEDICATION_PRESETS,
            "${ThornburyDbHelper.COL_PRESET_ID} = ?",
            arrayOf(presetId)
        )
    }

    /**
     * Seed initial presets if table is empty.
     */
    fun seedDefaultsIfEmpty(defaults: List<MedicationPreset>) {
        val db = dbHelper.writableDatabase
        val existing = getAllPresets()
        if (existing.isEmpty()) {
            db.beginTransaction()
            try {
                defaults.forEach { insertPreset(it) }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    /**
     * Reset presets to standard default formulary.
     */
    fun resetToDefaults(defaults: List<MedicationPreset>) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(ThornburyDbHelper.TABLE_MEDICATION_PRESETS, null, null)
            defaults.forEach { insertPreset(it) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun cursorToPreset(cursor: Cursor): MedicationPreset {
        val id = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PRESET_ID))
        val name = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PRESET_NAME)) ?: ""
        val dosage = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PRESET_DOSAGE)) ?: ""
        val frequency = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PRESET_FREQUENCY)) ?: ""
        val duration = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PRESET_DURATION)) ?: ""
        val instructions = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PRESET_INSTRUCTIONS)) ?: ""
        val category = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PRESET_CATEGORY)) ?: "General"
        val isCustom = cursor.getInt(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PRESET_IS_CUSTOM)) == 1

        return MedicationPreset(
            id = id,
            name = name,
            dosage = dosage,
            frequency = frequency,
            duration = duration,
            instructions = instructions,
            category = category,
            isCustom = isCustom
        )
    }
}
