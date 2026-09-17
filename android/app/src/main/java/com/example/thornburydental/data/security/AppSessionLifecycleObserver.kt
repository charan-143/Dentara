package com.example.thornburydental.data.security

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.thornburydental.data.db.LocalDatabaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppSessionLifecycleObserver : DefaultLifecycleObserver {

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var lastBackgroundTimeMs: Long = 0L
    private var isBackgrounded: Boolean = false

    // Timeout threshold in milliseconds for background inactivity (0 = lock immediately on background resume)
    var lockTimeoutMs: Long = 0L

    var isGatingEnabled: Boolean = true

    fun initPreferences(context: Context) {
        val prefs = context.getSharedPreferences("dentara_app_prefs", Context.MODE_PRIVATE)
        isGatingEnabled = prefs.getBoolean("biometric_auth_enabled", true)
        val timeoutMin = prefs.getInt("biometric_lock_timeout_min", 0)
        lockTimeoutMs = timeoutMin * 60 * 1000L
        lastBackgroundTimeMs = prefs.getLong("last_background_time_ms", 0L)

        if (!isGatingEnabled) {
            _isLocked.value = false
            return
        }

        if (lastBackgroundTimeMs > 0L) {
            val elapsedTime = System.currentTimeMillis() - lastBackgroundTimeMs
            if (elapsedTime >= lockTimeoutMs) {
                _isLocked.value = true
            } else {
                _isLocked.value = false
            }
        } else {
            // First launch with biometrics enabled -> require unlock
            _isLocked.value = true
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (!isGatingEnabled) return
        val now = System.currentTimeMillis()
        lastBackgroundTimeMs = now
        isBackgrounded = true

        LocalDatabaseManager.appContext?.getSharedPreferences("dentara_app_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putLong("last_background_time_ms", now)
            ?.apply()

        // Pre-emptively lock session on backgrounding only if timeout is Immediately (0L)
        if (lockTimeoutMs == 0L) {
            _isLocked.value = true
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (!isGatingEnabled) return

        if (lastBackgroundTimeMs == 0L) {
            LocalDatabaseManager.appContext?.getSharedPreferences("dentara_app_prefs", Context.MODE_PRIVATE)?.let { prefs ->
                lastBackgroundTimeMs = prefs.getLong("last_background_time_ms", 0L)
            }
        }

        if (isBackgrounded || lastBackgroundTimeMs > 0L) {
            val elapsedTime = System.currentTimeMillis() - lastBackgroundTimeMs
            if (elapsedTime >= lockTimeoutMs) {
                _isLocked.value = true
            }
            isBackgrounded = false
        }
    }

    fun lockSession() {
        _isLocked.value = true
    }

    fun unlockSession() {
        _isLocked.value = false
    }

    fun setLocked(locked: Boolean) {
        _isLocked.value = locked
    }
}
