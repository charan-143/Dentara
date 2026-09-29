package com.example.thornburydental.ui.components.speech

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thornburydental.speech.ClinicalFindings
import com.example.thornburydental.speech.ClinicalNoteFinding
import com.example.thornburydental.speech.ClinicalNoteResult
import com.example.thornburydental.speech.ParsedVoiceCommand
import com.example.thornburydental.speech.PerioMeasurementFinding
import com.example.thornburydental.speech.PocketDepthResult
import com.example.thornburydental.speech.ToothConditionFinding
import com.example.thornburydental.speech.TreatmentPlanFinding
import com.example.thornburydental.speech.VoiceCommandParser
import com.example.thornburydental.theme.*

/**
 * Interactive Structured Clinical Findings Preview Sheet.
 * Displays AI-extracted dental entities (Odontogram, Perio, Notes, Treatment Plan)
 * for clinician review and single-tap logging to the patient chart.
 */
@Composable
fun ClinicalFindingsPreviewSheet(
    findings: ClinicalFindings,
    onConfirmApply: (ClinicalFindings) -> Unit,
    onDismiss: () -> Unit,
    autoApplyEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val needsReview = findings.requiresReview

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        color = ThornburySurfaceCard,
        shadowElevation = 12.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (needsReview) Icons.Default.Warning else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (needsReview) ThornburyWarning else ThornburyPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (needsReview) "Clinical Review Required" else "AI Clinical Findings Extracted",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (needsReview) ThornburyWarning else ThornburyInk
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = if (findings.isAiExtracted) ThornburyPrimaryWash else ThornburySurfaceSoft,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (findings.isAiExtracted) Icons.Default.SmartToy else Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (findings.isAiExtracted) ThornburyPrimary else ThornburyMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (findings.isAiExtracted) "Gemini AI Scribe" else "On-Device Scribe",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (findings.isAiExtracted) ThornburyPrimary else ThornburyMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Raw audio transcript box
            Text(
                text = "Spoken Dictation Transcript:",
                style = MaterialTheme.typography.labelMedium,
                color = ThornburyMuted
            )
            Text(
                text = "\"${findings.rawTranscript}\"",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = ThornburyInk,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(
                        ThornburyCanvas,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Categorized Items
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. Odontogram Tooth Conditions
                if (findings.toothConditions.isNotEmpty()) {
                    Text(
                        text = "🦷 Tooth Charting (${findings.toothConditions.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = ThornburyPrimary
                    )
                    findings.toothConditions.forEach { item ->
                        ToothConditionItemCard(item)
                    }
                }

                // 2. Periodontal Probing Depths
                if (findings.perioMeasurements.isNotEmpty()) {
                    Text(
                        text = "📏 Periodontal Pocket Depths (${findings.perioMeasurements.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = ThornburyPrimary
                    )
                    findings.perioMeasurements.forEach { item ->
                        PerioMeasurementItemCard(item)
                    }
                }

                // 3. Treatment Plans
                if (findings.treatmentPlanItems.isNotEmpty()) {
                    Text(
                        text = "📋 Treatment Plan Recommendations (${findings.treatmentPlanItems.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = ThornburyPrimary
                    )
                    findings.treatmentPlanItems.forEach { item ->
                        TreatmentPlanFindingCard(item)
                    }
                }

                // 4. Dedicated Examination Fields
                val exam = findings.examFindings
                val hasExamFields = exam.chiefComplaints.isNotEmpty() ||
                        exam.painSeverity.isNotBlank() ||
                        exam.sensitivityTriggers.isNotEmpty() ||
                        exam.softTissue.isNotEmpty() ||
                        exam.calculus.isNotEmpty() ||
                        exam.cariesRisk.isNotBlank() ||
                        exam.diagnosis.isNotBlank()

                if (hasExamFields) {
                    Text(
                        text = "🩺 Clinical Examination Fields",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = ThornburyPrimary
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (exam.chiefComplaints.isNotEmpty()) {
                                Text("Chief Complaint: ${exam.chiefComplaints.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = ThornburyInk)
                            }
                            if (exam.painSeverity.isNotBlank()) {
                                Text("Pain Severity: ${exam.painSeverity}", style = MaterialTheme.typography.bodySmall, color = ThornburyInk)
                            }
                            if (exam.sensitivityTriggers.isNotEmpty()) {
                                Text("Sensitivity: ${exam.sensitivityTriggers.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = ThornburyInk)
                            }
                            if (exam.softTissue.isNotEmpty()) {
                                Text("Soft Tissue: ${exam.softTissue.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = ThornburyInk)
                            }
                            if (exam.calculus.isNotEmpty()) {
                                Text("Calculus / Plaque: ${exam.calculus.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = ThornburyInk)
                            }
                            if (exam.cariesRisk.isNotBlank()) {
                                Text("Caries Risk: ${exam.cariesRisk}", style = MaterialTheme.typography.bodySmall, color = ThornburyInk)
                            }
                            if (exam.diagnosis.isNotBlank()) {
                                Text("Primary Diagnosis: ${exam.diagnosis}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ThornburyPrimary)
                            }
                        }
                    }
                }

                // 5. Extra Clinical Notes (Only true extra notes!)
                if (findings.examFindings.extraNotes.isNotEmpty() || findings.clinicalNotes.isNotEmpty()) {
                    val allExtra = (findings.examFindings.extraNotes + findings.clinicalNotes.map { it.text }).distinct()
                    Text(
                        text = "📝 Extra Clinical Notes (${allExtra.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = ThornburyPrimary
                    )
                    allExtra.forEach { note ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ThornburyHairline)
                        ) {
                            Text(
                                text = "• $note",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ThornburyInk,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // Warnings Card
            if (findings.warnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ThornburyWarningWash),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ThornburyWarning.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Safety Warnings",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ThornburyWarning
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        findings.warnings.forEach { warning ->
                            Text(
                                text = "• $warning",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyInk
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ThornburyHairline)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = ThornburyMuted)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Discard", color = ThornburyMuted)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = { onConfirmApply(findings) },
                    enabled = !findings.isEmpty,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary, contentColor = Color.White)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log to Patient Chart (${findings.totalCount})")
                }
            }
        }
    }
}

@Composable
private fun ToothConditionItemCard(item: ToothConditionFinding) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, ThornburyHairline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Tooth #${item.toothNumber}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ThornburyInk
                )
                Text(
                    text = "Condition: ${item.condition.label}" + if (item.surface.isNotBlank()) " (${item.surface})" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = ThornburyMuted
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ThornburyPrimaryWash
            ) {
                Text(
                    text = item.condition.code,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThornburyPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PerioMeasurementItemCard(item: PerioMeasurementFinding) {
    val isCriticalDepth = item.depthMm >= 6
    val isWarningDepth = item.depthMm >= 4

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCriticalDepth -> ThornburyErrorWash
                isWarningDepth -> ThornburyWarningWash
                else -> ThornburySuccessWash
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when {
                isCriticalDepth -> ThornburyError.copy(alpha = 0.3f)
                isWarningDepth -> ThornburyWarning.copy(alpha = 0.3f)
                else -> ThornburySuccess.copy(alpha = 0.3f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Tooth #${item.toothNumber} (${item.site})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ThornburyInk
                )
                Text(
                    text = "Probing Depth: ${item.depthMm} mm",
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        isCriticalDepth -> ThornburyError
                        isWarningDepth -> ThornburyWarning
                        else -> ThornburySuccess
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (item.isBleeding) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(ThornburyError, shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Healing,
                        contentDescription = "BOP",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "BOP Bleeding",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ClinicalNoteFindingCard(item: ClinicalNoteFinding) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = item.category,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = ThornburyPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyMedium,
                color = ThornburyInk
            )
        }
    }
}

@Composable
private fun TreatmentPlanFindingCard(item: TreatmentPlanFinding) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, ThornburyHairline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = item.procedure,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ThornburyInk
                )
                if (item.toothNumber != null) {
                    Text(
                        text = "Target Tooth: #${item.toothNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThornburyMuted
                    )
                }
            }

            if (item.estimatedCost > 0) {
                Text(
                    text = "$${item.estimatedCost.toInt()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ThornburyPrimary
                )
            }
        }
    }
}

