package com.example.thornburydental.ui.clinic

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.Patient
import com.example.thornburydental.data.ReportAttachment
import com.example.thornburydental.theme.*
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

/**
 * Transient, in-flight upload tracked locally while [DentalRepository.uploadAttachment] runs.
 * Never inserted into the real `attachments` list — only a successful upload's returned
 * [ReportAttachment] is, so [AddReportScreen]'s onSave callback only ever sees real files.
 */
private data class PendingUpload(
    val tempId: String,
    val displayName: String,
    val sizeStr: String,
    val mimeType: String,
    val isUploading: Boolean,
    val error: String? = null
)

/**
 * Dedicated Full-Page View for Creating Diagnostic Records & Uploading Report Files/Images.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReportScreen(
    patient: Patient,
    onBack: () -> Unit,
    onSave: (
        kind: String,
        title: String,
        clinician: String,
        summary: String,
        attachments: List<ReportAttachment>
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val kinds = listOf("Radiograph", "Charting", "CBCT Scan", "Chairside test", "Lab Report")
    var selectedKind by remember { mutableStateOf(kinds[0]) }

    var title by remember { mutableStateOf("") }
    val clinicianName = "Dr. Ingrid Halvorsen"
    var summary by remember { mutableStateOf("") }
    val attachments = remember { mutableStateListOf<ReportAttachment>() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pendingUploads = remember { mutableStateListOf<PendingUpload>() }

    // Kicks off a real upload for a document/image the user picked via SAF. Tracks progress in
    // `pendingUploads` (never in the real `attachments` list) and only promotes it to a real
    // ReportAttachment once DentalRepository.uploadAttachment actually succeeds.
    fun startUpload(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            // Some document providers (e.g. camera/scanner apps) don't support persistable
            // grants — the Uri is still readable for this session, so just continue.
        }

        var displayName = uri.lastPathSegment ?: "attachment"
        var sizeBytes = -1L
        try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        cursor.getString(nameIndex)?.let { name -> displayName = name }
                    }
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            // Fall back to the Uri-derived display name and "Unknown size" below.
        }

        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val sizeStr = if (sizeBytes > 0) {
            String.format(Locale.US, "%.1f MB", sizeBytes / (1024.0 * 1024.0))
        } else {
            "Unknown size"
        }

        val tempId = UUID.randomUUID().toString()
        pendingUploads.add(
            PendingUpload(
                tempId = tempId,
                displayName = displayName,
                sizeStr = sizeStr,
                mimeType = mimeType,
                isUploading = true
            )
        )

        scope.launch {
            val result = DentalRepository.uploadAttachment(
                context = context,
                uri = uri,
                displayName = displayName,
                mimeType = mimeType,
                sizeStr = sizeStr
            )
            result.onSuccess { attachment ->
                pendingUploads.removeAll { it.tempId == tempId }
                attachments.add(attachment)
            }.onFailure { error ->
                val idx = pendingUploads.indexOfFirst { it.tempId == tempId }
                if (idx != -1) {
                    pendingUploads[idx] = pendingUploads[idx].copy(
                        isUploading = false,
                        error = error.message ?: "Upload failed"
                    )
                }
            }
        }
    }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { startUpload(it) }
    }

    val quickTitles = listOf(
        "Periapical Radiograph Tooth #19",
        "Bite-wing Radiographs (Right & Left)",
        "OPG Panoramic Radiograph",
        "CBCT 3D Scan #19 Apical Region",
        "Full mouth 6-point periodontal chart",
        "Cold Pulp Vitality Test #18-#20"
    )

    val quickTemplates = listOf(
        "No interproximal caries; crestal bone height stable.",
        "Persistent radiolucency at root apex; non-vital pulp response.",
        "Generalised 2-3mm probing depths with isolated 5mm bleeding pocket."
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "New Diagnostic Record",
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
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ThornburyInk
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThornburyCanvas)
            )
        },
        containerColor = ThornburyCanvas,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. Investigation Kind Selector
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft),
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Investigation Type",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        kinds.forEach { kind ->
                            val isSelected = kind == selectedKind
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedKind = kind },
                                label = {
                                    Text(
                                        text = kind,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (kind) {
                                            "Radiograph" -> Icons.Default.Image
                                            "CBCT Scan" -> Icons.Default.ViewInAr
                                            "Charting" -> Icons.Default.FormatListNumbered
                                            "Chairside test" -> Icons.Default.Science
                                            else -> Icons.Default.Biotech
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ThornburyPrimary,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White,
                                    containerColor = ThornburyCanvas,
                                    labelColor = ThornburyInk
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Title & Clinical Findings Card
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft),
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Report & Investigation Title *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("e.g. Periapical Radiograph Tooth #19") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickTitles.forEach { quickTitle ->
                            Surface(
                                modifier = Modifier.clickable { title = quickTitle },
                                shape = RoundedCornerShape(12.dp),
                                color = ThornburyCanvas,
                                border = BorderStroke(1.dp, ThornburyHairlineSoft)
                            ) {
                                Text(
                                    text = quickTitle,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = ThornburyPrimaryText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Clinical Findings & Diagnostic Summary *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = summary,
                        onValueChange = { summary = it },
                        placeholder = { Text("Enter findings, bone levels, radiographic signs, or measurements...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickTemplates.forEach { template ->
                            Surface(
                                modifier = Modifier.clickable {
                                    summary = if (summary.isBlank()) template else "$summary $template"
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = ThornburyCanvas,
                                border = BorderStroke(1.dp, ThornburyHairlineSoft)
                            ) {
                                Text(
                                    text = "+ \"${template.take(34)}...\"",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                    color = ThornburyMuted
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. File & Image Attachment Uploader Section
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft),
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = ThornburyPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Report Files & Radiographs (${attachments.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyInk
                            )
                        }

                        Text(
                            text = "Supports DICOM, PNG, PDF",
                            style = MaterialTheme.typography.labelSmall,
                            color = ThornburyMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Upload Zone Box
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                pickerLauncher.launch(arrayOf("image/*", "application/pdf", "*/*"))
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = ThornburyCanvas,
                        border = BorderStroke(1.dp, ThornburyPrimary.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = ThornburyPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap to Upload Radiograph / Document File",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyPrimaryText
                            )
                            Text(
                                text = "Attach DICOM 3D scans, X-Rays, periodontal charts, or lab PDFs",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick attachment shortcuts — each launches the SAME real system picker
                    // with a narrower mime-type filter. None of these ever fabricate an
                    // attachment; they only ever start a real pick + upload flow.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { pickerLauncher.launch(arrayOf("image/*")) },
                            label = { Text("Choose Radiograph (Image)") },
                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )

                        AssistChip(
                            onClick = { pickerLauncher.launch(arrayOf("*/*")) },
                            label = { Text("Choose CBCT / DICOM") },
                            leadingIcon = { Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )

                        AssistChip(
                            onClick = { pickerLauncher.launch(arrayOf("application/pdf", "*/*")) },
                            label = { Text("Choose Lab Report (PDF)") },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }

                    // Pending uploads — real files currently uploading, or ones whose upload
                    // failed. These are never part of `attachments` / the onSave payload.
                    if (pendingUploads.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        pendingUploads.forEach { pending ->
                            val hasFailed = pending.error != null
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .then(
                                        if (hasFailed) {
                                            Modifier.clickable {
                                                pendingUploads.removeAll { it.tempId == pending.tempId }
                                            }
                                        } else {
                                            Modifier
                                        }
                                    ),
                                shape = RoundedCornerShape(10.dp),
                                color = ThornburyCanvas,
                                border = BorderStroke(
                                    1.dp,
                                    if (hasFailed) ThornburyError.copy(alpha = 0.5f) else ThornburyHairline
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        if (hasFailed) {
                                            Icon(
                                                imageVector = Icons.Default.ErrorOutline,
                                                contentDescription = null,
                                                tint = ThornburyError,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = ThornburyPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = pending.displayName,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = ThornburyInk
                                            )
                                            Text(
                                                text = if (hasFailed) {
                                                    "Upload failed — tap to remove"
                                                } else {
                                                    "${pending.sizeStr} • Uploading..."
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (hasFailed) ThornburyError else ThornburyMuted
                                            )
                                        }
                                    }

                                    IconButton(onClick = { pendingUploads.removeAll { it.tempId == pending.tempId } }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = ThornburyError,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Attached items list
                    if (attachments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        attachments.forEachIndexed { index, att ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = ThornburyCanvas,
                                border = BorderStroke(1.dp, ThornburyHairline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = when {
                                                att.name.endsWith(".pdf") -> Icons.Default.PictureAsPdf
                                                att.name.endsWith(".dicom") -> Icons.Default.ViewInAr
                                                else -> Icons.Default.Image
                                            },
                                            contentDescription = null,
                                            tint = ThornburyPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = att.name,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = ThornburyInk
                                            )
                                            Text(
                                                text = "${att.sizeStr} • Attached",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ThornburyMuted
                                            )
                                        }
                                    }

                                    IconButton(onClick = { attachments.removeAt(index) }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = ThornburyError,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = {
                        if (title.isNotBlank() && summary.isNotBlank()) {
                            onSave(selectedKind, title.trim(), clinicianName, summary.trim(), attachments.toList())
                        }
                    },
                    enabled = title.isNotBlank() && summary.isNotBlank(),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Diagnostic Record", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
