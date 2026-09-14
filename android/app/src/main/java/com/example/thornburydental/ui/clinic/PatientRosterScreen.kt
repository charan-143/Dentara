package com.example.thornburydental.ui.clinic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.Patient
import com.example.thornburydental.theme.*
import com.example.thornburydental.ui.components.ThornburyDatePickerField

@Composable
fun PatientRosterScreen(
    onSelectPatient: (String) -> Unit,
    onNavigateToRegisterPatient: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val patients by DentalRepository.patients.collectAsState()
    val appointments by DentalRepository.appointments.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf<String>("all") } // "all", "upcoming"
    var showRegisterDialog by remember { mutableStateOf(false) }

    val upcomingPatientIds = remember(appointments) {
        appointments.filter { it.status == "confirmed" }.map { it.patientId }.toSet()
    }
    val upcomingCount = remember(patients, upcomingPatientIds) {
        patients.count { upcomingPatientIds.contains(it.id) }
    }

    val filteredPatients = remember(patients, searchQuery, filterMode, upcomingPatientIds) {
        patients.filter { p ->
            // Filter mode
            val matchesFilter = when (filterMode) {
                "upcoming" -> upcomingPatientIds.contains(p.id)
                else -> true
            }
            if (!matchesFilter) return@filter false

            // Search query
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                p.name.lowercase().contains(q) ||
                p.opNo.lowercase().contains(q) ||
                p.email.lowercase().contains(q) ||
                p.phone.lowercase().contains(q)
            } else true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ThornburyCanvas)
    ) {
        // Top Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ThornburyCanvas,
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Title and Register Patient Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Patient Records",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = ThornburyInk
                        )
                        Text(
                            text = "${patients.size} registered patients • Confidential dental records",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyMuted
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Prominent Register Patient Action Button
                    Button(
                        onClick = onNavigateToRegisterPatient,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThornburyPrimary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Register Patient",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Field (24dp pill shape matching Today tab)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search by name, OP, or phone...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ThornburyMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = ThornburyMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = ThornburyMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ThornburyCanvas,
                        unfocusedContainerColor = ThornburyCanvas,
                        focusedBorderColor = ThornburyPrimary,
                        unfocusedBorderColor = ThornburyHairline,
                        focusedTextColor = ThornburyInk,
                        unfocusedTextColor = ThornburyInk
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Unified Material 3 FilterChips row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterChips = listOf(
                        Triple("all", "All", patients.size),
                        Triple("upcoming", "Upcoming", upcomingCount)
                    )

                    filterChips.forEach { (key, label, count) ->
                        val isSelected = filterMode == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { filterMode = key },
                            label = {
                                Text(
                                    text = "$label ($count)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = ThornburySurfaceSoft,
                                labelColor = ThornburyBody,
                                selectedContainerColor = ThornburyPrimary,
                                selectedLabelColor = ThornburyOnPrimary,
                                selectedLeadingIconColor = ThornburyOnPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = ThornburyHairline,
                                selectedBorderColor = ThornburyPrimary
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
        }

        // Patients List
        if (filteredPatients.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PersonSearch,
                        contentDescription = null,
                        tint = ThornburyMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No patients match the search criteria.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThornburyMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredPatients, key = { it.id }) { patient ->
                    PatientListItemCard(
                        patient = patient,
                        onClick = { onSelectPatient(patient.id) }
                    )
                }
            }
        }
    }

    // Register Patient Dialog Modal
    if (showRegisterDialog) {
        RegisterPatientDialog(
            onDismiss = { showRegisterDialog = false },
            onRegistered = { newPatient ->
                showRegisterDialog = false
                onSelectPatient(newPatient.id)
            }
        )
    }
}

@Composable
private fun PatientListItemCard(
    patient: Patient,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = patient.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "OP: ${patient.opNo} • DOB: ${patient.dob}",
                        style = ClinicalCodeStyle.copy(fontSize = 11.sp),
                        color = ThornburyMuted
                    )
                }

                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = ThornburySurfaceSoft,
                    border = BorderStroke(1.dp, ThornburyHairline)
                ) {
                    Text(
                        text = "Last: ${patient.lastVisit}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = ThornburyBody
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${patient.phone} • ${patient.email}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ThornburyBody
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = ThornburyCanvas,
                    border = BorderStroke(1.dp, ThornburyBorder)
                ) {
                    Text(
                        text = "Open Chart →",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyPrimaryText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Registration Dialog matching website NewPatientForm (components/clinical-forms.tsx)
 */
@Composable
fun RegisterPatientDialog(
    onDismiss: () -> Unit,
    onRegistered: (Patient) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var opNo by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
            border = BorderStroke(1.dp, ThornburyBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Register New Patient",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = ThornburyInk
                        )
                        Text(
                            text = "Outpatient (OP) number is assigned or auto-generated.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyMuted
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = ThornburyMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                errorMessage?.let { err ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = ThornburyErrorWash,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ThornburyError.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = err,
                            color = ThornburyError,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Full Name (Required)
                Text("Full Name *", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. Arthur Pendelton") },
                    singleLine = true,
                    colors = thornburyTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Date of Birth & OP Number Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        ThornburyDatePickerField(
                            value = dob,
                            onValueChange = { dob = it },
                            label = "Date of Birth",
                            placeholder = "YYYY-MM-DD",
                            isOptional = true,
                            helperText = null
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("OP Number", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = opNo,
                            onValueChange = { opNo = it },
                            placeholder = { Text("Auto-generated") },
                            singleLine = true,
                            colors = thornburyTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Phone & Email Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Phone Number (Optional)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            placeholder = { Text("+1 (503) 555-0199") },
                            singleLine = true,
                            colors = thornburyTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Email Address (Optional)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text("arthur@example.com") },
                            singleLine = true,
                            colors = thornburyTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Address
                Text("Postal Address", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    placeholder = { Text("Street address, City, State, ZIP...") },
                    singleLine = true,
                    colors = thornburyTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ThornburyInk)
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Patient name is required."
                                return@Button
                            }

                            val newPatient = DentalRepository.registerPatient(
                                name = name.trim(),
                                opNo = opNo.trim().ifBlank { null },
                                dob = dob.trim().ifBlank { "Not specified" },
                                phone = phone.trim().ifBlank { "Not provided" },
                                email = email.trim().ifBlank { "Not provided" },
                                address = address.trim().ifBlank { "Not recorded" },
                                allergies = emptyList(),
                                medicalAlerts = emptyList(),
                                medicalHistory = ""
                            )

                            onRegistered(newPatient)
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThornburyPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Register Patient", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
