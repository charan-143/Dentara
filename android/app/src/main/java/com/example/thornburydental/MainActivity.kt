package com.example.thornburydental

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.theme.ThornburyDentalTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val isDebuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    if (!isDebuggable) {
      window.setFlags(
          android.view.WindowManager.LayoutParams.FLAG_SECURE,
          android.view.WindowManager.LayoutParams.FLAG_SECURE
      )
    }

    enableEdgeToEdge()
    setContent {
      val isDarkModeEnabled by DentalRepository.isDarkModeEnabled.collectAsState()
      ThornburyDentalTheme(darkTheme = isDarkModeEnabled) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() }
      }
    }
  }
}
