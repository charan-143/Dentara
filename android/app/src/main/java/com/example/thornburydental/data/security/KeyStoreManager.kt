package com.example.thornburydental.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages secure AES-256 passphrase generation and Android KeyStore key management
 * for SQLCipher database encryption at rest.
 */
object KeyStoreManager {

    private const val TAG = "KeyStoreManager"
    private const val KEY_ALIAS = "dentara_db_passphrase_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val PREFS_NAME = "thornbury_security_prefs"
    private const val KEY_ENCRYPTED_PASSPHRASE = "encrypted_db_passphrase"
    private const val KEY_PASSPHRASE_IV = "db_passphrase_iv"
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128

    @Volatile
    private var cachedPassphrase: String? = null

    /**
     * Retrieve or generate the 256-bit database encryption passphrase.
     * The passphrase is securely encrypted using an AES-256 key stored in the Android KeyStore.
     */
    @Synchronized
    fun getPassphrase(context: Context): String {
        cachedPassphrase?.let { return it }

        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedBase64 = prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null)
        val ivBase64 = prefs.getString(KEY_PASSPHRASE_IV, null)

        if (!encryptedBase64.isNullOrEmpty() && !ivBase64.isNullOrEmpty()) {
            try {
                val encryptedBytes = Base64.decode(encryptedBase64, Base64.DEFAULT)
                val iv = Base64.decode(ivBase64, Base64.DEFAULT)
                val secretKey = getOrCreateMasterKey()

                val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

                val decryptedBytes = cipher.doFinal(encryptedBytes)
                val passphrase = String(decryptedBytes, Charsets.UTF_8)
                cachedPassphrase = passphrase
                return passphrase
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decrypt existing database passphrase from KeyStore, regenerating...", e)
            }
        }

        // Generate a new secure 256-bit (32-byte) random passphrase
        val passphrase = generateSecurePassphrase()
        try {
            val secretKey = getOrCreateMasterKey()
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(passphrase.toByteArray(Charsets.UTF_8))

            prefs.edit()
                .putString(KEY_ENCRYPTED_PASSPHRASE, Base64.encodeToString(encryptedBytes, Base64.DEFAULT))
                .putString(KEY_PASSPHRASE_IV, Base64.encodeToString(iv, Base64.DEFAULT))
                .apply()

            cachedPassphrase = passphrase
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store encrypted passphrase in KeyStore", e)
            cachedPassphrase = passphrase
        }

        return passphrase
    }

    /**
     * Retrieve the passphrase as a CharArray for SQLCipher APIs.
     */
    fun getPassphraseCharArray(context: Context): CharArray {
        return getPassphrase(context).toCharArray()
    }

    /**
     * Clear cached passphrase in memory.
     */
    fun clearCache() {
        cachedPassphrase = null
    }

    private fun generateSecurePassphrase(): String {
        val randomBytes = ByteArray(32) // 256 bits
        SecureRandom().nextBytes(randomBytes)
        return Base64.encodeToString(randomBytes, Base64.NO_WRAP)
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keyGenSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }
}
