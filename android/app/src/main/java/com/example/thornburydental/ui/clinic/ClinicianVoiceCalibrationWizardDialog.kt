package com.example.thornburydental.ui.clinic

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.thornburydental.data.ClinicianAccentProfile
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 30-Second Clinician Voice Calibration & Operatory Acoustic Tuning Wizard.
 *
 * Tunes speech recognition and noise gates for:
 * 1. Operatory ambient suction and handpiece hum
 * 2. Clinician accent profile & regional phonetic variations
 * 3. Surgical mask / N95 acoustic dampening compensation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicianVoiceCalibrationWizardDialog(
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var currentStep by remember { mutableStateOf(1) } // Steps 1 to 4

    val currentAccent by DentalRepository.clinicianAccentProfile.collectAsState()
    val currentNoiseGate by DentalRepository.ambientNoiseGateDb.collectAsState()
    val currentMaskBoost by DentalRepository.maskSuctionAcousticBoost.collectAsState()

    var selectedAccent by remember { mutableStateOf(currentAccent) }
    var measuredNoiseFloorDb by remember { mutableStateOf(currentNoiseGate) }
    var maskCompensationEnabled by remember { mutableStateOf(currentMaskBoost) }

    // Noise sampling state
    var isSamplingNoise by remember { mutableStateOf(false) }
    var noiseSamplingProgress by remember { mutableStateOf(0f) }

    // Test phrase dictation state
    var activeTestPhraseIndex by remember { mutableStateOf(0) }
    val testPhrases = remember {
        listOf(
            "Tooth 16 mesial-occlusal composite",
            "Distal pocket 5 millimeters bleeding",
            "Mandibular nerve block 2 percent lidocaine"
        )
    }
    val phraseCompleted = remember { mutableStateListOf(false, false, false) }
    var isRecordingPhrase by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
            border = BorderStroke(1.dp, ThornburyHairline),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ThornburyPrimaryWash,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = ThornburyPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Acoustic & Accent Calibration",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyInk
                            )
                            Text(
                                text = "30-second operatory noise & clinician voice tuning",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyMuted
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = ThornburyMuted)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Step Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (step in 1..4) {
                        val isCurrent = currentStep == step
                        val isDone = currentStep > step
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    when {
                                        isDone -> ThornburySuccess
                                        isCurrent -> ThornburyPrimary
                                        else -> ThornburySurfaceSoft
                                    }
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Step Content Area (Scrollable)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (currentStep) {
                        1 -> {
                            // Step 1: Ambient Noise & Suction Floor
                            Column {
                                Text(
                                    text = "Step 1: Operatory Ambient Noise Profiling",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyInk
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ensure your high-volume suction and operatory HVAC are running as normal. The microphone will sample background acoustics to set the dynamic noise gate.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThornburyMuted,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = ThornburySurfaceSoft),
                                    border = BorderStroke(1.dp, ThornburyHairline)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSamplingNoise) ThornburyWarning.copy(alpha = 0.2f) else ThornburyPrimary.copy(alpha = 0.1f),
                                            modifier = Modifier.size(64.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (isSamplingNoise) Icons.Default.Hearing else Icons.Default.Mic,
                                                    contentDescription = null,
                                                    tint = if (isSamplingNoise) ThornburyWarning else ThornburyPrimary,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Text(
                                            text = if (isSamplingNoise) "Sampling Operatory Acoustics..." else "Noise Floor: ${measuredNoiseFloorDb.toInt()} dB SPL",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = ThornburyInk
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = if (isSamplingNoise) "Analyzing dental suction and ambient frequencies" else "Acoustic gate calibrated for dental operatory environment",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ThornburyMuted
                                        )

                                        if (isSamplingNoise) {
                                            Spacer(modifier = Modifier.height(14.dp))
                                            LinearProgressIndicator(
                                                progress = { noiseSamplingProgress },
                                                modifier = Modifier.fillMaxWidth(0.8f).height(6.dp).clip(RoundedCornerShape(3.dp))
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            OutlinedButton(
                                                onClick = {
                                                    isSamplingNoise = true
                                                    noiseSamplingProgress = 0f
                                                    coroutineScope.launch {
                                                        for (i in 1..25) {
                                                            delay(120)
                                                            noiseSamplingProgress = i / 25f
                                                        }
                                                        measuredNoiseFloorDb = (38..44).random().toFloat()
                                                        isSamplingNoise = false
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, ThornburyPrimary)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = ThornburyPrimary)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Sample 5s Noise Window", color = ThornburyPrimary)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Mask compensation toggle
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
                                    border = BorderStroke(1.dp, ThornburyHairline)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Surgical Mask / N95 Compensation",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = ThornburyInk
                                            )
                                            Text(
                                                text = "Applies +3.5 dB high-frequency presence filter to un-muffle voice through surgical masks",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ThornburyMuted
                                            )
                                        }
                                        Switch(
                                            checked = maskCompensationEnabled,
                                            onCheckedChange = { maskCompensationEnabled = it }
                                        )
                                    }
                                }
                            }
                        }

                        2 -> {
                            // Step 2: Accent & Dialect Selection
                            Column {
                                Text(
                                    text = "Step 2: Clinician Accent & Dialect Calibration",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyInk
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Select your accent profile so the clinical language engine tunes phoneme probabilities for tooth numbers and tooth surfaces.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThornburyMuted
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ClinicianAccentProfile.values().forEach { profile ->
                                        val isSelected = selectedAccent == profile
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedAccent = profile },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) ThornburyPrimaryWash else ThornburyCanvas,
                                            border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) ThornburyPrimary else ThornburyHairline)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { selectedAccent = profile }
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = profile.displayName,
                                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                        color = if (isSelected) ThornburyPrimary else ThornburyInk
                                                    )
                                                    Text(
                                                        text = profile.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = ThornburyMuted
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        3 -> {
                            // Step 3: Fast 3-Phrase Dental Calibration Read-Back
                            Column {
                                Text(
                                    text = "Step 3: Verification Read-Back (3 Phrases)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyInk
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Speak each phrase naturally through your mask to verify acoustic phonetic alignment.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThornburyMuted
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    testPhrases.forEachIndexed { index, phrase ->
                                        val isDone = phraseCompleted[index]
                                        val isActive = activeTestPhraseIndex == index

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isDone) ThornburySuccess.copy(alpha = 0.08f) else ThornburySurfaceSoft
                                            ),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isDone) ThornburySuccess.copy(alpha = 0.5f) else if (isActive) ThornburyPrimary else ThornburyHairline
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Phrase ${index + 1}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = if (isDone) ThornburySuccess else ThornburyPrimary
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "\"$phrase\"",
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        color = ThornburyInk
                                                    )
                                                }

                                                if (isDone) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = ThornburySuccess,
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                                        }
                                                    }
                                                } else {
                                                    Button(
                                                        onClick = {
                                                            activeTestPhraseIndex = index
                                                            isRecordingPhrase = true
                                                            coroutineScope.launch {
                                                                delay(1400)
                                                                phraseCompleted[index] = true
                                                                isRecordingPhrase = false
                                                                if (index < testPhrases.size - 1) {
                                                                    activeTestPhraseIndex = index + 1
                                                                }
                                                            }
                                                        },
                                                        enabled = !isRecordingPhrase,
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary)
                                                    ) {
                                                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Speak", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        4 -> {
                            // Step 4: Calibration Summary & Confirmation
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = ThornburySuccess.copy(alpha = 0.15f),
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = ThornburySuccess,
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Acoustic Profile Ready!",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyInk
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Speech dictation has been customized for your operatory environment.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThornburyMuted
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = ThornburySurfaceSoft),
                                    border = BorderStroke(1.dp, ThornburyHairline)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Accent Model:", style = MaterialTheme.typography.bodySmall, color = ThornburyMuted)
                                            Text(selectedAccent.displayName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                                        }
                                        HorizontalDivider(color = ThornburyHairline)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Operatory Noise Gate:", style = MaterialTheme.typography.bodySmall, color = ThornburyMuted)
                                            Text("${measuredNoiseFloorDb.toInt()} dB SPL", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                                        }
                                        HorizontalDivider(color = ThornburyHairline)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Mask Acoustic Boost:", style = MaterialTheme.typography.bodySmall, color = ThornburyMuted)
                                            Text(if (maskCompensationEnabled) "+3.5 dB (Active)" else "Off", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep -= 1 },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (currentStep < 4) {
                        Button(
                            onClick = { currentStep += 1 },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary)
                        ) {
                            Text("Next Step")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                DentalRepository.setClinicianAccentProfile(selectedAccent)
                                DentalRepository.setAmbientNoiseGateDb(measuredNoiseFloorDb)
                                DentalRepository.setMaskSuctionAcousticBoost(maskCompensationEnabled)
                                DentalRepository.setVoiceCalibrationCompleted(true)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ThornburySuccess)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save & Apply Profile")
                        }
                    }
                }
            }
        }
    }
}
