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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

@OptIn(ExperimentalMaterial3Api::class)
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

    val todayAppointments = remember(appointments) {
        val today = todayIsoDate()
        appointments.filter { it.date == today }
    }

    val confirmedList = remember(todayAppointments) { todayAppointments.filter { it.status == "confirmed" } }
    val completedList = remember(todayAppointments) { todayAppointments.filter { it.status == "completed" } }
    val cancelledList = remember(todayAppointments) { todayAppointments.filter { it.status == "cancelled" } }

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
                if (!matchName && !matchOpNo && !matchType) return@filter false
            }
            true
        }
    }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = if (hour < 12) "Good morning" else if (hour < 17) "Good afternoon" else "Good evening"
    val clinicianName = currentUser?.name ?: "Doctor"
    val todayDateLabel = remember {
        SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Calendar.getInstance().time)
    }

    val windowSizeClass = LocalWindowWidthSizeClass.current
    val isCompact = windowSizeClass == WindowWidthSizeClass.COMPACT

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ThornburyCanvas)
    ) {
        // Top Header
        TodayQueueHeader(
            greeting = greeting,
            clinicianName = clinicianName,
            todayDateLabel = todayDateLabel,
            remainingCount = confirmedList.size,
            seenCount = completedList.size
        )

        if (isCompact) {
            // COMPACT LAYOUT (Single scrolling column for phones)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 36.dp)
            ) {
                if (nextAppt != null) {
                    item(key = "spotlight_card") {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                            NextPatientSpotlightCard(
                                nextAppt = nextAppt,
                                onOpenPatientChart = onOpenPatientChart,
                                onMarkSeen = { DentalRepository.updateAppointmentStatus(nextAppt.id, "completed") }
                            )
                        }
                    }
                }

                item(key = "search_and_filters") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QueueSearchBar(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it }
                        )
                        QueueFilterChips(
                            filterMode = filterMode,
                            onFilterModeChange = { filterMode = it },
                            totalCount = todayAppointments.size,
                            confirmedCount = confirmedList.size,
                            completedCount = completedList.size,
                            cancelledCount = cancelledList.size
                        )
                    }
                }

                item(key = "section_header") {
                    TodayScheduleHeader(
                        count = filteredAppointments.size,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }

                if (filteredAppointments.isEmpty()) {
                    item(key = "empty_state") {
                        EmptyQueueCard(
                            searchQuery = searchQuery,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                    }
                } else {
                    items(filteredAppointments, key = { it.id }) { row ->
                        Box(
                            modifier = Modifier
                                .animateItem()
                                .padding(horizontal = 20.dp, vertical = 5.dp)
                        ) {
                            AppointmentRowCard(
                                row = row,
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
                // Left Column: Spotlight Card + Daily Practice Stats
                Column(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (nextAppt != null) {
                        NextPatientSpotlightCard(
                            nextAppt = nextAppt,
                            onOpenPatientChart = onOpenPatientChart,
                            onMarkSeen = { DentalRepository.updateAppointmentStatus(nextAppt.id, "completed") }
                        )
                    } else {
                        NoNextPatientSpotlightCard()
                    }

                    TodayStatsCard(
                        confirmedCount = confirmedList.size,
                        completedCount = completedList.size,
                        cancelledCount = cancelledList.size,
                        totalCount = todayAppointments.size
                    )
                }

                // Right Column: Search + Filter Chips + Scheduled Appointments List
                Column(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                ) {
                    QueueSearchBar(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    QueueFilterChips(
                        filterMode = filterMode,
                        onFilterModeChange = { filterMode = it },
                        totalCount = todayAppointments.size,
                        confirmedCount = confirmedList.size,
                        completedCount = completedList.size,
                        cancelledCount = cancelledList.size
                    )

                    TodayScheduleHeader(count = filteredAppointments.size)

                    if (filteredAppointments.isEmpty()) {
                        EmptyQueueCard(
                            searchQuery = searchQuery,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(filteredAppointments, key = { it.id }) { row ->
                                Box(modifier = Modifier.animateItem()) {
                                    AppointmentRowCard(
                                        row = row,
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

    // Cancellation Safety Confirmation Dialog with Material 3 styling
    if (appointmentToCancel != null) {
        val appt = appointmentToCancel!!
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
                    text = "Are you sure you want to cancel the surgery appointment for ${appt.patientName} at ${formatTimeWithAmPm(appt.time)}?",
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
            containerColor = ThornburyCanvas,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.adaptiveDialogWidth(480.dp)
        )
    }
}

// =============================================================================
// REUSABLE MATERIAL 3 SUB-COMPONENTS WITH FLUID ANIMATIONS
// =============================================================================

@Composable
private fun TodayQueueHeader(
    greeting: String,
    clinicianName: String,
    todayDateLabel: String,
    remainingCount: Int,
    seenCount: Int
) {
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
                        text = "$greeting, $clinicianName",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = todayDateLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThornburyMuted
                    )
                }

                // Animated Badge
                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = ThornburySurfaceSoft,
                    border = BorderStroke(1.dp, ThornburyHairlineSoft),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedContent(
                            targetState = remainingCount,
                            transitionSpec = {
                                (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                            },
                            label = "RemainingCountAnim"
                        ) { count ->
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = ThornburyPrimaryText
                            )
                        }
                        Text(
                            text = " Remaining • ",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = ThornburyPrimaryText
                        )
                        AnimatedContent(
                            targetState = seenCount,
                            transitionSpec = {
                                (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                            },
                            label = "SeenCountAnim"
                        ) { count ->
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = ThornburySuccess
                            )
                        }
                        Text(
                            text = " Seen",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = ThornburySuccess
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NextPatientSpotlightCard(
    nextAppt: Appointment,
    onOpenPatientChart: (String) -> Unit,
    onMarkSeen: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_indicator")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = ThornburySurfaceCard),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp, pressedElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Pulsing Tag + Time/Duration Pill
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
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(ThornburyOnPrimary.copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(7.dp))
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
                    shape = RoundedCornerShape(10.dp),
                    color = ThornburyCanvas,
                    border = BorderStroke(1.dp, ThornburyHairline)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = ThornburyInk,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${formatTimeWithAmPm(nextAppt.time)} • ${nextAppt.durationMin} MIN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = ThornburyInk
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Patient Identity & Meta Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenPatientChart(nextAppt.patientId) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = ThornburyPrimaryWash,
                    border = BorderStroke(1.dp, ThornburyPrimary.copy(alpha = 0.3f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = nextAppt.patientName.take(2).uppercase(),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyPrimaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = nextAppt.patientName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${calculateAge(nextAppt.patientDob)} yrs)",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = ThornburyBodyStrong
                            )
                        }

                        Text(
                            text = nextAppt.procedure,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = ThornburyBodyStrong,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons with Material 3 styling
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onOpenPatientChart(nextAppt.patientId) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThornburyPrimary,
                        contentColor = ThornburyOnPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
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
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                FilledTonalButton(
                    onClick = onMarkSeen,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = ThornburySuccessWash,
                        contentColor = ThornburySuccess
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ThornburySuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mark Seen",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun NoNextPatientSpotlightCard() {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft.copy(alpha = 0.6f)),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = ThornburySuccessWash,
                border = BorderStroke(1.dp, ThornburySuccess.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = null,
                        tint = ThornburySuccess,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Queue Cleared",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = ThornburyInk
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "No upcoming patients remaining in today's surgery schedule.",
                style = MaterialTheme.typography.bodySmall,
                color = ThornburyMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TodayStatsCard(
    confirmedCount: Int,
    completedCount: Int,
    cancelledCount: Int,
    totalCount: Int
) {
    val targetProgress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "ProgressAnimation"
    )

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Clinical Progress",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ThornburyCanvas,
                    border = BorderStroke(1.dp, ThornburyHairlineSoft)
                ) {
                    Text(
                        text = "$totalCount visits total",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = ThornburyMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Animated Linear Progress Indicator
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = ThornburyPrimary,
                trackColor = ThornburyHairline
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnimatedContent(
                    targetState = (animatedProgress * 100).toInt(),
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "PercentAnim"
                ) { percent ->
                    Text(
                        text = "$percent% completed",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyPrimaryText
                    )
                }
                Text(
                    text = "$completedCount of $totalCount seen",
                    style = MaterialTheme.typography.labelSmall,
                    color = ThornburyMuted
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = ThornburyHairlineSoft)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProgressMetricItem(
                    label = "Upcoming",
                    count = confirmedCount,
                    color = ThornburyPrimaryText,
                    icon = Icons.Default.Schedule
                )
                ProgressMetricItem(
                    label = "Seen",
                    count = completedCount,
                    color = ThornburySuccess,
                    icon = Icons.Default.CheckCircle
                )
                ProgressMetricItem(
                    label = "Cancelled",
                    count = cancelledCount,
                    color = ThornburyError,
                    icon = Icons.Default.Cancel
                )
            }
        }
    }
}

@Composable
private fun ProgressMetricItem(
    label: String,
    count: Int,
    color: Color,
    icon: ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            AnimatedContent(
                targetState = count,
                transitionSpec = {
                    (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                },
                label = "MetricItemCountAnim"
            ) { c ->
                Text(
                    text = "$c",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = color
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = ThornburyMuted
        )
    }
}

@Composable
private fun QueueSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
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
                IconButton(
                    onClick = { onSearchQueryChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = ThornburyMuted,
                        modifier = Modifier.size(18.dp)
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
}

@Composable
private fun QueueFilterChips(
    filterMode: String,
    onFilterModeChange: (String) -> Unit,
    totalCount: Int,
    confirmedCount: Int,
    completedCount: Int,
    cancelledCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val filterOptions = listOf(
            "all" to "All ($totalCount)",
            "confirmed" to "Upcoming ($confirmedCount)",
            "completed" to "Seen ($completedCount)",
            "cancelled" to "Cancelled ($cancelledCount)"
        )

        filterOptions.forEach { (mode, label) ->
            val isSelected = filterMode == mode
            val targetColor by animateColorAsState(
                targetValue = if (isSelected) ThornburyPrimary else ThornburySurfaceSoft,
                animationSpec = tween(250),
                label = "ChipColorAnim"
            )

            FilterChip(
                selected = isSelected,
                onClick = { onFilterModeChange(mode) },
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
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = targetColor,
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

@Composable
private fun TodayScheduleHeader(
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.EventNote,
                contentDescription = null,
                tint = ThornburyPrimaryText,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Today's Schedule",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = ThornburyInk
            )
        }

        Surface(
            shape = RoundedCornerShape(9999.dp),
            color = ThornburySurfaceSoft,
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            AnimatedContent(
                targetState = count,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ScheduleCountAnim"
            ) { c ->
                Text(
                    text = "$c scheduled",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyPrimaryText
                )
            }
        }
    }
}

@Composable
private fun AppointmentRowCard(
    row: Appointment,
    onOpenPatientChart: (String) -> Unit,
    onCancelClick: (Appointment) -> Unit
) {
    val isConfirmed = row.status == "confirmed"
    val isCompleted = row.status == "completed"
    val isCancelled = row.status == "cancelled"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConfirmed) ThornburySurfaceCard else ThornburyCanvas
        ),
        border = BorderStroke(
            1.dp,
            if (isConfirmed) ThornburyHairline else ThornburyHairlineSoft
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isConfirmed) 1.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Time Badge and Animated Status Pill
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
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = ThornburyInk,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${formatTimeWithAmPm(row.time)} • ${row.durationMin} MIN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = ThornburyInk
                        )
                    }
                }

                AnimatedContent(
                    targetState = row.status,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.92f)) togetherWith
                                (fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.92f))
                    },
                    label = "StatusBadgeAnim"
                ) { status ->
                    when (status) {
                        "confirmed" -> {
                            Surface(
                                shape = RoundedCornerShape(9999.dp),
                                color = ThornburySuccessWash,
                                border = BorderStroke(1.dp, ThornburySuccess.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(ThornburySuccess)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Upcoming",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = ThornburySuccess
                                    )
                                }
                            }
                        }
                        "completed" -> {
                            Surface(
                                shape = RoundedCornerShape(9999.dp),
                                color = ThornburyInfoWash,
                                border = BorderStroke(1.dp, ThornburyAccentTeal.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = ThornburyPrimaryText,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
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
                        "cancelled" -> {
                            Surface(
                                shape = RoundedCornerShape(9999.dp),
                                color = ThornburyErrorWash,
                                border = BorderStroke(1.dp, ThornburyError.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = ThornburyError,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Cancelled",
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
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Middle: Patient Name, Age, OP Code & Procedure
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenPatientChart(row.patientId) }
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = if (isConfirmed) ThornburyPrimaryWash else ThornburySurfaceSoft,
                    border = BorderStroke(1.dp, ThornburyHairline)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = row.patientName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isConfirmed) ThornburyPrimaryText else ThornburyMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = row.patientName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${calculateAge(row.patientDob)} yrs)",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "OP: ${row.patientOpNo}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = ThornburyMuted
                        )
                        Text(
                            text = " • ${row.procedure}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = ThornburyBodyStrong,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom: Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onOpenPatientChart(row.patientId) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ThornburyHairline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ThornburyInk),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
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
                        onClick = { DentalRepository.updateAppointmentStatus(row.id, "completed") },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ThornburySuccessWash,
                            contentColor = ThornburySuccess
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = ThornburySuccess,
                            modifier = Modifier.size(16.dp)
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
                        onClick = { onCancelClick(row) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ThornburyError.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ThornburyError
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = ThornburyError,
                            modifier = Modifier.size(16.dp)
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
private fun EmptyQueueCard(
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "EmptyFloatingIcon")
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
                        tint = ThornburyPrimaryText,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (searchQuery.isNotBlank()) "No matching surgery visits" else "Queue is empty",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = ThornburyInk
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (searchQuery.isNotBlank()) {
                    "Try refining your search terms or clearing the query."
                } else {
                    "No surgery appointments match the active filter mode."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = ThornburyMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
