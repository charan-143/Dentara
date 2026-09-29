package com.example.thornburydental.ui.clinic

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.Patient
import com.example.thornburydental.theme.*
import com.example.thornburydental.util.calculateAge
import com.example.thornburydental.util.formatAsDdMmYyyy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientRosterScreen(
    onSelectPatient: (String) -> Unit,
    onNavigateToRegisterPatient: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val patients by DentalRepository.patients.collectAsState()
    val appointments by DentalRepository.appointments.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("all") } // "all", "upcoming"

    val upcomingPatientIds = remember(appointments) {
        appointments.filter { it.status == "confirmed" }.map { it.patientId }.toSet()
    }
    val upcomingCount = remember(patients, upcomingPatientIds) {
        patients.count { upcomingPatientIds.contains(it.id) }
    }

    val filteredPatients = remember(patients, searchQuery, filterMode, upcomingPatientIds) {
        patients.filter { p ->
            val matchesFilter = when (filterMode) {
                "upcoming" -> upcomingPatientIds.contains(p.id)
                else -> true
            }
            if (!matchesFilter) return@filter false

            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                p.name.lowercase().contains(q) ||
                p.opNo.lowercase().contains(q) ||
                p.email.lowercase().contains(q) ||
                p.phone.lowercase().contains(q)
            } else true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ThornburyCanvas)
    ) {
        // Top Header
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
                // Title and Register Patient Action Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Patient Records",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp
                            ),
                            color = ThornburyInk
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AnimatedContent(
                                targetState = patients.size,
                                transitionSpec = {
                                    fadeIn() togetherWith fadeOut()
                                },
                                label = "TotalPatientsAnim"
                            ) { count ->
                                Text(
                                    text = "$count registered patients",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = ThornburyPrimaryText
                                )
                            }
                            Text(
                                text = " • Confidential dental records",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onNavigateToRegisterPatient,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThornburyPrimary,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Register",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search by name, OP number, or phone...",
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
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = ThornburyMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ThornburyCanvas,
                        unfocusedContainerColor = ThornburyCanvas,
                        focusedBorderColor = ThornburyPrimary,
                        unfocusedBorderColor = ThornburyHairline,
                        focusedTextColor = ThornburyInk,
                        unfocusedTextColor = ThornburyInk,
                        cursorColor = ThornburyPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Material 3 FilterChips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterChips = listOf(
                        Triple("all", "All Patients", patients.size),
                        Triple("upcoming", "Upcoming Surgery", upcomingCount)
                    )

                    filterChips.forEach { (key, label, count) ->
                        val isSelected = filterMode == key
                        val targetBg by animateColorAsState(
                            targetValue = if (isSelected) ThornburyPrimary else ThornburySurfaceSoft,
                            animationSpec = tween(220),
                            label = "PatientFilterChipColor"
                        )

                        FilterChip(
                            selected = isSelected,
                            onClick = { filterMode = key },
                            label = {
                                Text(
                                    text = "$label ($count)",
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
                                containerColor = targetBg,
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

        // Patients List / Grid
        if (filteredPatients.isEmpty()) {
            EmptyPatientsCard(
                searchQuery = searchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 340.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredPatients, key = { it.id }) { patient ->
                    Box(modifier = Modifier.animateItem()) {
                        PatientListItemCard(
                            patient = patient,
                            onClick = { onSelectPatient(patient.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientListItemCard(
    patient: Patient,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = ThornburySurfaceCard),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp, pressedElevation = 5.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Avatar, Name, Age & Last Visit Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape,
                        color = ThornburyPrimaryWash,
                        border = BorderStroke(1.dp, ThornburyPrimary.copy(alpha = 0.25f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = patient.name.take(2).uppercase(),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyPrimaryText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = patient.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${calculateAge(patient.dob)} yrs)",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "OP: ${patient.opNo} • DOB: ${formatAsDdMmYyyy(patient.dob)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = ThornburyMuted
                        )
                    }
                }

                if (patient.lastVisit.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ThornburySurfaceSoft,
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Text(
                            text = "Last: ${patient.lastVisit}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = ThornburyBody
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = ThornburyHairlineSoft)
            Spacer(modifier = Modifier.height(10.dp))

            // Contact Info
            if (patient.phone.isNotBlank() || patient.email.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (patient.phone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = ThornburyMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = patient.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyBody
                            )
                        }
                    }

                    if (patient.email.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = ThornburyMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = patient.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyBody,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Action: Open Chart Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = ThornburyPrimaryWash,
                        contentColor = ThornburyPrimaryText
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Open Patient Chart",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPatientsCard(
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "EmptyPatientFloating")
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
        modifier = modifier,
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
                        imageVector = Icons.Default.PersonSearch,
                        contentDescription = null,
                        tint = ThornburyPrimaryText,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (searchQuery.isNotBlank()) "No matching patients" else "No patients found",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = ThornburyInk
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (searchQuery.isNotBlank()) {
                    "No patient records match \"$searchQuery\". Try checking the name, OP, or phone number."
                } else {
                    "No patient records in this category."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = ThornburyMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
