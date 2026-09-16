package com.example.thornburydental.ui.clinic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thornburydental.data.PlanAddendum
import com.example.thornburydental.data.PlanStep
import com.example.thornburydental.data.TreatmentPlan
import com.example.thornburydental.theme.*

/**
 * Modern Redesigned Treatment Plans Tab.
 * Eliminates redundant cryptographic hashes and lock/unlock buttons.
 * Provides a clean, clinical overview of phased treatment plans,
 * procedure steps with tooth tracking, status indicators, and progress.
 */
@Composable
fun PatientTreatmentPlansTabView(
    plans: List<TreatmentPlan>,
    patientDiagnosis: String? = null,
    onCreatePlanClick: () -> Unit,
    onToggleStepCompletion: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ThornburyCanvas)
    ) {
        // Header Action Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ThornburyCanvas,
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = "Treatment Plans",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Phased clinical care plans & procedure progress",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThornburyMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onCreatePlanClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThornburyPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "New Plan",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // Linked Primary Diagnosis Banner (if recorded)
        if (!patientDiagnosis.isNullOrBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ThornburySurfaceSoft,
                border = BorderStroke(1.dp, ThornburyHairlineSoft)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(ThornburyPrimary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.FactCheck,
                            contentDescription = null,
                            tint = ThornburyPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PRIMARY CLINICAL DIAGNOSIS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = ThornburyPrimaryText
                        )
                        Text(
                            text = patientDiagnosis,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = ThornburyInk,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (plans.isEmpty()) {
            // Friendly Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard),
                    border = BorderStroke(1.dp, ThornburyHairline)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(ThornburySurfaceSoft, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                tint = ThornburyPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Treatment Plans Recorded",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Create a phased care plan with clinical procedure codes, tooth charting, and progress tracking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onCreatePlanClick,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Create Initial Treatment Plan",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
            return
        }

        // Summary Metric Strip across all plans
        val totalProcedures = plans.sumOf { it.steps.size }
        val totalCompleted = plans.sumOf { it.steps.count { step -> step.completed } }
        val overallProgress = if (totalProcedures > 0) totalCompleted.toFloat() / totalProcedures else 0f

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overall Clinical Progress Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ThornburySurfaceSoft),
                    border = BorderStroke(1.dp, ThornburyHairline)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = ThornburyPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Overall Treatment Progress",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyInk
                                )
                            }
                            Text(
                                text = "$totalCompleted of $totalProcedures procedures completed (${(overallProgress * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (overallProgress == 1f) ThornburyAccentTeal else ThornburyPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { overallProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (overallProgress == 1f) ThornburyAccentTeal else ThornburyPrimary,
                            trackColor = ThornburyCanvas
                        )
                    }
                }
            }

            // Treatment Plan Cards
            items(plans, key = { it.id }) { plan ->
                TreatmentPlanCard(
                    plan = plan,
                    onToggleStep = { stepId -> onToggleStepCompletion(plan.id, stepId) }
                )
            }
        }
    }
}

@Composable
private fun TreatmentPlanCard(
    plan: TreatmentPlan,
    onToggleStep: (String) -> Unit
) {
    val totalSteps = plan.steps.size
    val completedSteps = plan.steps.count { it.completed }
    val isAllCompleted = totalSteps > 0 && completedSteps == totalSteps
    val isInProgress = completedSteps > 0 && !isAllCompleted

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Top Row: Title + Clinical Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp)
                ) {
                    Text(
                        text = if (plan.title.isNotBlank()) plan.title else "Treatment Plan #${plan.id}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Plan #${plan.id} • ${plan.clinicianName} • ${plan.dateCreated}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ThornburyMuted
                    )
                }

                // Modern Clinical Status Chip
                val badgeColor = when {
                    isAllCompleted -> ThornburyAccentTeal
                    isInProgress -> ThornburyPrimary
                    else -> ThornburyMuted
                }
                val badgeBg = when {
                    isAllCompleted -> ThornburySuccessWash
                    isInProgress -> ThornburyPrimary.copy(alpha = 0.08f)
                    else -> ThornburySurfaceSoft
                }
                val badgeLabel = when {
                    isAllCompleted -> "COMPLETED"
                    isInProgress -> "IN PROGRESS"
                    else -> "PROPOSED"
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = badgeBg,
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                isAllCompleted -> Icons.Default.CheckCircle
                                isInProgress -> Icons.Default.PendingActions
                                else -> Icons.Default.Schedule
                            },
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = badgeColor
                        )
                    }
                }
            }

            // Optional Diagnosis indication within plan
            if (plan.diagnosis.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ThornburySurfaceSoft,
                    border = BorderStroke(1.dp, ThornburyHairlineSoft),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyMuted
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = plan.diagnosis,
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Plan Progress Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Procedures ($completedSteps of $totalSteps completed)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
                val percent = if (totalSteps > 0) (completedSteps * 100) / totalSteps else 0
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isAllCompleted) ThornburyAccentTeal else ThornburyPrimaryText
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            val planProgress = if (totalSteps > 0) completedSteps.toFloat() / totalSteps else 0f
            LinearProgressIndicator(
                progress = { planProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (isAllCompleted) ThornburyAccentTeal else ThornburyPrimary,
                trackColor = ThornburySurfaceSoft
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Procedure Steps List
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                plan.steps.forEach { step ->
                    ProcedureStepRow(
                        step = step,
                        onToggle = { onToggleStep(step.id) }
                    )
                }
            }

            // Clinical Addenda & Notes (if any)
            if (plan.addenda.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = ThornburyHairlineSoft)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Clinical Addenda & Amendments (${plan.addenda.size}):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                plan.addenda.forEach { addendum ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = ThornburySurfaceSoft,
                        border = BorderStroke(1.dp, ThornburyHairlineSoft)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "By ${addendum.author}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyPrimaryText
                                )
                                Text(
                                    text = addendum.date,
                                    style = ClinicalCodeStyle.copy(fontSize = 10.sp),
                                    color = ThornburyMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = addendum.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyInk
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProcedureStepRow(
    step: PlanStep,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(8.dp),
        color = if (step.completed) ThornburySuccessWash.copy(alpha = 0.5f) else ThornburySurfaceSoft,
        border = BorderStroke(
            1.dp,
            if (step.completed) ThornburySuccess.copy(alpha = 0.35f) else ThornburyHairlineSoft
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkmark Toggle Button
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (step.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (step.completed) "Mark incomplete" else "Mark complete",
                    tint = if (step.completed) ThornburySuccess else ThornburyMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Tooth badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (step.toothNumber != null) ThornburyPrimary.copy(alpha = 0.10f) else ThornburySurfaceSoft,
                        border = BorderStroke(0.5.dp, if (step.toothNumber != null) ThornburyPrimary.copy(alpha = 0.35f) else ThornburyHairline)
                    ) {
                        Text(
                            text = if (step.toothNumber != null) "Tooth #${step.toothNumber}" else "General",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = if (step.toothNumber != null) ThornburyPrimaryText else ThornburyMuted,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Procedure Code chip
                    if (step.code.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ThornburyCanvas,
                            border = BorderStroke(0.5.dp, ThornburyHairline)
                        ) {
                            Text(
                                text = step.code,
                                style = ClinicalCodeStyle.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = ThornburyMuted,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = step.procedure,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (step.completed) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    color = if (step.completed) ThornburySuccess else ThornburyInk
                )

                if (step.completed) {
                    Text(
                        text = "Completed in surgery",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = ThornburySuccess
                    )
                }
            }
        }
    }
}
