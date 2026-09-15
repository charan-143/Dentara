package com.example.thornburydental.ui.clinic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thornburydental.data.Appointment
import com.example.thornburydental.data.AuthRepository
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.theme.*
import com.example.thornburydental.util.calculateAge
import com.example.thornburydental.util.formatTimeWithAmPm
import com.example.thornburydental.util.parseTimeToMinutes
import com.example.thornburydental.util.todayIsoDate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun TodayQueueScreen(
    onOpenPatientChart: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val appointments by DentalRepository.appointments.collectAsState()
    val currentUser by AuthRepository.currentUser.collectAsState()

    var filterMode by remember { mutableStateOf("all") } // "all", "confirmed", "completed", "cancelled"
    var searchQuery by remember { mutableStateOf("") }
    var appointmentToCancel by remember { mutableStateOf<Appointment?>(null) }

    // This tab is specifically "Today" — now that appointments carry a real
    // date, scope everything below to just today's date rather than every
    // appointment ever booked (the Schedule tab's calendar covers other days).
    val todayAppointments = remember(appointments) {
        val today = todayIsoDate()
        appointments.filter { it.date == today }
    }

    val confirmedList = remember(todayAppointments) { todayAppointments.filter { it.status == "confirmed" } }
    val completedList = remember(todayAppointments) { todayAppointments.filter { it.status == "completed" } }
    val cancelledList = remember(todayAppointments) { todayAppointments.filter { it.status == "cancelled" } }

    // "Next in chair" = the earliest confirmed appointment at or after the
    // current time-of-day, falling back to the first confirmed appointment
    // overall if every confirmed slot has already passed today.
    val nextAppt = remember(confirmedList) {
        val nowMinutes = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
        confirmedList.firstOrNull { parseTimeToMinutes(it.time) >= nowMinutes } ?: confirmedList.firstOrNull()
    }

    val filteredAppointments = remember(todayAppointments, filterMode, searchQuery) {
        todayAppointments.filter { row ->
            if (filterMode != "all" && row.status != filterMode) return@filter false
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                val matchName = row.patientName.lowercase().contains(q)
                val matchOpNo = row.patientOpNo.lowercase().contains(q)
                val matchType = row.procedure.lowercase().contains(q)
                val matchRoom = row.room.lowercase().contains(q)
                if (!matchName && !matchOpNo && !matchType && !matchRoom) return@filter false
            }
            true
        }
    }

    // Dynamic greeting
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = if (hour < 12) "Good morning" else if (hour < 17) "Good afternoon" else "Good evening"
    val clinicianName = currentUser?.name ?: "there"
    val todayDateLabel = remember {
        SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Calendar.getInstance().time)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ThornburyCanvas),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // =====================================================================
        // 1. MATERIAL 3 CLEAN HEADER
        // =====================================================================
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ThornburyCanvas,
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$greeting, $clinicianName",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ThornburyInk
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$todayDateLabel • Surgery 1",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ThornburyMuted
                            )
                        }

                        // Compact M3 status pill
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = ThornburySurfaceSoft,
                            border = BorderStroke(1.dp, ThornburyHairline)
                        ) {
                            Text(
                                text = "${confirmedList.size} Remaining • ${completedList.size} Seen",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyPrimaryText
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 2. NEXT PATIENT IN CHAIR SPOTLIGHT
        // =====================================================================
        if (nextAppt != null) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = ThornburySurfaceCard),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        // Header pill / badge & Time badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(9999.dp),
                                color = ThornburyPrimary
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(ThornburyOnPrimary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "NEXT IN CHAIR",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.2.sp
                                        ),
                                        color = ThornburyOnPrimary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ThornburyCanvas,
                                border = BorderStroke(1.dp, ThornburyHairline)
                            ) {
                                Text(
                                    text = "${formatTimeWithAmPm(nextAppt.time)} • ${nextAppt.durationMin} MIN",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = ThornburyInk
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Patient name, age & OP number
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onOpenPatientChart(nextAppt.patientId) }
                        ) {
                            Text(
                                text = nextAppt.patientName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyInk
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${calculateAge(nextAppt.patientDob)} yrs)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ThornburyMuted
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ThornburySurfaceSoft,
                                border = BorderStroke(1.dp, ThornburyHairlineSoft)
                            ) {
                                Text(
                                    text = "OP: ${nextAppt.patientOpNo}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = ThornburyBodyStrong
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Procedure and room
                        Text(
                            text = "${nextAppt.procedure} • ${nextAppt.room}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ThornburyBodyStrong
                        )



                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons: Filled Open Chart & Outlined Mark Seen
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onOpenPatientChart(nextAppt.patientId) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ThornburyPrimary,
                                    contentColor = ThornburyOnPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Open Chart",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }

                            OutlinedButton(
                                onClick = { DentalRepository.updateAppointmentStatus(nextAppt.id, "completed") },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ThornburyInk
                                ),
                                border = BorderStroke(1.dp, ThornburyHairline),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = ThornburySuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Mark Seen",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 3. SEARCH & MATERIAL 3 FILTER CHIPS
        // =====================================================================
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                // Search Bar (24dp pill shape)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    placeholder = {
                        Text(
                            text = "Search patient, OP, type, room...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ThornburyMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = ThornburyMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = ThornburyMuted
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ThornburyInk,
                        unfocusedTextColor = ThornburyInk,
                        focusedContainerColor = ThornburyCanvas,
                        unfocusedContainerColor = ThornburyCanvas,
                        focusedBorderColor = ThornburyPrimary,
                        unfocusedBorderColor = ThornburyHairline,
                        cursorColor = ThornburyPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Material 3 FilterChips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterOptions = listOf(
                        "all" to "All (${todayAppointments.size})",
                        "confirmed" to "Upcoming (${confirmedList.size})",
                        "completed" to "Seen (${completedList.size})",
                        "cancelled" to "Cancelled (${cancelledList.size})"
                    )

                    filterOptions.forEach { (mode, label) ->
                        val isSelected = filterMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { filterMode = mode },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = ThornburySurfaceSoft,
                                labelColor = ThornburyBody,
                                selectedContainerColor = ThornburyPrimary,
                                selectedLabelColor = ThornburyOnPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = ThornburyHairline,
                                selectedBorderColor = ThornburyPrimary
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
        }

        // =====================================================================
        // 4. TODAY'S SCHEDULE HEADER
        // =====================================================================
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Schedule",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = ThornburyInk
                )
                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = ThornburySurfaceSoft,
                    border = BorderStroke(1.dp, ThornburyHairline)
                ) {
                    Text(
                        text = "${filteredAppointments.size} scheduled",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyPrimaryText
                    )
                }
            }
        }

        // =====================================================================
        // 5. APPOINTMENTS LIST / EMPTY STATE
        // =====================================================================
        if (filteredAppointments.isEmpty()) {
            item {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, ThornburyHairline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = ThornburyMuted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching appointments" else "No appointments found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = ThornburyInk
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Try refining your search terms or clearing the search query." else "No appointments match the selected filter tab.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredAppointments, key = { it.id }) { row ->
                val isConfirmed = row.status == "confirmed"
                val isCompleted = row.status == "completed"
                val isCancelled = row.status == "cancelled"

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (isConfirmed) ThornburySurfaceCard else ThornburyCanvas
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isConfirmed) ThornburyHairline else ThornburyHairlineSoft
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Top Row: Time badge (left) and Status badge (right)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ThornburySurfaceSoft,
                                border = BorderStroke(1.dp, ThornburyHairline)
                            ) {
                                Text(
                                    text = "${formatTimeWithAmPm(row.time)} • ${row.durationMin} MIN",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = ThornburyInk
                                )
                            }

                            when {
                                isConfirmed -> {
                                    Surface(
                                        shape = RoundedCornerShape(9999.dp),
                                        color = ThornburySuccessWash,
                                        border = BorderStroke(1.dp, ThornburySuccess.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = "Upcoming",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            ),
                                            color = ThornburySuccess
                                        )
                                    }
                                }
                                isCompleted -> {
                                    Surface(
                                        shape = RoundedCornerShape(9999.dp),
                                        color = ThornburyInfoWash,
                                        border = BorderStroke(1.dp, ThornburyAccentTeal.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = ThornburyPrimaryText,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Seen",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                ),
                                                color = ThornburyPrimaryText
                                            )
                                        }
                                    }
                                }
                                isCancelled -> {
                                    Surface(
                                        shape = RoundedCornerShape(9999.dp),
                                        color = ThornburyErrorWash,
                                        border = BorderStroke(1.dp, ThornburyError.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = "Cancelled",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            ),
                                            color = ThornburyError
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Middle Section: Patient info, OP & room, procedure & clinician
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onOpenPatientChart(row.patientId) }
                        ) {
                            Text(
                                text = row.patientName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyInk
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${calculateAge(row.patientDob)} yrs)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ThornburyMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "OP: ${row.patientOpNo} • ${row.room}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = ThornburyMuted
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "${row.procedure} • ${row.clinicianName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyBodyStrong
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bottom Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onOpenPatientChart(row.patientId) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Chart",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }

                            if (isConfirmed) {
                                OutlinedButton(
                                    onClick = { DentalRepository.updateAppointmentStatus(row.id, "completed") },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = ThornburySuccess
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = ThornburySuccess,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Mark Seen",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }

                                OutlinedButton(
                                    onClick = { appointmentToCancel = row },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = ThornburyError
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = ThornburyError,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Cancel",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Cancellation Safety Confirmation Dialog
    if (appointmentToCancel != null) {
        val appt = appointmentToCancel!!
        AlertDialog(
            onDismissRequest = { appointmentToCancel = null },
            title = {
                Text(
                    text = "Confirm Cancellation",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to cancel the appointment for ${appt.patientName} (${appt.time} in ${appt.room})? This action will mark it as cancelled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThornburyBody
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        DentalRepository.updateAppointmentStatus(appt.id, "cancelled")
                        appointmentToCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThornburyError)
                ) {
                    Text("Cancel Appointment", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { appointmentToCancel = null }) {
                    Text("Keep Appointment", color = ThornburyInk)
                }
            },
            containerColor = ThornburyCanvas,
            shape = RoundedCornerShape(12.dp)
        )
    }
}
