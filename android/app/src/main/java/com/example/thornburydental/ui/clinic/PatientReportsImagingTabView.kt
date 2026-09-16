package com.example.thornburydental.ui.clinic

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.thornburydental.data.*
import com.example.thornburydental.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private enum class ReportsViewMode {
    RECORDS_AND_FILES,
    IMAGING_GALLERY
}

/**
 * Modern Redesigned Reports and Imaging Tab.
 * Displays real uploaded radiographs, dental photography, and laboratory/clinical documents.
 * Offers dual view modes (Record Cards with inline attachment previews vs Visual 2-column Imaging Gallery),
 * direct 1-tap FileProvider document opening/sharing, and direct quick upload actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientReportsImagingTabView(
    patient: Patient,
    reports: List<DiagnosticReport>,
    onAddReportClick: () -> Unit,
    onReportClick: (DiagnosticReport) -> Unit,
    onReportAttachmentClick: ((DiagnosticReport, Int) -> Unit)? = null,
    onToggleRelease: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var viewMode by remember { mutableStateOf(ReportsViewMode.RECORDS_AND_FILES) }
    var showQuickUploadMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }

    val currentUser by AuthRepository.currentUser.collectAsState()
    val clinicianName = currentUser?.name ?: (DentalRepository.clinicians.firstOrNull()?.name ?: "Dr. Ingrid Halvorsen")

    // Temporary camera capture state
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var cameraPhotoFile by remember { mutableStateOf<File?>(null) }

    // Helper: start upload and attach to a new DiagnosticReport
    fun processAndAttachFile(
        uri: Uri,
        defaultTitle: String,
        kind: String,
        customDisplayName: String? = null,
        customMimeType: String? = null,
        customSizeBytes: Long? = null
    ) {
        var displayName = customDisplayName ?: uri.lastPathSegment ?: "document"
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
            } catch (_: Exception) {}
        }

        val mimeType = customMimeType ?: context.contentResolver.getType(uri) ?: when {
            displayName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            displayName.endsWith(".jpg", ignoreCase = true) || displayName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            displayName.endsWith(".png", ignoreCase = true) -> "image/png"
            else -> "application/octet-stream"
        }

        val sizeStr = if (sizeBytes > 0) {
            String.format(Locale.US, "%.1f MB", sizeBytes / (1024.0 * 1024.0))
        } else {
            "Attached"
        }

        isUploading = true
        scope.launch {
            val result = DentalRepository.uploadAttachment(
                context = context,
                uri = uri,
                displayName = displayName,
                mimeType = mimeType,
                sizeStr = sizeStr
            )

            isUploading = false
            result.onSuccess { attachment ->
                DentalRepository.addReport(
                    patientId = patient.id,
                    kind = kind,
                    title = defaultTitle,
                    summary = "Attached $displayName ($sizeStr) uploaded directly to patient chart.",
                    clinicianName = clinicianName,
                    attachments = listOf(attachment)
                )
                Toast.makeText(context, "Uploaded: $displayName", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                Toast.makeText(context, "Upload failed: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Camera Capture Launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = cameraPhotoUri
            val file = cameraPhotoFile
            if (uri != null && file != null && file.exists() && file.length() > 0) {
                val timeStamp = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date())
                val fileName = "Radiograph_${System.currentTimeMillis()}.jpg"
                processAndAttachFile(
                    uri = uri,
                    defaultTitle = "Clinical Photo ($timeStamp)",
                    kind = "Radiograph",
                    customDisplayName = fileName,
                    customMimeType = "image/jpeg",
                    customSizeBytes = file.length()
                )
            }
        }
    }

    fun launchCamera() {
        try {
            val photoDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
            val photoFile = File.createTempFile("photo_${System.currentTimeMillis()}_", ".jpg", photoDir)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            cameraPhotoUri = uri
            cameraPhotoFile = photoFile
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery Image Picker Launcher
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val timeStamp = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date())
            processAndAttachFile(
                uri = it,
                defaultTitle = "Dental Radiograph ($timeStamp)",
                kind = "Radiograph"
            )
        }
    }

    // Document / PDF Picker Launcher
    val pickDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val timeStamp = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date())
            processAndAttachFile(
                uri = it,
                defaultTitle = "Diagnostic Lab Report ($timeStamp)",
                kind = "Lab Report"
            )
        }
    }

    // Filter logic
    val kinds = listOf("All", "Radiographs & X-Rays", "Photos & Scans", "Lab Reports & Docs")
    val filteredReports = remember(reports, selectedFilter) {
        when (selectedFilter) {
            "All" -> reports
            "Radiographs & X-Rays" -> reports.filter {
                it.kind.equals("Radiograph", ignoreCase = true) ||
                        it.kind.equals("CBCT Scan", ignoreCase = true) ||
                        it.title.contains("X-Ray", ignoreCase = true) ||
                        it.title.contains("OPG", ignoreCase = true)
            }
            "Photos & Scans" -> reports.filter {
                it.kind.equals("Charting", ignoreCase = true) ||
                        it.title.contains("Photo", ignoreCase = true) ||
                        it.attachments.any { att -> att.mimeType.startsWith("image/") }
            }
            "Lab Reports & Docs" -> reports.filter {
                it.kind.equals("Lab Report", ignoreCase = true) ||
                        it.kind.equals("Chairside test", ignoreCase = true) ||
                        it.attachments.any { att -> att.mimeType == "application/pdf" || att.name.endsWith(".pdf", ignoreCase = true) }
            }
            else -> reports
        }
    }

    val totalAttachments = remember(reports) {
        reports.sumOf { it.attachments.size }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ThornburyCanvas)
    ) {
        // =====================================================================
        // Header & Quick Upload Actions Bar
        // =====================================================================
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ThornburyCanvas,
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reports & Imaging",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${reports.size} records • $totalAttachments attached files",
                            style = MaterialTheme.typography.labelSmall,
                            color = ThornburyMuted
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Quick Upload Button (Opens sheet / actions)
                        FilledTonalButton(
                            onClick = { showQuickUploadMenu = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = ThornburySurfaceSoft,
                                contentColor = ThornburyPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }

                        // Add Full Diagnostic Report
                        Button(
                            onClick = onAddReportClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThornburyPrimary,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Report", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                if (isUploading) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = ThornburyPrimary,
                        trackColor = ThornburySurfaceSoft
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // View Mode Segmented Control Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Segmented Button Container for Column vs Grid View
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ThornburySurfaceSoft,
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Column View Button
                            Surface(
                                onClick = { viewMode = ReportsViewMode.RECORDS_AND_FILES },
                                shape = RoundedCornerShape(7.dp),
                                color = if (viewMode == ReportsViewMode.RECORDS_AND_FILES) ThornburyPrimary else Color.Transparent,
                                modifier = Modifier.height(34.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ViewAgenda,
                                        contentDescription = "Column View",
                                        tint = if (viewMode == ReportsViewMode.RECORDS_AND_FILES) Color.White else ThornburyMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Column",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (viewMode == ReportsViewMode.RECORDS_AND_FILES) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (viewMode == ReportsViewMode.RECORDS_AND_FILES) Color.White else ThornburyInk
                                    )
                                }
                            }

                            // Grid View Button
                            Surface(
                                onClick = { viewMode = ReportsViewMode.IMAGING_GALLERY },
                                shape = RoundedCornerShape(7.dp),
                                color = if (viewMode == ReportsViewMode.IMAGING_GALLERY) ThornburyPrimary else Color.Transparent,
                                modifier = Modifier.height(34.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GridView,
                                        contentDescription = "Grid View",
                                        tint = if (viewMode == ReportsViewMode.IMAGING_GALLERY) Color.White else ThornburyMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Grid",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (viewMode == ReportsViewMode.IMAGING_GALLERY) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (viewMode == ReportsViewMode.IMAGING_GALLERY) Color.White else ThornburyInk
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = if (viewMode == ReportsViewMode.RECORDS_AND_FILES) "Detailed Records List" else "2-Column Imaging Grid",
                        style = MaterialTheme.typography.labelSmall,
                        color = ThornburyMuted
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Full-Width Modality Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    kinds.forEach { kind ->
                        val isSelected = selectedFilter == kind
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = kind },
                            label = { Text(kind, style = MaterialTheme.typography.labelSmall) },
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

        // =====================================================================
        // Tab Body Content
        // =====================================================================
        if (filteredReports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ThornburySurfaceSoft),
                    border = BorderStroke(1.dp, ThornburyHairline)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ThornburyPrimary.copy(alpha = 0.1f),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = ThornburyPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No Diagnostic Media or Reports",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Capture radiographs, upload intraoral photos, or attach laboratory documents for this patient chart.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { launchCamera() },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ThornburyPrimary)
                            ) {
                                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = ThornburyPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Camera", color = ThornburyPrimary, style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = { pickImageLauncher.launch("image/*") },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary)
                            ) {
                                Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload Image", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        } else {
            when (viewMode) {
                ReportsViewMode.RECORDS_AND_FILES -> {
                    // Standard Records List with Rich Inline Attachments
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filteredReports, key = { it.id }) { report ->
                            DiagnosticReportRecordCard(
                                report = report,
                                onReportClick = { onReportClick(report) },
                                onReportAttachmentClick = { attIdx ->
                                    onReportAttachmentClick?.invoke(report, attIdx) ?: onReportClick(report)
                                },
                                onToggleRelease = { onToggleRelease(report.id) },
                                onOpenFile = { att -> AttachmentViewerUtils.openAttachment(context, att) },
                                onShareFile = { att -> AttachmentViewerUtils.shareAttachment(context, att) },
                                onQuickAttachImage = { pickImageLauncher.launch("image/*") },
                                onQuickAttachDoc = { pickDocumentLauncher.launch("application/pdf") }
                            )
                        }
                    }
                }

                ReportsViewMode.IMAGING_GALLERY -> {
                    // Visual 2-Column Dental Imaging Gallery
                    val galleryItems = remember(filteredReports) {
                        filteredReports.flatMap { rep ->
                            if (rep.attachments.isNotEmpty()) {
                                rep.attachments.mapIndexed { idx, att -> Triple(rep, att, idx) }
                            } else {
                                listOf(Triple(rep, null, 0))
                            }
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(galleryItems) { (report, attachment, attachmentIndex) ->
                            ImagingGalleryCard(
                                report = report,
                                attachment = attachment,
                                onClick = {
                                    onReportAttachmentClick?.invoke(report, attachmentIndex) ?: onReportClick(report)
                                },
                                onOpenFile = { att -> AttachmentViewerUtils.openAttachment(context, att) }
                            )
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // Quick Upload Modal Bottom Sheet / Menu
    // =========================================================================
    if (showQuickUploadMenu) {
        ModalBottomSheet(
            onDismissRequest = { showQuickUploadMenu = false },
            containerColor = ThornburyCanvas,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Upload Clinical Media & Documents",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
                Text(
                    text = "Select a source to attach to ${patient.name}'s dental records",
                    style = MaterialTheme.typography.bodySmall,
                    color = ThornburyMuted
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Camera Action
                UploadSourceItem(
                    icon = Icons.Default.CameraAlt,
                    title = "Take Radiograph or Intraoral Photo",
                    subtitle = "Capture directly with camera hardware",
                    tint = ThornburyPrimary,
                    onClick = {
                        showQuickUploadMenu = false
                        launchCamera()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Gallery / Image Action
                UploadSourceItem(
                    icon = Icons.Default.PhotoLibrary,
                    title = "Upload X-Ray / Image File",
                    subtitle = "Browse JPG, PNG, or OPG scans from gallery",
                    tint = ThornburyAccentTeal,
                    onClick = {
                        showQuickUploadMenu = false
                        pickImageLauncher.launch("image/*")
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // PDF / Document Action
                UploadSourceItem(
                    icon = Icons.Default.PictureAsPdf,
                    title = "Upload PDF Document / Lab Slip",
                    subtitle = "Attach clinical laboratory results, consent forms, or reports",
                    tint = Color(0xFFC0392B),
                    onClick = {
                        showQuickUploadMenu = false
                        pickDocumentLauncher.launch("application/pdf")
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// =============================================================================
// Diagnostic Report Record Card with Real Attachment Thumbnails
// =============================================================================

@Composable
private fun DiagnosticReportRecordCard(
    report: DiagnosticReport,
    onReportClick: () -> Unit,
    onReportAttachmentClick: ((Int) -> Unit)? = null,
    onToggleRelease: () -> Unit,
    onOpenFile: (ReportAttachment) -> Unit,
    onShareFile: (ReportAttachment) -> Unit,
    onQuickAttachImage: () -> Unit,
    onQuickAttachDoc: () -> Unit
) {
    val isReleased = report.releasedAt != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onReportClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Kind icon, Title, Kind Pill, Release Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (report.kind) {
                            "Radiograph" -> ThornburyPrimary.copy(alpha = 0.12f)
                            "CBCT Scan" -> ThornburyAccentTeal.copy(alpha = 0.12f)
                            "Lab Report" -> Color(0xFFC0392B).copy(alpha = 0.12f)
                            else -> ThornburySurfaceSoft
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (report.kind) {
                                    "Radiograph" -> Icons.Default.Image
                                    "Charting" -> Icons.Default.FormatListNumbered
                                    "CBCT Scan" -> Icons.Default.ViewInAr
                                    "Lab Report" -> Icons.Default.Science
                                    else -> Icons.Default.Description
                                },
                                contentDescription = null,
                                tint = when (report.kind) {
                                    "Radiograph" -> ThornburyPrimary
                                    "CBCT Scan" -> ThornburyAccentTeal
                                    "Lab Report" -> Color(0xFFC0392B)
                                    else -> ThornburyInk
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = report.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ThornburySurfaceSoft,
                                border = BorderStroke(1.dp, ThornburyHairline)
                            ) {
                                Text(
                                    text = report.kind,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = ThornburyPrimaryText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = report.takenAt,
                                style = MaterialTheme.typography.labelSmall,
                                color = ThornburyMuted
                            )
                        }
                    }
                }

                // Release Toggle Pill
                Surface(
                    onClick = onToggleRelease,
                    shape = RoundedCornerShape(9999.dp),
                    color = if (isReleased) ThornburyAccentTeal.copy(alpha = 0.12f) else ThornburySurfaceSoft,
                    border = BorderStroke(1.dp, if (isReleased) ThornburyAccentTeal else ThornburyHairline)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isReleased) Icons.Default.CheckCircle else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isReleased) ThornburyAccentTeal else ThornburyMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isReleased) "Portal Active" else "Private",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                            color = if (isReleased) ThornburyAccentTeal else ThornburyMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Findings / Summary Text
            Text(
                text = report.summary,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = ThornburyBodyStrong
            )

            // =================================================================
            // Attachments Preview Section
            // =================================================================
            if (report.attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = ThornburyHairlineSoft)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "ATTACHED FILES (${report.attachments.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = ThornburyMuted
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    report.attachments.forEach { att ->
                        val isImage = att.mimeType.startsWith("image/") ||
                                att.name.endsWith(".jpg", ignoreCase = true) ||
                                att.name.endsWith(".jpeg", ignoreCase = true) ||
                                att.name.endsWith(".png", ignoreCase = true)

                        if (isImage) {
                            // Image Attachment Preview Card
                            val attIndex = report.attachments.indexOf(att).coerceAtLeast(0)
                            InlineImageAttachmentCard(
                                attachment = att,
                                onClick = {
                                    if (onReportAttachmentClick != null) {
                                        onReportAttachmentClick(attIndex)
                                    } else {
                                        onReportClick()
                                    }
                                }
                            )
                        } else {
                            // Document / PDF Attachment Card with Direct Open Action
                            InlineDocumentAttachmentCard(
                                attachment = att,
                                onOpen = { onOpenFile(att) },
                                onShare = { onShareFile(att) }
                            )
                        }
                    }
                }
            } else {
                // No files attached: Prompt with quick attach options
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = ThornburySurfaceSoft.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, ThornburyHairline)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "No media files attached",
                            style = MaterialTheme.typography.labelSmall,
                            color = ThornburyMuted
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = onQuickAttachImage,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp), tint = ThornburyPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Photo", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = ThornburyPrimary)
                            }
                            TextButton(
                                onClick = onQuickAttachDoc,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp), tint = ThornburyPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Doc", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = ThornburyPrimary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = ThornburyHairlineSoft)
            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Logged in Clinician & Tap prompt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Clinician: ${report.clinicianName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ThornburyMuted
                )
                Text(
                    text = "Tap to inspect →",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = ThornburyPrimary
                )
            }
        }
    }
}

// =============================================================================
// Inline Image Attachment Card (Thumbnail with Zoom)
// =============================================================================

@Composable
private fun InlineImageAttachmentCard(
    attachment: ReportAttachment,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val bitmapState by produceState<android.graphics.Bitmap?>(initialValue = null, attachment.uri) {
        value = AttachmentViewerUtils.decodeBitmapSafely(context, attachment.uri, 400, 300)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = ThornburyCanvas,
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF101618)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmapState != null) {
                    Image(
                        bitmap = bitmapState!!.asImageBitmap(),
                        contentDescription = attachment.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = ThornburyPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.name,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${attachment.sizeStr} • Dental Radiograph / Photo",
                    style = MaterialTheme.typography.labelSmall,
                    color = ThornburyMuted
                )
            }

            IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = "Inspect",
                    tint = ThornburyPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// =============================================================================
// Inline Document Attachment Card (PDF / Lab with Open & Share)
// =============================================================================

@Composable
private fun InlineDocumentAttachmentCard(
    attachment: ReportAttachment,
    onOpen: () -> Unit,
    onShare: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(10.dp),
        color = ThornburyCanvas,
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFC0392B).copy(alpha = 0.12f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color(0xFFC0392B),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.name,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${attachment.sizeStr} • Clinical Document / PDF",
                    style = MaterialTheme.typography.labelSmall,
                    color = ThornburyMuted
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onOpen, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open",
                        tint = ThornburyPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = ThornburyMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// =============================================================================
// Visual 2-Column Imaging Gallery Card
// =============================================================================

@Composable
private fun ImagingGalleryCard(
    report: DiagnosticReport,
    attachment: ReportAttachment?,
    onClick: () -> Unit,
    onOpenFile: (ReportAttachment) -> Unit
) {
    val context = LocalContext.current
    val uriStr = attachment?.uri ?: report.image
    val isPdf = attachment?.name?.endsWith(".pdf", ignoreCase = true) == true

    val bitmapState by produceState<android.graphics.Bitmap?>(initialValue = null, uriStr) {
        if (!isPdf) {
            value = AttachmentViewerUtils.decodeBitmapSafely(context, uriStr, 400, 400)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isPdf && attachment != null) onOpenFile(attachment) else onClick()
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFF0F1518)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmapState != null) {
                    Image(
                        bitmap = bitmapState!!.asImageBitmap(),
                        contentDescription = report.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (isPdf) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFFC0392B),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "PDF Document",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = ThornburyPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = report.kind,
                            style = MaterialTheme.typography.labelSmall,
                            color = ThornburyMuted
                        )
                    }
                }

                // Modality pill badge in top right
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = if (isPdf) "PDF" else report.kind,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = attachment?.name ?: report.title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = report.takenAt,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = ThornburyMuted
                )
            }
        }
    }
}

@Composable
private fun UploadSourceItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = ThornburySurfaceSoft,
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = tint.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ThornburyMuted
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = ThornburyMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
