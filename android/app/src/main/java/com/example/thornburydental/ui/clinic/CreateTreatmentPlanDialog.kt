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
    onSave: (title: String, clinicianName: String, steps: List<PlanStep>) -> Unit
) {
    var planTitle by remember { mutableStateOf("Phase 1: Comprehensive Treatment") }
    var treatmentPlanDetails by remember { mutableStateOf("") }
    var clinicianName by remember { mutableStateOf(DentalRepository.clinicians.firstOrNull()?.name ?: "") }

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
                            text = "New Treatment Plan",
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
                    // Patient Clinical Diagnosis Reference Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = ThornburySurfaceSoft,
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalInformation,
                                contentDescription = null,
                                tint = ThornburyPrimary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "PATIENT CLINICAL DIAGNOSIS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = ThornburyMuted
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                val currentDiag = patient.diagnosis?.primaryDiagnosis
                                Text(
                                    text = if (!currentDiag.isNullOrBlank()) currentDiag else "No primary diagnosis recorded yet in patient chart.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (!currentDiag.isNullOrBlank()) ThornburyInk else ThornburyMuted
                                )
                                if (patient.diagnosis?.prognosis != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Prognosis: ${patient.diagnosis.prognosis}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ThornburyAccentTeal
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // FIELD 1: PLAN TITLE
                    Text(
                        text = "Treatment Plan Title *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = planTitle,
                        onValueChange = { planTitle = it },
                        placeholder = { Text("e.g. Phase 1: Restorative & Scaling, Implant Placement...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = thornburyTextFieldColors(containerColor = ThornburyCanvas),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Attending Clinician
                    Text(
                        text = "Attending Clinician *",
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // FIELD 2: TREATMENT PLAN PROCEDURES
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
                                    text = "Procedure Steps & Sequence *",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyInk
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Enter each clinical procedure on a separate line",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyMuted
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = treatmentPlanDetails,
                                onValueChange = { treatmentPlanDetails = it },
                                placeholder = {
                                    Text("Line 1: Full mouth periodontal probing and charting\nLine 2: Tooth #30 DO composite resin restoration\nLine 3: Tooth #19 core build-up and crown preparation")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = thornburyTextFieldColors(containerColor = ThornburyCanvas)
                            )
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
                            if (planTitle.isNotBlank() && treatmentPlanDetails.isNotBlank()) {
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
                                onSave(planTitle.trim(), clinicianName, planSteps)
                            }
                        },
                        enabled = planTitle.isNotBlank() && treatmentPlanDetails.isNotBlank(),
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
                            text = "Save Plan",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
