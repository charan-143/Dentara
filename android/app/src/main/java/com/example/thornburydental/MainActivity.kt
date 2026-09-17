package com.example.thornburydental

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.security.AppSessionLifecycleObserver
import com.example.thornburydental.reminder.ReminderManager
import com.example.thornburydental.theme.ThornburyDentalTheme
import com.example.thornburydental.ui.components.BiometricLockOverlay

class MainActivity : FragmentActivity() {

  private val notificationPermissionLauncher = registerForActivityResult(
      ActivityResultContracts.RequestPermission()
  ) { isGranted ->
      if (isGranted) {
          ReminderManager.rescheduleAllActiveReminders(this)
      }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val isDebuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    if (!isDebuggable) {
      window.setFlags(
          android.view.WindowManager.LayoutParams.FLAG_SECURE,
          android.view.WindowManager.LayoutParams.FLAG_SECURE
      )
    }

    // Initialize notification channels and schedule active reminders
    ReminderManager.createNotificationChannels(this)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            ReminderManager.rescheduleAllActiveReminders(this)
        }
    } else {
        ReminderManager.rescheduleAllActiveReminders(this)
    }

    DentalRepository.initializePreferencesSynchronously(this)
    enableEdgeToEdge()
    setContent {
      val isDarkModeEnabled by DentalRepository.isDarkModeEnabled.collectAsState()
      val isLocked by AppSessionLifecycleObserver.isLocked.collectAsState()

      ThornburyDentalTheme(darkTheme = isDarkModeEnabled) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          Box(modifier = Modifier.fillMaxSize()) {
            MainNavigation()
            if (isLocked) {
              BiometricLockOverlay(
                onUnlockSuccess = {
                  AppSessionLifecycleObserver.unlockSession()
                }
              )
            }
          }
        }
      }
    }
  }
}
