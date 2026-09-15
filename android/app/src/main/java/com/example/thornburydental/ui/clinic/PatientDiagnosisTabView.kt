package com.example.thornburydental.ui.clinic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thornburydental.data.Patient
import com.example.thornburydental.theme.*

/**
 * Dedicated Clinical Diagnosis Tab View.
 * Displays the patient's singular, persistent clinical diagnosis, findings, prognosis,
 * and systemic considerations, with full edit/update functionality.
 */
@Composable
fun PatientDiagnosisTabView(
    patient: Patient,
    onEditDiagnosisClick: () -> Unit,
    onCreatePlanClick: () -> Unit
) {
    val diagnosis = patient.diagnosis

    Column(modifier = Modifier.fillMaxSize()) {
        // Diagnosis Action Header Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ThornburyCanvas,
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Clinical Diagnosis",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    Text(
                        text = if (diagnosis != null) "Last updated: ${diagnosis.lastUpdated} by ${diagnosis.clinicianName}"
                        else "One-time primary clinical diagnosis",
                        style = MaterialTheme.typography.labelSmall,
                        color = ThornburyMuted
                    )
                }

                Button(
                    onClick = onEditDiagnosisClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThornburyPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (diagnosis != null) Icons.Default.Edit else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (diagnosis != null) "Edit Diagnosis" else "Record Diagnosis",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        if (diagnosis == null) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ThornburySurfaceSoft),
                    border = BorderStroke(1.dp, ThornburyHairline)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = ThornburyCanvas,
                            border = BorderStroke(1.dp, ThornburyHairlineSoft),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MedicalInformation,
                                    contentDescription = null,
                                    tint = ThornburyPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No Clinical Diagnosis Recorded",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Record the patient's primary diagnosis, clinical findings, and prognosis. This persistent record forms the clinical basis for all subsequent treatment plans.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = onEditDiagnosisClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Record Clinical Diagnosis", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            return
        }

        // Populated Diagnosis View
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Primary Diagnosis Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
                border = BorderStroke(1.dp, ThornburyPrimary)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MedicalInformation,
                                contentDescription = null,
                                tint = ThornburyPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PRIMARY CLINICAL DIAGNOSIS",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = ThornburyPrimary
                            )
                        }

                        // Prognosis Badge
                        val progColor = when (diagnosis.prognosis.lowercase()) {
                            "favourable", "good" -> ThornburyAccentTeal
                            "guarded", "questionable" -> ThornburyWarning
                            "poor" -> ThornburyError
                            else -> ThornburyPrimary
                        }
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = progColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, progColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "Prognosis: ${diagnosis.prognosis}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = progColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = diagnosis.primaryDiagnosis,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
                        color = ThornburyInk
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = ThornburyHairlineSoft)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "First recorded: ${diagnosis.dateRecorded}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ThornburyMuted
                        )
                        Text(
                            text = "Attending: ${diagnosis.clinicianName}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = ThornburyInk
                        )
                    }
                }
            }

            // Clinical Findings & Diagnostic Evidence Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard),
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FactCheck,
                            contentDescription = null,
                            tint = ThornburyPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Clinical Findings & Examination Evidence",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (diagnosis.clinicalFindings.isNotBlank()) diagnosis.clinicalFindings
                        else "No detailed findings documented.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (diagnosis.clinicalFindings.isNotBlank()) ThornburyInk else ThornburyMuted
                    )
                }
            }

            // Systemic & Medical Considerations Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard),
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = ThornburyAccentTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Systemic & Medical Considerations",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (diagnosis.systemicConsiderations.isNotBlank()) diagnosis.systemicConsiderations
                        else "None noted.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (diagnosis.systemicConsiderations.isNotBlank()) ThornburyInk else ThornburyMuted
                    )
                }
            }

            // Next Actions / Treatment Planning Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = ThornburySurfaceSoft,
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Multiple Treatment Plans",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                        Text(
                            text = "You can create multiple distinct treatment phases (e.g. Phase 1, Phase 2, Maintenance) linked to this diagnosis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyMuted
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onCreatePlanClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Plan", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}
