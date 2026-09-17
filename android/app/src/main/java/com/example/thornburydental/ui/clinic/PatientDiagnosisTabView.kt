package com.example.thornburydental.ui.clinic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    onCreatePlanClick: () -> Unit = {}
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
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = "Clinical Diagnosis",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (diagnosis != null) {
                        Text(
                            text = "Last updated: ${diagnosis.lastUpdated}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ThornburyMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (diagnosis.clinicianName.isNotBlank()) {
                            Text(
                                text = "By ${diagnosis.clinicianName}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = ThornburyInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = "Initial diagnosis not yet recorded",
                            style = MaterialTheme.typography.labelSmall,
                            color = ThornburyMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                        text = if (diagnosis != null) "Edit Diagnosis" else "Create Diagnosis",
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
                            text = "Create the patient's initial primary diagnosis, clinical findings, and prognosis. Once saved, you can edit and update the diagnosis at any time as clinical findings evolve.",
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
                            Text("Create Clinical Diagnosis", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            return
        }

        val isCompact = LocalWindowWidthSizeClass.current == WindowWidthSizeClass.Compact

        // Populated Diagnosis View
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!isCompact) {
                // 2-Column Responsive Layout for Tablets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Left Column: Primary Diagnosis & Prognosis
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        PrimaryDiagnosisCard(
                            diagnosis = diagnosis,
                            onEditDiagnosisClick = onEditDiagnosisClick
                        )
                    }

                    // Right Column: Findings & Systemic Considerations
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ClinicalFindingsCard(diagnosis = diagnosis)
                        SystemicConsiderationsCard(diagnosis = diagnosis)
                    }
                }
            } else {
                // Single Column Phone Layout
                PrimaryDiagnosisCard(
                    diagnosis = diagnosis,
                    onEditDiagnosisClick = onEditDiagnosisClick
                )
                ClinicalFindingsCard(diagnosis = diagnosis)
                SystemicConsiderationsCard(diagnosis = diagnosis)
            }
        }
    }
}

@Composable
private fun PrimaryDiagnosisCard(
    diagnosis: com.example.thornburydental.data.PatientDiagnosis,
    onEditDiagnosisClick: () -> Unit
) {
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MedicalInformation,
                        contentDescription = null,
                        tint = ThornburyPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PRIMARY CLINICAL DIAGNOSIS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = ThornburyPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onEditDiagnosisClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Diagnosis",
                        tint = ThornburyPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = diagnosis.primaryDiagnosis,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 24.sp
                ),
                color = ThornburyInk
            )

            Spacer(modifier = Modifier.height(12.dp))

            val lowerProg = diagnosis.prognosis.lowercase()
            val progColor = when {
                lowerProg.contains("good") || lowerProg.contains("favour") || lowerProg.contains("excellent") -> ThornburyAccentTeal
                lowerProg.contains("guarded") || lowerProg.contains("questionable") || lowerProg.contains("fair") -> ThornburyWarning
                lowerProg.contains("poor") || lowerProg.contains("hopeless") -> ThornburyError
                else -> ThornburyPrimary
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = progColor.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, progColor.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = when {
                            progColor == ThornburyAccentTeal -> Icons.Default.CheckCircle
                            progColor == ThornburyWarning -> Icons.Default.Warning
                            progColor == ThornburyError -> Icons.Default.ErrorOutline
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = progColor,
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Prognosis:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = progColor,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = diagnosis.prognosis,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = ThornburyInk,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = ThornburyHairlineSoft)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "First recorded: ${diagnosis.dateRecorded}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ThornburyMuted
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Attending: ${diagnosis.clinicianName}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = ThornburyInk,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun ClinicalFindingsCard(diagnosis: com.example.thornburydental.data.PatientDiagnosis) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.FactCheck,
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
}

@Composable
private fun SystemicConsiderationsCard(diagnosis: com.example.thornburydental.data.PatientDiagnosis) {
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
}
