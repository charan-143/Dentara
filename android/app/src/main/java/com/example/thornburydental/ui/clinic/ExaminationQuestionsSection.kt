package com.example.thornburydental.ui.clinic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thornburydental.data.ExaminationAnswers
import com.example.thornburydental.theme.*

// =============================================================================
// Thornbury Dental - Examination Questions & Clinical Questionnaire Section
// =============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExaminationQuestionsSection(
    patientId: String,
    initialAnswers: ExaminationAnswers?,
    onSaveAnswers: (ExaminationAnswers) -> Unit,
    modifier: Modifier = Modifier
) {
    // Current questionnaire state initialized from passed answers or empty defaults
    var chiefComplaints by remember(patientId, initialAnswers) {
        mutableStateOf(initialAnswers?.chiefComplaints ?: emptyList())
    }
    var chiefComplaintOther by remember(patientId, initialAnswers) {
        mutableStateOf(initialAnswers?.chiefComplaintOther ?: "")
    }
    var painSeverity by remember(patientId, initialAnswers) {
        mutableStateOf(initialAnswers?.painSeverity ?: "")
    }
    var sensitivityTriggers by remember(patientId, initialAnswers) {
        mutableStateOf(initialAnswers?.sensitivityTriggers ?: emptyList())
    }
    var periodontalBleeding by remember(patientId, initialAnswers) {
        mutableStateOf(initialAnswers?.periodontalBleeding ?: emptyList())
    }
    var softTissue by remember(patientId, initialAnswers) {
        mutableStateOf(initialAnswers?.softTissue ?: emptyList())
    }
    var functionalHabits by remember(patientId, initialAnswers) {
        mutableStateOf(initialAnswers?.functionalHabits ?: emptyList())
    }
    var brushingFrequency by remember(patientId, initialAnswers) {
        mutableStateOf(initialAnswers?.brushingFrequency ?: "")
    }
    var flossingFrequency by remember(patientId, initialAnswers) {
        mutableStateOf(initialAnswers?.flossingFrequency ?: "")
    }
    var cariesRisk by remember(patientId, initialAnswers) {
        mutableStateOf(initialAnswers?.cariesRisk ?: "")
    }
    var clinicianNotes by remember(patientId, initialAnswers) {
        mutableStateOf(initialAnswers?.clinicianNotes ?: "")
    }

    var isExpanded by remember { mutableStateOf(true) }

    // Helper to notify caller of any answer changes
    fun emitChanges(
        newChiefComplaints: List<String> = chiefComplaints,
        newChiefComplaintOther: String = chiefComplaintOther,
        newPainSeverity: String = painSeverity,
        newSensitivityTriggers: List<String> = sensitivityTriggers,
        newPeriodontalBleeding: List<String> = periodontalBleeding,
        newSoftTissue: List<String> = softTissue,
        newFunctionalHabits: List<String> = functionalHabits,
        newBrushingFrequency: String = brushingFrequency,
        newFlossingFrequency: String = flossingFrequency,
        newCariesRisk: String = cariesRisk,
        newClinicianNotes: String = clinicianNotes
    ) {
        onSaveAnswers(
            ExaminationAnswers(
                chiefComplaints = newChiefComplaints,
                chiefComplaintOther = newChiefComplaintOther,
                painSeverity = newPainSeverity,
                sensitivityTriggers = newSensitivityTriggers,
                periodontalBleeding = newPeriodontalBleeding,
                softTissue = newSoftTissue,
                functionalHabits = newFunctionalHabits,
                brushingFrequency = newBrushingFrequency,
                flossingFrequency = newFlossingFrequency,
                cariesRisk = newCariesRisk,
                clinicianNotes = newClinicianNotes
            )
        )
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 64.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
            // Header bar with expand / collapse toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(bottom = if (isExpanded) 14.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ThornburyPrimaryWash),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            tint = ThornburyPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Chairside Clinical Questionnaire",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ThornburyInk
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = ThornburySuccessWash,
                                border = BorderStroke(1.dp, ThornburySuccess.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "LIVE SYNC",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.6.sp
                                    ),
                                    color = ThornburySuccess,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Comprehensive diagnostic history & risk assessment",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyMuted
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = ThornburyMuted
                    )
                }
            }

            if (isExpanded) {
                HorizontalDivider(color = ThornburyHairline, modifier = Modifier.padding(bottom = 16.dp))

                // Section 1: Chief Complaints
                ChiefComplaintsSubSection(
                    selectedComplaints = chiefComplaints,
                    otherText = chiefComplaintOther,
                    onToggleComplaint = { complaint ->
                        val updated = if (chiefComplaints.contains(complaint)) {
                            chiefComplaints - complaint
                        } else {
                            chiefComplaints + complaint
                        }
                        chiefComplaints = updated
                        emitChanges(newChiefComplaints = updated)
                    },
                    onOtherTextChanged = { newOther ->
                        chiefComplaintOther = newOther
                        emitChanges(newChiefComplaintOther = newOther)
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Section 2: Pain Severity & Sensitivity Triggers
                PainAndSensitivitySubSection(
                    selectedSeverity = painSeverity,
                    selectedTriggers = sensitivityTriggers,
                    onSelectSeverity = { severity ->
                        painSeverity = severity
                        emitChanges(newPainSeverity = severity)
                    },
                    onToggleTrigger = { trigger ->
                        val updated = if (sensitivityTriggers.contains(trigger)) {
                            sensitivityTriggers - trigger
                        } else {
                            sensitivityTriggers + trigger
                        }
                        sensitivityTriggers = updated
                        emitChanges(newSensitivityTriggers = updated)
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Section 3: Periodontal & Soft Tissue Findings
                PeriodontalAndSoftTissueSubSection(
                    periodontal = periodontalBleeding,
                    softTissue = softTissue,
                    onTogglePeriodontal = { item ->
                        val updated = if (periodontalBleeding.contains(item)) {
                            periodontalBleeding - item
                        } else {
                            periodontalBleeding + item
                        }
                        periodontalBleeding = updated
                        emitChanges(newPeriodontalBleeding = updated)
                    },
                    onToggleSoftTissue = { item ->
                        val updated = if (softTissue.contains(item)) {
                            softTissue - item
                        } else {
                            softTissue + item
                        }
                        softTissue = updated
                        emitChanges(newSoftTissue = updated)
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Section 4: Oral Hygiene & Functional Habits
                OralHygieneAndHabitsSubSection(
                    brushing = brushingFrequency,
                    flossing = flossingFrequency,
                    habits = functionalHabits,
                    onSelectBrushing = { freq ->
                        brushingFrequency = freq
                        emitChanges(newBrushingFrequency = freq)
                    },
                    onSelectFlossing = { freq ->
                        flossingFrequency = freq
                        emitChanges(newFlossingFrequency = freq)
                    },
                    onToggleHabit = { habit ->
                        val updated = if (functionalHabits.contains(habit)) {
                            functionalHabits - habit
                        } else {
                            functionalHabits + habit
                        }
                        functionalHabits = updated
                        emitChanges(newFunctionalHabits = updated)
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Section 5: Caries Risk Level
                CariesRiskSubSection(
                    selectedRisk = cariesRisk,
                    onSelectRisk = { risk ->
                        cariesRisk = risk
                        emitChanges(newCariesRisk = risk)
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Section 6: Clinician Examination Notes
                ClinicianNotesSubSection(
                    notes = clinicianNotes,
                    onNotesChanged = { newNotes ->
                        clinicianNotes = newNotes
                        emitChanges(newClinicianNotes = newNotes)
                    }
                )
            }
        }
    }
}
}

// =============================================================================
// 1. Chief Complaints SubSection
// =============================================================================

private val CHIEF_COMPLAINT_ITEMS = listOf(
    "Routine Checkup" to Icons.Outlined.CalendarMonth,
    "Toothache" to Icons.Outlined.Warning,
    "Broken / Chipped Tooth" to Icons.Outlined.Healing,
    "Sensitivity" to Icons.Outlined.Thermostat,
    "Bleeding Gums" to Icons.Outlined.WaterDrop,
    "Aesthetic Concern" to Icons.Outlined.AutoAwesome,
    "Follow-up" to Icons.Outlined.Refresh
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChiefComplaintsSubSection(
    selectedComplaints: List<String>,
    otherText: String,
    onToggleComplaint: (String) -> Unit,
    onOtherTextChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.QuestionAnswer,
                    contentDescription = null,
                    tint = ThornburyPrimaryText,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "1. Chief Complaints (Patient's Words)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CHIEF_COMPLAINT_ITEMS.forEach { (label, icon) ->
                    val isSelected = selectedComplaints.contains(label)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggleComplaint(label) },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (isSelected) ThornburyPrimary else ThornburyMuted
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = ThornburyCanvas,
                            labelColor = ThornburyBodyStrong,
                            iconColor = ThornburyMuted,
                            selectedContainerColor = ThornburyPrimaryWash,
                            selectedLabelColor = ThornburyPrimaryText,
                            selectedLeadingIconColor = ThornburyPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = ThornburyHairline,
                            selectedBorderColor = ThornburyPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = otherText,
                onValueChange = onOtherTextChanged,
                label = { Text("Other specific complaint / patient description", style = MaterialTheme.typography.bodySmall) },
                placeholder = { Text("E.g. Discomfort on lower left quadrant when drinking chilled liquids", style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall.copy(color = ThornburyInk),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThornburyPrimary,
                    unfocusedBorderColor = ThornburyHairline,
                    focusedContainerColor = ThornburyCanvas,
                    unfocusedContainerColor = ThornburyCanvas
                ),
                singleLine = false,
                maxLines = 3
            )
        }
    }
}

// =============================================================================
// 2. Pain Severity & Sensitivity Triggers
// =============================================================================

data class PainOption(
    val label: String,
    val description: String,
    val color: Color,
    val washColor: Color,
    val icon: ImageVector
)

private val PAIN_SEVERITY_OPTIONS = listOf(
    PainOption("None / Asymptomatic", "No discomfort reported", ThornburySuccess, ThornburySuccessWash, Icons.Outlined.SentimentSatisfied),
    PainOption("Mild", "Occasional dull twinges", ThornburyWarning, ThornburyWarningWash, Icons.Outlined.SentimentNeutral),
    PainOption("Moderate", "Noticeable, eating affected", ThornburyAccentAmber, ThornburyWarningWash, Icons.Outlined.WarningAmber),
    PainOption("Severe / Throbbing", "Acute constant pain", ThornburyError, ThornburyErrorWash, Icons.Outlined.LocalFireDepartment)
)

private val SENSITIVITY_TRIGGER_ITEMS = listOf(
    "Cold" to Icons.Outlined.AcUnit,
    "Hot" to Icons.Outlined.Whatshot,
    "Sweet / Acidic" to Icons.Outlined.Restaurant,
    "Biting / Pressure" to Icons.Outlined.FitnessCenter
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PainAndSensitivitySubSection(
    selectedSeverity: String,
    selectedTriggers: List<String>,
    onSelectSeverity: (String) -> Unit,
    onToggleTrigger: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = ThornburyPrimaryText,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "2. Pain Severity & Sensitivity Triggers",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Pain Severity Score",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = ThornburyBodyStrong
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PAIN_SEVERITY_OPTIONS.forEach { opt ->
                    val isSelected = selectedSeverity == opt.label
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSeverity(opt.label) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) opt.washColor else ThornburyCanvas,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) opt.color else ThornburyHairline
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = opt.icon,
                                    contentDescription = null,
                                    tint = opt.color,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = opt.label,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) ThornburyInk else ThornburyBodyStrong
                                    )
                                    Text(
                                        text = opt.description,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = ThornburyMuted
                                    )
                                }
                            }

                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectSeverity(opt.label) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = opt.color,
                                    unselectedColor = ThornburyHairline
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Sensitivity Triggers (Provoked Symptoms)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = ThornburyBodyStrong
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SENSITIVITY_TRIGGER_ITEMS.forEach { (label, icon) ->
                    val isSelected = selectedTriggers.contains(label)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggleTrigger(label) },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isSelected) ThornburyPrimary else ThornburyMuted
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = ThornburyCanvas,
                            selectedContainerColor = ThornburyPrimaryWash,
                            selectedLabelColor = ThornburyPrimaryText
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = ThornburyHairline,
                            selectedBorderColor = ThornburyPrimary
                        )
                    )
                }
            }
        }
    }
}

