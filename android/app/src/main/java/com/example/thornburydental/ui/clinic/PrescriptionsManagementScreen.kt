package com.example.thornburydental.ui.clinic

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.Patient
import com.example.thornburydental.theme.*

@Composable
fun PrescriptionsManagementScreen(
    onOpenPatientChart: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prescriptions by DentalRepository.prescriptions.collectAsState()
    val patients by DentalRepository.patients.collectAsState()
    val context = LocalContext.current

    var showIssueDialogForPatient by remember { mutableStateOf<Patient?>(null) }
    var showSelectPatientDialog by remember { mutableStateOf(false) }

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Prescription Registry",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Serif,
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
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(prescriptions, key = { it.id }) { rx ->
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

    // Select Patient Dialog
    if (showSelectPatientDialog) {
        AlertDialog(
            onDismissRequest = { showSelectPatientDialog = false },
            title = {
                Text(
                    text = "Select Patient to Prescribe",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
                    color = ThornburyInk
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    patients.forEach { p ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = ThornburySurfaceSoft,
                            border = BorderStroke(1.dp, ThornburyHairline),
                            onClick = {
                                showIssueDialogForPatient = p
                                showSelectPatientDialog = false
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
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSelectPatientDialog = false }) {
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
