package com.example.thornburydental.ui.clinic

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.thornburydental.data.DiagnosticReport
import com.example.thornburydental.data.ReportAttachment
import com.example.thornburydental.theme.*

/**
 * High-contrast negative color transform for digital radiograph inspection.
 */
private val negativeColorMatrix = ColorMatrix(
    floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f
    )
)

/**
 * Redesigned Diagnostic Media Lightbox Dialog.
 *
 * Exclusively focused on displaying real uploaded clinical documents, dental radiographs,
 * intraoral photography, and laboratory reports without simulated canvas illustrations or release buttons.
 *
 * Features:
 * - Clean medical inspection console with dark viewport
 * - High-resolution bitmap decoding with pan and pinch-to-zoom (up to 5x)
 * - Radiographic contrast inversion toggle
 * - Zoom controls (+, -, 100% reset)
 * - Dedicated full-fidelity PDF document viewport with 1-tap Open & Share
 * - Multi-attachment selector chips for reports with multiple files
 * - Structured clinical findings notes card
 */
@Composable
fun ReportViewerLightboxDialog(
    report: DiagnosticReport,
    onDismiss: () -> Unit,
    initialAttachmentIndex: Int = 0,
    onToggleRelease: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }
    var isInverted by remember { mutableStateOf(false) }

    var selectedAttachmentIndex by remember {
        mutableIntStateOf(initialAttachmentIndex.coerceIn(0, (report.attachments.size - 1).coerceAtLeast(0)))
    }

    val selectedAttachment = report.attachments.getOrNull(selectedAttachmentIndex)
        ?: report.attachments.firstOrNull()

    val isSelectedPdf = selectedAttachment != null && (
        selectedAttachment.name.endsWith(".pdf", ignoreCase = true) ||
        selectedAttachment.mimeType == "application/pdf"
    )

    val currentImageUri = if (!isSelectedPdf && selectedAttachment != null) {
        selectedAttachment.uri
    } else {
        report.attachments.firstOrNull {
            it.mimeType.startsWith("image/") ||
                it.name.endsWith(".jpg", ignoreCase = true) ||
                it.name.endsWith(".jpeg", ignoreCase = true) ||
                it.name.endsWith(".png", ignoreCase = true)
        }?.uri ?: report.image
    }

    val hasImageUri = !currentImageUri.isNullOrBlank()

    val realBitmap by produceState<Bitmap?>(initialValue = null, currentImageUri) {
        val uri = currentImageUri
        if (!uri.isNullOrBlank() && !isSelectedPdf) {
            value = AttachmentViewerUtils.decodeBitmapSafely(context, uri, 2048, 2048)
        } else {
            value = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 10.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // =============================================================
                // Clean Header: Title, Metadata & Close (X) Button
                // =============================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedAttachment?.name ?: report.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ThornburySurfaceSoft,
                                border = BorderStroke(1.dp, ThornburyHairline)
                            ) {
                                Text(
                                    text = report.kind,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                    color = ThornburyPrimaryText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "Taken: ${report.takenAt} • Clinician: ${report.clinicianName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ThornburyMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ThornburySurfaceSoft)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ThornburyInk,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // =============================================================
                // Attachment Selector Chips (if multiple files on this report)
                // =============================================================
                if (report.attachments.size > 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        report.attachments.forEachIndexed { idx, att ->
                            val isSelected = selectedAttachmentIndex == idx
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedAttachmentIndex = idx
                                    zoomScale = 1f
                                    panOffsetX = 0f
                                    panOffsetY = 0f
                                },
                                label = {
                                    Text(
                                        text = att.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (att.name.endsWith(".pdf", ignoreCase = true)) Icons.Default.PictureAsPdf else Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ThornburyPrimary,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White,
                                    containerColor = ThornburySurfaceSoft,
                                    labelColor = ThornburyInk,
                                    iconColor = ThornburyMuted
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // =============================================================
                // Main Media Viewport (Takes primary space)
                // =============================================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, ThornburySurfaceDarkElevated, RoundedCornerShape(14.dp)),
                    color = if (isInverted) Color(0xFFE9EEF0) else Color(0xFF0A0F12)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val zoomPanModifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    zoomScale = (zoomScale * zoom).coerceIn(0.8f, 5.0f)
                                    panOffsetX += pan.x
                                    panOffsetY += pan.y
                                }
                            }
                            .graphicsLayer {
                                scaleX = zoomScale
                                scaleY = zoomScale
                                translationX = panOffsetX
                                translationY = panOffsetY
                            }

                        if (isSelectedPdf && selectedAttachment != null) {
                            // Dedicated Clean PDF / Document Inspection Viewport
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFC0392B).copy(alpha = 0.15f),
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            tint = Color(0xFFE74C3C),
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = selectedAttachment.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${selectedAttachment.sizeStr} • Diagnostic Laboratory Report / Clinical PDF",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF8FA4A0)
                                )
                                Spacer(modifier = Modifier.height(22.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = { AttachmentViewerUtils.openAttachment(context, selectedAttachment) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary)
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Open Document", style = MaterialTheme.typography.labelMedium)
                                    }
                                    OutlinedButton(
                                        onClick = { AttachmentViewerUtils.shareAttachment(context, selectedAttachment) },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF4A635E))
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Share", color = Color.White, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        } else if (realBitmap != null) {
                            // Real Image / Radiograph Viewport
                            Image(
                                bitmap = realBitmap!!.asImageBitmap(),
                                contentDescription = selectedAttachment?.name ?: report.title,
                                contentScale = ContentScale.Fit,
                                modifier = zoomPanModifier,
                                colorFilter = if (isInverted) ColorFilter.colorMatrix(negativeColorMatrix) else null
                            )

                            // Floating Controls Overlay (Invert, Zoom -, Reset, Zoom +)
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = (if (isInverted) Color(0xFFE0E6E4) else Color(0xFF131B1F)).copy(alpha = 0.94f),
                                border = BorderStroke(1.dp, if (isInverted) Color(0xFFB0C4BF) else Color(0xFF2C3E44))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { isInverted = !isInverted },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.InvertColors,
                                            contentDescription = "Invert Contrast",
                                            tint = if (isInverted) ThornburyPrimary else ThornburyAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    VerticalDivider(
                                        modifier = Modifier.height(18.dp),
                                        color = if (isInverted) Color(0xFFB0C4BF) else Color(0xFF2C3E44)
                                    )

                                    IconButton(
                                        onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.8f) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Zoom Out",
                                            tint = if (isInverted) Color(0xFF12201E) else Color(0xFFEAF3F1),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    TextButton(
                                        onClick = {
                                            zoomScale = 1f
                                            panOffsetX = 0f
                                            panOffsetY = 0f
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(
                                            text = "${(zoomScale * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (isInverted) Color(0xFF12201E) else Color(0xFFEAF3F1)
                                        )
                                    }

                                    IconButton(
                                        onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(5.0f) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Zoom In",
                                            tint = if (isInverted) Color(0xFF12201E) else Color(0xFFEAF3F1),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Bottom Left File Info Pill
                            selectedAttachment?.let { att ->
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(12.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.Black.copy(alpha = 0.65f)
                                ) {
                                    Text(
                                        text = "${att.name} • ${att.sizeStr}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else {
                            // Empty / No Media Case
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = ThornburyMuted,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No Raw Media File Attached",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "This diagnostic record contains clinical findings without an uploaded image or document.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF8FA4A0),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // =============================================================
                // Clean Diagnostic Findings & Summary Card (Bottom)
                // =============================================================
                if (report.summary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = ThornburySurfaceSoft,
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notes,
                                        contentDescription = null,
                                        tint = ThornburyPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Clinical Findings",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = ThornburyInk
                                    )
                                }

                                Text(
                                    text = "ID: ${report.id.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    ),
                                    color = ThornburyMuted
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = report.summary,
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                color = ThornburyBodyStrong
                            )
                        }
                    }
                }
            }
        }
    }
}
