package com.example.thornburydental.ui.clinic

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.thornburydental.data.*
import com.example.thornburydental.theme.*
import com.example.thornburydental.ui.components.ThornburyDatePickerField
import com.example.thornburydental.util.ValidationUtils
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Modern Material 3 Patient Demographics & Comprehensive Clinical Profile View.
 * Provides high clinical information density, executive patient identification,
 * prominent allergy & medical alert warnings, categorized medical/dental histories,
 * verified contact details, and robust in-place editing.
 */
@Composable
fun PatientDemographicsM3View(
    patient: Patient,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    var showEditContactDialog by remember { mutableStateOf(false) }
    var activeHistoryEditType by remember { mutableStateOf<ClinicalHistoryEditType?>(null) }

    // Pre-parsed clinical bullets
    val medBullets = remember(patient.medicalHistory) { parseClinicalBullets(patient.medicalHistory) }
    val famBullets = remember(patient.familyHistory) { parseClinicalBullets(patient.familyHistory) }
    val dentalBullets = remember(patient.pastDentalHistory) { parseClinicalBullets(patient.pastDentalHistory) }

    val age = remember(patient.dob) { calculatePatientAge(patient.dob) }
    val bloodGroup = remember(patient.medicalHistory, patient.medicalAlerts) { deriveBloodGroup(patient) }

    // Combined critical alerts & allergies
    val combinedAlerts = remember(patient.allergies, patient.medicalAlerts) {
        val list = mutableListOf<String>()
        patient.allergies.forEach { allergy ->
            val desc = if (allergy.severity.isNotBlank()) "${allergy.allergen} (${allergy.severity})" else allergy.allergen
            list.add(desc)
        }
        patient.medicalAlerts.forEach { alert ->
            if (!list.any { it.contains(alert, ignoreCase = true) }) {
                list.add(alert)
            }
        }
        list
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ---------------------------------------------------------------------
        // 1. Executive Patient Identification Banner
        // ---------------------------------------------------------------------
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, ThornburyHairline), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = ThornburyCanvas),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Row: Avatar + Name + OP Badge + Edit Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(ThornburyPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getPatientMonogram(patient.name),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = patient.name,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ThornburyInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = { showEditContactDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = ThornburyPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // OP Number Chip with 1-tap copy
                            SuggestionChip(
                                onClick = {
                                    coroutineScope.launch {
                                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("OP Number", patient.opNo)))
                                    }
                                    Toast.makeText(context, "OP No ${patient.opNo} copied", Toast.LENGTH_SHORT).show()
                                },
                                label = {
                                    Text(
                                        text = "OP: ${patient.opNo}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ThornburyPrimaryText
                                    )
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy OP",
                                        modifier = Modifier.size(12.dp),
                                        tint = ThornburyPrimary
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = ThornburyPrimaryWash,
                                    labelColor = ThornburyPrimaryText
                                ),
                                border = BorderStroke(1.dp, ThornburyPrimary.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(8.dp)
                            )

                            // Status Chip
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ThornburySuccessWash,
                                border = BorderStroke(1.dp, ThornburySuccess.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "Active Patient",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburySuccess,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = ThornburyHairlineSoft)

                // Key Clinical Vitals Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DemographicMetricPill(
                        label = "AGE / DOB",
                        value = if (age != null) "$age yrs (${patient.dob})" else if (patient.dob.isNotBlank()) patient.dob else "Not recorded",
                        icon = Icons.Default.Cake
                    )
                    DemographicMetricPill(
                        label = "BLOOD GROUP",
                        value = bloodGroup ?: "Not on file",
                        icon = Icons.Default.LocalHospital
                    )
                    DemographicMetricPill(
                        label = "LAST VISIT",
                        value = patient.lastVisit,
                        icon = Icons.Default.History
                    )
                }

                HorizontalDivider(color = ThornburyHairlineSoft)

                // Quick Clinical Actions Row (Call, Email, Map, Edit)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            if (patient.phone.isNotBlank()) {
                                launchIntentSafely(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${patient.phone.trim()}")))
                            } else {
                                Toast.makeText(context, "No phone number on record", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ThornburySurfaceSoft,
                            contentColor = ThornburyPrimaryText
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Call", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    FilledTonalButton(
                        onClick = {
                            if (patient.email.isNotBlank()) {
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:${patient.email.trim()}")
                                    putExtra(Intent.EXTRA_SUBJECT, "Dentara - Patient Care: ${patient.name}")
                                }
                                launchIntentSafely(context, emailIntent)
                            } else {
                                Toast.makeText(context, "No email address on record", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ThornburySurfaceSoft,
                            contentColor = ThornburyPrimaryText
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Email", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    FilledTonalButton(
                        onClick = {
                            if (patient.address.isNotBlank()) {
                                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(patient.address)}"))
                                launchIntentSafely(context, mapIntent)
                            } else {
                                Toast.makeText(context, "No address on record", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ThornburySurfaceSoft,
                            contentColor = ThornburyPrimaryText
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Place, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Map", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // ---------------------------------------------------------------------
        // 2. Safety-Critical Clinical Alerts & Allergies Banner
        // ---------------------------------------------------------------------
        if (combinedAlerts.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = ThornburyErrorWash,
                border = BorderStroke(1.dp, ThornburyError.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = ThornburyError,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Safety Alerts & Known Allergies",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyError
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        combinedAlerts.forEach { alert ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, ThornburyError.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(ThornburyError)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = alert,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ThornburyError
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = ThornburySuccessWash,
                border = BorderStroke(1.dp, ThornburySuccess.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ThornburySuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "No Known Medical Alerts or Drug Allergies Recorded",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = ThornburySuccess
                    )
                }
            }
        }

        // ---------------------------------------------------------------------
        // 3. Contact & Residency Details Card
        // ---------------------------------------------------------------------
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = ThornburyCanvas),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = null,
                            tint = ThornburyPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Contact & Residential Information",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                    }

                    TextButton(
                        onClick = { showEditContactDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = ThornburyPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ThornburyPrimary)
                    }
                }

                HorizontalDivider(color = ThornburyHairlineSoft)

                // Phone Row
                ContactDetailItem(
                    title = if (patient.phone.isNotBlank()) patient.phone else "No telephone recorded",
                    subtitle = "Primary Telephone",
                    icon = Icons.Default.Call,
                    onCopy = {
                        coroutineScope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Phone", patient.phone))) }
                        Toast.makeText(context, "Phone copied", Toast.LENGTH_SHORT).show()
                    },
                    onAction = {
                        if (patient.phone.isNotBlank()) {
                            launchIntentSafely(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${patient.phone.trim()}")))
                        }
                    },
                    actionIcon = Icons.Default.Phone
                )

                HorizontalDivider(color = ThornburyHairlineSoft)

                // Email Row
                ContactDetailItem(
                    title = if (patient.email.isNotBlank()) patient.email else "No email address recorded",
                    subtitle = "Direct Patient Email",
                    icon = Icons.Default.Email,
                    onCopy = {
                        coroutineScope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Email", patient.email))) }
                        Toast.makeText(context, "Email copied", Toast.LENGTH_SHORT).show()
                    },
                    onAction = {
                        if (patient.email.isNotBlank()) {
                            launchIntentSafely(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${patient.email.trim()}")))
                        }
                    },
                    actionIcon = Icons.Default.Send
                )

                HorizontalDivider(color = ThornburyHairlineSoft)

                // Address Row
                ContactDetailItem(
                    title = if (patient.address.isNotBlank()) patient.address else "No postal address recorded",
                    subtitle = "Residential & Billing Address",
                    icon = Icons.Default.Place,
                    onCopy = {
                        coroutineScope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Address", patient.address))) }
                        Toast.makeText(context, "Address copied", Toast.LENGTH_SHORT).show()
                    },
                    onAction = {
                        if (patient.address.isNotBlank()) {
                            launchIntentSafely(context, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(patient.address)}")))
                        }
                    },
                    actionIcon = Icons.Default.Place
                )
            }
        }

        // ---------------------------------------------------------------------
        // 4. Categorized Clinical History Cards
        // ---------------------------------------------------------------------

        // A. Systemic Medical History Card
        ClinicalHistorySectionCard(
            title = "Systemic Medical History",
            subtitle = "Recorded medical conditions, chronic illnesses, and medications",
            icon = Icons.Default.LocalHospital,
            bullets = medBullets,
            emptyMessage = "No systemic conditions, chronic illnesses, or routine medications recorded.",
            onEditClick = { activeHistoryEditType = ClinicalHistoryEditType.MEDICAL }
        )

        // B. Past Dental Records & Restorations Card
        ClinicalHistorySectionCard(
            title = "Past Dental & Restorative History",
            subtitle = "Historical restorations, endodontic therapy, prosthetics, and implants",
            icon = Icons.Default.MedicalServices,
            bullets = dentalBullets,
            emptyMessage = "No prior restorative work, endodontic procedures, or trauma noted on file.",
            onEditClick = { activeHistoryEditType = ClinicalHistoryEditType.DENTAL }
        )

        // C. Family & Hereditary Risk Card
        ClinicalHistorySectionCard(
            title = "Family & Hereditary History",
            subtitle = "Familial periodontal tendencies, tooth loss patterns, or genetic conditions",
            icon = Icons.Default.Groups,
            bullets = famBullets,
            emptyMessage = "No familial oral health or genetic systemic conditions recorded.",
            onEditClick = { activeHistoryEditType = ClinicalHistoryEditType.FAMILY }
        )

        Spacer(modifier = Modifier.height(20.dp))
    }

    // =========================================================================
    // Material 3 Edit Dialogs
    // =========================================================================

    if (showEditContactDialog) {
        EditContactInfoM3Dialog(
            patient = patient,
            onDismiss = { showEditContactDialog = false },
            onSave = { updatedName, updatedOpNo, updatedDob, updatedPhone, updatedEmail, updatedAddress ->
                DentalRepository.updatePatientDemographics(
                    patientId = patient.id,
                    opNo = updatedOpNo,
                    name = updatedName,
                    phone = updatedPhone,
                    email = updatedEmail,
                    address = updatedAddress,
                    medicalHistory = patient.medicalHistory,
                    familyHistory = patient.familyHistory,
                    pastDentalHistory = patient.pastDentalHistory,
                    dob = updatedDob
                )
                showEditContactDialog = false
                Toast.makeText(context, "Patient profile updated", Toast.LENGTH_SHORT).show()
            }
        )
    }

    activeHistoryEditType?.let { historyType ->
        val initialText = when (historyType) {
            ClinicalHistoryEditType.MEDICAL -> patient.medicalHistory
            ClinicalHistoryEditType.FAMILY -> patient.familyHistory
            ClinicalHistoryEditType.DENTAL -> patient.pastDentalHistory
        }
        EditClinicalHistoryM3Dialog(
            type = historyType,
            initialText = initialText,
            onDismiss = { activeHistoryEditType = null },
            onSave = { updatedText ->
                when (historyType) {
                    ClinicalHistoryEditType.MEDICAL -> {
                        DentalRepository.updatePatientDemographics(
                            patientId = patient.id,
                            opNo = patient.opNo,
                            name = patient.name,
                            phone = patient.phone,
                            email = patient.email,
                            address = patient.address,
                            medicalHistory = updatedText,
                            familyHistory = patient.familyHistory,
                            pastDentalHistory = patient.pastDentalHistory,
                            dob = patient.dob
                        )
                    }
                    ClinicalHistoryEditType.FAMILY -> {
                        DentalRepository.updatePatientDemographics(
                            patientId = patient.id,
                            opNo = patient.opNo,
                            name = patient.name,
                            phone = patient.phone,
                            email = patient.email,
                            address = patient.address,
                            medicalHistory = patient.medicalHistory,
                            familyHistory = updatedText,
                            pastDentalHistory = patient.pastDentalHistory,
                            dob = patient.dob
                        )
                    }
                    ClinicalHistoryEditType.DENTAL -> {
                        DentalRepository.updatePatientDemographics(
                            patientId = patient.id,
                            opNo = patient.opNo,
                            name = patient.name,
                            phone = patient.phone,
                            email = patient.email,
                            address = patient.address,
                            medicalHistory = patient.medicalHistory,
                            familyHistory = patient.familyHistory,
                            pastDentalHistory = updatedText,
                            dob = patient.dob
                        )
                    }
                }
                activeHistoryEditType = null
                Toast.makeText(context, "${historyType.title} saved", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun DemographicMetricPill(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ThornburyMuted,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = ThornburyMuted
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = ThornburyInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ContactDetailItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onCopy: () -> Unit,
    onAction: () -> Unit,
    actionIcon: ImageVector
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = ThornburyInk
            )
        },
        supportingContent = {
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = ThornburyMuted)
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ThornburyPrimaryWash),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ThornburyPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = ThornburyMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onAction, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = "Action",
                        tint = ThornburyPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = ThornburyCanvas)
    )
}

@Composable
private fun ClinicalHistorySectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    bullets: List<String>,
    emptyMessage: String,
    onEditClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = ThornburyCanvas),
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = ThornburyPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyMuted
                        )
                    }
                }

                TextButton(
                    onClick = onEditClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = ThornburyPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ThornburyPrimary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (bullets.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ThornburySurfaceSoft,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = ThornburyMuted,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bullets.forEach { bullet ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(ThornburyPrimary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = bullet,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ThornburyInk,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class ClinicalHistoryEditType(val title: String, val subtitle: String) {
    MEDICAL("Systemic Medical History", "Record systemic illnesses, chronic conditions, and medications"),
    FAMILY("Family & Hereditary History", "Record familial periodontal traits, early tooth loss, or genetic disorders"),
    DENTAL("Past Dental & Restorative Records", "Record prior restorations, endodontic treatments, implants, or trauma")
}

@Composable
private fun EditContactInfoM3Dialog(
    patient: Patient,
    onDismiss: () -> Unit,
    onSave: (name: String, opNo: String, dob: String, phone: String, email: String, address: String) -> Unit
) {
    var name by remember { mutableStateOf(patient.name) }
    var opNo by remember { mutableStateOf(patient.opNo) }
    var dob by remember { mutableStateOf(patient.dob) }
    var phone by remember { mutableStateOf(patient.phone) }
    var email by remember { mutableStateOf(patient.email) }
    var address by remember { mutableStateOf(patient.address) }

    val isPhoneValid = phone.isBlank() || ValidationUtils.isValidPhone(phone)
    val isEmailValid = email.isBlank() || ValidationUtils.isValidEmail(email)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Personal & Contact Info",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = ThornburyMuted)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Patient Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = thornburyTextFieldColors()
                )

                OutlinedTextField(
                    value = opNo,
                    onValueChange = { opNo = it },
                    label = { Text("OP Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = thornburyTextFieldColors()
                )

                ThornburyDatePickerField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = "Date of Birth",
                    placeholder = "YYYY-MM-DD",
                    modifier = Modifier.fillMaxWidth(),
                    isOptional = true,
                    helperText = null
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = ValidationUtils.filterPhoneInput(it) },
                    label = { Text("Telephone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = phone.isNotBlank() && !ValidationUtils.isValidPhone(phone),
                    supportingText = {
                        if (phone.isNotBlank() && !ValidationUtils.isValidPhone(phone)) {
                            Text(
                                text = "Invalid phone number (must be 7-15 digits)",
                                style = MaterialTheme.typography.labelSmall,
                                color = ThornburyError
                            )
                        }
                    },
                    colors = thornburyTextFieldColors()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = email.isNotBlank() && !ValidationUtils.isValidEmail(email),
                    supportingText = {
                        if (email.isNotBlank() && !ValidationUtils.isValidEmail(email)) {
                            Text(
                                text = "Must be a valid email (e.g. name@example.com)",
                                style = MaterialTheme.typography.labelSmall,
                                color = ThornburyError
                            )
                        }
                    },
                    colors = thornburyTextFieldColors()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Residential & Billing Address") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = thornburyTextFieldColors()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = ThornburyMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && isPhoneValid && isEmailValid) {
                                onSave(name.trim(), opNo.trim(), dob.trim(), phone.trim(), email.trim(), address.trim())
                            }
                        },
                        enabled = name.isNotBlank() && isPhoneValid && isEmailValid,
                        colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditClinicalHistoryM3Dialog(
    type: ClinicalHistoryEditType,
    initialText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var content by remember { mutableStateOf(initialText) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = type.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = ThornburyMuted)
                    }
                }

                Text(
                    text = type.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ThornburyMuted
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Clinical observations (one per line)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    colors = thornburyTextFieldColors()
                )

                Text(
                    text = "Tip: Line breaks will render as individual bullet points on the clinical chart.",
                    style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                    color = ThornburyMuted
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = ThornburyMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(content.trim()) },
                        colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save History")
                    }
                }
            }
        }
    }
}

