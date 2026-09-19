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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thornburydental.speech.ClinicalNoteResult
import com.example.thornburydental.speech.ParsedVoiceCommand
import com.example.thornburydental.speech.PocketDepthResult
import com.example.thornburydental.speech.VoiceCommandParser
import kotlinx.coroutines.delay

@Composable
fun ParsedCommandPreviewSheet(
    rawTranscript: String,
    parsedCommand: ParsedVoiceCommand,
    onConfirmApply: (ParsedVoiceCommand) -> Unit,
    onDismiss: () -> Unit,
    autoApplyEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    // A reading the parser is unsure about is never written to the chart on its own; the
    // clinician has to look at it and accept it.
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
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp
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
                        tint = if (hasImplausibleDepth) Color(0xFFE65100) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasImplausibleDepth) "Review Required (>12mm)" else "Voice Dictation Recognized",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hasImplausibleDepth) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Say 'Confirm' or tap Apply",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Raw audio transcript box
            Text(
                text = "Spoken Audio Transcript:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "\"$rawTranscript\"",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                            // The parser says why, so the clinician can rephrase rather than guess.
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
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Check this before accepting",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        parsedCommand.warnings.forEach { warning ->
                            Text(
                                text = warning,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6D4C41)
                            )
                        }
                        if (parsedCommand.warnings.isEmpty()) {
                            Text(
                                text = "Some of the dictation could not be matched to a tooth or a depth.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6D4C41)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Glove Sterility Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Discard")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = { onConfirmApply(parsedCommand) },
                    enabled = parsedCommand !is ParsedVoiceCommand.Unrecognized,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
                isImplausibleDepth -> Color(0xFFFFE0B2)
                isCriticalDepth -> Color(0xFFFFEBEE)
                isWarningDepth -> Color(0xFFFFF3E0)
                else -> Color(0xFFE8F5E9)
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
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isImplausibleDepth) {
                        "Probing Pocket Depth: ${entry.depthMm} mm (Implausible >12mm)"
                    } else {
                        "Probing Pocket Depth: ${entry.depthMm} mm"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        isImplausibleDepth -> Color(0xFFBF360C)
                        isCriticalDepth -> Color(0xFFC62828)
                        isWarningDepth -> Color(0xFFE65100)
                        else -> Color(0xFF2E7D32)
                    },
                    fontWeight = if (isImplausibleDepth) FontWeight.Bold else FontWeight.Normal
                )
            }

            if (entry.isBleeding) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFFD32F2F), shape = RoundedCornerShape(8.dp))
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
