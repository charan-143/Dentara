package com.example.thornburydental.ui.clinic

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.Patient
import com.example.thornburydental.theme.*

private data class DrugOption(
    val name: String,
    val defaultDosage: String,
    val defaultFrequency: String,
    val defaultDuration: String,
    val defaultInstructions: String
)

private val DentalFormulary = listOf(
    DrugOption("Amoxicillin", "500 mg capsules", "1 capsule every 8 hours", "5 days", "Take with water. Complete the entire course."),
    DrugOption("Clindamycin", "300 mg capsules", "1 capsule every 6 hours", "7 days", "Penicillin-allergic option. Take with plenty of water."),
    DrugOption("Metronidazole", "400 mg tablets", "1 tablet every 8 hours", "5 days", "Avoid all alcohol during treatment and for 48 hours after."),
    DrugOption("Ibuprofen", "600 mg tablets", "1 tablet every 6 to 8 hours PRN", "3 days", "Take strictly with food or milk. Max 2400mg in 24 hours."),
    DrugOption("Paracetamol", "500 mg tablets", "2 tablets every 6 hours PRN", "3 days", "Max 4000mg in 24 hours. Do not take with other acetaminophen."),
    DrugOption("Chlorhexidine 0.2%", "300 mL rinse", "15 mL twice daily", "7 days", "Rinse for 60 seconds after brushing. Do not swallow.")
)

@Composable
fun IssuePrescriptionDialog(
    patient: Patient,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current

    var selectedDrugIndex by remember { mutableStateOf(0) }
    var selectedClinician by remember { mutableStateOf(DentalRepository.clinicians[0].name) }

    val drug = DentalFormulary[selectedDrugIndex]
    var dosage by remember(drug) { mutableStateOf(drug.defaultDosage) }
    var frequency by remember(drug) { mutableStateOf(drug.defaultFrequency) }
    var duration by remember(drug) { mutableStateOf(drug.defaultDuration) }
    var instructions by remember(drug) { mutableStateOf(drug.defaultInstructions) }

    // Real-time Allergy Cross-Check!
    val allergyWarning = remember(drug, patient) {
        DentalRepository.checkAllergyConflict(patient, drug.name)
    }

    var overrideConfirmed by remember { mutableStateOf(false) }
    var overrideReason by remember { mutableStateOf("") }

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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DentalRepository.clinicians.forEach { c ->
                        val isSelected = c.name == selectedClinician
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedClinician = c.name },
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
                                    onClick = { selectedClinician = c.name },
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

                Spacer(modifier = Modifier.height(14.dp))

                // Drug Formulary Selector
                Text(
                    text = "Select Drug from Formulary",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
                Spacer(modifier = Modifier.height(6.dp))

                DentalFormulary.forEachIndexed { idx, item ->
                    val isSelected = idx == selectedDrugIndex
                    val hasConflict = DentalRepository.checkAllergyConflict(patient, item.name) != null
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clickable { selectedDrugIndex = idx },
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
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (hasConflict) ThornburyError else ThornburyInk
                            )
                            if (hasConflict) {
                                Text(
                                    text = "Allergy Conflict",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyError
                                )
                            } else {
                                Text(
                                    text = item.defaultDosage,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ThornburyMuted
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Dosage & Frequency
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosage / Formulation") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = thornburyTextFieldColors(containerColor = ThornburyCanvas)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = frequency,
                        onValueChange = { frequency = it },
                        label = { Text("Frequency") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = thornburyTextFieldColors(containerColor = ThornburyCanvas)
                    )
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text("Duration") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = thornburyTextFieldColors(containerColor = ThornburyCanvas)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Pharmacist & Patient Instructions") },
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
                                THORNBURY DENTAL PRACTICE
                                18 Thornbury Row, Portland, OR 97210
                                Tel: +1 (503) 224-7700
                                ----------------------------------------
                                PRESCRIPTION FOR: ${patient.name}
                                OP: ${patient.opNo} | DOB: ${patient.dob}
                                
                                Rx: ${drug.name} $dosage
                                Sig: $frequency
                                Duration: $duration
                                Instructions: $instructions
                                
                                Prescriber: $selectedClinician
                                Verified against electronic allergy ledger.
                            """.trimIndent()

                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Prescription for ${patient.name} - Thornbury Dental")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Prescription Slip"))
                        },
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
                                drugName = drug.name,
                                dosage = dosage,
                                frequency = frequency,
                                duration = duration,
                                instructions = instructions,
                                overrideReason = if (allergyWarning != null) overrideReason else null
                            )
                            onSuccess()
                        },
                        enabled = (allergyWarning == null) || (overrideConfirmed && overrideReason.isNotBlank()),
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
}
