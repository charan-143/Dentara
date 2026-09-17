package com.example.thornburydental.data.db

import android.content.ContentValues
import android.database.Cursor
import net.sqlcipher.database.SQLiteDatabase
import com.example.thornburydental.data.User
import com.example.thornburydental.data.UserRole
import java.security.MessageDigest
import java.util.UUID

/**
 * Data Access Object for User authentication and credentials in SQLite database.
 * Provides SHA-256 salted password hashing, authentication, registration, and user querying.
 */
class UserDao(private val dbHelper: ThornburyDbHelper) {

    /**
     * Hash password using SHA-256 with a cryptographic salt.
     */
    fun hashPassword(password: String, salt: String = "ThornburySalt2026"): String {
        val md = MessageDigest.getInstance("SHA-256")
        val input = "$salt$password".toByteArray(Charsets.UTF_8)
        val digest = md.digest(input)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Authenticate a user by email and password.
     * Returns User if credentials match, null otherwise.
     */
    fun authenticate(email: String, password: String): User? {
        val cleanEmail = email.trim().lowercase()
        val expectedHash = hashPassword(password)
        val db = dbHelper.readableDatabase

        db.query(
            ThornburyDbHelper.TABLE_USERS,
            null,
            "LOWER(${ThornburyDbHelper.COL_USERS_EMAIL}) = ? AND ${ThornburyDbHelper.COL_USERS_PASSWORD_HASH} = ?",
            arrayOf(cleanEmail, expectedHash),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursorToUser(cursor)
            }
        }
        return null
    }

    /**
     * Register a new user with salted SHA-256 password hash.
     */
    fun registerUser(
        email: String,
        password: String,
        name: String,
        role: UserRole,
        phone: String = ""
    ): Result<User> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("Email address cannot be blank"))
        }
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("Password cannot be blank"))
        }
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Name cannot be blank"))
        }

        if (getUserByEmail(cleanEmail) != null) {
            return Result.failure(IllegalStateException("User with email $cleanEmail already exists"))
        }

        val userId = "usr-" + UUID.randomUUID().toString().take(8)
        val passwordHash = hashPassword(password)
        val createdAt = System.currentTimeMillis()

        val user = User(
            id = userId,
            email = cleanEmail,
            name = name.trim(),
            role = role,
            phone = phone.trim(),
            createdAt = createdAt
        )

        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(ThornburyDbHelper.COL_USERS_ID, user.id)
            put(ThornburyDbHelper.COL_USERS_EMAIL, user.email)
            put(ThornburyDbHelper.COL_USERS_PASSWORD_HASH, passwordHash)
            put(ThornburyDbHelper.COL_USERS_NAME, user.name)
            put(ThornburyDbHelper.COL_USERS_ROLE, user.role.name)
            put(ThornburyDbHelper.COL_USERS_PHONE, user.phone)
            put(ThornburyDbHelper.COL_USERS_CREATED_AT, user.createdAt)
        }

        return try {
            val rowId = db.insertWithOnConflict(
                ThornburyDbHelper.TABLE_USERS,
                null,
                values,
                SQLiteDatabase.CONFLICT_ABORT
            )
            if (rowId != -1L) {
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Failed to register user in database"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Look up a user by email address (case-insensitive).
     */
    fun getUserByEmail(email: String): User? {
        val cleanEmail = email.trim().lowercase()
        val db = dbHelper.readableDatabase

        db.query(
            ThornburyDbHelper.TABLE_USERS,
            null,
            "LOWER(${ThornburyDbHelper.COL_USERS_EMAIL}) = ?",
            arrayOf(cleanEmail),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursorToUser(cursor)
            }
        }
        return null
    }

    /**
     * Look up a user by unique identifier.
     */
    fun getUserById(id: String): User? {
        val db = dbHelper.readableDatabase

        db.query(
            ThornburyDbHelper.TABLE_USERS,
            null,
            "${ThornburyDbHelper.COL_USERS_ID} = ?",
            arrayOf(id),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursorToUser(cursor)
            }
        }
        return null
    }

    /**
     * Seed default clinician and patient users if not already present.
     */
    fun seedDefaultUsersIfEmpty() {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            if (getUserByEmail("dr.halvorsen@dentara.com") == null) {
                val clinicianValues = ContentValues().apply {
                    put(ThornburyDbHelper.COL_USERS_ID, "usr-clinician-halvorsen")
                    put(ThornburyDbHelper.COL_USERS_EMAIL, "dr.halvorsen@dentara.com")
                    put(ThornburyDbHelper.COL_USERS_PASSWORD_HASH, hashPassword("password123"))
                    put(ThornburyDbHelper.COL_USERS_NAME, "Dr. Ingrid Halvorsen")
                    put(ThornburyDbHelper.COL_USERS_ROLE, UserRole.CLINICIAN.name)
                    put(ThornburyDbHelper.COL_USERS_PHONE, "+44 20 7946 0912")
                    put(ThornburyDbHelper.COL_USERS_CREATED_AT, System.currentTimeMillis())
                }
                db.insertWithOnConflict(
                    ThornburyDbHelper.TABLE_USERS,
                    null,
                    clinicianValues,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }

            if (getUserByEmail("rosalind.achebe@example.org") == null) {
                val patientValues = ContentValues().apply {
                    put(ThornburyDbHelper.COL_USERS_ID, "usr-patient-achebe")
                    put(ThornburyDbHelper.COL_USERS_EMAIL, "rosalind.achebe@example.org")
                    put(ThornburyDbHelper.COL_USERS_PASSWORD_HASH, hashPassword("password123"))
                    put(ThornburyDbHelper.COL_USERS_NAME, "Rosalind Achebe")
                    put(ThornburyDbHelper.COL_USERS_ROLE, UserRole.PATIENT.name)
                    put(ThornburyDbHelper.COL_USERS_PHONE, "+1 (503) 224-7719")
                    put(ThornburyDbHelper.COL_USERS_CREATED_AT, System.currentTimeMillis())
                }
                db.insertWithOnConflict(
                    ThornburyDbHelper.TABLE_USERS,
                    null,
                    patientValues,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Retrieve all registered users.
     */
    fun getAllUsers(): List<User> {
        val db = dbHelper.readableDatabase
        val users = mutableListOf<User>()

        db.query(
            ThornburyDbHelper.TABLE_USERS,
            null,
            null,
            null,
            null,
            null,
            "${ThornburyDbHelper.COL_USERS_NAME} ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                users.add(cursorToUser(cursor))
            }
        }
        return users
    }

    private fun cursorToUser(cursor: Cursor): User {
        val id = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_USERS_ID))
        val email = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_USERS_EMAIL))
        val name = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_USERS_NAME))
        val roleStr = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_USERS_ROLE))
        val phone = cursor.getString(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_USERS_PHONE)) ?: ""
        val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_USERS_CREATED_AT))

        val role = try {
            UserRole.valueOf(roleStr)
        } catch (e: Exception) {
            UserRole.PATIENT
        }

        return User(
            id = id,
            email = email,
            name = name,
            role = role,
            phone = phone,
            createdAt = createdAt
        )
    }
}
