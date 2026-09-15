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
import androidx.compose.ui.text.input.KeyboardType
import com.example.thornburydental.util.ValidationUtils
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.thornburydental.data.*
import com.example.thornburydental.theme.*
import com.example.thornburydental.ui.components.ThornburyDatePickerField
import java.util.Calendar

// =============================================================================
// Material 3 Patient Demographics & Clinical Profile View
// =============================================================================

@Composable
fun PatientDemographicsM3View(
    patient: Patient,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    // Dialog states
    var showEditContactDialog by remember { mutableStateOf(false) }
    var activeHistoryEditType by remember { mutableStateOf<ClinicalHistoryEditType?>(null) }

    // Pre-parsed clinical bullet points
    val medBullets = remember(patient.medicalHistory) {
        parseClinicalBullets(patient.medicalHistory).filterNot { bullet ->
            bullet.contains("Mild Asthma", ignoreCase = true) ||
            bullet.contains("Allergy", ignoreCase = true) ||
            bullet.contains("Penicillin", ignoreCase = true) ||
            bullet.contains("Latex", ignoreCase = true) ||
            bullet.contains("NSAIDs", ignoreCase = true) ||
            bullet.contains("Sulfa", ignoreCase = true)
        }
    }
    val famBullets = remember(patient.familyHistory) { parseClinicalBullets(patient.familyHistory) }
    val dentalBullets = remember(patient.pastDentalHistory) { parseClinicalBullets(patient.pastDentalHistory) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---------------------------------------------------------------------
        // 1. Hero Patient Identification Card (M3 ElevatedCard)
        // ---------------------------------------------------------------------
        HeroPatientIdCard(
            patient = patient,
            onCopyOp = {
                coroutineScope.launch {
                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("OP Number", patient.opNo)))
                }
                Toast.makeText(context, "OP No ${patient.opNo} copied", Toast.LENGTH_SHORT).show()
            },
            onCall = {
                if (patient.phone.isNotBlank()) {
                    launchIntentSafely(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${patient.phone.trim()}")))
                } else {
                    Toast.makeText(context, "No phone number on record", Toast.LENGTH_SHORT).show()
                }
            },
            onEmail = {
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
            onViewMap = {
                if (patient.address.isNotBlank()) {
                    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(patient.address)}"))
                    launchIntentSafely(context, mapIntent)
                } else {
                    Toast.makeText(context, "No address on record", Toast.LENGTH_SHORT).show()
                }
            },
            onEditProfile = { showEditContactDialog = true }
        )

        Spacer(modifier = Modifier.height(0.dp))

        // ---------------------------------------------------------------------
        // 3. Categorized Clinical History Cards
        // ---------------------------------------------------------------------

        // A. Contact & Address Card
        ContactAddressCard(
            patient = patient,
            onEditClick = { showEditContactDialog = true },
            onCopyPhone = {
                coroutineScope.launch {
                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Phone", patient.phone)))
                }
                Toast.makeText(context, "Phone copied", Toast.LENGTH_SHORT).show()
            },
            onCallPhone = {
                if (patient.phone.isNotBlank()) {
                    launchIntentSafely(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${patient.phone.trim()}")))
                }
            },
            onCopyEmail = {
                coroutineScope.launch {
                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Email", patient.email)))
                }
                Toast.makeText(context, "Email copied", Toast.LENGTH_SHORT).show()
            },
            onSendEmail = {
                if (patient.email.isNotBlank()) {
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:${patient.email.trim()}")
                    }
                    launchIntentSafely(context, emailIntent)
                }
            },
            onCopyAddress = {
                coroutineScope.launch {
                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Address", patient.address)))
                }
                Toast.makeText(context, "Address copied", Toast.LENGTH_SHORT).show()
            },
            onOpenAddressMap = {
                if (patient.address.isNotBlank()) {
                    launchIntentSafely(context, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(patient.address)}")))
                }
            }
        )

        // B. Systemic Medical History Card
        ClinicalBulletCard(
            title = "Systemic Medical History",
            icon = Icons.Default.LocalHospital,
            iconTint = ThornburyPrimary,
            bullets = medBullets,
            emptyMessage = "No systemic illnesses, conditions, or routine medications recorded.",
            onEditClick = { activeHistoryEditType = ClinicalHistoryEditType.MEDICAL }
        )

        // C. Family History Card
        ClinicalBulletCard(
            title = "Family & Hereditary History",
            icon = Icons.Default.Groups,
            iconTint = ThornburyPrimaryText,
            bullets = famBullets,
            emptyMessage = "No hereditary periodontal or systemic familial conditions noted.",
            onEditClick = { activeHistoryEditType = ClinicalHistoryEditType.FAMILY }
        )

        // D. Past Dental Records Card
        ClinicalBulletCard(
            title = "Past Dental Records & Restorations",
            icon = Icons.Default.MedicalServices,
            iconTint = ThornburyPrimary,
            bullets = dentalBullets,
            emptyMessage = "No historical restorations, endodontic work, or orthodontic therapy noted.",
            onEditClick = { activeHistoryEditType = ClinicalHistoryEditType.DENTAL }
        )
    }

    // =========================================================================
    // Material 3 Editing Dialogs
    // =========================================================================

    // 1. Edit Personal & Contact Information Dialog
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
                Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 2. Edit Clinical History Dialog (Medical / Family / Dental)
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

