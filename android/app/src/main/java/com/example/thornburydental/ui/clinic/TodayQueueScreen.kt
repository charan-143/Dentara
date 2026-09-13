package com.example.thornburydental.ui.clinic

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thornburydental.data.Appointment
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.theme.*
import java.util.Calendar

private fun calculateAge(dobStr: String): Int {
    return try {
        val parts = dobStr.split("-").map { it.toInt() }
        val birthYear = parts[0]
        val birthMonth = parts[1] - 1
        val birthDay = parts[2]

        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - birthYear

        val currentMonth = today.get(Calendar.MONTH)
        val currentDay = today.get(Calendar.DAY_OF_MONTH)

        if (currentMonth < birthMonth || (currentMonth == birthMonth && currentDay < birthDay)) {
            age--
        }
        if (age < 0) 0 else age
    } catch (_: Exception) {
        38
    }
}

@Composable
fun TodayQueueScreen(
    onOpenPatientChart: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val appointments by DentalRepository.appointments.collectAsState()
    val draftPlans by DentalRepository.draftPlans.collectAsState()
    val heldResults by DentalRepository.heldResults.collectAsState()

    var filterMode by remember { mutableStateOf<String>("all") } // "all", "confirmed", "completed", "cancelled"
    var searchQuery by remember { mutableStateOf("") }

    val confirmedList = remember(appointments) { appointments.filter { it.status == "confirmed" } }
    val completedList = remember(appointments) { appointments.filter { it.status == "completed" } }
    val cancelledList = remember(appointments) { appointments.filter { it.status == "cancelled" } }

    val nextAppt = remember(confirmedList) { confirmedList.firstOrNull() }

    val activeMinutes = remember(confirmedList) { confirmedList.sumOf { it.durationMin } }

    val filteredAppointments = remember(appointments, filterMode, searchQuery) {
        appointments.filter { row ->
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ThornburyCanvas),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // =====================================================================
        // 1. TOPBAR HEADER (from app/clinic/page.tsx header.topbar)
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
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$greeting, Dr. Ingrid Halvorsen",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ThornburyInk
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Thursday, 10 September 2026 • Surgery 1 • ${confirmedList.size} appointments remaining today",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {},
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ThornburyInk),
                            border = BorderStroke(1.dp, ThornburyHairline)
                        ) {
                            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Schedule", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                        }

                        Button(
                            onClick = {},
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThornburyPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Appointment", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 2. KPI SUMMARY TILES (from app/clinic/page.tsx section.tiles)
        // =====================================================================
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tile 1: Accent Tile (Remaining Today)
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ThornburySurfaceSoft),
                        border = BorderStroke(1.5.dp, ThornburyPrimary)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = ThornburyPrimaryText,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Remaining Today",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = ThornburyPrimaryText
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${confirmedList.size}",
                                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyInk
                            )
                            Text(
                                text = "${appointments.size} total scheduled today",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = ThornburyMuted
                            )
                        }
                    }

                    // Tile 2: Patients Seen
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = ThornburySuccess,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Patients Seen",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = ThornburyBody
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${completedList.size}",
                                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyInk
                            )
                            val percent = if (appointments.isNotEmpty()) Math.round((completedList.size.toFloat() / appointments.size) * 100) else 0
                            Text(
                                text = if (completedList.isNotEmpty()) "$percent% of today completed" else "Ready for first patient",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = ThornburyMuted
                            )
                        }
                    }

                    // Tile 3: Chair Time Booked
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = ThornburyMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Chair Time",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = ThornburyBody
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${activeMinutes}m",
                                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyInk
                            )
                            Text(
                                text = "Active treatment minutes",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = ThornburyMuted
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 3. NEXT UP SPOTLIGHT BANNER (from components/today-view.tsx)
        // =====================================================================
        nextAppt?.let { next ->
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(ThornburySurfaceCreamStrong, ThornburySurfaceCard)
                                    )
                                )
                        ) {
                            // 4px left solid primary border bar
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .fillMaxHeight()
                                    .background(ThornburyPrimary)
                                    .align(Alignment.CenterStart)
                            )

                            Column(modifier = Modifier.padding(start = 18.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)) {
                                // Pulsing badge
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                    val alpha by infiniteTransition.animateFloat(
                                        initialValue = 0.4f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(900, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "pulseAlpha"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(ThornburyPrimary.copy(alpha = alpha))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "NEXT PATIENT UP",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 1.5.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = ThornburyPrimaryText
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Time box
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = ThornburyCanvas,
                                        border = BorderStroke(1.dp, ThornburyHairline)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = next.time,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.SansSerif
                                                ),
                                                color = ThornburyInk
                                            )
                                            Text(
                                                text = "${next.durationMin} MIN",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    letterSpacing = 0.5.sp
                                                ),
                                                color = ThornburyMuted
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    // Patient name & age
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = next.patientName,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = ThornburyInk
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "(${calculateAge(next.patientDob)} yrs)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ThornburyMuted
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${next.procedure} • ${next.room} • OP: ${next.patientOpNo}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ThornburyBody
                                        )
                                        if (!next.allergyList.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(9999.dp),
                                                color = ThornburyErrorWash,
                                                border = BorderStroke(1.dp, ThornburyError.copy(alpha = 0.3f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Warning,
                                                        contentDescription = null,
                                                        tint = ThornburyError,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Allergy: ${next.allergyList}",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = ThornburyError
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onOpenPatientChart(next.patientId) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ThornburyPrimary,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open Chart", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                                    }

                                    OutlinedButton(
                                        onClick = { DentalRepository.updateAppointmentStatus(next.id, "completed") },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ThornburyInk),
                                        border = BorderStroke(1.dp, ThornburyHairline),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = ThornburySuccess)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Mark Seen", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 4. TOOLBAR: SEGMENTED CONTROLS & SEARCH (from today-toolbar)
        // =====================================================================
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                // Segmented control
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ThornburySurfaceSoft,
                    border = BorderStroke(1.dp, ThornburyHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val filters = listOf(
                            "all" to "All (${appointments.size})",
                            "confirmed" to "Upcoming (${confirmedList.size})",
                            "completed" to "Seen (${completedList.size})",
                            "cancelled" to "Cancelled (${cancelledList.size})"
                        )
                        filters.forEach { (mode, title) ->
                            val isSelected = filterMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { filterMode = mode },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) ThornburyCanvas else Color.Transparent,
                                border = if (isSelected) BorderStroke(1.dp, ThornburyHairline) else null,
                                shadowElevation = if (isSelected) 1.dp else 0.dp
                            ) {
                                Text(
                                    text = title,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isSelected) ThornburyPrimaryText else ThornburyBody,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search patient, OP, type, room...", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = ThornburyMuted, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = ThornburyMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ThornburyCanvas,
                        unfocusedContainerColor = ThornburyCanvas,
                        focusedBorderColor = ThornburyPrimary,
                        unfocusedBorderColor = ThornburyHairline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // =====================================================================
        // 5. APPOINTMENTS LIST PANEL (from panel-body flush / today-row)
        // =====================================================================
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Appointments (${filteredAppointments.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    ),
                    color = ThornburyInk
                )
                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = ThornburyInfoWash,
                    border = BorderStroke(1.dp, ThornburyAccentTeal.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${confirmedList.size} active",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyPrimaryText
                    )
                }
            }
        }

        if (filteredAppointments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No appointments found matching current filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThornburyMuted
                    )
                }
            }
        } else {
            items(filteredAppointments, key = { it.id }) { row ->
                val isCancelled = row.status == "cancelled"
                val isCompleted = row.status == "completed"
                val isConfirmed = row.status == "confirmed"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 5.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCancelled) ThornburySurfaceSoft.copy(alpha = 0.6f) else ThornburySurfaceCard
                    ),
                    border = BorderStroke(1.dp, ThornburyHairline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: row-when
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(56.dp)
                                .background(ThornburyCanvas, RoundedCornerShape(6.dp))
                                .border(1.dp, ThornburyHairlineSoft, RoundedCornerShape(6.dp))
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = row.time,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isCancelled) ThornburyMuted else ThornburyInk
                            )
                            Text(
                                text = "${row.durationMin}m",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = ThornburyMuted
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Middle: row-main
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = row.patientName,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                color = if (isCancelled) ThornburyMuted else ThornburyInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { onOpenPatientChart(row.patientId) }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${calculateAge(row.patientDob)} yrs • OP: ${row.patientOpNo}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ThornburyMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (!row.allergyList.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "⚠ Allergy: ${row.allergyList}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = ThornburyError
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${row.procedure} • Room: ${row.room}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyBodyStrong
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Right: row-side with badges & quick actions
                        Column(horizontalAlignment = Alignment.End) {
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
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
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
                                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = ThornburyPrimaryText, modifier = Modifier.size(11.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Seen",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
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
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = ThornburyError
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = { onOpenPatientChart(row.patientId) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) {
                                    Text("Chart", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ThornburyPrimaryText)
                                }

                                if (isConfirmed) {
                                    IconButton(
                                        onClick = { DentalRepository.updateAppointmentStatus(row.id, "completed") },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = "Seen", tint = ThornburySuccess, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { DentalRepository.updateAppointmentStatus(row.id, "cancelled") },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", tint = ThornburyError, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 6. QUICK SHORTCUTS PANEL (from app/clinic/page.tsx)
        // =====================================================================
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "Quick Shortcuts",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    ),
                    color = ThornburyInk
                )
                Spacer(modifier = Modifier.height(8.dp))

                val shortcuts = listOf(
                    Triple("Book Appointment", Icons.Default.CalendarToday, ThornburyPrimary),
                    Triple("Register Patient", Icons.Default.PersonAdd, ThornburyPrimary),
                    Triple("Find Patient Record", Icons.Default.People, ThornburyPrimary)
                )

                shortcuts.forEach { (title, icon, color) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = ThornburySurfaceSoft,
                        border = BorderStroke(1.dp, ThornburyHairlineSoft)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = ThornburyBodyStrong
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 7. DRAFT PLANS & RESULTS TO RELEASE PANELS (from app/clinic/page.tsx)
        // =====================================================================
        if (draftPlans.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "Draft Treatment Plans (${draftPlans.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        ),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    draftPlans.forEach { draft ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { onOpenPatientChart(draft.patientId) },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = ThornburySurfaceSoft),
                            border = BorderStroke(1.dp, ThornburyHairlineSoft)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "${if (draft.phase == "pre") "Pre" else "Post"}-treatment • ${draft.patientName}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyInk
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = draft.procedure,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThornburyMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        if (heldResults.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "Results to Release (${heldResults.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        ),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    heldResults.forEach { result ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { onOpenPatientChart(result.patientId) },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = ThornburySurfaceSoft),
                            border = BorderStroke(1.dp, ThornburyHairlineSoft)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = result.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyInk
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${result.patientName} • ${result.kind}",
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
}
