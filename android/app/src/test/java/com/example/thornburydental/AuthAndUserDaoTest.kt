package com.example.thornburydental

import com.example.thornburydental.data.AuthRepository
import com.example.thornburydental.data.User
import com.example.thornburydental.data.UserRole
import com.example.thornburydental.data.db.ThornburyDbHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.security.MessageDigest

class AuthAndUserDaoTest {

    @Test
    fun testUserRoleEnum() {
        assertEquals(3, UserRole.values().size)
        assertEquals(UserRole.CLINICIAN, UserRole.valueOf("CLINICIAN"))
        assertEquals(UserRole.PATIENT, UserRole.valueOf("PATIENT"))
        assertEquals(UserRole.RECEPTIONIST, UserRole.valueOf("RECEPTIONIST"))
    }

    @Test
    fun testUserModel() {
        val now = System.currentTimeMillis()
        val user = User(
            id = "usr-001",
            email = "test@example.com",
            name = "Test User",
            role = UserRole.CLINICIAN,
            phone = "+44 20 7946 0912",
            createdAt = now
        )

        assertEquals("usr-001", user.id)
        assertEquals("test@example.com", user.email)
        assertEquals("Test User", user.name)
        assertEquals(UserRole.CLINICIAN, user.role)
        assertEquals("+44 20 7946 0912", user.phone)
        assertEquals(now, user.createdAt)
    }

    @Test
    fun testSha256PasswordHashing() {
        fun hashPassword(password: String, salt: String = "ThornburySalt2026"): String {
            val md = MessageDigest.getInstance("SHA-256")
            val input = "$salt$password".toByteArray(Charsets.UTF_8)
            val digest = md.digest(input)
            return digest.joinToString("") { "%02x".format(it) }
        }

        val hash1 = hashPassword("password123")
        val hash2 = hashPassword("password123")
        val hash3 = hashPassword("differentPassword")
        val hashWithDiffSalt = hashPassword("password123", "CustomSalt")

        assertEquals(64, hash1.length)
        assertEquals(hash1, hash2)
        assertNotEquals(hash1, hash3)
        assertNotEquals(hash1, hashWithDiffSalt)
    }

    @Test
    fun testUserTableDbConstants() {
        assertEquals("users", ThornburyDbHelper.TABLE_USERS)
        assertEquals("id", ThornburyDbHelper.COL_USERS_ID)
        assertEquals("email", ThornburyDbHelper.COL_USERS_EMAIL)
        assertEquals("password_hash", ThornburyDbHelper.COL_USERS_PASSWORD_HASH)
        assertEquals("name", ThornburyDbHelper.COL_USERS_NAME)
        assertEquals("role", ThornburyDbHelper.COL_USERS_ROLE)
        assertEquals("phone", ThornburyDbHelper.COL_USERS_PHONE)
        assertEquals("created_at", ThornburyDbHelper.COL_USERS_CREATED_AT)
    }

    @Test
    fun testAuthRepositoryInitialStateAndLogout() {
        AuthRepository.logout()
        assertNull(AuthRepository.currentUser.value)
        assertFalse(AuthRepository.isLoggedIn)
    }

    @Test
    fun testEmailAndRegistrationValidationRules() {
        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        
        // Valid emails
        assert(emailRegex.matches("dr.halvorsen@thornburydental.com"))
        assert(emailRegex.matches("rosalind.achebe@example.org"))
        assert(emailRegex.matches("user.name+tag@sub.domain.co.uk"))

        // Invalid emails
        assertFalse(emailRegex.matches("not-an-email"))
        assertFalse(emailRegex.matches("@domain.com"))
        assertFalse(emailRegex.matches("user@"))
        assertFalse(emailRegex.matches("user@domain"))

        // Password length check (>= 6)
        fun isPasswordValid(password: String): Boolean = password.length >= 6
        assert(isPasswordValid("password123"))
        assert(isPasswordValid("123456"))
        assertFalse(isPasswordValid("12345"))
        assertFalse(isPasswordValid(""))
    }
}
