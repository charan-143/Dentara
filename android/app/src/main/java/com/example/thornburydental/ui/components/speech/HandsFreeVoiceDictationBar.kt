package com.example.thornburydental.ui.components.speech

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thornburydental.speech.DictationTargetMode
import com.example.thornburydental.speech.VoiceDictationState
import com.example.thornburydental.speech.model.SpeechModelState
import com.example.thornburydental.theme.*

/**
 * Docked Floating Overlay for Hands-Free Voice Charting.
 *
 * Supports a compact floating FAB pill state (minimalist footer overlay)
 * and an expanded active voice control panel. Does not alter underlying screen layout margins.
 */
@Composable
fun HandsFreeVoiceDictationBar(
    state: VoiceDictationState,
    targetMode: DictationTargetMode,
    onStartListening: (DictationTargetMode) -> Unit,
    onStopListening: () -> Unit,
    onSelectTargetMode: (DictationTargetMode) -> Unit,
    modelState: SpeechModelState = SpeechModelState.Absent,
    onProvisionModel: () -> Unit = {},
    undoDescription: String? = null,
    onUndoLastEntry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isListening = state is VoiceDictationState.Listening
    val isTranscribing = state is VoiceDictationState.Transcribing
    val unavailableReason = (state as? VoiceDictationState.Unavailable)?.reason
    val isUnavailable = unavailableReason != null
    val canProvision = (state as? VoiceDictationState.Unavailable)?.canProvision == true
    val downloading = modelState as? SpeechModelState.Downloading
    val verifying = modelState is SpeechModelState.Verifying
    val currentAmplitude = if (state is VoiceDictationState.Listening) state.amplitude else 0f

    var isExpanded by remember { mutableStateOf(false) }

    // Auto-expand when active recording starts
    LaunchedEffect(isListening, isTranscribing) {
        if (isListening || isTranscribing) {
            isExpanded = true
        }
    }

    // Pulse animation for active recording ring
    val pulseScale = remember { Animatable(1f) }
    LaunchedEffect(isListening) {
        if (isListening) {
            pulseScale.animateTo(
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulseScale.snapTo(1f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (!isExpanded) {
            // Sleek Collapsed Floating FAB Pill
            Surface(
                onClick = { isExpanded = true },
                shape = RoundedCornerShape(28.dp),
                color = if (isListening) ThornburyErrorWash else ThornburyPrimaryWash,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, if (isListening) ThornburyError.copy(alpha = 0.5f) else ThornburyHairline)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isListening -> ThornburyError
                                    isUnavailable -> ThornburyMuted
                                    else -> ThornburySuccess
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Hands-Free Voice Dictation",
                        tint = if (isListening) ThornburyError else ThornburyPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isListening -> "Listening (${targetMode.title})..."
                            isUnavailable -> "Voice Charting Unavailable"
                            else -> "Hands-Free Voice Charting"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isListening) ThornburyError else ThornburyInk
                    )
                }
            }
        } else {
            // Expanded Floating Voice Control Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isListening) ThornburyPrimaryWash else ThornburySurfaceCard
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header Bar with status & Minimize action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isListening -> ThornburyError
                                            isTranscribing -> ThornburyWarning
                                            else -> ThornburySuccess
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Voice Dictation",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ThornburyInk
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { isExpanded = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExpandLess,
                                    contentDescription = "Minimize Floating Bar",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mode Selector Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DictationTargetMode.values().forEach { mode ->
                            val isSelected = targetMode == mode
                            AssistChip(
                                onClick = { onSelectTargetMode(mode) },
                                label = { Text(mode.title, fontSize = 12.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    // Undo the last dictated entry. Always reachable while there is history,
                    // because a wrong reading needs reverting more urgently than it needed
                    // entering, and hunting it down inside a notes string is not an option
                    // mid-examination.
                    if (undoDescription != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onUndoLastEntry,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Undo " + undoDescription)
                        }
                    }

                    // One-time model provisioning. The download is tens of megabytes,
                    // so it is never started implicitly - the clinician asks for it.
                    if (downloading != null || verifying) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (verifying) {
                                    "Checking the downloaded dictation model..."
                                } else {
                                    "Downloading offline dictation model... " +
                                        "${(downloading!!.fraction * 100).toInt()}%"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            if (verifying) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            } else {
                                LinearProgressIndicator(
                                    progress = { downloading!!.fraction },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else if (canProvision) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onProvisionModel,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download offline dictation (57 MB)")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Microphone Controls & Live Audio Meter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val partialText = (state as? VoiceDictationState.Listening)?.streamingPartialTranscript ?: ""
                            Text(
                                text = when {
                                    isListening && partialText.isNotBlank() -> "Heard: \"$partialText\""
                                    isListening -> "Listening... Dictate 6-site probing or tooth depths hands-free"
                                    isTranscribing -> "Transcribing dictation locally..."
                                    unavailableReason != null -> unavailableReason
                                    state is VoiceDictationState.Error -> state.message
                                    else -> "Hands-free dictation ready. VAD endpointing and 6-site sequences active."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    state is VoiceDictationState.Error -> ThornburyError
                                    isUnavailable -> ThornburyMuted
                                    else -> ThornburyBody
                                }
                            )

                            AnimatedVisibility(
                                visible = isListening,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = ThornburyPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .height(6.dp)
                                            .fillMaxWidth(currentAmplitude.coerceIn(0.1f, 1f))
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(ThornburyPrimary)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Large Glove-Friendly Microphone Action Button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.scale(if (isListening) pulseScale.value else 1f)
                        ) {
                            Surface(
                                onClick = {
                                    if (isListening) onStopListening() else onStartListening(targetMode)
                                },
                                enabled = !isUnavailable,
                                shape = CircleShape,
                                color = when {
                                    isUnavailable -> ThornburySurfaceSoft
                                    isListening -> ThornburyError
                                    else -> ThornburyPrimary
                                },
                                shadowElevation = if (isUnavailable) 0.dp else 8.dp,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = when {
                                            isUnavailable -> Icons.Default.MicOff
                                            isListening -> Icons.Default.Stop
                                            else -> Icons.Default.Mic
                                        },
                                        contentDescription = when {
                                            isUnavailable -> "Voice dictation unavailable"
                                            isListening -> "Stop Recording"
                                            else -> "Start Hands-Free Dictation"
                                        },
                                        tint = if (isUnavailable) {
                                            ThornburyMuted
                                        } else {
                                            Color.White
                                        },
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