// =============================================================================
// 3. Periodontal & Soft Tissue Findings
// =============================================================================

private val PERIODONTAL_OPTIONS = listOf(
    "No bleeding",
    "Bleeding on brushing",
    "Spontaneous bleeding",
    "Swollen / tender gums"
)

private val SOFT_TISSUE_OPTIONS = listOf(
    "Healthy & intact",
    "Aphthous ulcer",
    "Leukoplakia",
    "Swelling / Abscess"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeriodontalAndSoftTissueSubSection(
    periodontal: List<String>,
    softTissue: List<String>,
    onTogglePeriodontal: (String) -> Unit,
    onToggleSoftTissue: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = ThornburyPrimaryText,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "3. Periodontal & Soft Tissue Screening",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Gingival & Periodontal Bleeding",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = ThornburyBodyStrong
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PERIODONTAL_OPTIONS.forEach { item ->
                    val isSelected = periodontal.contains(item)
                    val isWarning = item != "No bleeding" && isSelected
                    FilterChip(
                        selected = isSelected,
                        onClick = { onTogglePeriodontal(item) },
                        label = {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = ThornburyCanvas,
                            selectedContainerColor = if (isWarning) ThornburyWarningWash else ThornburyPrimaryWash,
                            selectedLabelColor = if (isWarning) ThornburyWarning else ThornburyPrimaryText
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = ThornburyHairline,
                            selectedBorderColor = if (isWarning) ThornburyWarning else ThornburyPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Oral Mucosa & Soft Tissue",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = ThornburyBodyStrong
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SOFT_TISSUE_OPTIONS.forEach { item ->
                    val isSelected = softTissue.contains(item)
                    val isAbnormal = item != "Healthy & intact" && isSelected
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggleSoftTissue(item) },
                        label = {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = ThornburyCanvas,
                            selectedContainerColor = if (isAbnormal) ThornburyErrorWash else ThornburyPrimaryWash,
                            selectedLabelColor = if (isAbnormal) ThornburyError else ThornburyPrimaryText
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = ThornburyHairline,
                            selectedBorderColor = if (isAbnormal) ThornburyError else ThornburyPrimary
                        )
                    )
                }
            }
        }
    }
}

