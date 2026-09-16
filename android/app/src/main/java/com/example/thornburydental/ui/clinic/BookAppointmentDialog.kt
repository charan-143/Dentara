package com.example.thornburydental.ui.clinic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import com.example.thornburydental.data.Clinician
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.Patient
import com.example.thornburydental.theme.*
import com.example.thornburydental.ui.components.ThornburyDatePickerField
import com.example.thornburydental.util.addDaysToIsoDate
import com.example.thornburydental.util.isoDateDisplayLabel
import com.example.thornburydental.util.todayIsoDate

/**
 * Redesigned Follow-up Visit & Chairside Booking Dialog.
 *
 * Features quick procedure, date, and time selection chips, duration presets,
 * dual-tier chairside reminder scheduling, and NO surgery room buttons.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookAppointmentDialog(
    patient: Patient,
    initialDate: String = todayIsoDate(),
    onDismiss: () -> Unit,
    onSave: (
        procedure: String,
        clinicianId: String,
        clinicianName: String,
        date: String,
        time: String,
        duration: Int,
        room: String,
        reminderEnabled: Boolean,
        reminderLeadTimeMin: Int
    ) -> Unit
) {
    val clinicians = DentalRepository.clinicians
    val clinicianDisplayName by DentalRepository.clinicianDisplayName.collectAsState()
    val defaultLeadMin by DentalRepository.chairsideReminderDefaultMin.collectAsState()

    val attendingClinician = remember(clinicians, clinicianDisplayName) {
        clinicians.firstOrNull() ?: Clinician(
            id = "c1",
            name = clinicianDisplayName.ifBlank { "Dr. Ingrid Halvorsen" },
            credentials = "BDS (Hons), MFDS RCSEd",
            specialty = "Dental Surgery",
            room = "Surgery 1",
            bio = ""
        )
    }

    var procedure by remember { mutableStateOf("Follow-up Review") }
    var date by remember { mutableStateOf(initialDate) }
    var time by remember { mutableStateOf("09:30 AM") }
    var durationMin by remember { mutableIntStateOf(30) }
    var reminderEnabled by remember { mutableStateOf(true) }
    var reminderLeadTimeMin by remember { mutableIntStateOf(defaultLeadMin) }

    val isDateValid = remember(date) {
        Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(date.trim())
    }

    val isTimeValid = remember(time) {
        Regex("^\\d{1,2}:\\d{2}\\s*(AM|PM)$", RegexOption.IGNORE_CASE).matches(time.trim())
    }

    val quickProcedures = listOf(
        "Follow-up Review",
        "Suture Removal",
        "Restorative Care",
        "Crown Seating",
        "Periodontal Scaling",
        "Post-Op Assessment"
    )

    val quickTimes = listOf(
        "09:00 AM",
        "10:30 AM",
        "11:45 AM",
        "02:00 PM",
        "03:30 PM",
        "04:45 PM"
    )

    val quickDates = listOf(
        "Today" to todayIsoDate(),
        "Tomorrow" to addDaysToIsoDate(todayIsoDate(), 1),
        "In 3 Days" to addDaysToIsoDate(todayIsoDate(), 3),
        "Next Week" to addDaysToIsoDate(todayIsoDate(), 7)
    )

    val durationOptions = listOf(15, 30, 45, 60)

    val initials = remember(patient.name) {
        val parts = patient.name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        when {
            parts.size >= 2 -> "${parts.first().first().uppercaseChar()}${parts.last().first().uppercaseChar()}"
            parts.isNotEmpty() -> parts.first().take(2).uppercase()
            else -> "PT"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Patient Monogram & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(ThornburyPrimaryWash),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyPrimaryText
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Book Follow-up Visit",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyInk
                            )
                            Text(
                                text = "${patient.name} • ${patient.opNo}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyMuted
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = ThornburyMuted)
                    }
                }

                HorizontalDivider(color = ThornburyHairlineSoft)

                // 1. Scheduled Procedure Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Procedure",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )

                    // Quick Procedure Chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickProcedures.forEach { qp ->
                            val isSelected = procedure.equals(qp, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { procedure = qp },
                                label = {
                                    Text(
                                        text = qp,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ThornburyPrimaryWash,
                                    selectedLabelColor = ThornburyPrimaryText
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) ThornburyPrimary else ThornburyHairlineSoft
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = procedure,
                        onValueChange = { procedure = it },
                        label = { Text("Procedure Details") },
                        placeholder = { Text("e.g. Composite Restoration") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = thornburyTextFieldColors(containerColor = ThornburySurfaceSoft),
                        singleLine = true
                    )
                }

                // 2. Attending Clinician Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ThornburySurfaceSoft,
                    border = BorderStroke(1.dp, ThornburyHairlineSoft),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = ThornburyPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Attending: ${attendingClinician.name}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = ThornburyInk
                        )
                    }
                }

                // 3. Date Selection Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Appointment Date",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )

                    // Quick Date Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickDates.forEach { (label, dateVal) ->
                            val isSelected = date == dateVal
                            FilterChip(
                                selected = isSelected,
                                onClick = { date = dateVal },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ThornburyPrimaryWash,
                                    selectedLabelColor = ThornburyPrimaryText
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) ThornburyPrimary else ThornburyHairlineSoft
                                )
                            )
                        }
                    }

                    ThornburyDatePickerField(
                        value = date,
                        onValueChange = { date = it },
                        label = "Scheduled Date",
                        placeholder = "YYYY-MM-DD",
                        isOptional = false,
                        helperText = isoDateDisplayLabel(date).takeIf { isDateValid }
                    )
                }

                // 4. Time & Duration Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Time of Visit",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )

                    // Quick Time Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickTimes.forEach { qTime ->
                            val isSelected = time.equals(qTime, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { time = qTime },
                                label = {
                                    Text(
                                        text = qTime,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ThornburyPrimaryWash,
                                    selectedLabelColor = ThornburyPrimaryText
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) ThornburyPrimary else ThornburyHairlineSoft
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        placeholder = { Text("09:30 AM") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = thornburyTextFieldColors(containerColor = ThornburySurfaceSoft),
                        singleLine = true,
                        isError = time.isNotBlank() && !isTimeValid,
                        supportingText = {
                            if (time.isNotBlank() && !isTimeValid) {
                                Text("Format: hh:mm AM/PM (e.g. 09:30 AM)", color = ThornburyError, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    )
                }

                // Duration Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        durationOptions.forEach { dur ->
                            val isSelected = dur == durationMin
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { durationMin = dur },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) ThornburyPrimary else ThornburySurfaceSoft,
                                border = BorderStroke(1.dp, if (isSelected) ThornburyPrimary else ThornburyHairline)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${dur}m",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) Color.White else ThornburyInk
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Chairside Patient Arrival Reminder Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = ThornburySurfaceSoft,
                    border = BorderStroke(1.dp, ThornburyHairline)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (reminderEnabled) ThornburyPrimary else ThornburyMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Chairside Arrival Reminder",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = ThornburyInk
                                    )
                                    Text(
                                        text = if (reminderEnabled) "Alerts you $reminderLeadTimeMin min before arrival" else "Reminder disabled",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ThornburyMuted
                                    )
                                }
                            }
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { reminderEnabled = it }
                            )
                        }

                        if (reminderEnabled) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = ThornburyHairlineSoft)
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Alert Lead Time",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyInk
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    10 to "10 min",
                                    15 to "15 min",
                                    30 to "30 min",
                                    60 to "1 hour"
                                ).forEach { (leadMin, leadLabel) ->
                                    val isSelected = reminderLeadTimeMin == leadMin
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { reminderLeadTimeMin = leadMin },
                                        label = {
                                            Text(
                                                text = leadLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ThornburyPrimaryWash,
                                            selectedLabelColor = ThornburyPrimaryText
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = if (isSelected) ThornburyPrimary else ThornburyHairlineSoft
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Actions: Cancel & Confirm Booking
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Text("Cancel", color = ThornburyInk)
                    }

                    Button(
                        onClick = {
                            if (procedure.isNotBlank() && isDateValid && isTimeValid) {
                                onSave(
                                    procedure.trim(),
                                    attendingClinician.id,
                                    attendingClinician.name,
                                    date.trim(),
                                    time.trim(),
                                    durationMin,
                                    "Surgery 1",
                                    reminderEnabled,
                                    reminderLeadTimeMin
                                )
                            }
                        },
                        enabled = procedure.isNotBlank() && isDateValid && isTimeValid,
                        modifier = Modifier.weight(1.6f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThornburyPrimary,
                            contentColor = Color.White,
                            disabledContainerColor = ThornburyPrimaryDisabled,
                            disabledContentColor = ThornburyMuted
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Schedule Visit",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