// =============================================================================
// 1. Hero Patient Identification Card
// =============================================================================

@Composable
private fun HeroPatientIdCard(
    patient: Patient,
    onCopyOp: () -> Unit,
    onCall: () -> Unit,
    onEmail: () -> Unit,
    onViewMap: () -> Unit,
    onEditProfile: () -> Unit
) {
    val age = remember(patient.dob) { calculatePatientAge(patient.dob) }
    val bloodGroup = remember(patient.medicalHistory, patient.medicalAlerts) { deriveBloodGroup(patient) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, ThornburyHairline), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = ThornburyCanvas
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Row: Monogram Avatar + Name + OP Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Monogram Avatar with tonal background
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getPatientMonogram(patient.name),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = patient.name,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = ThornburyInk,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        IconButton(
                            onClick = onEditProfile,
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

                    // OP Number in M3 SuggestionChip with one-tap copy
                    SuggestionChip(
                        onClick = onCopyOp,
                        label = {
                            Text(
                                text = "OP: ${patient.opNo}",
                                style = ClinicalCodeStyle.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ThornburyPrimaryText
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy OP Number",
                                modifier = Modifier.size(13.dp),
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
                }
            }

            HorizontalDivider(color = ThornburyHairlineSoft)

            // Vital Metrics Row: Age, Gender, Blood Group, DOB, and Last Visit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VitalMetricItem(
                    label = "AGE",
                    value = if (age != null) "$age yrs" else "Unknown",
                    icon = Icons.Default.Cake
                )
                VitalMetricItem(
                    label = "GENDER",
                    value = "Unknown",
                    icon = Icons.Default.Person
                )
                VitalMetricItem(
                    label = "BLOOD GROUP",
                    value = bloodGroup ?: "Unknown",
                    icon = Icons.Default.LocalHospital
                )
                VitalMetricItem(
                    label = "LAST VISIT",
                    value = patient.lastVisit,
                    icon = Icons.Default.History
                )
            }

            HorizontalDivider(color = ThornburyHairlineSoft)

            // Rapid Quick-Action Row: Call, Email, View Map
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onCall,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = ThornburySurfaceSoft,
                        contentColor = ThornburyPrimaryText
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Call",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                FilledTonalButton(
                    onClick = onEmail,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = ThornburySurfaceSoft,
                        contentColor = ThornburyPrimaryText
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Email",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                FilledTonalButton(
                    onClick = onViewMap,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = ThornburySurfaceSoft,
                        contentColor = ThornburyPrimaryText
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Map",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun VitalMetricItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
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
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = ThornburyInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}



// =============================================================================
// 3. Categorized Clinical History Cards
// =============================================================================