// =============================================================================
// 4. Oral Hygiene & Functional Habits
// =============================================================================

private val BRUSHING_OPTIONS = listOf("2x/day", "1x/day", "Irregular")
private val FLOSSING_OPTIONS = listOf("Daily", "Occasional", "Rarely")
private val HABIT_OPTIONS = listOf(
    "No clenching/grinding",
    "Nocturnal bruxism",
    "Daytime clenching",
    "TMJ tightness"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OralHygieneAndHabitsSubSection(
    brushing: String,
    flossing: String,
    habits: List<String>,
    onSelectBrushing: (String) -> Unit,
    onSelectFlossing: (String) -> Unit,
    onToggleHabit: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CleanHands,
                    contentDescription = null,
                    tint = ThornburyPrimaryText,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "4. Oral Hygiene & Functional Habits",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Brushing Frequency
            Text(
                text = "Toothbrushing Frequency",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = ThornburyBodyStrong
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BRUSHING_OPTIONS.forEach { opt ->
                    val isSelected = brushing == opt
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectBrushing(opt) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) ThornburyPrimaryWash else ThornburyCanvas,
                        border = BorderStroke(1.dp, if (isSelected) ThornburyPrimary else ThornburyHairline)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = opt,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) ThornburyPrimaryText else ThornburyBodyStrong
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Flossing Frequency
            Text(
                text = "Interdental Cleaning / Flossing",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = ThornburyBodyStrong
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FLOSSING_OPTIONS.forEach { opt ->
                    val isSelected = flossing == opt
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectFlossing(opt) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) ThornburyPrimaryWash else ThornburyCanvas,
                        border = BorderStroke(1.dp, if (isSelected) ThornburyPrimary else ThornburyHairline)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = opt,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) ThornburyPrimaryText else ThornburyBodyStrong
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Parafunctional Habits
            Text(
                text = "Parafunctional & Occlusal Habits",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = ThornburyBodyStrong
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HABIT_OPTIONS.forEach { habit ->
                    val isSelected = habits.contains(habit)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggleHabit(habit) },
                        label = {
                            Text(
                                text = habit,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = ThornburyCanvas,
                            selectedContainerColor = ThornburyPrimaryWash,
                            selectedLabelColor = ThornburyPrimaryText
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = ThornburyHairline,
                            selectedBorderColor = ThornburyPrimary
                        )
                    )
                }
            }
        }
    }
}

