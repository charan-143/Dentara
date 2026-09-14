package com.example.thornburydental.ui.clinic

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.Patient
import com.example.thornburydental.theme.*

private fun getDrugCategory(drugName: String): String {
    val d = drugName.lowercase()
    return when {
        d.contains("amox") || d.contains("clinda") || d.contains("metro") || d.contains("cillin") || d.contains("mycin") || d.contains("antibiotic") || d.contains("keflex") || d.contains("cephalexin") || d.contains("augmentin") || d.contains("erythro") || d.contains("azithro") -> "antibiotics"
        d.contains("ibuprofen") || d.contains("paracetamol") || d.contains("acetaminophen") || d.contains("naproxen") || d.contains("codeine") || d.contains("tramadol") || d.contains("analgesic") || d.contains("tylenol") || d.contains("advil") -> "analgesics"
        else -> "rinses_other"
    }
}

@Composable
fun PrescriptionsManagementScreen(
    onOpenPatientChart: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prescriptions by DentalRepository.prescriptions.collectAsState()
    val patients by DentalRepository.patients.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var filterCategory by remember { mutableStateOf("all") } // "all", "antibiotics", "analgesics", "rinses_other"
    var showIssueDialogForPatient by remember { mutableStateOf<Patient?>(null) }
    var showSelectPatientDialog by remember { mutableStateOf(false) }

    val antibioticsCount = remember(prescriptions) {
        prescriptions.count { getDrugCategory(it.drugName) == "antibiotics" }
    }
    val analgesicsCount = remember(prescriptions) {
        prescriptions.count { getDrugCategory(it.drugName) == "analgesics" }
    }
    val rinsesOtherCount = remember(prescriptions) {
        prescriptions.count { getDrugCategory(it.drugName) == "rinses_other" }
    }

    val filteredPrescriptions = remember(prescriptions, searchQuery, filterCategory) {
        prescriptions.filter { rx ->
            val matchesCategory = when (filterCategory) {
                "antibiotics" -> getDrugCategory(rx.drugName) == "antibiotics"
                "analgesics" -> getDrugCategory(rx.drugName) == "analgesics"
                "rinses_other" -> getDrugCategory(rx.drugName) == "rinses_other"
                else -> true
            }
            if (!matchesCategory) return@filter false

            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                rx.drugName.lowercase().contains(q) ||
                rx.patientName.lowercase().contains(q) ||
                rx.instructions.lowercase().contains(q)
            } else true
        }
    }

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
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Prescription Registry",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = ThornburyInk
                        )
                        Text(
                            text = "Cross-checked against tamper-evident allergy ledger",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyMuted
                        )
                    }

                    Button(
                        onClick = { showSelectPatientDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThornburyPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Rx")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar (24dp pill shape matching Today tab)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search by drug, patient, or instructions...",
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
                                    contentDescription = "Clear search",
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

                // Material 3 FilterChips row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterChips = listOf(
                        Triple("all", "All", prescriptions.size),
                        Triple("antibiotics", "Antibiotics", antibioticsCount),
                        Triple("analgesics", "Analgesics", analgesicsCount),
                        Triple("rinses_other", "Rinses / Other", rinsesOtherCount)
                    )

                    filterChips.forEach { (mode, title, count) ->
                        val isSelected = filterCategory == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { filterCategory = mode },
                            label = {
                                Text(
                                    text = "$title ($count)",
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

        if (filteredPrescriptions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, ThornburyHairline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = ThornburyMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching prescriptions" else "No prescriptions in this category",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = ThornburyInk
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotBlank())
                                "Try adjusting your search terms or clearing the filter."
                            else
                                "No prescriptions recorded under the selected category.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyMuted,
                            textAlign = TextAlign.Center
                        )
                        if (searchQuery.isNotBlank() || filterCategory != "all") {
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = {
                                    searchQuery = ""
                                    filterCategory = "all"
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Reset Filters")
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredPrescriptions, key = { it.id }) { rx ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = rx.drugName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = ThornburyInk
                                    )
                                    Text(
                                        text = "Patient: ${rx.patientName}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = ThornburyPrimaryText
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(9999.dp),
                                    color = ThornburySurfaceSoft,
                                    border = BorderStroke(1.dp, ThornburyHairline)
                                ) {
                                    Text(
                                        text = rx.dosage,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ThornburyInk
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Sig: ${rx.frequency} (${rx.duration})",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = ThornburyBodyStrong
                            )
                            Text(
                                text = "Directions: ${rx.instructions}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyBody
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = ThornburyHairlineSoft)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Issued by ${rx.clinicianName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ThornburyMuted
                                    )
                                    Text(
                                        text = rx.issueDate,
                                        style = ClinicalCodeStyle.copy(fontSize = 10.sp),
                                        color = ThornburyMuted
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            val shareText = "Rx: ${rx.drugName} ${rx.dosage} for ${rx.patientName}. Sig: ${rx.frequency}. Issued by ${rx.clinicianName}, Thornbury Dental."
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Share Prescription"))
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = ThornburyPrimaryText)
                                    }
                                    TextButton(onClick = { onOpenPatientChart(rx.patientId) }) {
                                        Text("View Chart", color = ThornburyPrimaryText, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Select Patient Dialog
    if (showSelectPatientDialog) {
        var patientSearchQuery by remember { mutableStateOf("") }
        val filteredDialogPatients = remember(patients, patientSearchQuery) {
            if (patientSearchQuery.isBlank()) patients
            else {
                val q = patientSearchQuery.trim().lowercase()
                patients.filter {
                    it.name.lowercase().contains(q) ||
                    it.opNo.lowercase().contains(q) ||
                    it.phone.lowercase().contains(q)
                }
            }
        }

        AlertDialog(
            onDismissRequest = {
                showSelectPatientDialog = false
                patientSearchQuery = ""
            },
            title = {
                Text(
                    text = "Select Patient to Prescribe",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = ThornburyInk
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = patientSearchQuery,
                        onValueChange = { patientSearchQuery = it },
                        placeholder = { Text("Search by name, OP, phone...", style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp), tint = ThornburyMuted)
                        },
                        trailingIcon = {
                            if (patientSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { patientSearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp), tint = ThornburyMuted)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
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

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (filteredDialogPatients.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No patients match \"$patientSearchQuery\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ThornburyMuted
                                    )
                                }
                            }
                        } else {
                            items(filteredDialogPatients, key = { it.id }) { p ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = ThornburySurfaceSoft,
                                    border = BorderStroke(1.dp, ThornburyHairline),
                                    onClick = {
                                        showIssueDialogForPatient = p
                                        showSelectPatientDialog = false
                                        patientSearchQuery = ""
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(text = p.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                                            Text(text = "OP: ${p.opNo}", style = ClinicalCodeStyle.copy(fontSize = 11.sp), color = ThornburyMuted)
                                        }
                                        if (p.allergies.isNotEmpty()) {
                                            Text(
                                                text = "${p.allergies.size} Allergies",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = ThornburyError
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showSelectPatientDialog = false
                    patientSearchQuery = ""
                }) {
                    Text("Cancel", color = ThornburyMuted)
                }
            }
        )
    }

    // Issue Prescription Dialog
    showIssueDialogForPatient?.let { patient ->
        IssuePrescriptionDialog(
            patient = patient,
            onDismiss = { showIssueDialogForPatient = null },
            onSuccess = { showIssueDialogForPatient = null }
        )
    }
}
