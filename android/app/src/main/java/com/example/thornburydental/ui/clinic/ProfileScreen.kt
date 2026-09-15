package com.example.thornburydental.ui.clinic

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.thornburydental.data.AuthRepository
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.UserRole
import com.example.thornburydental.theme.*

/**
 * Dedicated User Profile & Practice Settings Screen.
 * Displays clinician credentials, surgery room assignment, security shield status,
 * practice preferences, and account actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by AuthRepository.currentUser.collectAsState()
    val displayName = currentUser?.name ?: "Not signed in"
    val roleLabel = when (currentUser?.role) {
        UserRole.CLINICIAN -> "Clinician"
        UserRole.RECEPTIONIST -> "Receptionist"
        UserRole.PATIENT -> "Patient"
        null -> "Guest"
    }
    val initials = remember(displayName) {
        val words = displayName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        when {
            words.size >= 2 -> "${words.first().first().uppercaseChar()}${words.last().first().uppercaseChar()}"
            words.isNotEmpty() -> words.first().take(2).uppercase()
            else -> "?"
        }
    }

    val selectedRoom by DentalRepository.defaultSurgeryRoom.collectAsState()
    val isDarkModeEnabled by DentalRepository.isDarkModeEnabled.collectAsState()
    val areNotificationsEnabled by DentalRepository.appointmentRemindersEnabled.collectAsState()
    var showSignOutDialog by remember { mutableStateOf(false) }

    val surgeryRooms = listOf("Surgery 1", "Surgery 2", "Surgery 3")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Clinician Profile & Settings",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = ThornburyInk
                        )
                        Text(
                            text = "Manage account, operatory defaults, and app preferences",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyMuted
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThornburyCanvas)
            )
        },
        containerColor = ThornburyCanvas,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. Profile Header Card
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceCard),
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar Circle
                    Box(
                        modifier = Modifier
                            .size(72.dp)
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
                            fontWeight = FontWeight.Bold,
                        ),
                        color = ThornburyInk
                    )

                    Text(
                        text = roleLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = ThornburyPrimaryText
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = ThornburySurfaceSoft,
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Text(
                            text = "Default Room: $selectedRoom",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = ThornburyMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Professional & Contact Details Card
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft),
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = ThornburyPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Professional Information",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    ProfileDetailRow(label = "Role", value = roleLabel)
                    HorizontalDivider(color = ThornburyHairlineSoft, modifier = Modifier.padding(vertical = 8.dp))

                    ProfileDetailRow(label = "Primary Clinic Room", value = selectedRoom)
                    HorizontalDivider(color = ThornburyHairlineSoft, modifier = Modifier.padding(vertical = 8.dp))

                    ProfileDetailRow(
                        label = "Email Address",
                        value = currentUser?.email?.takeIf { it.isNotBlank() } ?: "Not on record"
                    )
                    HorizontalDivider(color = ThornburyHairlineSoft, modifier = Modifier.padding(vertical = 8.dp))

                    ProfileDetailRow(
                        label = "Contact Phone",
                        value = currentUser?.phone?.takeIf { it.isNotBlank() } ?: "Not on record"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Practice & App Settings Card
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft),
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = ThornburyPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Practice & App Preferences",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Default Surgery Room Selector
                    Text(
                        text = "Default Surgery Room",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        surgeryRooms.forEach { room ->
                            val isSelected = room == selectedRoom
                            FilterChip(
                                selected = isSelected,
                                onClick = { DentalRepository.setDefaultSurgeryRoom(room) },
                                label = { Text(room, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ThornburyPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = ThornburyHairlineSoft)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Dark Mode Toggle Switch Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Operatory Dark Mode",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyInk
                            )
                            Text(
                                text = "High-contrast theme for darkened operatory suites",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyMuted
                            )
                        }
                        Switch(
                            checked = isDarkModeEnabled,
                            onCheckedChange = { DentalRepository.setDarkModeEnabled(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = ThornburyHairlineSoft)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Notification Reminders Switch Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Appointment Reminders",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyInk
                            )
                            Text(
                                text = "Receive alerts for next patient in chair",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyMuted
                            )
                        }
                        Switch(
                            checked = areNotificationsEnabled,
                            onCheckedChange = { DentalRepository.setAppointmentRemindersEnabled(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Security & Privacy Card
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft),
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = ThornburySuccess,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Clinical Data Security & Privacy",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = ThornburySuccessWash,
                        border = BorderStroke(1.dp, ThornburySuccess.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = ThornburySuccess,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "FLAG_SECURE Active",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburySuccess
                                )
                                Text(
                                    text = "Task-switcher screenshot protection enabled to safeguard patient records.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThornburyBodyStrong
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Account Actions Section
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft),
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Account Actions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Editing profile details isn't available yet.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Profile Details")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Changing password isn't available yet.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Change Account Password")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showSignOutDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThornburyPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Return to Brand Page", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Sign-out confirmation — a mis-tap on "Return to Brand Page" no longer signs
    // the user out immediately with no way to cancel.
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = {
                Text(
                    text = "Sign Out?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
            },
            text = {
                Text(
                    text = "You'll be returned to the welcome screen and will need to sign in again to access patient records.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThornburyBody
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary, contentColor = Color.White)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = ThornburyInk)
                }
            },
            containerColor = ThornburyCanvas,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = ThornburyMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = ThornburyInk
        )
    }
}
