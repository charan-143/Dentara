package com.example.thornburydental.data.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.thornburydental.data.UserProfilePreferences

/**
 * Data Access Object for persisting and retrieving user profile and intake preferences
 * in the local SQLite database.
 */
class UserPreferencesDao(private val dbHelper: ThornburyDbHelper) {

    companion object {
        const val PRIMARY_PROFILE_ID = "primary_profile"
    }

    /**
     * Retrieve the user profile and preferences if stored, or null if none saved yet.
     */
    fun getPreferences(): UserProfilePreferences? {
        val db = dbHelper.readableDatabase
        val projection = arrayOf(
            ThornburyDbHelper.COL_PREF_ID,
            ThornburyDbHelper.COL_PREF_FULL_NAME,
            ThornburyDbHelper.COL_PREF_PRONOUNS,
            ThornburyDbHelper.COL_PREF_DOB,
            ThornburyDbHelper.COL_PREF_PHONE,
            ThornburyDbHelper.COL_PREF_EMAIL,
            ThornburyDbHelper.COL_PREF_DENTAL_GOALS_JSON,
            ThornburyDbHelper.COL_PREF_ANXIETY_LEVEL,
            ThornburyDbHelper.COL_PREF_COMFORT_AMENITIES_JSON,
            ThornburyDbHelper.COL_PREF_ANESTHESIA_PREF,
            ThornburyDbHelper.COL_PREF_MEDICAL_ALERTS_JSON,
            ThornburyDbHelper.COL_PREF_LAST_VISIT,
            ThornburyDbHelper.COL_PREF_SCHEDULE_PREF,
            ThornburyDbHelper.COL_PREF_CONTACT_CHANNEL,
            ThornburyDbHelper.COL_PREF_ADDITIONAL_NOTES,
            ThornburyDbHelper.COL_PREF_IS_COMPLETED,
            ThornburyDbHelper.COL_PREF_UPDATED_AT
        )

        return db.query(
            ThornburyDbHelper.TABLE_USER_PREFERENCES,
            projection,
            "${ThornburyDbHelper.COL_PREF_ID} = ?",
            arrayOf(PRIMARY_PROFILE_ID),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                val fullName = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PREF_FULL_NAME)) ?: ""
                val pronouns = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PREF_PRONOUNS)) ?: ""
                val dob = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PREF_DOB)) ?: ""
                val phone = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PREF_PHONE)) ?: ""
                val email = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PREF_EMAIL)) ?: ""
                val dentalGoalsJson = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PREF_DENTAL_GOALS_JSON))
                val anxietyLevel = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PREF_ANXIETY_LEVEL)) ?: "Relaxed"
                val amenitiesJson = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PREF_COMFORT_AMENITIES_JSON))
                val anesthesia = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PREF_ANESTHESIA_PREF)) ?: "Standard Local Anesthetic"
                val medicalAlertsJson = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PREF_MEDICAL_ALERTS_JSON))
                val lastVisit = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PREF_LAST_VISIT)) ?: "Within 6 months"
                val schedule = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PREF_SCHEDULE_PREF)) ?: "Morning (8am - 12pm)"
                val contactChannel = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PREF_CONTACT_CHANNEL)) ?: "SMS / WhatsApp"
                val additionalNotes = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PREF_ADDITIONAL_NOTES)) ?: ""
                val isOnboardingCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PREF_IS_COMPLETED)) == 1
                val updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_PREF_UPDATED_AT))

                UserProfilePreferences(
                    id = PRIMARY_PROFILE_ID,
                    fullName = fullName,
                    pronouns = pronouns,
                    dob = dob,
                    phone = phone,
                    email = email,
                    dentalGoals = DbConverters.jsonToStringList(dentalGoalsJson),
                    anxietyLevel = anxietyLevel,
                    comfortAmenities = DbConverters.jsonToStringList(amenitiesJson),
                    anesthesiaPreference = anesthesia,
                    medicalAlerts = DbConverters.jsonToStringList(medicalAlertsJson),
                    lastVisit = lastVisit,
                    schedulePreference = schedule,
                    contactChannel = contactChannel,
                    additionalNotes = additionalNotes,
                    isOnboardingCompleted = isOnboardingCompleted,
                    updatedAt = updatedAt
                )
            } else {
                null
            }
        }
    }

    /**
     * Insert or update the user profile and preferences.
     */
    fun savePreferences(prefs: UserProfilePreferences) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(ThornburyDbHelper.COL_PREF_ID, PRIMARY_PROFILE_ID)
            put(ThornburyDbHelper.COL_PREF_FULL_NAME, prefs.fullName)
            put(ThornburyDbHelper.COL_PREF_PRONOUNS, prefs.pronouns)
            put(ThornburyDbHelper.COL_PREF_DOB, prefs.dob)
            put(ThornburyDbHelper.COL_PREF_PHONE, prefs.phone)
            put(ThornburyDbHelper.COL_PREF_EMAIL, prefs.email)
            put(ThornburyDbHelper.COL_PREF_DENTAL_GOALS_JSON, DbConverters.stringListToJson(prefs.dentalGoals))
            put(ThornburyDbHelper.COL_PREF_ANXIETY_LEVEL, prefs.anxietyLevel)
            put(ThornburyDbHelper.COL_PREF_COMFORT_AMENITIES_JSON, DbConverters.stringListToJson(prefs.comfortAmenities))
            put(ThornburyDbHelper.COL_PREF_ANESTHESIA_PREF, prefs.anesthesiaPreference)
            put(ThornburyDbHelper.COL_PREF_MEDICAL_ALERTS_JSON, DbConverters.stringListToJson(prefs.medicalAlerts))
            put(ThornburyDbHelper.COL_PREF_LAST_VISIT, prefs.lastVisit)
            put(ThornburyDbHelper.COL_PREF_SCHEDULE_PREF, prefs.schedulePreference)
            put(ThornburyDbHelper.COL_PREF_CONTACT_CHANNEL, prefs.contactChannel)
            put(ThornburyDbHelper.COL_PREF_ADDITIONAL_NOTES, prefs.additionalNotes)
            put(ThornburyDbHelper.COL_PREF_IS_COMPLETED, if (prefs.isOnboardingCompleted) 1 else 0)
            put(ThornburyDbHelper.COL_PREF_UPDATED_AT, System.currentTimeMillis())
        }

        db.insertWithOnConflict(
            ThornburyDbHelper.TABLE_USER_PREFERENCES,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    /**
     * Check whether onboarding questionnaire has already been completed.
     */
    fun isOnboardingCompleted(): Boolean {
        return getPreferences()?.isOnboardingCompleted == true
    }

    /**
     * Check whether a profile has already been stored.
     */
    fun hasPreferences(): Boolean {
        return getPreferences() != null
    }
}
