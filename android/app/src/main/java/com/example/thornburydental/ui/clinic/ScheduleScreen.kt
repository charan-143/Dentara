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

    // Appointments on the selected calendar day only — everything below this
    // (filters, counts, the list) is scoped to that one day.
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
                            text = "Surgery Schedule & Booking",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = ThornburyInk
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Manage surgery appointments, chair allocations, and book patient visits",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ThornburyMuted
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { showSelectPatientDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThornburyPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Book Appointment",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        if (isCompact) {
            // =================================================================
            // COMPACT LAYOUT (Single scrolling column for phones < 600dp)
            // =================================================================
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    WeekStripSelector(
                        selectedDate = selectedDate,
                        appointments = appointments,
                        onSelectDate = { selectedDate = it },
                        onPreviousWeek = { selectedDate = addDaysToIsoDate(selectedDate, -7) },
                        onNextWeek = { selectedDate = addDaysToIsoDate(selectedDate, 7) },
                        onJumpToToday = { selectedDate = todayIsoDate() }
                    )
                }

                item {
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

                if (filteredAppointments.isEmpty()) {
                    item {
                        ScheduleEmptyCard(
                            isDayEmpty = dayAppointments.isEmpty(),
                            selectedDate = selectedDate,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                        )
                    }
                } else {
                    items(filteredAppointments, key = { it.id }) { appt ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
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
            // =================================================================
            // TABLET / EXPANDED LAYOUT (2-column layout for >= 600dp)
            // =================================================================
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
                        shape = RoundedCornerShape(16.dp),
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
                        shape = RoundedCornerShape(16.dp),
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
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Selected Day Summary",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyInk
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = isoDateDisplayLabel(selectedDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyMuted
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "$confirmedCount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ThornburyPrimaryText)
                                    Text(text = "Upcoming", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = ThornburyMuted)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "$completedCount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ThornburySuccess)
                                    Text(text = "Seen", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = ThornburyMuted)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "$cancelledCount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ThornburyError)
                                    Text(text = "Cancelled", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = ThornburyMuted)
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
                            Text(
                                text = "${filteredAppointments.size} booked",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyPrimaryText
                            )
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

    // =========================================================================
    // MODALS & DIALOGS
    // =========================================================================

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
                        shape = RoundedCornerShape(10.dp),
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
                                shape = RoundedCornerShape(8.dp),
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
                                    shape = RoundedCornerShape(10.dp),
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
                                                text = "${patient.opNo} • DOB: ${patient.dob}",
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
            title = {
                Text(
                    text = "Cancel Appointment",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to cancel the appointment for ${appointmentToCancel?.patientName} (${appointmentToCancel?.procedure}) scheduled at ${formatTimeWithAmPm(appointmentToCancel?.time ?: "")}?",
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
                    colors = ButtonDefaults.buttonColors(containerColor = ThornburyError)
                ) {
                    Text("Confirm Cancel", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { appointmentToCancel = null }) {
                    Text("Keep Appointment", color = ThornburyInk)
                }
            },
            shape = RoundedCornerShape(16.dp),
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
                tint = ThornburyMuted
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = ThornburyMuted
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
            unfocusedBorderColor = ThornburyHairline
        ),
        singleLine = true
    )
}

@Composable
private fun ScheduleStatusFilterRow(
    statusFilter: String,
    onStatusFilterChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Appointment Status",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = ThornburyMuted
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val statusOptions = listOf(
                "all" to "All",
                "confirmed" to "Upcoming",
                "completed" to "Seen",
                "cancelled" to "Cancelled"
            )
            statusOptions.forEach { (key, label) ->
                val isSelected = statusFilter == key
                FilterChip(
                    selected = isSelected,
                    onClick = { onStatusFilterChange(key) },
                    label = { Text(label) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ThornburyPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = ThornburySurfaceSoft,
                        labelColor = ThornburyInk
                    )
                )
            }
        }
    }
}

@Composable
private fun ScheduleAppointmentCard(
    appt: Appointment,
    onOpenPatientChart: (String) -> Unit,
    onCancelClick: (Appointment) -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceCard),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
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
                                    text = "${appt.reminderLeadTimeMin}m reminder",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyInk
                                )
                            }
                        }
                    }

                    val (statusText, statusBg, statusFg) = when (appt.status) {
                        "completed" -> Triple("Seen", ThornburySuccessWash, ThornburySuccess)
                        "cancelled" -> Triple("Cancelled", ThornburyErrorWash, ThornburyError)
                        else -> Triple("Confirmed", ThornburyPrimaryWash, ThornburyPrimaryText)
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
                        modifier = Modifier.size(16.dp),
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
                        modifier = Modifier.size(16.dp),
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
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
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
                            Spacer(modifier = Modifier.width(4.dp))
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
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onOpenPatientChart(appt.patientId) },
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

                if (appt.status == "confirmed") {
                    OutlinedButton(
                        onClick = { DentalRepository.updateAppointmentStatus(appt.id, "completed") },
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
                        onClick = { onCancelClick(appt) },
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

@Composable
private fun ScheduleEmptyCard(
    isDayEmpty: Boolean,
    selectedDate: String,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = ThornburyCanvas),
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
                modifier = Modifier.size(48.dp),
                tint = ThornburyMutedSoft
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isDayEmpty) "No Appointments This Day" else "No Matching Appointments",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = ThornburyInk
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isDayEmpty) {
                    "Nothing booked for ${isoDateDisplayLabel(selectedDate)} yet."
                } else {
                    "No appointments on ${isoDateDisplayLabel(selectedDate)} match your active status filter or search query."
                },
                style = MaterialTheme.typography.bodySmall,
                color = ThornburyMuted,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Horizontal 7-day (Monday-Sunday) week strip: month/year label with
 * previous/next-week navigation and a "Today" jump button, then one column
 * per day showing the weekday letters, day-of-month in a circle (filled when
 * selected, tinted when it's today), and a small dot when that day has at
 * least one appointment on record (independent of the status/search
 * filters below, so the dot is a reliable "is anything booked here" signal).
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
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
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
                    if (!isIsoDateToday(selectedDate)) {
                        TextButton(onClick = onJumpToToday, contentPadding = PaddingValues(horizontal = 8.dp)) {
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

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                weekDates.forEach { date ->
                    val isSelected = date == selectedDate
                    val isToday = isIsoDateToday(date)
                    val hasAppointments = datesWithAppointments.contains(date)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
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
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> ThornburyPrimary
                                        isToday -> ThornburySurfaceSoft
                                        else -> Color.Transparent
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = isoDateDayOfMonth(date).toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = when {
                                    isSelected -> Color.White
                                    isToday -> ThornburyPrimaryText
                                    else -> ThornburyInk
                                }
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