/**
 * Backwards compatible ParsedCommandPreviewSheet.
 */
@Composable
fun ParsedCommandPreviewSheet(
    rawTranscript: String,
    parsedCommand: ParsedVoiceCommand,
    onConfirmApply: (ParsedVoiceCommand) -> Unit,
    onDismiss: () -> Unit,
    autoApplyEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val hasImplausibleDepth = when (parsedCommand) {
        is ParsedVoiceCommand.SinglePocketDepth -> parsedCommand.entry.depthMm > VoiceCommandParser.MAX_ORDINARY_DEPTH_MM
        is ParsedVoiceCommand.MultiplePocketDepths -> parsedCommand.entries.any { it.depthMm > VoiceCommandParser.MAX_ORDINARY_DEPTH_MM }
        else -> false
    }

    val needsReview = hasImplausibleDepth || parsedCommand.confidence < VoiceCommandParser.AUTO_APPLY_CONFIDENCE

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        color = ThornburySurfaceCard,
        shadowElevation = 12.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasImplausibleDepth) Icons.Default.Warning else Icons.Default.Verified,
                        contentDescription = null,
                        tint = if (hasImplausibleDepth) ThornburyWarning else ThornburyPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasImplausibleDepth) "Review Required (>12mm)" else "Voice Dictation Recognized",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hasImplausibleDepth) ThornburyWarning else ThornburyInk
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = ThornburyPrimaryWash,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = ThornburyPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Say 'Confirm' or tap Apply",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ThornburyPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Raw audio transcript box
            Text(
                text = "Spoken Audio Transcript:",
                style = MaterialTheme.typography.labelMedium,
                color = ThornburyMuted
            )
            Text(
                text = "\"$rawTranscript\"",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = ThornburyInk,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(
                        ThornburyCanvas,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Parsed Command Content
            when (parsedCommand) {
                is ParsedVoiceCommand.SinglePocketDepth -> {
                    PocketDepthItemCard(parsedCommand.entry)
                }
                is ParsedVoiceCommand.MultiplePocketDepths -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        parsedCommand.entries.forEach { entry ->
                            PocketDepthItemCard(entry)
                        }
                    }
                }
                is ParsedVoiceCommand.ClinicalNote -> {
                    ClinicalNoteItemCard(parsedCommand.entry)
                }
                is ParsedVoiceCommand.SpokenConfirmation -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (parsedCommand.confirmed) "Spoken confirmation: \"${parsedCommand.rawTranscript}\"" else "Spoken cancellation: \"${parsedCommand.rawTranscript}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                is ParsedVoiceCommand.SpokenUndo -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Spoken undo requested",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                is ParsedVoiceCommand.Unrecognized -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = parsedCommand.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            if (needsReview && parsedCommand !is ParsedVoiceCommand.Unrecognized) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ThornburyWarningWash),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ThornburyWarning.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Check this before accepting",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ThornburyWarning
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        parsedCommand.warnings.forEach { warning ->
                            Text(
                                text = warning,
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyInk
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ThornburyHairline)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = ThornburyMuted)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Discard", color = ThornburyMuted)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = { onConfirmApply(parsedCommand) },
                    enabled = parsedCommand !is ParsedVoiceCommand.Unrecognized,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary, contentColor = Color.White)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Apply to Chart")
                }
            }
        }
    }
}

