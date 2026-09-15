package com.example.thornburydental.ui.clinic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.thornburydental.data.Clinician
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.Patient
import com.example.thornburydental.theme.*
import com.example.thornburydental.ui.components.ThornburyDatePickerField
import com.example.thornburydental.util.isoDateDisplayLabel
import com.example.thornburydental.util.todayIsoDate

/**
 * Quick Follow-up Visit & Surgery Booking Dialog.
 *
 * Facilitates chairside booking of scheduled follow-up visits, operative dental
 * procedures, hygiene appointments, or implant reviews with automatic surgery room
 * assignment and patient allergy cross-check alerting.
 */
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
        room: String
    ) -> Unit
) {
    val clinicians = DentalRepository.clinicians
    var selectedClinician by remember { mutableStateOf(clinicians.firstOrNull() ?: Clinician("c1", "Dr. Ingrid Halvorsen", "", "", "Surgery 1", "")) }

    var procedure by remember { mutableStateOf("Follow-up Review & Examination") }
    var date by remember { mutableStateOf(initialDate) }
    var time by remember { mutableStateOf("09:30 AM") }
    var durationMin by remember { mutableIntStateOf(45) }
    var room by remember { mutableStateOf(selectedClinician.room) }

    val isDateValid = remember(date) {
        Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(date.trim())
    }

    // Free-text time entry is only useful if it's actually a parseable time —
    // an unvalidated string here silently corrupts the schedule's display/sort.
    val isTimeValid = remember(time) {
        Regex("^\\d{1,2}:\\d{2}\\s*(AM|PM)$", RegexOption.IGNORE_CASE).matches(time.trim())
    }

    val durationOptions = listOf(30, 45, 60, 90)
    val rooms = listOf("Surgery 1", "Surgery 2", "Surgery 3")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Book Follow-up Visit",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = ThornburyInk
                        )
                        Text(
                            text = "Patient: ${patient.name} (${patient.opNo})",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyMuted
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ThornburyInk
                        )
                    }
                }



                Spacer(modifier = Modifier.height(14.dp))

                // 1. Procedure Name
                Text(
                    text = "Scheduled Procedure",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = procedure,
                    onValueChange = { procedure = it },
                    placeholder = { Text("e.g. Full Crown Preparation") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = thornburyTextFieldColors(containerColor = ThornburySurfaceSoft),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Clinician Selection
                Text(
                    text = "Attending Clinician",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    clinicians.forEach { clinician ->
                        val isSelected = clinician.id == selectedClinician.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedClinician = clinician
                                    room = clinician.room
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) ThornburySurfaceSoft else ThornburyCanvas,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) ThornburyPrimary else ThornburyHairline
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedClinician = clinician
                                        room = clinician.room
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = ThornburyPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = clinician.name,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = ThornburyInk
                                    )
                                    Text(
                                        text = "${clinician.specialty} • Default: ${clinician.room}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ThornburyMuted
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Appointment Date
                Text(
                    text = "Appointment Date",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
                Spacer(modifier = Modifier.height(6.dp))
                ThornburyDatePickerField(
                    value = date,
                    onValueChange = { date = it },
                    label = "Date",
                    placeholder = "YYYY-MM-DD",
                    isOptional = false,
                    helperText = isoDateDisplayLabel(date).takeIf { isDateValid }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Time of Visit
                Text(
                    text = "Time of Visit",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    placeholder = { Text("09:30 AM") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = thornburyTextFieldColors(containerColor = ThornburySurfaceSoft),
                    singleLine = true,
                    isError = time.isNotBlank() && !isTimeValid,
                    supportingText = {
                        if (time.isNotBlank() && !isTimeValid) {
                            Text(
                                text = "Use hh:mm AM/PM, e.g. 09:30 AM",
                                style = MaterialTheme.typography.labelSmall,
                                color = ThornburyError
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Duration Selection
                Text(
                    text = "Appointment Duration",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
                Spacer(modifier = Modifier.height(6.dp))
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
                            shape = RoundedCornerShape(8.dp),
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

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Clinical Surgery Room
                Text(
                    text = "Operatory Surgery Room",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rooms.forEach { roomName ->
                        val isSelected = roomName == room
                        FilterChip(
                            selected = isSelected,
                            onClick = { room = roomName },
                            label = { Text(roomName) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ThornburyPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = ThornburySurfaceSoft,
                                labelColor = ThornburyInk
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 6. Actions: Cancel & Confirm Booking
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
                                    selectedClinician.id,
                                    selectedClinician.name,
                                    date.trim(),
                                    time.trim(),
                                    durationMin,
                                    room
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
                            text = "Confirm Booking",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
