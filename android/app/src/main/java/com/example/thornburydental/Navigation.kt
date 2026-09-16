package com.example.thornburydental

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.Patient
import com.example.thornburydental.theme.*
import com.example.thornburydental.ui.clinic.AddReportDialog
import com.example.thornburydental.ui.clinic.IssuePrescriptionDialog
import com.example.thornburydental.ui.clinic.PatientDetailChartScreen
import com.example.thornburydental.ui.clinic.PatientRosterScreen
import com.example.thornburydental.ui.clinic.ProfileScreen
import com.example.thornburydental.ui.clinic.RegisterPatientScreen
import com.example.thornburydental.ui.clinic.ScheduleScreen
import com.example.thornburydental.ui.clinic.TodayQueueScreen
import com.example.thornburydental.ui.onboarding.OnboardingWizardScreen
import com.example.thornburydental.ui.onboarding.WelcomeBrandScreen

enum class RootDestination {
    WELCOME,
    ONBOARDING_WIZARD,
    CLINIC
}

enum class AppDestination(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Default.CalendarToday),
    PATIENTS("Patients", Icons.Default.Groups),
    SCHEDULE("Schedule", Icons.Default.Event),
    PROFILE("Profile", Icons.Default.AccountCircle)
}

@Composable
fun MainNavigation() {
    val initialOnboardingDone = remember { DentalRepository.isOnboardingCompleted() }
    var rootDestination by remember {
        mutableStateOf(if (initialOnboardingDone) RootDestination.CLINIC else RootDestination.WELCOME)
    }
    var currentTab by remember { mutableStateOf(AppDestination.TODAY) }
    var activePatientId by remember { mutableStateOf<String?>(null) }
    var prescriptionTargetPatient by remember { mutableStateOf<Patient?>(null) }
    var showRegisterPatientScreen by remember { mutableStateOf(false) }
    var reportPatientTarget by remember { mutableStateOf<Patient?>(null) }

    when (rootDestination) {
        RootDestination.WELCOME -> {
            WelcomeBrandScreen(
                onGetStarted = {
                    rootDestination = RootDestination.ONBOARDING_WIZARD
                }
            )
        }
        RootDestination.ONBOARDING_WIZARD -> {
            OnboardingWizardScreen(
                onOnboardingFinished = {
                    rootDestination = RootDestination.CLINIC
                },
                onCancelToWelcome = {
                    rootDestination = RootDestination.WELCOME
                }
            )
        }
        RootDestination.CLINIC -> {
            // Centralized tab click handler: handles pop-to-root and seamless branch transitions
            fun navigateToTab(destination: AppDestination) {
                if (currentTab == destination) {
                    // Reselecting active tab -> Pop to root
                    if (reportPatientTarget != null) {
                        reportPatientTarget = null
                    }
                    if (showRegisterPatientScreen) {
                        showRegisterPatientScreen = false
                    }
                    if (activePatientId != null) {
                        activePatientId = null
                    }
                } else {
                    // Switching tabs -> Dismiss child screens and switch branch
                    reportPatientTarget = null
                    showRegisterPatientScreen = false
                    activePatientId = null
                    currentTab = destination
                }
            }

            // Hierarchical Back Handling
            // 1. Report modal
            BackHandler(enabled = reportPatientTarget != null) {
                reportPatientTarget = null
            }

            // 2. Register patient form
            BackHandler(enabled = reportPatientTarget == null && showRegisterPatientScreen) {
                showRegisterPatientScreen = false
            }

            // 3. Patient detail chart
            BackHandler(enabled = reportPatientTarget == null && !showRegisterPatientScreen && activePatientId != null) {
                activePatientId = null
            }

            // 4. Tab stack: Back from non-TODAY root tabs navigates to TODAY
            BackHandler(enabled = reportPatientTarget == null && !showRegisterPatientScreen && activePatientId == null && currentTab != AppDestination.TODAY) {
                currentTab = AppDestination.TODAY
            }

            // 5. Initial onboarding exit to welcome
            BackHandler(enabled = !initialOnboardingDone && reportPatientTarget == null && !showRegisterPatientScreen && activePatientId == null && currentTab == AppDestination.TODAY) {
                rootDestination = RootDestination.WELCOME
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = ThornburyCanvas,
                bottomBar = {
                    // Persistent navigation bar across all pages
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
                            AppDestination.entries.forEach { destination ->
                                val isSelected = currentTab == destination
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { navigateToTab(destination) },
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
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when {
                        showRegisterPatientScreen -> {
                            RegisterPatientScreen(
                                onBack = { showRegisterPatientScreen = false },
                                onRegistered = { newPatient ->
                                    showRegisterPatientScreen = false
                                    activePatientId = newPatient.id
                                }
                            )
                        }
                        activePatientId != null -> {
                            PatientDetailChartScreen(
                                patientId = activePatientId!!,
                                onBack = { activePatientId = null },
                                onOpenIssuePrescription = { p -> prescriptionTargetPatient = p },
                                onOpenAddReportScreen = { p -> reportPatientTarget = p }
                            )
                        }
                        else -> {
                            when (currentTab) {
                                AppDestination.TODAY -> TodayQueueScreen(
                                    onOpenPatientChart = { patientId -> activePatientId = patientId }
                                )
                                AppDestination.PATIENTS -> PatientRosterScreen(
                                    onSelectPatient = { patientId -> activePatientId = patientId },
                                    onNavigateToRegisterPatient = { showRegisterPatientScreen = true }
                                )
                                AppDestination.SCHEDULE -> ScheduleScreen(
                                    onOpenPatientChart = { patientId -> activePatientId = patientId },
                                    onNavigateToRegisterPatient = { showRegisterPatientScreen = true }
                                )
                                AppDestination.PROFILE -> ProfileScreen(
                                    onSignOut = { rootDestination = RootDestination.WELCOME }
                                )
                            }
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

            // Modal Add Report Dialog (if ever invoked globally)
            reportPatientTarget?.let { patient ->
                AddReportDialog(
                    patient = patient,
                    onDismiss = { reportPatientTarget = null },
                    onSave = { kind, title, clinician, summary, attachments ->
                        DentalRepository.addReport(
                            patientId = patient.id,
                            kind = kind,
                            title = title,
                            summary = summary,
                            clinicianName = clinician,
                            attachments = attachments
                        )
                        reportPatientTarget = null
                    }
                )
            }
        }
    }
}
