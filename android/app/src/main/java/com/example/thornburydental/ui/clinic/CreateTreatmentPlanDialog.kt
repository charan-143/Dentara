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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.Patient
import com.example.thornburydental.data.PlanStep
import com.example.thornburydental.theme.*

/**
 * Clean Two-Field Treatment Plan Creation Dialog.
 * Field 1: Diagnosis
 * Field 2: Treatment Plan Details
 * Price and fee fields are completely omitted.
 */
@Composable
fun CreateTreatmentPlanDialog(
    patient: Patient,
    onDismiss: () -> Unit,
    onSave: (diagnosis: String, clinicianName: String, steps: List<PlanStep>) -> Unit
) {
    var diagnosis by remember { mutableStateOf("") }
    var treatmentPlanDetails by remember { mutableStateOf("") }
    val clinicianName = "Dr. Ingrid Halvorsen"

    val quickDiagnosisList = listOf(
        "Class II recurrent caries #30, localized gingivitis",
        "Symptomatic irreversible pulpitis #19 requiring endodontics",
        "Generalised Stage III Periodontitis - active deep pockets",
        "Missing tooth #19; candidate for single dental implant",
        "Defective restoration #14 with fractured disto-lingual cusp"
    )

    val quickPlanTemplates = listOf(
        "Phase 1: Endodontic root canal therapy #19 + Core buildup.\nPhase 2: Full porcelain crown restoration.",
        "Phase 1: Periodontal scaling & root planing (Quads 1 & 4).\nPhase 2: 6-week re-evaluation & oral hygiene instruction.",
        "Phase 1: Resin composite restoration (2 surfaces) #30."
    )

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
                            text = "Create Treatment Plan",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
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

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // FIELD 1: DIAGNOSIS
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MedicalInformation,
                                    contentDescription = null,
                                    tint = ThornburyPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "1. Diagnosis *",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyInk
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = diagnosis,
                                onValueChange = { diagnosis = it },
                                placeholder = { Text("Enter clinical diagnosis (e.g. Irreversible pulpitis #19)...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = thornburyTextFieldColors(containerColor = ThornburyCanvas),
                                minLines = 2
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick Suggestions
                            Text(
                                text = "Quick Diagnostic Presets:",
                                style = MaterialTheme.typography.labelSmall,
                                color = ThornburyMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                quickDiagnosisList.forEach { diag ->
                                    Surface(
                                        modifier = Modifier.clickable { diagnosis = diag },
                                        shape = RoundedCornerShape(10.dp),
                                        color = ThornburyCanvas,
                                        border = BorderStroke(1.dp, ThornburyHairlineSoft)
                                    ) {
                                        Text(
                                            text = "+ ${diag.take(36)}...",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = ThornburyPrimaryText
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // FIELD 2: TREATMENT PLAN DETAILS
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Assignment,
                                    contentDescription = null,
                                    tint = ThornburyPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "2. Treatment Plan Details & Clinical Procedures *",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyInk
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = treatmentPlanDetails,
                                onValueChange = { treatmentPlanDetails = it },
                                placeholder = {
                                    Text("Enter detailed clinical treatment plan steps, procedure codes, teeth involved, and sequence...")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = thornburyTextFieldColors(containerColor = ThornburyCanvas)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick Templates
                            Text(
                                text = "Sample Plan Templates:",
                                style = MaterialTheme.typography.labelSmall,
                                color = ThornburyMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                quickPlanTemplates.forEachIndexed { idx, tmpl ->
                                    Surface(
                                        modifier = Modifier.clickable {
                                            treatmentPlanDetails = if (treatmentPlanDetails.isBlank()) tmpl else "$treatmentPlanDetails\n\n$tmpl"
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = ThornburyCanvas,
                                        border = BorderStroke(1.dp, ThornburyHairlineSoft)
                                    ) {
                                        Text(
                                            text = "+ Staged Plan ${idx + 1}",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = ThornburyPrimaryText
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
                            if (diagnosis.isNotBlank() && treatmentPlanDetails.isNotBlank()) {
                                // Convert lines into clean PlanSteps without fees
                                val lines = treatmentPlanDetails.lines().filter { it.isNotBlank() }
                                val planSteps = lines.mapIndexed { idx, line ->
                                    PlanStep(
                                        id = "step-${idx + 1}",
                                        toothNumber = null,
                                        procedure = line.trim(),
                                        code = "D${1000 + idx}",
                                        fee = 0.0,
                                        completed = false
                                    )
                                }
                                onSave(diagnosis.trim(), clinicianName, planSteps)
                            }
                        },
                        enabled = diagnosis.isNotBlank() && treatmentPlanDetails.isNotBlank(),
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
                            text = "Save Treatment Plan",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
