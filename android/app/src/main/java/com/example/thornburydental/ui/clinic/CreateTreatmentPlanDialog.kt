package com.example.thornburydental.ui.clinic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.runtime.snapshots.SnapshotStateList
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
    var toothInput by mutableStateOf(initialTooth)
    var procedure by mutableStateOf(initialProcedure)
}

fun parseToothNumbers(input: String): List<Int> {
    if (input.isBlank()) return emptyList()
    val numbers = mutableListOf<Int>()
    val parts = input.split(Regex("""[,;\s]+"""))
    for (part in parts) {
        val trimmed = part.trim()
        if (trimmed.contains("-")) {
            val rangeParts = trimmed.split("-")
            if (rangeParts.size == 2) {
                val start = rangeParts[0].filter { it.isDigit() }.toIntOrNull()
                val end = rangeParts[1].filter { it.isDigit() }.toIntOrNull()
                if (start != null && end != null && start <= end) {
                    for (t in start..end) {
                        if (!numbers.contains(t)) numbers.add(t)
                    }
                }
            }
        } else {
            val single = trimmed.filter { it.isDigit() }.toIntOrNull()
            if (single != null && !numbers.contains(single)) {
                numbers.add(single)
            }
        }
    }
    return numbers
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
    var clinicianName by remember { mutableStateOf(DentalRepository.clinicians.firstOrNull()?.name ?: "Dr. Ingrid Halvorsen") }

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
                .adaptiveDialogWidth(680.dp)
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
                        text = "Attending Clinician",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
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
                                    text = activeClinician?.name ?: clinicianName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = ThornburyInk
                                )
                                Text(
                                    text = activeClinician?.specialty ?: "Comprehensive Dental Care",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ThornburyMuted
                                )
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
                                                    val targetStep = steps.find { it.toothInput.isBlank() && it.procedure.isBlank() }
                                                        ?: steps.lastOrNull()
                                                    if (targetStep != null) {
                                                        if (targetStep.toothInput.isBlank()) {
                                                            targetStep.toothInput = tooth
                                                        } else {
                                                            val existing = parseToothNumbers(targetStep.toothInput)
                                                            val tInt = tooth.toIntOrNull()
                                                            if (tInt != null && !existing.contains(tInt)) {
                                                                targetStep.toothInput = "${targetStep.toothInput}, $tooth"
                                                            }
                                                        }
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
                                                // Teeth # input with interactive arch modal picker
                                                ToothSelectorField(
                                                    toothInput = step.toothInput,
                                                    onToothInputChanged = { input -> step.toothInput = input },
                                                    isChild = patient.isChild,
                                                    modifier = Modifier.width(140.dp)
                                                )

                                                // Procedure Description input
                                                OutlinedTextField(
                                                    value = step.procedure,
                                                    onValueChange = { input ->
                                                        step.procedure = input
                                                        // Smart extraction: if toothInput is blank, detect #XX or Tooth XX
                                                        if (step.toothInput.isBlank()) {
                                                            val match = Regex("""(?:Tooth\s*#?|#)\s*([1-9]|[12][0-9]|3[0-2])\b""", RegexOption.IGNORE_CASE).find(input)
                                                            if (match != null) {
                                                                step.toothInput = match.groupValues[1]
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
                                    val parsedTeeth = parseToothNumbers(s.toothInput)
                                    PlanStep(
                                        id = "step-${idx + 1}",
                                        toothNumber = parsedTeeth.firstOrNull(),
                                        toothNumbers = parsedTeeth,
                                        procedure = s.procedure.trim(),
                                        code = "",
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

@Composable
fun ToothSelectorField(
    toothInput: String,
    onToothInputChanged: (String) -> Unit,
    isChild: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showModal by remember { mutableStateOf(false) }
    val parsedTeeth = remember(toothInput) { parseToothNumbers(toothInput) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = toothInput,
            onValueChange = { input ->
                onToothInputChanged(input.filter { it.isDigit() || it == ',' || it == '-' || it == ' ' || it == ';' })
            },
            label = { Text("Tooth (FDI)", fontSize = 11.sp) },
            placeholder = { Text(if (isChild) "e.g. 51, 52" else "e.g. 11, 12", fontSize = 10.sp) },
            trailingIcon = {
                IconButton(
                    onClick = { showModal = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = "Tooth Selector Arch",
                        tint = ThornburyPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = thornburyTextFieldColors(containerColor = ThornburyCanvas)
        )

        if (parsedTeeth.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayList = if (parsedTeeth.size > 3) parsedTeeth.take(3) else parsedTeeth
                displayList.forEach { t ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ThornburyPrimary.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, ThornburyPrimary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "$t",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            color = ThornburyPrimaryText,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                if (parsedTeeth.size > 3) {
                    Text(
                        text = "+${parsedTeeth.size - 3}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = ThornburyMuted
                    )
                }
            }
        }
    }

    if (showModal) {
        ToothSelectionModalDialog(
            isChild = isChild,
            selectedTeeth = parsedTeeth,
            onDismiss = { showModal = false },
            onTeethSelected = { updatedTeeth ->
                onToothInputChanged(updatedTeeth.joinToString(", "))
                showModal = false
            }
        )
    }
}

@Composable
fun ToothSelectionModalDialog(
    isChild: Boolean = false,
    selectedTeeth: List<Int>,
    onDismiss: () -> Unit,
    onTeethSelected: (List<Int>) -> Unit
) {
    val currentSelection = remember { mutableStateListOf<Int>().apply { addAll(selectedTeeth) } }

    val q1Teeth = if (isChild) listOf(55, 54, 53, 52, 51) else listOf(18, 17, 16, 15, 14, 13, 12, 11)
    val q2Teeth = if (isChild) listOf(61, 62, 63, 64, 65) else listOf(21, 22, 23, 24, 25, 26, 27, 28)
    val q3Teeth = if (isChild) listOf(71, 72, 73, 74, 75) else listOf(31, 32, 33, 34, 35, 36, 37, 38)
    val q4Teeth = if (isChild) listOf(85, 84, 83, 82, 81) else listOf(48, 47, 46, 45, 44, 43, 42, 41)

    val upperArch = q1Teeth + q2Teeth
    val lowerArch = q4Teeth + q3Teeth

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .adaptiveDialogWidth(560.dp)
                .fillMaxWidth(0.95f)
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isChild) "Tooth Selector (FDI Pediatric • 20 Teeth)" else "Tooth Selector (FDI Permanent • 32 Teeth)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                        Text(
                            text = if (currentSelection.isEmpty()) "General / Non-tooth specific" else "Selected: ${currentSelection.size} teeth (${currentSelection.sorted().joinToString { "$it" }})",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyMuted
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = ThornburyInk)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick selector buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = upperArch.all { currentSelection.contains(it) },
                        onClick = {
                            if (upperArch.all { currentSelection.contains(it) }) {
                                currentSelection.removeAll(upperArch)
                            } else {
                                upperArch.forEach { if (!currentSelection.contains(it)) currentSelection.add(it) }
                            }
                        },
                        label = { Text(if (isChild) "Upper Primary (51-65)" else "Upper Arch (11-28)", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = lowerArch.all { currentSelection.contains(it) },
                        onClick = {
                            if (lowerArch.all { currentSelection.contains(it) }) {
                                currentSelection.removeAll(lowerArch)
                            } else {
                                lowerArch.forEach { if (!currentSelection.contains(it)) currentSelection.add(it) }
                            }
                        },
                        label = { Text(if (isChild) "Lower Primary (71-85)" else "Lower Arch (31-48)", fontSize = 11.sp) }
                    )
                    if (currentSelection.isNotEmpty()) {
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = { currentSelection.clear() },
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("Clear", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // UPPER ARCH (MAXILLARY)
                    Column {
                        Text(
                            text = if (isChild) "UPPER PRIMARY ARCH (MAXILLARY)" else "UPPER ARCH (MAXILLARY)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                            color = ThornburyPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuadrantBox(
                                title = if (isChild) "Q5 • Upper Right (55–51)" else "Q1 • Upper Right (18–11)",
                                teeth = q1Teeth,
                                currentSelection = currentSelection,
                                modifier = Modifier.weight(1f)
                            )
                            QuadrantBox(
                                title = if (isChild) "Q6 • Upper Left (61–65)" else "Q2 • Upper Left (21–28)",
                                teeth = q2Teeth,
                                currentSelection = currentSelection,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // LOWER ARCH (MANDIBULAR)
                    Column {
                        Text(
                            text = if (isChild) "LOWER PRIMARY ARCH (MANDIBULAR)" else "LOWER ARCH (MANDIBULAR)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                            color = ThornburyPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuadrantBox(
                                title = if (isChild) "Q8 • Lower Right (85–81)" else "Q4 • Lower Right (48–41)",
                                teeth = q4Teeth,
                                currentSelection = currentSelection,
                                modifier = Modifier.weight(1f)
                            )
                            QuadrantBox(
                                title = if (isChild) "Q7 • Lower Left (71–75)" else "Q3 • Lower Left (31–38)",
                                teeth = q3Teeth,
                                currentSelection = currentSelection,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onTeethSelected(currentSelection.sorted())
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary)
                    ) {
                        Text("Apply Selection", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuadrantBox(
    title: String,
    teeth: List<Int>,
    currentSelection: SnapshotStateList<Int>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = ThornburySurfaceSoft,
        border = BorderStroke(0.5.dp, ThornburyHairline)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            val isAllSelected = teeth.all { currentSelection.contains(it) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isAllSelected) {
                            currentSelection.removeAll(teeth)
                        } else {
                            teeth.forEach { if (!currentSelection.contains(it)) currentSelection.add(it) }
                        }
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = ThornburyInk
                )
                Text(
                    text = if (isAllSelected) "Deselect" else "All",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = ThornburyPrimary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (teeth.size <= 5) {
                // Pediatric Quadrant (5 Primary Teeth in 1 clean single row)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    teeth.forEach { toothNum ->
                        ToothGridItem(
                            toothNumber = toothNum,
                            isSelected = currentSelection.contains(toothNum),
                            onClick = {
                                if (currentSelection.contains(toothNum)) currentSelection.remove(toothNum)
                                else currentSelection.add(toothNum)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                // Permanent Adult Quadrant (8 Teeth split into 2 uniform rows of 4)
                val row1 = teeth.take(4)
                val row2 = teeth.drop(4)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row1.forEach { toothNum ->
                            ToothGridItem(
                                toothNumber = toothNum,
                                isSelected = currentSelection.contains(toothNum),
                                onClick = {
                                    if (currentSelection.contains(toothNum)) currentSelection.remove(toothNum)
                                    else currentSelection.add(toothNum)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row2.forEach { toothNum ->
                            ToothGridItem(
                                toothNumber = toothNum,
                                isSelected = currentSelection.contains(toothNum),
                                onClick = {
                                    if (currentSelection.contains(toothNum)) currentSelection.remove(toothNum)
                                    else currentSelection.add(toothNum)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row2.size < 4) {
                            repeat(4 - row2.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToothGridItem(
    toothNumber: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .sizeIn(maxWidth = 36.dp, maxHeight = 36.dp)
                .aspectRatio(1f)
                .clickable(onClick = onClick),
            shape = CircleShape,
            color = if (isSelected) ThornburyPrimary else ThornburyCanvas,
            border = BorderStroke(
                1.dp,
                if (isSelected) ThornburyPrimary else ThornburyHairline
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$toothNumber",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) Color.White else ThornburyInk
                )
            }
        }
    }
}
