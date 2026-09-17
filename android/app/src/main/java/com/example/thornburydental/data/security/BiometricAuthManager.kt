package com.example.thornburydental.data.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuthManager {

    val ALLOWED_AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun canAuthenticate(context: Context): Int {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(ALLOWED_AUTHENTICATORS)
    }

    fun isBiometricAvailable(context: Context): Boolean {
        return canAuthenticate(context) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun getBiometricStatusMessage(context: Context): String {
        return when (canAuthenticate(context)) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                "Biometric authentication is ready."
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                "No biometric features available on this device."
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                "Biometric hardware is currently unavailable."
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                "No biometrics or device credentials enrolled on this device."
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                "Security update required for biometric authentication."
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED ->
                "Biometric authentication is unsupported on this device."
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN ->
                "Biometric status is unknown."
            else ->
                "Biometric authentication is unavailable."
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock Dentara",
        subtitle: String = "Authenticate to access patient data and clinical records",
        description: String = "Use your fingerprint, face unlock, or device PIN/pattern to continue.",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: (() -> Unit)? = null
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailed?.invoke()
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