@Composable
private fun ContactAddressCard(
    patient: Patient,
    onEditClick: () -> Unit,
    onCopyPhone: () -> Unit,
    onCallPhone: () -> Unit,
    onCopyEmail: () -> Unit,
    onSendEmail: () -> Unit,
    onCopyAddress: () -> Unit,
    onOpenAddressMap: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = ThornburyCanvas),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Card Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = ThornburyPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Contact & Address",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                }

                TextButton(
                    onClick = onEditClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = ThornburyPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Edit",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyPrimary
                    )
                }
            }

            HorizontalDivider(color = ThornburyHairlineSoft)

            // Phone ListItem
            ListItem(
                headlineContent = {
                    Text(
                        text = if (patient.phone.isNotBlank()) patient.phone else "No telephone recorded",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (patient.phone.isNotBlank()) ThornburyInk else ThornburyMuted
                    )
                },
                supportingContent = {
                    Text("Primary Telephone", style = MaterialTheme.typography.labelSmall, color = ThornburyMuted)
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
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = ThornburyPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                trailingContent = {
                    Row {
                        IconButton(onClick = onCopyPhone, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Phone",
                                tint = ThornburyMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(onClick = onCallPhone, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Dial Phone",
                                tint = ThornburyPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = ThornburyCanvas)
            )

            HorizontalDivider(color = ThornburyHairlineSoft)

            // Email ListItem
            ListItem(
                headlineContent = {
                    Text(
                        text = if (patient.email.isNotBlank()) patient.email else "No email recorded",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (patient.email.isNotBlank()) ThornburyInk else ThornburyMuted
                    )
                },
                supportingContent = {
                    Text("Direct Patient Email", style = MaterialTheme.typography.labelSmall, color = ThornburyMuted)
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
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = ThornburyPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                trailingContent = {
                    Row {
                        IconButton(onClick = onCopyEmail, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Email",
                                tint = ThornburyMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(onClick = onSendEmail, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Send Email",
                                tint = ThornburyPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = ThornburyCanvas)
            )

            HorizontalDivider(color = ThornburyHairlineSoft)

            // Postal Address ListItem
            ListItem(
                headlineContent = {
                    Text(
                        text = if (patient.address.isNotBlank()) patient.address else "No address recorded on file",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (patient.address.isNotBlank()) ThornburyInk else ThornburyMuted
                    )
                },
                supportingContent = {
                    Text("Residential & Billing Address", style = MaterialTheme.typography.labelSmall, color = ThornburyMuted)
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
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = ThornburyPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                trailingContent = {
                    Row {
                        IconButton(onClick = onCopyAddress, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Address",
                                tint = ThornburyMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(onClick = onOpenAddressMap, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = "View Map",
                                tint = ThornburyPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = ThornburyCanvas)
            )
        }
    }
}

@Composable
private fun ClinicalBulletCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                }

                TextButton(
                    onClick = onEditClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = ThornburyPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Edit",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (bullets.isEmpty()) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontStyle = FontStyle.Italic
                    ),
                    color = ThornburyMuted
                )
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

// =============================================================================
// 4. Material 3 Dialog Implementations
// =============================================================================

enum class ClinicalHistoryEditType(val title: String, val subtitle: String) {
    MEDICAL("Systemic Medical History", "Record systemic conditions, chronic illnesses, and medications"),
    FAMILY("Family & Hereditary History", "Record familial traits, early tooth loss, or hereditary disorders"),
    DENTAL("Past Dental Records", "Record prior restorations, endodontic treatments, implants, or trauma")
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = ThornburyCanvas,
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
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
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
                                text = "Invalid phone number (must be 7-15 digits, no letters)",
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
                                text = "Must be a valid email (e.g. name@example.com with '@' and domain)",
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
                    label = { Text("Postal / Residential Address") },
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThornburyPrimary,
                            contentColor = Color.White
                        ),
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = ThornburyCanvas,
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
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
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
                    text = "Tip: Items separated by line breaks will render as individual bullet points on the clinical chart.",
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThornburyPrimary,
                            contentColor = Color.White
                        ),
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

// NOTE: `Patient` has no `gender` field today, so there is no real value to
// show here — a prior version of this function *guessed* gender from a
// five-name hardcoded list (defaulting to "Adult" otherwise), which is both
// usually wrong and inappropriate for a medical record. Until a real
// `gender` field is added to the `Patient` model (a deferred schema change),
// this vital always reads "Unknown" rather than fabricating a value.

/**
 * Looks for an explicit blood type (e.g. "O+", "AB-") mentioned in the
 * patient's free-text medical history/alerts. Returns null — never a
 * fabricated default — when none is on record; blood type is safety-critical
 * data and must never be guessed.
 */
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
