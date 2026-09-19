package com.example.thornburydental.ui.clinic

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.thornburydental.data.AuthRepository
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.ToothNumberingSystem
import com.example.thornburydental.data.UserRole
import com.example.thornburydental.reminder.ReminderManager
import com.example.thornburydental.theme.*

/**
 * Redesigned Clinician Profile & Practice Settings Screen.
 * Displays clinician credentials, schedule reminders, clinical ergonomics,
 * local database telemetry, data privacy/FLAG_SECURE status, and account actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clinicianName by DentalRepository.clinicianDisplayName.collectAsState()
    val currentUser by AuthRepository.currentUser.collectAsState()
    val displayName = clinicianName.ifBlank { currentUser?.name ?: "Dr. Ingrid Halvorsen" }
    val roleLabel = when (currentUser?.role) {
        UserRole.CLINICIAN -> "Lead Dental Surgeon"
        UserRole.RECEPTIONIST -> "Practice Coordinator"
        UserRole.PATIENT -> "Patient"
        null -> "Lead Clinician"
    }

    val initials = remember(displayName) {
        val words = displayName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        when {
            words.size >= 2 -> "${words.first().first().uppercaseChar()}${words.last().first().uppercaseChar()}"
            words.isNotEmpty() -> words.first().take(2).uppercase()
            else -> "IH"
        }
    }

    val isDarkModeEnabled by DentalRepository.isDarkModeEnabled.collectAsState()
    val isBiometricAuthEnabled by DentalRepository.isBiometricAuthEnabled.collectAsState()
    val isVoiceChartingEnabled by DentalRepository.isVoiceChartingEnabled.collectAsState()
    val toothNumberingSystem by DentalRepository.toothNumberingSystem.collectAsState()
    val biometricLockTimeoutMinutes by DentalRepository.biometricLockTimeoutMinutes.collectAsState()
    val areNotificationsEnabled by DentalRepository.appointmentRemindersEnabled.collectAsState()
    val morningReminderEnabled by DentalRepository.morningReminderEnabled.collectAsState()
    val morningReminderTime by DentalRepository.morningReminderTime.collectAsState()
    val chairsideReminderDefaultMin by DentalRepository.chairsideReminderDefaultMin.collectAsState()

    val patients by DentalRepository.patients.collectAsState()
    val treatmentPlans by DentalRepository.treatmentPlans.collectAsState()
    val reports by DentalRepository.reports.collectAsState()
    val appointments by DentalRepository.appointments.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ThornburyCanvas)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ThornburyCanvas,
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Text(
                    text = "Clinician Profile & Practice",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = ThornburyInk
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Operatory management, clinical ergonomics, and system preferences",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThornburyMuted
                )
            }
        }

        val windowSizeClass = LocalWindowWidthSizeClass.current
        val isCompact = windowSizeClass == WindowWidthSizeClass.COMPACT

        if (isCompact) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ClinicianIdentityCard(
                    initials = initials,
                    displayName = displayName,
                    roleLabel = roleLabel,
                    onEditClick = { showEditProfileDialog = true }
                )

                ScheduleRemindersCard(
                    morningReminderEnabled = morningReminderEnabled,
                    morningReminderTime = morningReminderTime,
                    chairsideReminderDefaultMin = chairsideReminderDefaultMin,
                    onMorningReminderChange = { enabled, time -> DentalRepository.updateMorningReminderSettings(enabled, time) },
                    onChairsideReminderChange = { min -> DentalRepository.updateChairsideReminderDefault(min) },
                    context = context
                )

                ClinicalErgonomicsCard(
                    isDarkModeEnabled = isDarkModeEnabled,
                    areNotificationsEnabled = areNotificationsEnabled,
                    isVoiceChartingEnabled = isVoiceChartingEnabled,
                    toothNumberingSystem = toothNumberingSystem,
                    onDarkModeChange = { DentalRepository.setDarkModeEnabled(it) },
                    onNotificationsChange = { DentalRepository.setAppointmentRemindersEnabled(it) },
                    onVoiceChartingChange = { DentalRepository.setVoiceChartingEnabled(it) },
                    onToothNumberingChange = { DentalRepository.setToothNumberingSystem(it) }
                )

                SecurityShieldTelemetryCard(
                    patientsCount = patients.size,
                    plansCount = treatmentPlans.size,
                    reportsCount = reports.size,
                    visitsCount = appointments.size,
                    isBiometricAuthEnabled = isBiometricAuthEnabled,
                    biometricLockTimeoutMinutes = biometricLockTimeoutMinutes,
                    onBiometricAuthChange = { DentalRepository.setBiometricAuthEnabled(it) },
                    onBiometricTimeoutChange = { DentalRepository.setBiometricLockTimeoutMinutes(it) }
                )

                AccountSecurityCard(
                    onEditProfileClick = { showEditProfileDialog = true }
                )

                Spacer(modifier = Modifier.height(28.dp))
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ClinicianIdentityCard(
                        initials = initials,
                        displayName = displayName,
                        roleLabel = roleLabel,
                        onEditClick = { showEditProfileDialog = true }
                    )

                    ClinicalErgonomicsCard(
                        isDarkModeEnabled = isDarkModeEnabled,
                        areNotificationsEnabled = areNotificationsEnabled,
                        isVoiceChartingEnabled = isVoiceChartingEnabled,
                        toothNumberingSystem = toothNumberingSystem,
                        onDarkModeChange = { DentalRepository.setDarkModeEnabled(it) },
                        onNotificationsChange = { DentalRepository.setAppointmentRemindersEnabled(it) },
                        onVoiceChartingChange = { DentalRepository.setVoiceChartingEnabled(it) },
                        onToothNumberingChange = { DentalRepository.setToothNumberingSystem(it) }
                    )

                    AccountSecurityCard(
                        onEditProfileClick = { showEditProfileDialog = true }
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ScheduleRemindersCard(
                        morningReminderEnabled = morningReminderEnabled,
                        morningReminderTime = morningReminderTime,
                        chairsideReminderDefaultMin = chairsideReminderDefaultMin,
                        onMorningReminderChange = { enabled, time -> DentalRepository.updateMorningReminderSettings(enabled, time) },
                        onChairsideReminderChange = { min -> DentalRepository.updateChairsideReminderDefault(min) },
                        context = context
                    )

                SecurityShieldTelemetryCard(
                    patientsCount = patients.size,
                    plansCount = treatmentPlans.size,
                    reportsCount = reports.size,
                    visitsCount = appointments.size,
                    isBiometricAuthEnabled = isBiometricAuthEnabled,
                    biometricLockTimeoutMinutes = biometricLockTimeoutMinutes,
                    onBiometricAuthChange = { DentalRepository.setBiometricAuthEnabled(it) },
                    onBiometricTimeoutChange = { DentalRepository.setBiometricLockTimeoutMinutes(it) }
                )
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Dialogs: Edit Profile, Change Password
    // -------------------------------------------------------------------------

    if (showEditProfileDialog) {
        var clinicianNameInput by remember { mutableStateOf(displayName) }
        var clinicianPhone by remember { mutableStateOf(currentUser?.phone ?: "+1 (503) 224-7700") }
        var clinicianEmail by remember { mutableStateOf(currentUser?.email ?: "halvorsen@thornburydental.com") }

        Dialog(
            onDismissRequest = { showEditProfileDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .adaptiveDialogWidth(500.dp)
                    .wrapContentHeight()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Edit Clinician Profile", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                        IconButton(onClick = { showEditProfileDialog = false }, modifier = Modifier.size(28.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = ThornburyMuted)
                        }
                    }

                    OutlinedTextField(
                        value = clinicianNameInput,
                        onValueChange = { clinicianNameInput = it },
                        label = { Text("Clinician Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = thornburyTextFieldColors()
                    )

                    OutlinedTextField(
                        value = clinicianPhone,
                        onValueChange = { clinicianPhone = it },
                        label = { Text("Surgery Contact Telephone") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = thornburyTextFieldColors()
                    )

                    OutlinedTextField(
                        value = clinicianEmail,
                        onValueChange = { clinicianEmail = it },
                        label = { Text("Surgery Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = thornburyTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showEditProfileDialog = false }) {
                            Text("Cancel", color = ThornburyMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (clinicianNameInput.isNotBlank()) {
                                    DentalRepository.updateClinicianName(clinicianNameInput)
                                }
                                showEditProfileDialog = false
                                Toast.makeText(context, "Clinician profile details updated", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClinicianIdentityCard(
    initials: String,
    displayName: String,
    roleLabel: String,
    onEditClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, ThornburyHairline), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = ThornburyCanvas),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(ThornburyPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = displayName,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = ThornburyInk
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "BDS, MSc Oral Surgery & Implantology",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = ThornburyPrimaryText
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ThornburyPrimaryWash,
                    border = BorderStroke(1.dp, ThornburyPrimary.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "GDC #284910",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyPrimaryText
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ThornburySuccessWash,
                    border = BorderStroke(1.dp, ThornburySuccess.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ThornburySuccess)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "In Surgery • Active",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ThornburySuccess
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = ThornburyHairlineSoft)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = null,
                        tint = ThornburyPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Clinical Role:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThornburyMuted
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = roleLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                TextButton(
                    onClick = onEditClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = ThornburyPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Edit Name & Info",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleRemindersCard(
    morningReminderEnabled: Boolean,
    morningReminderTime: String,
    chairsideReminderDefaultMin: Int,
    onMorningReminderChange: (Boolean, String) -> Unit,
    onChairsideReminderChange: (Int) -> Unit,
    context: android.content.Context
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = ThornburyPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Schedule Reminders & Daily Briefing", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Automated morning patient briefing and chairside arrival alerts", style = MaterialTheme.typography.bodySmall, color = ThornburyMuted)

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Morning Schedule Briefing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Daily Morning Briefing", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                    Text(text = "Receive an agenda notification every morning with today's booked patients", style = MaterialTheme.typography.bodySmall, color = ThornburyMuted)
                }
                Switch(
                    checked = morningReminderEnabled,
                    onCheckedChange = { enabled ->
                        onMorningReminderChange(enabled, morningReminderTime)
                    }
                )
            }

            if (morningReminderEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ThornburyCanvas,
                    border = BorderStroke(1.dp, ThornburyHairlineSoft),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Alarm, contentDescription = null, tint = ThornburyPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Briefing Time", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                            }

                            OutlinedButton(
                                onClick = {
                                    val parts = morningReminderTime.split(":")
                                    val initialHour = parts.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull() ?: 8
                                    val initialMinute = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            val formatted = String.format(java.util.Locale.US, "%02d:%02d", hourOfDay, minute)
                                            onMorningReminderChange(true, formatted)
                                        },
                                        initialHour,
                                        initialMinute,
                                        false
                                    ).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                border = BorderStroke(1.dp, ThornburyPrimary.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = ReminderManager.formatTime12Hour(morningReminderTime),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = ThornburyPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick presets
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(listOf("07:00", "07:30", "08:00", "08:30", "09:00")) { presetTime ->
                                val isSelected = morningReminderTime == presetTime
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onMorningReminderChange(true, presetTime) },
                                    label = {
                                        Text(
                                            text = ReminderManager.formatTime12Hour(presetTime),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ThornburyPrimaryWash,
                                        selectedLabelColor = ThornburyPrimaryText
                                    )
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = ThornburyHairlineSoft, modifier = Modifier.padding(vertical = 12.dp))

            // 2. Patient Chairside Arrival Alert Lead Time
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Patient Arrival Alert Lead Time", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                Text(text = "Default notification window before appointment time when patient arrival reminder is enabled", style = MaterialTheme.typography.bodySmall, color = ThornburyMuted)

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        listOf(
                            10 to "10 min",
                            15 to "15 min",
                            30 to "30 min",
                            45 to "45 min",
                            60 to "1 hour"
                        )
                    ) { (leadMin, label) ->
                        val isSelected = chairsideReminderDefaultMin == leadMin
                        FilterChip(
                            selected = isSelected,
                            onClick = { onChairsideReminderChange(leadMin) },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ThornburyPrimaryWash,
                                selectedLabelColor = ThornburyPrimaryText
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClinicalErgonomicsCard(
    isDarkModeEnabled: Boolean,
    areNotificationsEnabled: Boolean,
    isVoiceChartingEnabled: Boolean,
    toothNumberingSystem: ToothNumberingSystem,
    onDarkModeChange: (Boolean) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onVoiceChartingChange: (Boolean) -> Unit,
    onToothNumberingChange: (ToothNumberingSystem) -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = ThornburyPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Clinical Ergonomics & Interface", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dark Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Operatory Dark Mode", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                    Text(text = "High-contrast theme optimized for darkened operatory suites", style = MaterialTheme.typography.bodySmall, color = ThornburyMuted)
                }
                Switch(
                    checked = isDarkModeEnabled,
                    onCheckedChange = onDarkModeChange
                )
            }

            HorizontalDivider(color = ThornburyHairlineSoft, modifier = Modifier.padding(vertical = 10.dp))

            // Hands-Free Voice Charting (off by default; opt in per device)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = ThornburyPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Hands-Free Voice Charting", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                    }
                    Text(text = "On-device dictation for periodontal depths and clinical notes. Needs a one-time 57 MB language download; audio never leaves this device.", style = MaterialTheme.typography.bodySmall, color = ThornburyMuted)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = isVoiceChartingEnabled,
                    onCheckedChange = onVoiceChartingChange
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tooth numbering. Dictation refuses to guess between the schemes: 14 is the
            // upper-left first molar in Universal and the upper-right first premolar in FDI.
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Dictated tooth numbering",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
                Text(
                    text = "Voice charting reads spoken tooth numbers in this scheme only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ThornburyMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToothNumberingSystem.entries.forEach { system ->
                        FilterChip(
                            selected = system == toothNumberingSystem,
                            onClick = { onToothNumberingChange(system) },
                            label = { Text(system.displayName + " (" + system.example + ")") }
                        )
                    }
                }
            }

            HorizontalDivider(color = ThornburyHairlineSoft, modifier = Modifier.padding(vertical = 10.dp))

            // Chairside Alerts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Chairside Arrival Alerts", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                    Text(text = "Notify when the next patient checks in at reception", style = MaterialTheme.typography.bodySmall, color = ThornburyMuted)
                }
                Switch(
                    checked = areNotificationsEnabled,
                    onCheckedChange = onNotificationsChange
                )
            }

            HorizontalDivider(color = ThornburyHairlineSoft, modifier = Modifier.padding(vertical = 10.dp))

            // Notation System Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Dental Charting Notation", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                    Text(text = "Universal System (#1 to #32) active with FDI cross-reference", style = MaterialTheme.typography.bodySmall, color = ThornburyMuted)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ThornburyPrimaryWash,
                    border = BorderStroke(1.dp, ThornburyPrimary.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "Universal 1-32",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyPrimaryText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityShieldTelemetryCard(
    patientsCount: Int,
    plansCount: Int,
    reportsCount: Int,
    visitsCount: Int,
    isBiometricAuthEnabled: Boolean,
    biometricLockTimeoutMinutes: Int,
    onBiometricAuthChange: (Boolean) -> Unit,
    onBiometricTimeoutChange: (Int) -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = ThornburySuccess, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Data Security & Patient Privacy", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Biometric Auth Session Lock Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(ThornburyPrimary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = ThornburyPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "Biometric & Fingerprint Security", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                        Text(text = "Require fingerprint, facial recognition, or PIN on resume", style = MaterialTheme.typography.bodySmall, color = ThornburyMuted)
                    }
                }
                Switch(
                    checked = isBiometricAuthEnabled,
                    onCheckedChange = onBiometricAuthChange
                )
            }

            if (isBiometricAuthEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Require lock after backgrounding:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            listOf(
                                0 to "Immediately",
                                1 to "1 min",
                                5 to "5 min",
                                15 to "15 min",
                                30 to "30 min"
                            )
                        ) { (timeoutMin, label) ->
                            val isSelected = biometricLockTimeoutMinutes == timeoutMin
                            FilterChip(
                                selected = isSelected,
                                onClick = { onBiometricTimeoutChange(timeoutMin) },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ThornburyPrimaryWash,
                                    selectedLabelColor = ThornburyPrimaryText
                                )
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = ThornburyHairlineSoft, modifier = Modifier.padding(vertical = 12.dp))

            // SQLCipher Encryption & FLAG_SECURE badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = ThornburySuccessWash,
                    border = BorderStroke(1.dp, ThornburySuccess.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = ThornburySuccess, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "SQLCipher AES-256", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = ThornburySuccess)
                            Text(text = "Database encrypted at rest", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = ThornburyBodyStrong)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = ThornburySuccessWash,
                    border = BorderStroke(1.dp, ThornburySuccess.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = ThornburySuccess, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "FLAG_SECURE Active", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = ThornburySuccess)
                            Text(text = "Screenshot protection enabled", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = ThornburyBodyStrong)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(text = "Local Clinic Database Telemetry", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryStatBadge(label = "PATIENTS", value = "$patientsCount", icon = Icons.Default.Groups)
                TelemetryStatBadge(label = "PLANS", value = "$plansCount", icon = Icons.Default.Assignment)
                TelemetryStatBadge(label = "REPORTS", value = "$reportsCount", icon = Icons.Default.Image)
                TelemetryStatBadge(label = "VISITS", value = "$visitsCount", icon = Icons.Default.Event)
            }
        }
    }
}

@Composable
private fun AccountSecurityCard(
    onEditProfileClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "Account & Authentication", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)

            OutlinedButton(
                onClick = onEditProfileClick,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.AccountBox, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edit Clinician Profile Details", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun TelemetryStatBadge(
    label: String,
    value: String,
    icon: ImageVector
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = ThornburyCanvas,
        border = BorderStroke(1.dp, ThornburyHairlineSoft),
        modifier = Modifier.width(74.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = ThornburyPrimary, modifier = Modifier.size(16.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
            Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = ThornburyMuted)
        }
    }
}