// =============================================================================
// Helper Functions: Parsing & Clinical Logic
// =============================================================================

private fun launchIntentSafely(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open external app", Toast.LENGTH_SHORT).show()
    }
}

private fun getPatientMonogram(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "?"
    val words = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        words.size >= 2 -> "${words.first().first().uppercaseChar()}${words.last().first().uppercaseChar()}"
        words.isNotEmpty() -> words.first().take(2).uppercase()
        else -> "?"
    }
}

private fun calculatePatientAge(dob: String): Int? {
    if (dob.isBlank()) return null
    return try {
        val parts = dob.trim().split("-")
        if (parts.isNotEmpty()) {
            val year = parts[0].toIntOrNull() ?: return null
            val month = if (parts.size > 1) parts[1].toIntOrNull() ?: 1 else 1
            val day = if (parts.size > 2) parts[2].toIntOrNull() ?: 1 else 1

            val cal = Calendar.getInstance()
            val currYear = cal.get(Calendar.YEAR)
            val currMonth = cal.get(Calendar.MONTH) + 1
            val currDay = cal.get(Calendar.DAY_OF_MONTH)

            var age = currYear - year
            if (currMonth < month || (currMonth == month && currDay < day)) {
                age--
            }
            if (age >= 0) age else null
        } else null
    } catch (e: Exception) {
        null
    }
}

private fun deriveBloodGroup(patient: Patient): String? {
    val regex = Regex("\\b(A|B|AB|O)[+-]\\b", RegexOption.IGNORE_CASE)
    val match = regex.find(patient.medicalHistory) ?: regex.find(patient.medicalAlerts.joinToString(" "))
    return match?.value?.uppercase()
}

private fun parseClinicalBullets(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    val lines = raw.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    val result = mutableListOf<String>()
    for (line in lines) {
        val cleaned = line.replace(Regex("^(\\s*[-*•]\\s*|\\s*\\d+[.)]\\s*)"), "").trim()
        if (cleaned.isNotEmpty()) result.add(cleaned)
    }
    if (result.size <= 1 && raw.contains(".")) {
        val sentences = raw.split(Regex("\\.\\s+")).map { it.trim().removeSuffix(".") }.filter { it.isNotEmpty() }
        if (sentences.size > 1) return sentences
    }
    return if (result.isNotEmpty()) result else listOf(raw.trim())
}
