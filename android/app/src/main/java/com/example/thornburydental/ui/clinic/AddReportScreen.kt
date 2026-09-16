package com.example.thornburydental.ui.clinic

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.thornburydental.data.AuthRepository
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.Patient
import com.example.thornburydental.data.ReportAttachment
import com.example.thornburydental.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Transient, in-flight upload tracked locally while [DentalRepository.uploadAttachment] runs.
 * Never inserted into the real `attachments` list — only a successful upload's returned
 * [ReportAttachment] is, so onSave callback only ever sees real files.
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
 * Responsive Material 3 Dialog Box for Creating Diagnostic Records & Attaching Media/Files.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReportDialog(
    patient: Patient,
    onDismiss: () -> Unit,
    onSave: (
        kind: String,
        title: String,
        clinician: String,
        summary: String,
        attachments: List<ReportAttachment>
    ) -> Unit
) {
    val kinds = listOf("Radiograph", "Charting", "CBCT Scan", "Chairside test", "Lab Report")
    var selectedKind by remember { mutableStateOf(kinds[0]) }

    var title by remember { mutableStateOf("") }
    val currentUser by AuthRepository.currentUser.collectAsState()
    val clinicianName = currentUser?.name ?: "Dr. Ingrid Halvorsen"
    var summary by remember { mutableStateOf("") }
    val attachments = remember { mutableStateListOf<ReportAttachment>() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pendingUploads = remember { mutableStateListOf<PendingUpload>() }

    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var cameraPhotoFile by remember { mutableStateOf<File?>(null) }

    fun startUpload(
        uri: Uri,
        customDisplayName: String? = null,
        customMimeType: String? = null,
        customSizeBytes: Long? = null
    ) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            // Document providers without persistable permission support
        }

        var displayName = customDisplayName ?: uri.lastPathSegment ?: "attachment"
        var sizeBytes = customSizeBytes ?: -1L
        if (customDisplayName == null || customSizeBytes == null) {
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
                        if (nameIndex != -1 && customDisplayName == null) {
                            cursor.getString(nameIndex)?.let { name -> displayName = name }
                        }
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1 && !cursor.isNull(sizeIndex) && customSizeBytes == null) {
                            sizeBytes = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                // Fall back to default
            }
        }

        val mimeType = customMimeType ?: context.contentResolver.getType(uri) ?: "application/octet-stream"
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

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = cameraPhotoUri
            val file = cameraPhotoFile
            if (uri != null && file != null && file.exists() && file.length() > 0) {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val displayName = "Radiograph_$timeStamp.jpg"
                startUpload(
                    uri = uri,
                    customDisplayName = displayName,
                    customMimeType = "image/jpeg",
                    customSizeBytes = file.length()
                )
            }
        }
    }

    fun launchCamera() {
        try {
            val photoDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
            val photoFile = File.createTempFile(
                "radiograph_${System.currentTimeMillis()}_",
                ".jpg",
                photoDir
            )
            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            cameraPhotoFile = photoFile
            cameraPhotoUri = photoUri
            takePictureLauncher.launch(photoUri)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Unable to launch camera: ${e.localizedMessage ?: "Unknown error"}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { startUpload(it) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "New Diagnostic Record",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = ThornburyInk
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Patient: ${patient.name} • OP: ${patient.opNo}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyMuted
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ThornburyInk
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Form Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Investigation Kind Selector
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Investigation Type",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyInk
                            )
                            Spacer(modifier = Modifier.height(8.dp))

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
                                        shape = RoundedCornerShape(14.dp),
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

                    // 2. Title & Clinical Findings Card
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Record Title *",
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
                                singleLine = true,
                                colors = thornburyTextFieldColors()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Diagnostic Summary & Clinical Findings *",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyInk
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = summary,
                                onValueChange = { summary = it },
                                placeholder = { Text("Enter bone levels, radiographic signs, or measurements...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = thornburyTextFieldColors()
                            )
                        }
                    }

                    // 3. File & Image Attachment Uploader Section
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = null,
                                    tint = ThornburyPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Attached Files & Radiographs (${attachments.size})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyInk
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Capture directly via device camera or choose local media",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyMuted
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Take Picture Primary Action
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { launchCamera() },
                                shape = RoundedCornerShape(10.dp),
                                color = ThornburyPrimary.copy(alpha = 0.06f),
                                border = BorderStroke(1.dp, ThornburyPrimary)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(ThornburyPrimary, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = "Camera",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Take Picture with Camera",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = ThornburyInk
                                        )
                                        Text(
                                            text = "Capture chairside radiograph or intraoral photo",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ThornburyMuted
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Upload from Files Secondary Action
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        pickerLauncher.launch(arrayOf("image/*", "application/pdf", "*/*"))
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = ThornburyCanvas,
                                border = BorderStroke(1.dp, ThornburyHairline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(ThornburySurfaceSoft, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudUpload,
                                            contentDescription = "Upload",
                                            tint = ThornburyPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Upload from Storage / Files",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                            color = ThornburyInk
                                        )
                                        Text(
                                            text = "Select DICOM, saved JPG/PNG, or lab PDF",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ThornburyMuted
                                        )
                                    }
                                }
                            }

                            // Pending uploads
                            if (pendingUploads.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                pendingUploads.forEach { pending ->
                                    val hasFailed = pending.error != null
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .then(
                                                if (hasFailed) {
                                                    Modifier.clickable {
                                                        pendingUploads.removeAll { it.tempId == pending.tempId }
                                                    }
                                                } else {
                                                    Modifier
                                                }
                                            ),
                                        shape = RoundedCornerShape(8.dp),
                                        color = ThornburyCanvas,
                                        border = BorderStroke(
                                            1.dp,
                                            if (hasFailed) ThornburyError.copy(alpha = 0.5f) else ThornburyHairline
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                if (hasFailed) {
                                                    Icon(
                                                        imageVector = Icons.Default.ErrorOutline,
                                                        contentDescription = null,
                                                        tint = ThornburyError,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                } else {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(18.dp),
                                                        strokeWidth = 2.dp,
                                                        color = ThornburyPrimary
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = pending.displayName,
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = ThornburyInk
                                                    )
                                                    Text(
                                                        text = if (hasFailed) "Upload failed — tap to remove" else "${pending.sizeStr} • Uploading...",
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
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Attached list
                            if (attachments.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                attachments.forEachIndexed { index, att ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = ThornburyCanvas,
                                        border = BorderStroke(1.dp, ThornburyHairline)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
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
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = att.name,
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = ThornburyInk
                                                    )
                                                    Text(
                                                        text = "${att.sizeStr} • Ready",
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
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Pinned Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
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
                        Text("Save Record", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

/**
 * Backward-compatible wrapper for any external caller referencing [AddReportScreen].
 * Directly delegates to the responsive [AddReportDialog].
 */
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
    AddReportDialog(
        patient = patient,
        onDismiss = onBack,
        onSave = onSave
    )
}
