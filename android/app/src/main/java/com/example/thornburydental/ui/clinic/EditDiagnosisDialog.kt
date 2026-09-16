package com.example.thornburydental.ui.clinic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.Patient
import com.example.thornburydental.data.PatientDiagnosis
import com.example.thornburydental.theme.*
import com.example.thornburydental.util.todayIsoDate

/**
 * Modal dialog for recording or editing/updating a patient's one-time persistent clinical diagnosis.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditDiagnosisDialog(
    patient: Patient,
    onDismiss: () -> Unit,
    onSave: (PatientDiagnosis) -> Unit
) {
    val existing = patient.diagnosis

    var primaryDiagnosis by remember { mutableStateOf(existing?.primaryDiagnosis ?: "") }
    var clinicalFindings by remember { mutableStateOf(existing?.clinicalFindings ?: "") }
    var systemicConsiderations by remember { mutableStateOf(existing?.systemicConsiderations ?: patient.medicalHistory) }
    var clinicianName by remember {
        mutableStateOf(existing?.clinicianName ?: DentalRepository.clinicians.firstOrNull()?.name ?: "")
    }

    val prognosisOptions = listOf("Good", "Favourable", "Guarded", "Poor", "Questionable")
    val initialTier = remember(existing?.prognosis) {
        val p = existing?.prognosis ?: "Good"
        prognosisOptions.firstOrNull { p.startsWith(it, ignoreCase = true) } ?: "Good"
    }
    val initialNotes = remember(existing?.prognosis) {
        val p = existing?.prognosis ?: ""
        val matched = prognosisOptions.firstOrNull { p.startsWith(it, ignoreCase = true) }
        if (matched != null) {
            p.removePrefix(matched).trim()
        } else {
            p
        }
    }

    var selectedTier by remember { mutableStateOf(initialTier) }
    var prognosisNotes by remember { mutableStateOf(initialNotes) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (existing != null) "Edit Clinical Diagnosis" else "Create Clinical Diagnosis",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                        Text(
                            text = "Patient: ${patient.name} • ${patient.opNo}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyMuted
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ThornburyInk
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Informational banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = ThornburySurfaceSoft,
                        border = BorderStroke(1.dp, ThornburyHairlineSoft)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = ThornburyPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (existing == null)
                                    "Create the patient's initial primary diagnosis. You can review and edit these clinical details at any time as treatment progresses."
                                else
                                    "Updating this clinical diagnosis will save your edits with a new revision timestamp while preserving patient history.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyMuted
                            )
                        }
                    }

                    // Primary Diagnosis Field
                    Column {
                        Text(
                            text = "Primary Clinical Diagnosis *",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = primaryDiagnosis,
                            onValueChange = { primaryDiagnosis = it },
                            placeholder = { Text("e.g. Generalized Stage III, Grade B Periodontitis with localized apical radiolucency #19") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = thornburyTextFieldColors(containerColor = ThornburyCanvas),
                            minLines = 2
                        )
                    }

                    // Clinical Findings Field
                    Column {
                        Text(
                            text = "Clinical Findings & Diagnostic Evidence",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = clinicalFindings,
                            onValueChange = { clinicalFindings = it },
                            placeholder = { Text("Summarize probing depths, mobility, caries lesions, periapical pathology, or soft tissue status...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = thornburyTextFieldColors(containerColor = ThornburyCanvas)
                        )
                    }

                    // Prognosis Selector & Qualifying Notes
                    Column {
                        Text(
                            text = "Clinical Prognosis *",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            prognosisOptions.forEach { opt ->
                                val isSelected = opt.equals(selectedTier, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedTier = opt },
                                    label = {
                                        Text(
                                            text = opt,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ThornburyPrimary,
                                        selectedLabelColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = prognosisNotes,
                            onValueChange = { prognosisNotes = it },
                            placeholder = { Text("Qualifying factors (e.g. following endodontic retreatment)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = thornburyTextFieldColors(containerColor = ThornburyCanvas),
                            singleLine = true
                        )
                    }

                    // Systemic Considerations Field
                    Column {
                        Text(
                            text = "Systemic & Medical Considerations",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = systemicConsiderations,
                            onValueChange = { systemicConsiderations = it },
                            placeholder = { Text("Comorbidities, medications, bleeding risks, or allergy cautions...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = thornburyTextFieldColors(containerColor = ThornburyCanvas),
                            minLines = 2
                        )
                    }

                    // Attending Clinician
                    Column {
                        Text(
                            text = "Diagnosing Clinician *",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            DentalRepository.clinicians.forEach { c ->
                                val isSelected = c.name == clinicianName
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { clinicianName = c.name },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) ThornburySurfaceSoft else ThornburyCanvas,
                                    border = BorderStroke(1.dp, if (isSelected) ThornburyPrimary else ThornburyHairline)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { clinicianName = c.name },
                                            colors = RadioButtonDefaults.colors(selectedColor = ThornburyPrimary)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${c.name} (${c.room})",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = ThornburyInk
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Text("Cancel", color = ThornburyInk)
                    }

                    Button(
                        onClick = {
                            if (primaryDiagnosis.isNotBlank()) {
                                val finalPrognosis = if (prognosisNotes.isNotBlank()) {
                                    "$selectedTier ${prognosisNotes.trim()}"
                                } else {
                                    selectedTier
                                }
                                val updated = PatientDiagnosis(
                                    primaryDiagnosis = primaryDiagnosis.trim(),
                                    clinicalFindings = clinicalFindings.trim(),
                                    prognosis = finalPrognosis,
                                    systemicConsiderations = systemicConsiderations.trim(),
                                    dateRecorded = existing?.dateRecorded?.ifBlank { todayIsoDate() } ?: todayIsoDate(),
                                    lastUpdated = todayIsoDate(),
                                    clinicianName = clinicianName
                                )
                                onSave(updated)
                            }
                        },
                        enabled = primaryDiagnosis.isNotBlank(),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThornburyPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (existing != null) "Save Changes" else "Create Diagnosis",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
