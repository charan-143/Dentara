package com.example.thornburydental.ui.clinic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.Patient
import com.example.thornburydental.data.PlanStep
import com.example.thornburydental.theme.*

class DraftStep(
    val id: String = java.util.UUID.randomUUID().toString(),
    initialTooth: String = "",
    initialProcedure: String = ""
) {
    var toothNumber by mutableStateOf(initialTooth)
    var procedure by mutableStateOf(initialProcedure)
}

/**
 * Treatment Plan Creation Dialog.
 * Allows entering plan title, attending clinician, and individual procedure steps
 * with explicit Tooth Number (1–32 or general) tracking.
 * Completely free of pricing and fees.
 */
@Composable
fun CreateTreatmentPlanDialog(
    patient: Patient,
    onDismiss: () -> Unit,
    onSave: (title: String, clinicianName: String, steps: List<PlanStep>) -> Unit
) {
    var planTitle by remember { mutableStateOf("Phase 1: Comprehensive Treatment") }
    var clinicianName by remember { mutableStateOf(DentalRepository.clinicians.firstOrNull()?.name ?: "") }

    // Auto-detect teeth mentioned in patient's diagnosis
    val suggestedTeeth = remember(patient) {
        val fullDiag = listOfNotNull(
            patient.diagnosis?.primaryDiagnosis,
            patient.diagnosis?.clinicalFindings
        ).joinToString(" ")
        Regex("""#(\d{1,2})""").findAll(fullDiag).map { it.groupValues[1] }.distinct().toList()
    }

    val steps = remember {
        mutableStateListOf<DraftStep>().apply {
            if (suggestedTeeth.isNotEmpty()) {
                suggestedTeeth.forEach { tooth ->
                    add(DraftStep(initialTooth = tooth, initialProcedure = ""))
                }
            } else {
                add(DraftStep(initialTooth = "", initialProcedure = ""))
            }
        }
    }

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

                    // FIELD 2: TREATMENT PLAN PROCEDURES WITH TOOTH NUMBERS
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
                                    text = "Procedures & Tooth Numbers *",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyInk
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Specify tooth number (1–32) and clinical procedure description for each step.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyMuted
                            )

                            // Quick tooth chips from diagnosis
                            if (suggestedTeeth.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Teeth in diagnosis: ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ThornburyMuted
                                    )
                                    suggestedTeeth.forEach { tooth ->
                                        Surface(
                                            modifier = Modifier
                                                .padding(horizontal = 3.dp)
                                                .clickable {
                                                    val emptyStep = steps.find { it.toothNumber.isBlank() && it.procedure.isBlank() }
                                                    if (emptyStep != null) {
                                                        emptyStep.toothNumber = tooth
                                                    } else {
                                                        steps.add(DraftStep(initialTooth = tooth))
                                                    }
                                                },
                                            shape = RoundedCornerShape(6.dp),
                                            color = ThornburyPrimary.copy(alpha = 0.1f),
                                            border = BorderStroke(0.5.dp, ThornburyPrimary.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                text = "Tooth #$tooth",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                ),
                                                color = ThornburyPrimaryText,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Steps List
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                steps.forEachIndexed { index, step ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        color = ThornburyCanvas,
                                        border = BorderStroke(1.dp, ThornburyHairline)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = ThornburySurfaceSoft,
                                                        modifier = Modifier.size(20.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = "${index + 1}",
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 10.sp
                                                                ),
                                                                color = ThornburyInk
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Step ${index + 1}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = ThornburyInk
                                                    )
                                                }

                                                if (steps.size > 1) {
                                                    IconButton(
                                                        onClick = { steps.removeAt(index) },
                                                        modifier = Modifier.size(22.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Remove step",
                                                            tint = ThornburyMuted,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Tooth # input
                                                OutlinedTextField(
                                                    value = step.toothNumber,
                                                    onValueChange = { input ->
                                                        step.toothNumber = input.filter { it.isDigit() }.take(2)
                                                    },
                                                    label = { Text("Tooth #", fontSize = 11.sp) },
                                                    placeholder = { Text("1-32", fontSize = 11.sp) },
                                                    modifier = Modifier.width(88.dp),
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = thornburyTextFieldColors(containerColor = ThornburyCanvas)
                                                )

                                                // Procedure Description input
                                                OutlinedTextField(
                                                    value = step.procedure,
                                                    onValueChange = { input ->
                                                        step.procedure = input
                                                        // Smart extraction: if toothNumber is blank, detect #XX or Tooth XX
                                                        if (step.toothNumber.isBlank()) {
                                                            val match = Regex("""(?:Tooth\s*#?|#)\s*([1-9]|[12][0-9]|3[0-2])\b""", RegexOption.IGNORE_CASE).find(input)
                                                            if (match != null) {
                                                                step.toothNumber = match.groupValues[1]
                                                            }
                                                        }
                                                    },
                                                    label = { Text("Procedure Description", fontSize = 11.sp) },
                                                    placeholder = { Text("e.g. Composite restoration, Crown prep...", fontSize = 11.sp) },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = thornburyTextFieldColors(containerColor = ThornburyCanvas)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Add Step Button
                                OutlinedButton(
                                    onClick = { steps.add(DraftStep()) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, ThornburyPrimary.copy(alpha = 0.4f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = ThornburyPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Add Another Procedure Step",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = ThornburyPrimary
                                    )
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
                            val validSteps = steps.filter { it.procedure.isNotBlank() }
                            if (planTitle.isNotBlank() && validSteps.isNotEmpty()) {
                                val planSteps = validSteps.mapIndexed { idx, s ->
                                    val toothNum = s.toothNumber.trim().toIntOrNull()
                                    PlanStep(
                                        id = "step-${idx + 1}",
                                        toothNumber = toothNum,
                                        procedure = s.procedure.trim(),
                                        code = "D${1000 + idx}",
                                        fee = 0.0,
                                        completed = false
                                    )
                                }
                                onSave(planTitle.trim(), clinicianName, planSteps)
                            }
                        },
                        enabled = planTitle.isNotBlank() && steps.any { it.procedure.isNotBlank() },
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
