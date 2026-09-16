package com.example.thornburydental.ui.clinic

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.MedicationPreset
import com.example.thornburydental.data.Patient
import com.example.thornburydental.theme.*

@Composable
fun IssuePrescriptionDialog(
    patient: Patient,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val presets by DentalRepository.medicationPresets.collectAsState()

    var selectedClinician by remember {
        mutableStateOf(DentalRepository.clinicians.firstOrNull()?.name ?: "Dr. Ingrid Halvorsen")
    }

    var isCustomMode by remember { mutableStateOf(false) }
    var selectedPresetId by remember(presets) { mutableStateOf(presets.firstOrNull()?.id) }

    val initialPreset = remember(presets) { presets.firstOrNull() }
    var drugName by remember { mutableStateOf(initialPreset?.name ?: "") }
    var dosage by remember { mutableStateOf(initialPreset?.dosage ?: "") }
    var frequency by remember { mutableStateOf(initialPreset?.frequency ?: "") }
    var duration by remember { mutableStateOf(initialPreset?.duration ?: "") }
    var instructions by remember { mutableStateOf(initialPreset?.instructions ?: "") }

    var saveAsPreset by remember { mutableStateOf(false) }
    var presetCategory by remember { mutableStateOf("Custom") }
    var showManagePresetsDialog by remember { mutableStateOf(false) }

    // Synchronize initial values if presets become available later
    LaunchedEffect(presets) {
        if (!isCustomMode && drugName.isBlank() && presets.isNotEmpty()) {
            val first = presets.first()
            selectedPresetId = first.id
            drugName = first.name
            dosage = first.dosage
            frequency = first.frequency
            duration = first.duration
            instructions = first.instructions
        }
    }

    // Real-time Allergy Cross-Check!
    val allergyWarning = remember(drugName, patient) {
        if (drugName.isNotBlank()) DentalRepository.checkAllergyConflict(patient, drugName) else null
    }

    var overrideConfirmed by remember { mutableStateOf(false) }
    var overrideReason by remember { mutableStateOf("") }

    val isFormValid = drugName.isNotBlank() && dosage.isNotBlank() && frequency.isNotBlank() &&
            (allergyWarning == null || (overrideConfirmed && overrideReason.isNotBlank()))

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Modal Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Issue Dental Prescription",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = ThornburyInk
                        )
                        Text(
                            text = "For: ${patient.name} (${patient.opNo})",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyMuted
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = ThornburyInk)
                    }
                }

                // Allergy Alert Banner
                if (allergyWarning != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = ThornburyErrorWash,
                        border = BorderStroke(1.dp, ThornburyError)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = ThornburyError,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = allergyWarning,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ThornburyError
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = overrideConfirmed,
                            onCheckedChange = { overrideConfirmed = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = ThornburyError,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Override allergy contraindication with senior clinician sign-off",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = ThornburyInk,
                            modifier = Modifier.clickable { overrideConfirmed = !overrideConfirmed }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = overrideReason,
                        onValueChange = { overrideReason = it },
                        label = { Text("Clinical Justification (required)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = thornburyTextFieldColors(containerColor = ThornburyCanvas)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Prescribing Clinician
                Text(
                    text = "Prescribing Clinician",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
                Spacer(modifier = Modifier.height(6.dp))
                val activeClinician = DentalRepository.clinicians.firstOrNull()
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = ThornburySurfaceSoft,
                    border = BorderStroke(1.dp, ThornburyHairline)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = ThornburyPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = activeClinician?.name ?: selectedClinician,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = ThornburyInk
                            )
                            Text(
                                text = "${activeClinician?.specialty ?: "Comprehensive Dental Care"} • ${activeClinician?.room ?: "Surgery 1"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ThornburyMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode Selector: Presets vs Custom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Medication Source",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )

                    TextButton(
                        onClick = { showManagePresetsDialog = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp), tint = ThornburyPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Manage Presets", fontSize = 12.sp, color = ThornburyPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Segmented Toggle Tabs
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ThornburySurfaceSoft,
                    border = BorderStroke(1.dp, ThornburyHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (!isCustomMode) ThornburyCanvas else Color.Transparent,
                            border = if (!isCustomMode) BorderStroke(1.dp, ThornburyHairline) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    isCustomMode = false
                                    // Reset to selected preset if one was selected
                                    val current = presets.find { it.id == selectedPresetId } ?: presets.firstOrNull()
                                    if (current != null) {
                                        selectedPresetId = current.id
                                        drugName = current.name
                                        dosage = current.dosage
                                        frequency = current.frequency
                                        duration = current.duration
                                        instructions = current.instructions
                                    }
                                }
                        ) {
                            Text(
                                text = "Formulary Presets",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (!isCustomMode) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (!isCustomMode) ThornburyPrimaryText else ThornburyMuted,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isCustomMode) ThornburyCanvas else Color.Transparent,
                            border = if (isCustomMode) BorderStroke(1.dp, ThornburyHairline) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    isCustomMode = true
                                    selectedPresetId = null
                                }
                        ) {
                            Text(
                                text = "Custom Medication",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isCustomMode) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isCustomMode) ThornburyPrimaryText else ThornburyMuted,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!isCustomMode) {
                    // Presets List
                    Text(
                        text = "Choose from Presets",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    presets.forEach { item ->
                        val isSelected = item.id == selectedPresetId
                        val hasConflict = DentalRepository.checkAllergyConflict(patient, item.name) != null

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable {
                                    selectedPresetId = item.id
                                    drugName = item.name
                                    dosage = item.dosage
                                    frequency = item.frequency
                                    duration = item.duration
                                    instructions = item.instructions
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) ThornburySurfaceSoft else ThornburyCanvas,
                            border = BorderStroke(
                                1.dp,
                                if (hasConflict) ThornburyError else if (isSelected) ThornburyPrimary else ThornburyHairline
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (hasConflict) ThornburyError else ThornburyInk
                                    )
                                    if (item.isCustom) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = ThornburyAccentAmber.copy(alpha = 0.18f)
                                        ) {
                                            Text(
                                                text = "Custom",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                                                color = ThornburyAccentAmber,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                if (hasConflict) {
                                    Text(
                                        text = "Allergy Conflict",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ThornburyError
                                    )
                                } else {
                                    Text(
                                        text = item.dosage,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ThornburyMuted
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Custom Medication Inputs
                    OutlinedTextField(
                        value = drugName,
                        onValueChange = { drugName = it },
                        label = { Text("Medication / Drug Name *") },
                        placeholder = { Text("e.g. Augmentin, Azithromycin, Doxycycline") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = thornburyTextFieldColors(containerColor = ThornburyCanvas)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = saveAsPreset,
                            onCheckedChange = { saveAsPreset = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = ThornburyPrimary,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Save this medication as a preset for future use",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = ThornburyInk,
                            modifier = Modifier.clickable { saveAsPreset = !saveAsPreset }
                        )
                    }

                    if (saveAsPreset) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = presetCategory,
                            onValueChange = { presetCategory = it },
                            label = { Text("Preset Category (e.g. Antibiotics, Analgesics)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = thornburyTextFieldColors(containerColor = ThornburyCanvas)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Dosage & Frequency
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosage / Formulation *") },
                    placeholder = { Text("e.g. 500 mg capsules / 625 mg tablets") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = thornburyTextFieldColors(containerColor = ThornburyCanvas)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = frequency,
                    onValueChange = { frequency = it },
                    label = { Text("Frequency * (e.g. 1 tablet every 8 hours)") },
                    placeholder = { Text("e.g. 1 capsule twice daily") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = thornburyTextFieldColors(containerColor = ThornburyCanvas)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Duration (e.g. 5 days / until finished)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = thornburyTextFieldColors(containerColor = ThornburyCanvas)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Pharmacist & Patient Instructions") },
                    placeholder = { Text("e.g. Take strictly with food. Finish full course.") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = thornburyTextFieldColors(containerColor = ThornburyCanvas)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val shareText = """
                                DENTARA DENTAL PRACTICE
                                18 Dentara Way, Portland, OR 97210
                                Tel: +1 (503) 224-7700
                                ----------------------------------------
                                PRESCRIPTION FOR: ${patient.name}
                                OP: ${patient.opNo} | DOB: ${patient.dob}
                                
                                Rx: $drugName $dosage
                                Sig: $frequency
                                Duration: $duration
                                Instructions: $instructions
                                
                                Prescriber: $selectedClinician
                                Verified against electronic allergy ledger.
                            """.trimIndent()

                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Prescription for ${patient.name} - Dentara Dental Practice")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Prescription Slip"))
                        },
                        enabled = isFormValid,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }

                    Button(
                        onClick = {
                            DentalRepository.issuePrescription(
                                patient = patient,
                                clinicianName = selectedClinician,
                                drugName = drugName.trim(),
                                dosage = dosage.trim(),
                                frequency = frequency.trim(),
                                duration = duration.trim(),
                                instructions = instructions.trim(),
                                overrideReason = if (allergyWarning != null) overrideReason else null,
                                saveAsPreset = isCustomMode && saveAsPreset,
                                presetCategory = presetCategory.trim().ifBlank { "Custom" }
                            )
                            onSuccess()
                        },
                        enabled = isFormValid,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThornburyPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign & Issue")
                    }
                }
            }
        }
    }

    if (showManagePresetsDialog) {
        ManagePresetsDialog(
            onDismiss = { showManagePresetsDialog = false }
        )
    }
}
