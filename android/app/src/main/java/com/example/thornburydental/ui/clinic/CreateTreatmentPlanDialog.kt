package com.example.thornburydental.ui.clinic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontFamily
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
import java.util.Locale

/**
 * Pre-configured Dental Procedure Template with Standard ADA CDT Codes.
 */
private data class ProcedurePreset(
    val code: String,
    val name: String,
    val fee: Double,
    val requiresTooth: Boolean = false
)

private val StandardDentalProcedures = listOf(
    ProcedurePreset("D0150", "Comprehensive Oral Evaluation", 95.0, false),
    ProcedurePreset("D1110", "Prophylaxis - Adult Dental Cleaning", 110.0, false),
    ProcedurePreset("D2392", "Resin Composite Restoration - 2 surfaces", 240.0, true),
    ProcedurePreset("D2750", "Full Porcelain Crown", 1150.0, true),
    ProcedurePreset("D3330", "Molar Endodontic Root Canal Therapy", 1250.0, true),
    ProcedurePreset("D4341", "Periodontal Scaling & Root Planing", 280.0, false),
    ProcedurePreset("D6010", "Surgical Placement of Implant Body", 2100.0, true)
)

/**
 * Comprehensive Treatment Plan Creation Dialog.
 *
 * Allows clinicians to specify a primary diagnosis, assign the treating clinician,
 * assemble staged clinical procedures using standard ADA CDT codes, assign targeted
 * teeth numbers (1-32 or general), calculate fees in real-time, and publish immutable plans.
 */
