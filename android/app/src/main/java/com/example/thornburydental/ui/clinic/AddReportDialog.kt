package com.example.thornburydental.ui.clinic

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.Patient
import com.example.thornburydental.theme.*

/**
 * Add Diagnostic Report / Radiograph / Clinical Test Dialog.
 *
 * Allows clinicians to attach diagnostic imaging, periodontal charting, CBCT scans,
 * or laboratory investigations directly to a patient's electronic health record with
 * explicit control over immediate release to the patient portal.
 */
@Composable
fun AddReportDialog(
    patient: Patient,
    onDismiss: () -> Unit,
    onSave: (
        kind: String,
        title: String,
        clinician: String,
        summary: String,
        releasedImmediately: Boolean
    ) -> Unit
) {
    val kinds = listOf("Radiograph", "Charting", "CBCT Scan", "Chairside test", "Lab Report")
    var selectedKind by remember { mutableStateOf(kinds[0]) }

    var title by remember { mutableStateOf("") }
    var selectedClinician by remember {
        mutableStateOf(DentalRepository.clinicians.firstOrNull()?.name ?: "Dr. Ingrid Halvorsen")
    }
    var summary by remember { mutableStateOf("") }
    var releasedImmediately by remember { mutableStateOf(false) }

    val quickTitles = listOf(
        "Periapical Radiograph Tooth #19",
        "Bite-wing Radiographs (Right & Left)",
        "OPG Panoramic Radiograph",
        "CBCT 3D Scan #19 Apical Region",
        "Full mouth 6-point periodontal chart",
        "Cold Pulp Vitality Test #18-#20"
    )

    val quickTemplates = listOf(
        "No interproximal caries; crestal bone height stable.",
        "Persistent radiolucency at root apex; non-vital pulp response.",
        "Generalised 2-3mm probing depths with isolated 5mm bleeding pocket."
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(
                modifier = Modifier
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
                            text = "New Diagnostic Record",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            ),
                            color = ThornburyInk
                        )
                        Text(
                            text = "Patient: ${patient.name} (${patient.opNo})",
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

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Kind Selector
                Text(
                    text = "Investigation Kind",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    kinds.forEach { kind ->
                        val isSelected = kind == selectedKind
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedKind = kind },
                            label = {
                                Text(
                                    text = kind,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (kind) {
                                        "Radiograph" -> Icons.Default.Image
                                        "CBCT Scan" -> Icons.Default.ViewInAr
                                        "Charting" -> Icons.Default.FormatListNumbered
                                        "Chairside test" -> Icons.Default.Science
                                        else -> Icons.Default.Biotech
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ThornburyPrimary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                                containerColor = ThornburySurfaceSoft,
                                labelColor = ThornburyInk
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Title Field & Quick Suggestions
                Text(
                    text = "Report / Image Title",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Periapical Radiograph Tooth #19") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThornburyPrimary,
                        unfocusedBorderColor = ThornburyHairline
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Title Suggestions Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickTitles.forEach { quickTitle ->
                        Surface(
                            modifier = Modifier.clickable { title = quickTitle },
                            shape = RoundedCornerShape(12.dp),
                            color = ThornburySurfaceSoft,
                            border = BorderStroke(1.dp, ThornburyHairlineSoft)
                        ) {
                            Text(
                                text = quickTitle,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = ThornburyPrimaryText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Attending Clinician Selector
                Text(
                    text = "Attending Clinician",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DentalRepository.clinicians.forEach { clinician ->
                        val isSelected = clinician.name == selectedClinician
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedClinician = clinician.name },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) ThornburySurfaceSoft else ThornburyCanvas,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) ThornburyPrimary else ThornburyHairline
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedClinician = clinician.name },
                                    colors = RadioButtonDefaults.colors(selectedColor = ThornburyPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = clinician.name,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = ThornburyInk
                                    )
                                    Text(
                                        text = "${clinician.specialty} • ${clinician.room}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ThornburyMuted
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Clinical Findings / Diagnostic Summary
                Text(
                    text = "Clinical Findings & Diagnostic Summary",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    placeholder = { Text("Enter detailed findings, bone levels, radiographic signs, or measurements...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThornburyPrimary,
                        unfocusedBorderColor = ThornburyHairline
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Quick phrase inserts
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickTemplates.forEach { template ->
                        Surface(
                            modifier = Modifier.clickable {
                                summary = if (summary.isBlank()) template else "$summary $template"
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = ThornburySurfaceSoft,
                            border = BorderStroke(1.dp, ThornburyHairlineSoft)
                        ) {
                            Text(
                                text = "+ \"${template.take(34)}...\"",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = ThornburyMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. Patient Safety Release Switch
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (releasedImmediately) ThornburySuccessWash else ThornburySurfaceSoft,
                    border = BorderStroke(
                        1.dp,
                        if (releasedImmediately) ThornburySuccess.copy(alpha = 0.5f) else ThornburyHairline
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (releasedImmediately) Icons.Default.Visibility else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (releasedImmediately) ThornburySuccess else ThornburyWarning,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Save & Release Immediately",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (releasedImmediately) ThornburySuccess else ThornburyInk
                                )
                            }
                            Text(
                                text = if (releasedImmediately)
                                    "Result will instantly be visible in patient's portal."
                                else
                                    "Result remains 'Held in Surgery' until clinician signs off.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = ThornburyMuted
                            )
                        }

                        Switch(
                            checked = releasedImmediately,
                            onCheckedChange = { releasedImmediately = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ThornburySuccess,
                                uncheckedThumbColor = ThornburyMuted,
                                uncheckedTrackColor = ThornburyHairline
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Cancel & Save
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
                            if (title.isNotBlank() && summary.isNotBlank()) {
                                onSave(
                                    selectedKind,
                                    title.trim(),
                                    selectedClinician,
                                    summary.trim(),
                                    releasedImmediately
                                )
                            }
                        },
                        enabled = title.isNotBlank() && summary.isNotBlank(),
                        modifier = Modifier.weight(1.6f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThornburyPrimary,
                            contentColor = Color.White,
                            disabledContainerColor = ThornburyPrimaryDisabled,
                            disabledContentColor = ThornburyMuted
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save Diagnostic Record",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
