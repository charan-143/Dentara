package com.example.thornburydental

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.Patient
import com.example.thornburydental.theme.*
import com.example.thornburydental.ui.clinic.IssuePrescriptionDialog
import com.example.thornburydental.ui.clinic.PatientDetailChartScreen
import com.example.thornburydental.ui.clinic.PatientRosterScreen
import com.example.thornburydental.ui.clinic.PrescriptionsManagementScreen
import com.example.thornburydental.ui.clinic.TodayQueueScreen
import com.example.thornburydental.ui.publicsite.PracticeHomeScreen

// Removed Practice tab per user specification - purely clinical workspace
enum class AppDestination(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Default.CalendarToday),
    PATIENTS("Patients", Icons.Default.FolderShared),
    PRESCRIPTIONS("Prescriptions", Icons.Default.Medication)
}

@Composable
fun MainNavigation() {
    var currentTab by remember { mutableStateOf(AppDestination.TODAY) }
    var activePatientId by remember { mutableStateOf<String?>(null) }
    var prescriptionTargetPatient by remember { mutableStateOf<Patient?>(null) }

    // Intercept back button if currently viewing a detailed patient chart
    if (activePatientId != null) {
        BackHandler {
            activePatientId = null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ThornburyCanvas,
        bottomBar = {
            if (activePatientId == null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ThornburyCanvas,
                    border = BorderStroke(1.dp, ThornburyHairline)
                ) {
                    NavigationBar(
                        containerColor = ThornburyCanvas,
                        contentColor = ThornburyInk,
                        tonalElevation = 0.dp
                    ) {
                        AppDestination.values().forEach { destination ->
                            val isSelected = currentTab == destination
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentTab = destination },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = destination.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ThornburyPrimary,
                                    selectedTextColor = ThornburyPrimary,
                                    unselectedIconColor = ThornburyMuted,
                                    unselectedTextColor = ThornburyMuted,
                                    indicatorColor = ThornburySurfaceSoft
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            if (activePatientId != null) {
                PatientDetailChartScreen(
                    patientId = activePatientId!!,
                    onBack = { activePatientId = null },
                    onOpenIssuePrescription = { p -> prescriptionTargetPatient = p }
                )
            } else {
                when (currentTab) {
                    AppDestination.TODAY -> TodayQueueScreen(
                        onOpenPatientChart = { patientId -> activePatientId = patientId }
                    )
                    AppDestination.PATIENTS -> PatientRosterScreen(
                        onSelectPatient = { patientId -> activePatientId = patientId }
                    )
                    AppDestination.PRESCRIPTIONS -> PrescriptionsManagementScreen(
                        onOpenPatientChart = { patientId -> activePatientId = patientId }
                    )
                }
            }
        }
    }

    // Modal prescribing dialog
    prescriptionTargetPatient?.let { patient ->
        IssuePrescriptionDialog(
            patient = patient,
            onDismiss = { prescriptionTargetPatient = null },
            onSuccess = { prescriptionTargetPatient = null }
        )
    }
}
