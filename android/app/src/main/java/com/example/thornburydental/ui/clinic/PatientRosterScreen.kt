package com.example.thornburydental.ui.clinic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.thornburydental.data.Allergy
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.Patient
import com.example.thornburydental.theme.*

@Composable
fun PatientRosterScreen(
    onSelectPatient: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val patients by DentalRepository.patients.collectAsState()
    val appointments by DentalRepository.appointments.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf<String>("all") } // "all", "allergies", "upcoming"
    var showRegisterDialog by remember { mutableStateOf(false) }

    val allergyCount = remember(patients) { patients.count { it.allergies.isNotEmpty() } }
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
                "allergies" -> p.allergies.isNotEmpty()
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
                                fontFamily = FontFamily.Serif,
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
                        onClick = { showRegisterDialog = true },
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

                // KPI Chips row (matching patient-roster-view.tsx)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Registered
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ThornburySurfaceSoft,
                        border = BorderStroke(1.dp, ThornburyHairlineSoft),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${patients.size}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyInk
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Registered",
                                style = MaterialTheme.typography.labelSmall,
                                color = ThornburyMuted
                            )
                        }
                    }

                    // Allergies
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ThornburyErrorWash,
                        border = BorderStroke(1.dp, ThornburyError.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "$allergyCount",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyError
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Allergies",
                                style = MaterialTheme.typography.labelSmall,
                                color = ThornburyError
                            )
                        }
                    }

                    // Upcoming
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ThornburySuccessWash,
                        border = BorderStroke(1.dp, ThornburySuccess.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "$upcomingCount",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = ThornburySuccess
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Upcoming",
                                style = MaterialTheme.typography.labelSmall,
                                color = ThornburySuccess
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Segmented control tabs (matching patient-roster-view.tsx)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ThornburySurfaceSoft,
                    border = BorderStroke(1.dp, ThornburyHairlineSoft),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val filterOptions = listOf(
                            "all" to "All (${patients.size})",
                            "allergies" to "With Allergies ($allergyCount)",
                            "upcoming" to "Upcoming ($upcomingCount)"
                        )
                        filterOptions.forEach { (key, label) ->
                            val isSelected = filterMode == key
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) ThornburyCanvas else Color.Transparent,
                                border = if (isSelected) BorderStroke(1.dp, ThornburyBorder) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { filterMode = key }
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) ThornburyPrimaryText else ThornburyBody,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, OP, or phone...", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = ThornburyMuted,
                            modifier = Modifier.size(18.dp)
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
                    shape = RoundedCornerShape(8.dp),
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

            // Allergies & Medical Warnings Strip
            if (patient.allergies.isNotEmpty() || patient.medicalAlerts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    patient.allergies.take(2).forEach { allergy ->
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = ThornburyErrorWash,
                            border = BorderStroke(1.dp, ThornburyError.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = ThornburyError,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = allergy.allergen,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = ThornburyError
                                )
                            }
                        }
                    }

                    patient.medicalAlerts.take(1).forEach { alert ->
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = ThornburyWarningWash,
                            border = BorderStroke(1.dp, ThornburyWarning.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = alert,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = ThornburyWarning
                            )
                        }
                    }
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
    var allergiesText by remember { mutableStateOf("") }
    var medicalHistoryText by remember { mutableStateOf("") }
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
                                fontFamily = FontFamily.Serif,
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
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Date of Birth & OP Number Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Date of Birth *", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = dob,
                            onValueChange = { dob = it },
                            placeholder = { Text("YYYY-MM-DD") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
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
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Phone & Email Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Phone Number", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            placeholder = { Text("+1 (503) 555-0199") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Email Address", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text("arthur@example.com") },
                            singleLine = true,
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
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Allergies
                Text("Known Allergies (comma-separated)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = allergiesText,
                    onValueChange = { allergiesText = it },
                    placeholder = { Text("e.g. Penicillin, Latex, Sulfa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Medical History
                Text("Medical History & Conditions", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = medicalHistoryText,
                    onValueChange = { medicalHistoryText = it },
                    placeholder = { Text("e.g. Mild Asthma, Hypertension, Diabetes") },
                    maxLines = 3,
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
                            if (dob.isBlank()) {
                                errorMessage = "Date of birth is required."
                                return@Button
                            }

                            val parsedAllergies = allergiesText.split(",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                                .map { AllergenStr ->
                                    Allergy(
                                        allergen = AllergenStr,
                                        severity = "Recorded Alert",
                                        reaction = "Clinical sensitivity check required"
                                    )
                                }

                            val parsedAlerts = medicalHistoryText.split("\n", ",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }

                            val newPatient = DentalRepository.registerPatient(
                                name = name.trim(),
                                opNo = opNo.trim().ifBlank { null },
                                dob = dob.trim(),
                                phone = phone.trim().ifBlank { "Not provided" },
                                email = email.trim().ifBlank { "Not provided" },
                                address = address.trim().ifBlank { "Not recorded" },
                                allergies = parsedAllergies,
                                medicalAlerts = parsedAlerts,
                                medicalHistory = medicalHistoryText.trim()
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
