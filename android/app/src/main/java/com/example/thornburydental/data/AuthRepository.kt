package com.example.thornburydental.data

import com.example.thornburydental.data.db.LocalDatabaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository managing user authentication state, credential verification, registration,
 * and role-based quick login flows for Thornbury Dental.
 */
object AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    val isLoggedIn: Boolean
        get() = _currentUser.value != null

    /**
     * Authenticate with email and password, updating [currentUser] on success.
     */
    fun login(email: String, password: String): Result<User> {
        if (!LocalDatabaseManager.isInitialized) {
            return Result.failure(IllegalStateException("Database is not initialized"))
        }
        val user = LocalDatabaseManager.userDao.authenticate(email, password)
        return if (user != null) {
            _currentUser.value = user
            Result.success(user)
        } else {
            Result.failure(IllegalArgumentException("Invalid email or password"))
        }
    }

    /**
     * Register a new user and log them in immediately.
     */
    fun register(
        email: String,
        password: String,
        name: String,
        role: UserRole,
        phone: String = ""
    ): Result<User> {
        if (!LocalDatabaseManager.isInitialized) {
            return Result.failure(IllegalStateException("Database is not initialized"))
        }
        val result = LocalDatabaseManager.userDao.registerUser(
            email = email,
            password = password,
            name = name,
            role = role,
            phone = phone
        )
        result.onSuccess { user ->
            _currentUser.value = user
        }
        return result
    }

    /**
     * Clear the active session and log the current user out.
     */
    fun logout() {
        _currentUser.value = null
    }

    /**
     * Update the display name of the current logged-in user.
     */
    fun updateCurrentUserName(newName: String) {
        val current = _currentUser.value
        if (current != null) {
            _currentUser.value = current.copy(name = newName)
        } else {
            _currentUser.value = User(
                id = "c1",
                email = "dr.halvorsen@dentara.com",
                name = newName,
                role = UserRole.CLINICIAN
            )
        }
    }

    /**
     * Quick login as default clinician (Dr. Ingrid Halvorsen).
     */
    fun quickLoginAsClinician(): Result<User> {
        if (!LocalDatabaseManager.isInitialized) {
            return Result.failure(IllegalStateException("Database is not initialized"))
        }
        LocalDatabaseManager.userDao.seedDefaultUsersIfEmpty()
        val user = LocalDatabaseManager.userDao.getUserByEmail("dr.halvorsen@dentara.com")
            ?: LocalDatabaseManager.userDao.authenticate("dr.halvorsen@dentara.com", "password123")
        return if (user != null) {
            _currentUser.value = user
            Result.success(user)
        } else {
            Result.failure(IllegalStateException("Clinician user not found"))
        }
    }

    /**
     * Quick login as default patient (Rosalind Achebe).
     */
    fun quickLoginAsPatient(): Result<User> {
        if (!LocalDatabaseManager.isInitialized) {
            return Result.failure(IllegalStateException("Database is not initialized"))
        }
        LocalDatabaseManager.userDao.seedDefaultUsersIfEmpty()
        val user = LocalDatabaseManager.userDao.getUserByEmail("rosalind.achebe@example.org")
            ?: LocalDatabaseManager.userDao.authenticate("rosalind.achebe@example.org", "password123")
        return if (user != null) {
            _currentUser.value = user
            Result.success(user)
        } else {
            Result.failure(IllegalStateException("Patient user not found"))
        }
    }
}