@Composable
fun CreateTreatmentPlanDialog(
    patient: Patient,
    onDismiss: () -> Unit,
    onSave: (diagnosis: String, clinicianName: String, steps: List<PlanStep>) -> Unit
) {
    var diagnosis by remember { mutableStateOf("") }
    var selectedClinician by remember {
        mutableStateOf(DentalRepository.clinicians.firstOrNull()?.name ?: "Dr. Ingrid Halvorsen")
    }

    val steps = remember { mutableStateListOf<PlanStep>() }

    // Step addition staging inputs
    var stagedToothNumber by remember { mutableStateOf<Int?>(null) }
    var isCustomProcedureOpen by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customCode by remember { mutableStateOf("") }
    var customFee by remember { mutableStateOf("") }

    val quickDiagnosisList = listOf(
        "Class II recurrent caries #30, localized gingivitis",
        "Symptomatic irreversible pulpitis #19 requiring endodontics",
        "Generalised Stage III Periodontitis - active deep pockets",
        "Missing tooth #19; candidate for single dental implant",
        "Defective restoration #14 with fractured disto-lingual cusp"
    )

    val totalFee = steps.sumOf { it.fee }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
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
                                fontFamily = FontFamily.Serif
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

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. Clinical Diagnosis
                    Text(
                        text = "Clinical Diagnosis",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = diagnosis,
                        onValueChange = { diagnosis = it },
                        placeholder = { Text("e.g. Class II recurrent caries #30, localized gingivitis") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThornburyPrimary,
                            unfocusedBorderColor = ThornburyHairline
                        ),
                        singleLine = false,
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick Diagnosis Suggestions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickDiagnosisList.forEach { diag ->
                            Surface(
                                modifier = Modifier.clickable { diagnosis = diag },
                                shape = RoundedCornerShape(12.dp),
                                color = ThornburySurfaceSoft,
                                border = BorderStroke(1.dp, ThornburyHairlineSoft)
                            ) {
                                Text(
                                    text = diag,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = ThornburyPrimaryText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Primary Treating Clinician
                    Text(
                        text = "Lead Clinician",
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
                        DentalRepository.clinicians.forEach { clinician ->
                            val isSelected = clinician.name == selectedClinician
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedClinician = clinician.name },
                                label = {
                                    Column {
                                        Text(
                                            text = clinician.name,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        )
                                        Text(
                                            text = clinician.specialty,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = if (isSelected) Color.White.copy(alpha = 0.85f) else ThornburyMuted
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ThornburyPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = ThornburySurfaceSoft,
                                    labelColor = ThornburyInk
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Step Builder: Tooth Selector for next added procedure
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ThornburySurfaceSoft,
                        border = BorderStroke(1.dp, ThornburyHairline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Select Tooth for Procedure",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyInk
                                )
                                Text(
                                    text = if (stagedToothNumber != null) "Selected: Tooth #${stagedToothNumber}" else "General (Full Arch)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ThornburyPrimary
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Tooth selection horizontal list
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // General (null) chip
                                FilterChip(
                                    selected = stagedToothNumber == null,
                                    onClick = { stagedToothNumber = null },
                                    label = { Text("General", style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ThornburyPrimary,
                                        selectedLabelColor = Color.White,
                                        containerColor = ThornburyCanvas
                                    )
                                )

                                // Teeth 1 to 32
                                for (tooth in 1..32) {
                                    val isSelected = stagedToothNumber == tooth
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { stagedToothNumber = tooth },
                                        label = { Text("#$tooth", style = MaterialTheme.typography.labelSmall) },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ThornburyPrimary,
                                            selectedLabelColor = Color.White,
                                            containerColor = ThornburyCanvas
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Quick Procedure Suggestions List
                    Text(
                        text = "Quick Procedure Suggestions (Click to Add)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StandardDentalProcedures.forEach { proc ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newStep = PlanStep(
                                            id = "s-${System.currentTimeMillis()}-${steps.size + 1}",
                                            toothNumber = if (proc.requiresTooth) (stagedToothNumber ?: 19) else stagedToothNumber,
                                            procedure = proc.name,
                                            code = proc.code,
                                            fee = proc.fee,
                                            completed = false
                                        )
                                        steps.add(newStep)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = ThornburyCanvas,
                                border = BorderStroke(1.dp, ThornburyHairlineSoft)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = ThornburyPrimaryWash,
                                            border = BorderStroke(1.dp, ThornburyPrimary.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                text = proc.code,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = ThornburyPrimaryText
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = proc.name,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                color = ThornburyInk
                                            )
                                            if (proc.requiresTooth) {
                                                Text(
                                                    text = if (stagedToothNumber != null) "Will assign to Tooth #$stagedToothNumber" else "Target: Tooth # (pick above)",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = ThornburyMuted
                                                )
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = String.format(Locale.US, "$%,.0f", proc.fee),
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = ThornburyInk
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.AddCircleOutline,
                                            contentDescription = "Add Step",
                                            tint = ThornburyPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option to expand Custom Procedure builder
                    TextButton(
                        onClick = { isCustomProcedureOpen = !isCustomProcedureOpen },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            imageVector = if (isCustomProcedureOpen) Icons.Default.ExpandLess else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = ThornburyPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCustomProcedureOpen) "Hide Custom Procedure" else "+ Add Custom Procedure",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyPrimary
                        )
                    }

                    if (isCustomProcedureOpen) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ThornburySurfaceSoft,
                            border = BorderStroke(1.dp, ThornburyHairline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                OutlinedTextField(
                                    value = customName,
                                    onValueChange = { customName = it },
                                    label = { Text("Procedure Description") },
                                    placeholder = { Text("e.g. Core build-up including pins") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customCode,
                                        onValueChange = { customCode = it },
                                        label = { Text("ADA Code") },
                                        placeholder = { Text("D2950") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    OutlinedTextField(
                                        value = customFee,
                                        onValueChange = { customFee = it },
                                        label = { Text("Fee ($)") },
                                        placeholder = { Text("295.00") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        val feeVal = customFee.toDoubleOrNull() ?: 0.0
                                        if (customName.isNotBlank() && feeVal > 0) {
                                            steps.add(
                                                PlanStep(
                                                    id = "s-${System.currentTimeMillis()}-${steps.size + 1}",
                                                    toothNumber = stagedToothNumber,
                                                    procedure = customName.trim(),
                                                    code = if (customCode.isNotBlank()) customCode.trim().uppercase() else "D9999",
                                                    fee = feeVal,
                                                    completed = false
                                                )
                                            )
                                            customName = ""
                                            customCode = ""
                                            customFee = ""
                                            isCustomProcedureOpen = false
                                        }
                                    },
                                    enabled = customName.isNotBlank() && customFee.toDoubleOrNull() != null,
                                    modifier = Modifier.align(Alignment.End),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ThornburyPrimary,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Add to Plan", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. Configured Plan Steps List
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Assembled Steps (${steps.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                        if (steps.isNotEmpty()) {
                            TextButton(onClick = { steps.clear() }) {
                                Text("Clear All", style = MaterialTheme.typography.labelSmall, color = ThornburyError)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (steps.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = ThornburyCanvas,
                            border = BorderStroke(1.dp, ThornburyHairline)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No procedures added yet. Select from the quick suggestions above to construct the treatment sequence.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThornburyMuted
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            steps.forEachIndexed { idx, step ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = ThornburySurfaceCard,
                                    border = BorderStroke(1.dp, ThornburyHairline)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // Step sequence number
                                            Surface(
                                                shape = CircleShape,
                                                color = ThornburyPrimary,
                                                contentColor = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "${idx + 1}",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp
                                                        )
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = step.procedure,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = ThornburyInk
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = ThornburyCanvas,
                                                        border = BorderStroke(1.dp, ThornburyHairlineSoft)
                                                    ) {
                                                        Text(
                                                            text = step.code,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontFamily = FontFamily.Monospace,
                                                                fontSize = 10.sp
                                                            ),
                                                            color = ThornburyPrimaryText
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = if (step.toothNumber != null) "Tooth #${step.toothNumber}" else "General / Full mouth",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                    color = ThornburyMuted
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = String.format(Locale.US, "$%,.2f", step.fee),
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                ),
                                                color = ThornburyInk
                                            )
                                            IconButton(
                                                onClick = { steps.removeAt(idx) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Remove Step",
                                                    tint = ThornburyError,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // =============================================================
                // Bottom Fixed Summary Bar: Live Total Fee & Submit
                // =============================================================
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ThornburySurfaceSoft,
                    border = BorderStroke(1.dp, ThornburyHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Estimated Total Fee",
                                style = MaterialTheme.typography.labelSmall,
                                color = ThornburyMuted
                            )
                            Text(
                                text = String.format(Locale.US, "$%,.2f", totalFee),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = ThornburyPrimaryText
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ThornburyHairline)
                            ) {
                                Text("Cancel", color = ThornburyInk)
                            }

                            Button(
                                onClick = {
                                    if (diagnosis.isNotBlank() && steps.isNotEmpty()) {
                                        onSave(diagnosis.trim(), selectedClinician, steps.toList())
                                    }
                                },
                                enabled = diagnosis.isNotBlank() && steps.isNotEmpty(),
                                shape = RoundedCornerShape(8.dp),
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
                                    text = "Publish Plan",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