@Composable
private fun PocketDepthItemCard(entry: PocketDepthResult) {
    val isImplausibleDepth = entry.depthMm > VoiceCommandParser.MAX_ORDINARY_DEPTH_MM
    val isCriticalDepth = entry.depthMm >= 6
    val isWarningDepth = entry.depthMm >= 4

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isImplausibleDepth || isCriticalDepth -> ThornburyErrorWash
                isWarningDepth -> ThornburyWarningWash
                else -> ThornburySuccessWash
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when {
                isImplausibleDepth || isCriticalDepth -> ThornburyError.copy(alpha = 0.3f)
                isWarningDepth -> ThornburyWarning.copy(alpha = 0.3f)
                else -> ThornburySuccess.copy(alpha = 0.3f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Tooth #${entry.toothNumber} (${entry.site.label})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ThornburyInk
                )
                Text(
                    text = if (isImplausibleDepth) {
                        "Probing Pocket Depth: ${entry.depthMm} mm (Implausible >12mm)"
                    } else {
                        "Probing Pocket Depth: ${entry.depthMm} mm"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        isImplausibleDepth || isCriticalDepth -> ThornburyError
                        isWarningDepth -> ThornburyWarning
                        else -> ThornburySuccess
                    },
                    fontWeight = if (isImplausibleDepth) FontWeight.Bold else FontWeight.Normal
                )
            }

            if (entry.isBleeding) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(ThornburyError, shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Healing,
                        contentDescription = "Bleeding on Probing",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "BOP Bleeding",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ClinicalNoteItemCard(entry: ClinicalNoteResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Target: ${entry.targetSection}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.noteText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
