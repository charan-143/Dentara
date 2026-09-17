package com.example.thornburydental

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

            val windowSizeClass = rememberWindowWidthSizeClass()
            val isCompact = windowSizeClass == WindowWidthSizeClass.COMPACT

            CompositionLocalProvider(LocalWindowWidthSizeClass provides windowSizeClass) {
                @Composable
                fun ScreenContentHost(modifier: Modifier = Modifier) {
                    val activeKey = when {
                        showRegisterPatientScreen -> "REGISTER"
                        activePatientId != null -> "PATIENT_${activePatientId}"
                        else -> "TAB_${currentTab.name}"
                    }

                    AnimatedContent(
                        targetState = activeKey,
                        transitionSpec = {
                            val targetIsChild = targetState.startsWith("PATIENT_") || targetState == "REGISTER"
                            val initialIsChild = initialState.startsWith("PATIENT_") || initialState == "REGISTER"

                            if (targetIsChild) {
                                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                                        fadeIn(animationSpec = tween(280)) togetherWith
                                slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                                        fadeOut(animationSpec = tween(200))
                            } else if (initialIsChild) {
                                slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                                        fadeIn(animationSpec = tween(240)) togetherWith
                                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                                        fadeOut(animationSpec = tween(200))
                            } else {
                                val targetTab = runCatching { AppDestination.valueOf(targetState.removePrefix("TAB_")) }.getOrNull()
                                val initialTab = runCatching { AppDestination.valueOf(initialState.removePrefix("TAB_")) }.getOrNull()

                                if (targetTab != null && initialTab != null && targetTab.ordinal != initialTab.ordinal) {
                                    if (targetTab.ordinal > initialTab.ordinal) {
                                        (slideInHorizontally(initialOffsetX = { (it * 0.20f).toInt() }, animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                                                fadeIn(animationSpec = tween(240, easing = LinearOutSlowInEasing))) togetherWith
                                        (slideOutHorizontally(targetOffsetX = { (-it * 0.20f).toInt() }, animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                                                fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)))
                                    } else {
                                        (slideInHorizontally(initialOffsetX = { (-it * 0.20f).toInt() }, animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                                                fadeIn(animationSpec = tween(240, easing = LinearOutSlowInEasing))) togetherWith
                                        (slideOutHorizontally(targetOffsetX = { (it * 0.20f).toInt() }, animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                                                fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)))
                                    }
                                } else {
                                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(180))
                                }
                            }
                        },
                        modifier = modifier.fillMaxSize(),
                        label = "ScreenNavigationTransition"
                    ) { key ->
                        when {
                            key == "REGISTER" -> {
                                RegisterPatientScreen(
                                    onBack = { showRegisterPatientScreen = false },
                                    onRegistered = { newPatient ->
                                        showRegisterPatientScreen = false
                                        activePatientId = newPatient.id
                                    }
                                )
                            }
                            key.startsWith("PATIENT_") -> {
                                val pId = key.removePrefix("PATIENT_")
                                PatientDetailChartScreen(
                                    patientId = pId,
                                    onBack = { activePatientId = null },
                                    onOpenIssuePrescription = { p -> prescriptionTargetPatient = p },
                                    onOpenAddReportScreen = { p -> reportPatientTarget = p }
                                )
                            }
                            key.startsWith("TAB_") -> {
                                val tabName = key.removePrefix("TAB_")
                                val dest = runCatching { AppDestination.valueOf(tabName) }.getOrDefault(currentTab)
                                when (dest) {
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
                            else -> {
                                TodayQueueScreen(
                                    onOpenPatientChart = { patientId -> activePatientId = patientId }
                                )
                            }
                        }
                    }
                }

                if (isCompact) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = ThornburyCanvas,
                        bottomBar = {
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
                        ScreenContentHost(modifier = Modifier.padding(innerPadding))
                    }
                } else {
                    // Tablet & Wide Screen Navigation Rail layout
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ThornburyCanvas)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(92.dp),
                            color = ThornburyCanvas,
                            border = BorderStroke(1.dp, ThornburyHairline)
                        ) {
                            NavigationRail(
                                containerColor = ThornburyCanvas,
                                contentColor = ThornburyInk,
                                header = {
                                    Column(
                                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(top = 18.dp, bottom = 16.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = ThornburyPrimaryWash,
                                            border = BorderStroke(1.dp, ThornburyHairline)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MedicalServices,
                                                contentDescription = "Dentara",
                                                tint = ThornburyPrimary,
                                                modifier = Modifier
                                                    .padding(8.dp)
                                                    .size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "DENTARA",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 1.2.sp
                                            ),
                                            color = ThornburyInk
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                Spacer(modifier = Modifier.weight(1f))
                                AppDestination.entries.forEach { destination ->
                                    val isSelected = currentTab == destination
                                    NavigationRailItem(
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
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = ThornburyPrimary,
                                            selectedTextColor = ThornburyPrimary,
                                            unselectedIconColor = ThornburyMuted,
                                            unselectedTextColor = ThornburyMuted,
                                            indicatorColor = ThornburySurfaceSoft
                                        ),
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1.5f))
                            }
                        }
                        ScreenContentHost(modifier = Modifier.weight(1f))
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