// =============================================================================
// 5. Caries Risk Level
// =============================================================================

data class CariesRiskOption(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val washColor: Color
)

private val CARIES_RISK_OPTIONS = listOf(
    CariesRiskOption(
        label = "Low Risk",
        description = "No active lesions, good hygiene, regular recall",
        icon = Icons.Outlined.Shield,
        color = ThornburySuccess,
        washColor = ThornburySuccessWash
    ),
    CariesRiskOption(
        label = "Moderate Risk",
        description = "1-2 lesions in past 3 yrs, irregular flossing",
        icon = Icons.Outlined.Warning,
        color = ThornburyWarning,
        washColor = ThornburyWarningWash
    ),
    CariesRiskOption(
        label = "High Risk",
        description = "Active lesions, frequent sugars, high salivary count",
        icon = Icons.Outlined.Dangerous,
        color = ThornburyError,
        washColor = ThornburyErrorWash
    )
)

@Composable
private fun CariesRiskSubSection(
    selectedRisk: String,
    onSelectRisk: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = ThornburyPrimaryText,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "5. Caries Risk Assessment (CRA)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CARIES_RISK_OPTIONS.forEach { opt ->
                    val isSelected = selectedRisk == opt.label
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectRisk(opt.label) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) opt.washColor else ThornburyCanvas,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) opt.color else ThornburyHairline
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) opt.color.copy(alpha = 0.2f) else ThornburySurfaceSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = opt.icon,
                                    contentDescription = null,
                                    tint = opt.color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = opt.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                    ),
                                    color = if (isSelected) ThornburyInk else ThornburyBodyStrong
                                )
                                Text(
                                    text = opt.description,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = ThornburyMuted
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = opt.color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// 6. Clinician Examination Notes
// =============================================================================

private val PRESET_SUGGESTIONS = listOf(
    "Good plaque control, no active caries",
    "Generalized marginal gingivitis",
    "Active carious lesion, prompt restoration advised",
    "Localized cervical dentin hypersensitivity",
    "Mild nocturnal bruxism; occlusal splint discussed",
    "Routine prophylaxis and 6-month recall recommended"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClinicianNotesSubSection(
    notes: String,
    onNotesChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = null,
                    tint = ThornburyPrimaryText,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "6. Clinician Examination Notes & Plan Directives",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Quick Clinical Suggestions (Tap to append):",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = ThornburyMuted
            )

            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PRESET_SUGGESTIONS.forEach { suggestion ->
                    Surface(
                        modifier = Modifier.clickable {
                            val newText = if (notes.isBlank()) {
                                suggestion
                            } else {
                                "$notes. $suggestion"
                            }
                            onNotesChanged(newText)
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = ThornburySurfaceSoft,
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = ThornburyPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = ThornburyBodyStrong
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChanged,
                label = { Text("Clinician Notes & Findings") },
                placeholder = { Text("Record intraoral observations, soft tissue findings, and proposed treatment pathways...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(color = ThornburyInk),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThornburyPrimary,
                    unfocusedBorderColor = ThornburyHairline,
                    focusedContainerColor = ThornburyCanvas,
                    unfocusedContainerColor = ThornburyCanvas
                ),
                maxLines = 6
            )
        }
    }
}
