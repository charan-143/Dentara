package com.example.thornburydental.ui.clinic

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.foundation.verticalScroll
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
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.Patient
import com.example.thornburydental.theme.*
import com.example.thornburydental.util.addDaysToIsoDate
import com.example.thornburydental.util.calculateAge
import com.example.thornburydental.util.formatTimeWithAmPm
import com.example.thornburydental.util.isIsoDateToday
import com.example.thornburydental.util.isoDateDayOfMonth
import com.example.thornburydental.util.isoDateDisplayLabel
import com.example.thornburydental.util.isoDateMonthYearLabel
import com.example.thornburydental.util.isoDateWeekdayShortLabel
import com.example.thornburydental.util.todayIsoDate
import com.example.thornburydental.util.weekDatesContaining

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onOpenPatientChart: (String) -> Unit = {},
    onNavigateToRegisterPatient: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val appointments by DentalRepository.appointments.collectAsState()
    val patients by DentalRepository.patients.collectAsState()

    var selectedDate by remember { mutableStateOf(todayIsoDate()) }
    var statusFilter by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }
    var showSelectPatientDialog by remember { mutableStateOf(false) }
    var bookingTargetPatient by remember { mutableStateOf<Patient?>(null) }
    var appointmentToCancel by remember { mutableStateOf<Appointment?>(null) }

    val dayAppointments = remember(appointments, selectedDate) {
        appointments.filter { it.date == selectedDate }
    }

    val filteredAppointments = remember(dayAppointments, statusFilter, searchQuery) {
        dayAppointments.filter { appt ->
            if (statusFilter != "all" && appt.status != statusFilter) return@filter false
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                val matchName = appt.patientName.lowercase().contains(q)
                val matchOp = appt.patientOpNo.lowercase().contains(q)
                val matchProc = appt.procedure.lowercase().contains(q)
                val matchClin = appt.clinicianName.lowercase().contains(q)
                if (!matchName && !matchOp && !matchProc && !matchClin) return@filter false
            }
            true
        }
    }

    val windowSizeClass = LocalWindowWidthSizeClass.current
    val isCompact = windowSizeClass == WindowWidthSizeClass.COMPACT

    val confirmedCount = remember(dayAppointments) { dayAppointments.count { it.status == "confirmed" } }
    val completedCount = remember(dayAppointments) { dayAppointments.count { it.status == "completed" } }
    val cancelledCount = remember(dayAppointments) { dayAppointments.count { it.status == "cancelled" } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ThornburyCanvas)
    ) {
        // Shared Top Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ThornburyCanvas,
            border = BorderStroke(1.dp, ThornburyHairline),
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Surgery Schedule",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp
                            ),
                            color = ThornburyInk
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Manage chair allocations and book surgical visits",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ThornburyMuted
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { showSelectPatientDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThornburyPrimary,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Book Visit",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        if (isCompact) {
            // COMPACT LAYOUT (Single scrolling column for phones)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 36.dp)
            ) {
                item(key = "week_strip") {
                    WeekStripSelector(
                        selectedDate = selectedDate,
                        appointments = appointments,
                        onSelectDate = { selectedDate = it },
                        onPreviousWeek = { selectedDate = addDaysToIsoDate(selectedDate, -7) },
                        onNextWeek = { selectedDate = addDaysToIsoDate(selectedDate, 7) },
                        onJumpToToday = { selectedDate = todayIsoDate() }
                    )
                }

                item(key = "search_and_filters") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ScheduleSearchField(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it }
                        )

                        ScheduleStatusFilterRow(
                            statusFilter = statusFilter,
                            onStatusFilterChange = { statusFilter = it }
                        )
                    }
                }

                item(key = "schedule_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Appointments for ${isoDateDisplayLabel(selectedDate)}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = ThornburySurfaceSoft,
                            border = BorderStroke(1.dp, ThornburyHairline)
                        ) {
                            AnimatedContent(
                                targetState = filteredAppointments.size,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "FilteredApptCountAnim"
                            ) { count ->
                                Text(
                                    text = "$count booked",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyPrimaryText
                                )
                            }
                        }
                    }
                }

                if (filteredAppointments.isEmpty()) {
                    item(key = "empty_state") {
                        ScheduleEmptyCard(
                            isDayEmpty = dayAppointments.isEmpty(),
                            selectedDate = selectedDate,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                        )
                    }
                } else {
                    items(filteredAppointments, key = { it.id }) { appt ->
                        Box(
                            modifier = Modifier
                                .animateItem()
                                .padding(horizontal = 20.dp, vertical = 5.dp)
                        ) {
                            ScheduleAppointmentCard(
                                appt = appt,
                                onOpenPatientChart = onOpenPatientChart,
                                onCancelClick = { appointmentToCancel = it }
                            )
                        }
                    }
                }
            }
        } else {
            // TABLET / EXPANDED LAYOUT (2-column responsive layout)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Calendar Strip, Filters & Quick Day Metrics
                Column(
                    modifier = Modifier
                        .weight(0.40f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = ThornburyCanvas),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        WeekStripSelector(
                            selectedDate = selectedDate,
                            appointments = appointments,
                            onSelectDate = { selectedDate = it },
                            onPreviousWeek = { selectedDate = addDaysToIsoDate(selectedDate, -7) },
                            onNextWeek = { selectedDate = addDaysToIsoDate(selectedDate, 7) },
                            onJumpToToday = { selectedDate = todayIsoDate() }
                        )
                    }

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Filter by Status",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyInk
                            )
                            ScheduleStatusFilterRow(
                                statusFilter = statusFilter,
                                onStatusFilterChange = { statusFilter = it }
                            )
                        }
                    }

                    // Day Metrics Summary
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Day Summary",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyInk
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = isoDateDisplayLabel(selectedDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyMuted
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AnimatedContent(targetState = confirmedCount, label = "SummaryConfirmed") { c ->
                                        Text(text = "$c", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = ThornburyPrimaryText)
                                    }
                                    Text(text = "Upcoming", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = ThornburyMuted)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AnimatedContent(targetState = completedCount, label = "SummaryCompleted") { c ->
                                        Text(text = "$c", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = ThornburySuccess)
                                    }
                                    Text(text = "Seen", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = ThornburyMuted)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AnimatedContent(targetState = cancelledCount, label = "SummaryCancelled") { c ->
                                        Text(text = "$c", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = ThornburyError)
                                    }
                                    Text(text = "Cancelled", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = ThornburyMuted)
                                }
                            }
                        }
                    }
                }

                // Right Column: Search & Appointments List
                Column(
                    modifier = Modifier
                        .weight(0.60f)
                        .fillMaxHeight()
                ) {
                    ScheduleSearchField(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Appointments for ${isoDateDisplayLabel(selectedDate)}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = ThornburySurfaceSoft,
                            border = BorderStroke(1.dp, ThornburyHairline)
                        ) {
                            AnimatedContent(
                                targetState = filteredAppointments.size,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "TabletFilteredCount"
                            ) { count ->
                                Text(
                                    text = "$count booked",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyPrimaryText
                                )
                            }
                        }
                    }

                    if (filteredAppointments.isEmpty()) {
                        ScheduleEmptyCard(
                            isDayEmpty = dayAppointments.isEmpty(),
                            selectedDate = selectedDate,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(filteredAppointments, key = { it.id }) { appt ->
                                Box(modifier = Modifier.animateItem()) {
                                    ScheduleAppointmentCard(
                                        appt = appt,
                                        onOpenPatientChart = onOpenPatientChart,
                                        onCancelClick = { appointmentToCancel = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal 1: Select Patient Dialog
    if (showSelectPatientDialog) {
        var patientSearchQuery by remember { mutableStateOf("") }
        val filteredPatients = remember(patients, patientSearchQuery) {
            patients.filter { p ->
                if (patientSearchQuery.isBlank()) true
                else {
                    val q = patientSearchQuery.trim().lowercase()
                    p.name.lowercase().contains(q) || p.opNo.lowercase().contains(q)
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showSelectPatientDialog = false },
            title = {
                Text(
                    text = "Select Patient for Booking",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = ThornburyInk
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                ) {
                    OutlinedTextField(
                        value = patientSearchQuery,
                        onValueChange = { patientSearchQuery = it },
                        placeholder = { Text("Search patient name or OP number...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = ThornburyMuted
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredPatients.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No registered patients match your search.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyMuted
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    showSelectPatientDialog = false
                                    onNavigateToRegisterPatient()
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ThornburyPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = ThornburyPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Register New Patient", color = ThornburyPrimary)
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredPatients, key = { it.id }) { patient ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            bookingTargetPatient = patient
                                            showSelectPatientDialog = false
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = ThornburySurfaceSoft,
                                    border = BorderStroke(1.dp, ThornburyHairlineSoft)
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
                                                text = patient.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = ThornburyInk
                                            )
                                            Text(
                                                text = "OP: ${patient.opNo} • DOB: ${patient.dob}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ThornburyMuted
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = ThornburyPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSelectPatientDialog = false }) {
                    Text("Close", color = ThornburyInk)
                }
            },
            shape = RoundedCornerShape(18.dp),
            containerColor = ThornburyCanvas,
            modifier = Modifier.adaptiveDialogWidth(500.dp)
        )
    }

    // Modal 2: Book Appointment Dialog
    if (bookingTargetPatient != null) {
        BookAppointmentDialog(
            patient = bookingTargetPatient!!,
            initialDate = selectedDate,
            onDismiss = { bookingTargetPatient = null },
            onSave = { proc, clinId, clinName, date, time, dur, room, reminderEnabled, reminderLeadMin ->
                DentalRepository.bookAppointment(
                    patient = bookingTargetPatient!!,
                    clinicianId = clinId,
                    clinicianName = clinName,
                    date = date,
                    time = time,
                    durationMin = dur,
                    room = room,
                    procedure = proc,
                    reminderEnabled = reminderEnabled,
                    reminderLeadTimeMin = reminderLeadMin
                )
                bookingTargetPatient = null
            }
        )
    }

    // Modal 3: Appointment Cancellation Confirmation Dialog
    if (appointmentToCancel != null) {
        AlertDialog(
            onDismissRequest = { appointmentToCancel = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = ThornburyError,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Cancel Appointment",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to cancel the surgery appointment for ${appointmentToCancel?.patientName} (${appointmentToCancel?.procedure}) at ${formatTimeWithAmPm(appointmentToCancel?.time ?: "")}?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThornburyBody
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        appointmentToCancel?.let { appt ->
                            DentalRepository.updateAppointmentStatus(appt.id, "cancelled")
                        }
                        appointmentToCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThornburyError),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Confirm Cancel", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { appointmentToCancel = null },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ThornburyHairline)
                ) {
                    Text("Keep Appointment", color = ThornburyInk)
                }
            },
            shape = RoundedCornerShape(18.dp),
            containerColor = ThornburyCanvas,
            modifier = Modifier.adaptiveDialogWidth(480.dp)
        )
    }
}

// =============================================================================
// REUSABLE SUB-COMPONENTS
// =============================================================================

@Composable
private fun ScheduleSearchField(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = {
            Text(
                text = "Search patient, OP, procedure...",
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
            AnimatedVisibility(
                visible = searchQuery.isNotEmpty(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = ThornburyMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = ThornburyCanvas,
            unfocusedContainerColor = ThornburyCanvas,
            focusedBorderColor = ThornburyPrimary,
            unfocusedBorderColor = ThornburyHairline,
            cursorColor = ThornburyPrimary
        ),
        singleLine = true
    )
}

@Composable
private fun ScheduleStatusFilterRow(
    statusFilter: String,
    onStatusFilterChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val statusOptions = listOf(
            "all" to "All Statuses",
            "confirmed" to "Upcoming",
            "completed" to "Seen",
            "cancelled" to "Cancelled"
        )
        statusOptions.forEach { (key, label) ->
            val isSelected = statusFilter == key
            val targetBg by animateColorAsState(
                targetValue = if (isSelected) ThornburyPrimary else ThornburySurfaceSoft,
                animationSpec = tween(220),
                label = "ScheduleFilterChipBg"
            )

            FilterChip(
                selected = isSelected,
                onClick = { onStatusFilterChange(key) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = ThornburyOnPrimary
                        )
                    }
                } else null,
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ThornburyPrimary,
                    selectedLabelColor = ThornburyOnPrimary,
                    containerColor = targetBg,
                    labelColor = ThornburyBody
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

@Composable
private fun ScheduleAppointmentCard(
    appt: Appointment,
    onOpenPatientChart: (String) -> Unit,
    onCancelClick: (Appointment) -> Unit
) {
    val isConfirmed = appt.status == "confirmed"

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = ThornburySurfaceCard),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp, pressedElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Top Row: Time Badge, Reminder Pill, and Animated Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ThornburySurfaceSoft,
                    border = BorderStroke(1.dp, ThornburyHairlineSoft)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = ThornburyPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${formatTimeWithAmPm(appt.time)} • ${appt.durationMin} MIN",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyPrimaryText
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (appt.reminderEnabled) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ThornburySurfaceSoft,
                            border = BorderStroke(1.dp, ThornburyHairline)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Reminder enabled",
                                    tint = ThornburyPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${appt.reminderLeadTimeMin}m",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyInk
                                )
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = appt.status,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                        },
                        label = "ScheduleApptStatusAnim"
                    ) { status ->
                        val (statusText, statusBg, statusFg) = when (status) {
                            "completed" -> Triple("Seen", ThornburySuccessWash, ThornburySuccess)
                            "cancelled" -> Triple("Cancelled", ThornburyErrorWash, ThornburyError)
                            else -> Triple("Upcoming", ThornburyPrimaryWash, ThornburyPrimaryText)
                        }
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = statusBg,
                            border = BorderStroke(1.dp, statusFg.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = statusText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = statusFg
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onOpenPatientChart(appt.patientId) }
                ) {
                    Text(
                        text = appt.patientName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${calculateAge(appt.patientDob)} yrs)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThornburyMuted
                    )
                }

                Text(
                    text = "OP: ${appt.patientOpNo}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = ThornburyMuted
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = ThornburyPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = appt.procedure,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = ThornburyInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = ThornburyMuted
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = appt.clinicianName,
                        style = MaterialTheme.typography.bodySmall,
                        color = ThornburyMuted
                    )
                }

                if (!appt.allergyList.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ThornburyErrorWash
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = ThornburyError
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Allergies: ${appt.allergyList}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyError
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = ThornburyHairlineSoft, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onOpenPatientChart(appt.patientId) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ThornburyHairline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ThornburyInk),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Chart",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (isConfirmed) {
                    FilledTonalButton(
                        onClick = { DentalRepository.updateAppointmentStatus(appt.id, "completed") },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ThornburySuccessWash,
                            contentColor = ThornburySuccess
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = ThornburySuccess,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Mark Seen",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    OutlinedButton(
                        onClick = { onCancelClick(appt) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ThornburyError.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ThornburyError
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = ThornburyError,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Cancel",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleEmptyCard(
    isDayEmpty: Boolean,
    selectedDate: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ScheduleEmptyFloating")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FloatingAnimation"
    )

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
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
            Surface(
                modifier = Modifier
                    .size(64.dp)
                    .offset(y = floatOffset.dp),
                shape = CircleShape,
                color = ThornburySurfaceCard,
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.EventBusy,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = ThornburyPrimaryText
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isDayEmpty) "No Appointments This Day" else "No Matching Appointments",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = ThornburyInk
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isDayEmpty) {
                    "Nothing booked for ${isoDateDisplayLabel(selectedDate)} yet."
                } else {
                    "No appointments on ${isoDateDisplayLabel(selectedDate)} match your active status filter or search query."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = ThornburyMuted,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Modern Material 3 Horizontal 7-day (Monday-Sunday) week strip with smooth animated date selector
 */
@Composable
private fun WeekStripSelector(
    selectedDate: String,
    appointments: List<Appointment>,
    onSelectDate: (String) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onJumpToToday: () -> Unit
) {
    val weekDates = remember(selectedDate) { weekDatesContaining(selectedDate) }
    val datesWithAppointments = remember(appointments) { appointments.map { it.date }.toSet() }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ThornburyCanvas,
        border = BorderStroke(1.dp, ThornburyHairline),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = isoDateMonthYearLabel(selectedDate),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(
                        visible = !isIsoDateToday(selectedDate),
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        TextButton(
                            onClick = onJumpToToday,
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyPrimary
                            )
                        }
                    }
                    IconButton(onClick = onPreviousWeek, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous week",
                            tint = ThornburyInk
                        )
                    }
                    IconButton(onClick = onNextWeek, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next week",
                            tint = ThornburyInk
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                weekDates.forEach { date ->
                    val isSelected = date == selectedDate
                    val isToday = isIsoDateToday(date)
                    val hasAppointments = datesWithAppointments.contains(date)

                    val targetContainerColor by animateColorAsState(
                        targetValue = when {
                            isSelected -> ThornburyPrimary
                            isToday -> ThornburySurfaceSoft
                            else -> Color.Transparent
                        },
                        animationSpec = tween(220),
                        label = "DayPillBg"
                    )

                    val targetTextColor by animateColorAsState(
                        targetValue = when {
                            isSelected -> Color.White
                            isToday -> ThornburyPrimaryText
                            else -> ThornburyInk
                        },
                        animationSpec = tween(220),
                        label = "DayPillText"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectDate(date) }
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = isoDateWeekdayShortLabel(date),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = if (isSelected) ThornburyPrimary else ThornburyMuted
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(targetContainerColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = isoDateDayOfMonth(date).toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = targetTextColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (hasAppointments) ThornburyPrimary else Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}
