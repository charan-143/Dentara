package com.example.thornburydental.ui.clinic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.thornburydental.data.DiagnosticReport
import com.example.thornburydental.theme.*

/**
 * Diagnostic Report & Radiographic Imaging Lightbox Dialog.
 *
 * Provides a high-fidelity diagnostic examination console featuring:
 * - Patient safety release status banner (RELEASED TO PATIENT vs HELD IN SURGERY)
 * - Immediate Toggle Release action to grant or revoke portal access
 * - Interactive Radiographic Canvas with color invert, zoom & pan controls
 * - Clinically authentic simulated dental radiographs (Periapical Root & Panoramic Arch views)
 *   with radiopaque enamel caps, dentin shading, radiolucent pulp canals, trabecular alveolar bone,
 *   periodontal ligament (PDL) spaces, and anatomical orientation markers.
 * - Clinical findings & diagnostic summary card.
 */
@Composable
fun ReportViewerLightboxDialog(
    report: DiagnosticReport,
    onDismiss: () -> Unit,
    onToggleRelease: () -> Unit
) {
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }
    var isInverted by remember { mutableStateOf(false) }

    // Auto-detect preferred view mode from report details, but allow user toggle
    val isInitiallyPanoramic = report.title.contains("Panoramic", ignoreCase = true) ||
            report.title.contains("OPG", ignoreCase = true) ||
            report.summary.contains("generalised", ignoreCase = true)
    var isPanoramicMode by remember { mutableStateOf(isInitiallyPanoramic) }

    val isReleased = report.releasedAt != null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // =============================================================
                // Modal Header: Title, Badges & Close Button
                // =============================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = report.title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = ThornburyInk
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            // Kind Badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ThornburySurfaceSoft,
                                border = BorderStroke(1.dp, ThornburyHairline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (report.kind) {
                                            "Radiograph" -> Icons.Default.Image
                                            "CBCT Scan" -> Icons.Default.ViewInAr
                                            "Charting" -> Icons.Default.FormatListNumbered
                                            else -> Icons.Default.Science
                                        },
                                        contentDescription = null,
                                        tint = ThornburyPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = report.kind,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = ThornburyPrimaryText
                                    )
                                }
                            }

                            // Release Status Badge
                            if (isReleased) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ThornburySuccessWash,
                                    border = BorderStroke(1.dp, ThornburySuccess.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = ThornburySuccess,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "RELEASED TO PATIENT",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            ),
                                            color = ThornburySuccess
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ThornburyWarningWash,
                                    border = BorderStroke(1.dp, ThornburyWarning.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = ThornburyWarning,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "HELD IN SURGERY",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            ),
                                            color = ThornburyWarning
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Recorded by ${report.clinicianName} • Taken: ${report.takenAt}" +
                                    if (report.releasedAt != null) " • Released: ${report.releasedAt}" else " • Unreleased",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyMuted
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Lightbox",
                                tint = ThornburyInk
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // =============================================================
                // Action Bar: Release Toggle & Lightbox Mode Switches
                // =============================================================
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ThornburySurfaceSoft,
                    border = BorderStroke(1.dp, ThornburyHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Release Toggle Button
                        Button(
                            onClick = onToggleRelease,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isReleased) ThornburyWarningWash else ThornburyPrimary,
                                contentColor = if (isReleased) ThornburyWarning else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = if (isReleased) BorderStroke(1.dp, ThornburyWarning) else null,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isReleased) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isReleased) "Hold in Surgery" else "Release to Patient",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        // View mode selector (Periapical vs Panoramic)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = !isPanoramicMode,
                                onClick = { isPanoramicMode = false },
                                label = { Text("Periapical Focus", style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ThornburyPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = ThornburyCanvas
                                )
                            )
                            FilterChip(
                                selected = isPanoramicMode,
                                onClick = { isPanoramicMode = true },
                                label = { Text("Panoramic Arch", style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ThornburyPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = ThornburyCanvas
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // =============================================================
                // Radiographic Canvas Lightbox Viewport
                // =============================================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.5.dp, ThornburySurfaceDarkElevated, RoundedCornerShape(14.dp)),
                    color = if (isInverted) Color(0xFFE9EEF0) else Color(0xFF0C1014)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // The interactive radiographic rendering canvas
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .clipToBounds()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        zoomScale = (zoomScale * zoom).coerceIn(0.8f, 4.0f)
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
                        ) {
                            if (isPanoramicMode) {
                                drawPanoramicRadiograph(
                                    isInverted = isInverted,
                                    hasPathology = report.summary.contains("bone loss", ignoreCase = true) ||
                                            report.summary.contains("furcation", ignoreCase = true)
                                )
                            } else {
                                drawPeriapicalRadiograph(
                                    isInverted = isInverted,
                                    hasLesion = report.summary.contains("radiolucency", ignoreCase = true) ||
                                            report.summary.contains("apical", ignoreCase = true) ||
                                            report.kind == "CBCT Scan",
                                    toothNumber = 19
                                )
                            }
                        }

                        // Medical Watermark / Radiographic metadata overlay
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "THORNBURY DIGITAL RADIOLOGY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isInverted) Color(0xFF263330) else Color(0xFF6FC6BA)
                            )
                            Text(
                                text = "70 kVp • 7 mA • 0.16s • High-Res CMOS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                ),
                                color = if (isInverted) Color(0xFF5B6E6A) else Color(0xFF9FB4AF)
                            )
                        }

                        // Orientation marker (R / L)
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp),
                            shape = CircleShape,
                            color = (if (isInverted) Color.Black else Color.White).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (isPanoramicMode) "R / L" else "R",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                color = if (isInverted) Color(0xFF12201E) else Color(0xFFEAF3F1)
                            )
                        }

                        // Millimeter calibration ruler stamp in bottom-left
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = (if (isInverted) Color.Black else Color.White).copy(alpha = 0.25f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(28.dp)
                                            .height(2.dp)
                                            .background(if (isInverted) Color.Black else Color.White)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "10 mm",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp
                                        ),
                                        color = if (isInverted) Color(0xFF12201E) else Color(0xFFEAF3F1)
                                    )
                                }
                            }
                        }

                        // Floating Viewport Controls (Invert, Zoom +, Zoom -, Reset)
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = (if (isInverted) Color(0xFFE0E6E4) else Color(0xFF172623)).copy(alpha = 0.92f),
                            border = BorderStroke(1.dp, if (isInverted) Color(0xFFB0C4BF) else Color(0xFF2C443E))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Invert negative/positive view button
                                IconButton(
                                    onClick = { isInverted = !isInverted },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.InvertColors,
                                        contentDescription = "Invert Radiograph Colors",
                                        tint = if (isInverted) ThornburyPrimary else ThornburyAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                VerticalDivider(
                                    modifier = Modifier.height(20.dp),
                                    color = if (isInverted) Color(0xFFB0C4BF) else Color(0xFF2C443E)
                                )

                                // Zoom Out
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

                                // Reset zoom
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

                                // Zoom In
                                IconButton(
                                    onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(4f) },
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
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // =============================================================
                // Clinical Summary & Diagnostic Findings Card
                // =============================================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard),
                    border = BorderStroke(1.dp, ThornburyHairline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = ThornburyPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Diagnostic Summary & Radiographic Findings",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyInk
                                )
                            }

                            Text(
                                text = "Exam ID: ${report.id.uppercase()}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = ThornburyMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = ThornburyCanvas,
                            border = BorderStroke(1.dp, ThornburyHairlineSoft)
                        ) {
                            Text(
                                text = report.summary,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                color = ThornburyBodyStrong,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Key Findings Breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ClinicalMetricPill(
                                label = "Modality",
                                value = report.kind,
                                modifier = Modifier.weight(1f)
                            )
                            ClinicalMetricPill(
                                label = "Radiologist / Clinician",
                                value = report.clinicianName.substringAfter("Dr. "),
                                modifier = Modifier.weight(1f)
                            )
                            ClinicalMetricPill(
                                label = "Portal Access",
                                value = if (isReleased) "Active" else "Restricted",
                                isPositive = isReleased,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Close Button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ThornburyHairline)
                ) {
                    Text(
                        text = "Close Lightbox",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = ThornburyInk
                    )
                }
            }
        }
    }
}

@Composable
private fun ClinicalMetricPill(
    label: String,
    value: String,
    isPositive: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = ThornburyCanvas,
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = ThornburyMuted
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isPositive) ThornburyInk else ThornburyWarning
            )
        }
    }
}

// =============================================================================
// Realistic Radiographic Canvas Renderers
// =============================================================================

/**
 * Draws a realistic periapical dental radiograph simulating molar crown, pulp chamber,
 * roots, periodontal ligament space, alveolar bone trabeculae, and potential apical lesion.
 */
private fun DrawScope.drawPeriapicalRadiograph(
    isInverted: Boolean,
    hasLesion: Boolean,
    toothNumber: Int
) {
    val boneColor = if (isInverted) Color(0xFFF2F5F4) else Color(0xFF141C1E)
    val trabecularLineColor = if (isInverted) Color(0xFFD6DFDC) else Color(0xFF222E30)
    val enamelColor = if (isInverted) Color(0xFF263330) else Color(0xFFF1F7F6)
    val dentinColor = if (isInverted) Color(0xFF7A8E88) else Color(0xFF8BA29F)
    val pulpColor = if (isInverted) Color(0xFFDCEAE7) else Color(0xFF162120)
    val pdlColor = if (isInverted) Color(0xFFE5EDE9) else Color(0xFF0F1817)
    val laminaDuraColor = if (isInverted) Color(0xFF47534F) else Color(0xFFD3E4E0)

    val w = size.width
    val h = size.height

    // 1. Trabecular alveolar bone background with subtle cancellous stippling
    drawRect(color = boneColor)

    // Alveolar trabeculae network pattern
    val bonePatternRows = 16
    val bonePatternCols = 24
    for (r in 0 until bonePatternRows) {
        for (c in 0 until bonePatternCols) {
            val px = (c + 0.5f) * (w / bonePatternCols) + (if (r % 2 == 0) 6f else -6f)
            val py = (r + 0.5f) * (h / bonePatternRows)
            drawCircle(
                color = trabecularLineColor.copy(alpha = 0.45f),
                radius = 2.5f,
                center = Offset(px, py)
            )
        }
    }

    // Alveolar crest bone line (scalloped interdental bone peaks)
    val crestY = h * 0.42f
    val crestPath = Path().apply {
        moveTo(0f, crestY)
        cubicTo(w * 0.25f, crestY - 14f, w * 0.4f, crestY + 12f, w * 0.5f, crestY - 18f)
        cubicTo(w * 0.6f, crestY + 14f, w * 0.75f, crestY - 12f, w, crestY + 8f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(path = crestPath, color = boneColor.copy(alpha = 0.7f))

    val centerX = w * 0.5f
    val crownTop = h * 0.16f
    val crownBottom = h * 0.42f
    val rootApexDistal = h * 0.86f
    val rootApexMesial = h * 0.84f

    // 2. Anatomical Lamina Dura (thin dense cortical line surrounding tooth socket)
    val laminaPath = Path().apply {
        moveTo(centerX - 95f, crownBottom + 4f)
        // Mesial root socket
        cubicTo(centerX - 100f, h * 0.62f, centerX - 75f, rootApexMesial + 8f, centerX - 45f, rootApexMesial + 6f)
        // Furcation septum
        cubicTo(centerX - 35f, h * 0.62f, centerX - 10f, h * 0.52f, centerX + 6f, h * 0.52f)
        cubicTo(centerX + 22f, h * 0.52f, centerX + 40f, h * 0.62f, centerX + 50f, rootApexDistal + 6f)
        // Distal root socket
        cubicTo(centerX + 80f, rootApexDistal + 6f, centerX + 105f, h * 0.62f, centerX + 95f, crownBottom + 4f)
    }
    drawPath(path = laminaPath, color = laminaDuraColor, style = Stroke(width = 3.5f, cap = StrokeCap.Round))

    // 3. Periodontal Ligament (PDL) Space (thin radiolucent line inside the lamina dura)
    drawPath(path = laminaPath, color = pdlColor, style = Stroke(width = 2.0f, cap = StrokeCap.Round))

    // 4. Dentin Body (Roots + Core Crown)
    val dentinPath = Path().apply {
        // Occlusal dentin cusp tips
        moveTo(centerX - 85f, crownTop + 20f)
        lineTo(centerX - 50f, crownTop + 8f)
        lineTo(centerX, crownTop + 18f)
        lineTo(centerX + 50f, crownTop + 8f)
        lineTo(centerX + 85f, crownTop + 20f)

        // Distal crown contour
        cubicTo(centerX + 92f, crownTop + 50f, centerX + 90f, crownBottom - 10f, centerX + 78f, crownBottom)

        // Distal root contour
        cubicTo(centerX + 75f, h * 0.62f, centerX + 70f, rootApexDistal - 4f, centerX + 50f, rootApexDistal)
        cubicTo(centerX + 35f, rootApexDistal, centerX + 30f, h * 0.68f, centerX + 16f, h * 0.56f)

        // Inter-radicular furcation notch
        cubicTo(centerX + 5f, h * 0.54f, centerX - 5f, h * 0.54f, centerX - 16f, h * 0.56f)

        // Mesial root inner and apex
        cubicTo(centerX - 30f, h * 0.68f, centerX - 35f, rootApexMesial, centerX - 50f, rootApexMesial)
        cubicTo(centerX - 70f, rootApexMesial - 4f, centerX - 75f, h * 0.62f, centerX - 78f, crownBottom)

        // Mesial crown contour
        cubicTo(centerX - 90f, crownBottom - 10f, centerX - 92f, crownTop + 50f, centerX - 85f, crownTop + 20f)
        close()
    }
    drawPath(path = dentinPath, color = dentinColor)

    // 5. Radiopaque Enamel Cap (brightest layer over occlusal crown)
    val enamelPath = Path().apply {
        moveTo(centerX - 90f, crownTop + 45f)
        cubicTo(centerX - 92f, crownTop + 14f, centerX - 60f, crownTop - 2f, centerX - 46f, crownTop)
        cubicTo(centerX - 20f, crownTop + 12f, centerX - 5f, crownTop + 12f, centerX, crownTop + 10f)
        cubicTo(centerX + 5f, crownTop + 12f, centerX + 20f, crownTop + 12f, centerX + 46f, crownTop)
        cubicTo(centerX + 60f, crownTop - 2f, centerX + 92f, crownTop + 14f, centerX + 90f, crownTop + 45f)

        // Enamel-Dentin Junction (EDJ) interface
        lineTo(centerX + 84f, crownTop + 45f)
        cubicTo(centerX + 78f, crownTop + 26f, centerX + 50f, crownTop + 14f, centerX, crownTop + 24f)
        cubicTo(centerX - 50f, crownTop + 14f, centerX - 78f, crownTop + 26f, centerX - 84f, crownTop + 45f)
        close()
    }
    drawPath(path = enamelPath, color = enamelColor)

    // 6. Radiolucent Dental Pulp Chamber & Root Canals (dark center channels)
    val pulpPath = Path().apply {
        // Pulp chamber body with mesial and distal pulp horns
        moveTo(centerX - 42f, crownTop + 42f) // mesial pulp horn
        lineTo(centerX - 20f, crownTop + 52f)
        lineTo(centerX + 20f, crownTop + 52f)
        lineTo(centerX + 42f, crownTop + 42f) // distal pulp horn
        lineTo(centerX + 45f, crownBottom - 10f)

        // Distal root canal taper
        cubicTo(centerX + 44f, h * 0.60f, centerX + 52f, rootApexDistal - 24f, centerX + 50f, rootApexDistal - 6f)
        lineTo(centerX + 44f, rootApexDistal - 6f)
        cubicTo(centerX + 42f, rootApexDistal - 24f, centerX + 32f, h * 0.60f, centerX + 26f, crownBottom - 6f)

        // Chamber floor
        lineTo(centerX - 26f, crownBottom - 6f)

        // Mesial root canal taper
        cubicTo(centerX - 32f, h * 0.60f, centerX - 42f, rootApexMesial - 24f, centerX - 44f, rootApexMesial - 6f)
        lineTo(centerX - 50f, rootApexMesial - 6f)
        cubicTo(centerX - 52f, rootApexMesial - 24f, centerX - 44f, h * 0.60f, centerX - 45f, crownBottom - 10f)
        close()
    }
    drawPath(path = pulpPath, color = pulpColor)

    // 7. Pathological Periapical Radiolucency (Apical Periodontitis lesion) if present
    if (hasLesion) {
        val lesionCenterX = centerX + 50f
        val lesionCenterY = rootApexDistal + 10f
        val lesionRadius = 26f

        // Dark radiolucent halo around distal apex
        drawCircle(
            color = if (isInverted) Color(0xFFD6E2E0).copy(alpha = 0.85f) else Color(0xFF090D0E).copy(alpha = 0.88f),
            radius = lesionRadius,
            center = Offset(lesionCenterX, lesionCenterY)
        )

        // Measurement caliper crosshairs & callout ring
        drawCircle(
            color = ThornburyAccent.copy(alpha = 0.75f),
            radius = lesionRadius + 3f,
            center = Offset(lesionCenterX, lesionCenterY),
            style = Stroke(width = 1.5f, cap = StrokeCap.Round)
        )

        // Crosshair marks
        drawLine(
            color = ThornburyAccent.copy(alpha = 0.6f),
            start = Offset(lesionCenterX - lesionRadius - 8f, lesionCenterY),
            end = Offset(lesionCenterX + lesionRadius + 8f, lesionCenterY),
            strokeWidth = 1f
        )
        drawLine(
            color = ThornburyAccent.copy(alpha = 0.6f),
            start = Offset(lesionCenterX, lesionCenterY - lesionRadius - 8f),
            end = Offset(lesionCenterX, lesionCenterY + lesionRadius + 8f),
            strokeWidth = 1f
        )
    }

    // 8. Adjacent interproximal tooth contact shadows (Mesial & Distal neighbours)
    val neighborDentin = dentinColor.copy(alpha = 0.65f)
    // Mesial neighbour (Tooth #20 premolar outline)
    drawRoundRect(
        color = neighborDentin,
        topLeft = Offset(centerX - 180f, crownTop + 20f),
        size = Size(75f, h * 0.55f),
        cornerRadius = CornerRadius(24f, 24f)
    )
    // Distal neighbour (Tooth #18 second molar outline)
    drawRoundRect(
        color = neighborDentin,
        topLeft = Offset(centerX + 115f, crownTop + 14f),
        size = Size(90f, h * 0.60f),
        cornerRadius = CornerRadius(26f, 26f)
    )
}

/**
 * Draws a realistic full dental panoramic radiograph (Orthopantomogram / OPG)
 * displaying both maxillary and mandibular arches, condyles, and alveolar bone.
 */
private fun DrawScope.drawPanoramicRadiograph(
    isInverted: Boolean,
    hasPathology: Boolean
) {
    val boneColor = if (isInverted) Color(0xFFF0F4F3) else Color(0xFF131A1C)
    val sinusColor = if (isInverted) Color(0xFFE4EDE9) else Color(0xFF0D1415)
    val enamelColor = if (isInverted) Color(0xFF263330) else Color(0xFFF1F7F6)
    val dentinColor = if (isInverted) Color(0xFF7A8E88) else Color(0xFF8AA19E)
    val pulpColor = if (isInverted) Color(0xFFDCEAE7) else Color(0xFF162120)

    val w = size.width
    val h = size.height

    // 1. Dark panoramic exposure background
    drawRect(color = boneColor)

    // 2. Maxillary Sinus & Nasal Cavity radiolucent shadows
    val leftSinus = Path().apply {
        moveTo(w * 0.18f, h * 0.28f)
        cubicTo(w * 0.22f, h * 0.16f, w * 0.38f, h * 0.16f, w * 0.42f, h * 0.28f)
        cubicTo(w * 0.38f, h * 0.38f, w * 0.22f, h * 0.38f, w * 0.18f, h * 0.28f)
        close()
    }
    drawPath(path = leftSinus, color = sinusColor)

    val rightSinus = Path().apply {
        moveTo(w * 0.58f, h * 0.28f)
        cubicTo(w * 0.62f, h * 0.16f, w * 0.78f, h * 0.16f, w * 0.82f, h * 0.28f)
        cubicTo(w * 0.78f, h * 0.38f, w * 0.62f, h * 0.38f, w * 0.58f, h * 0.28f)
        close()
    }
    drawPath(path = rightSinus, color = sinusColor)

    // 3. Bilateral Mandibular Condyles & Rami
    val leftRamus = Path().apply {
        moveTo(w * 0.05f, h * 0.20f) // Condyle head
        lineTo(w * 0.08f, h * 0.22f)
        cubicTo(w * 0.10f, h * 0.45f, w * 0.14f, h * 0.70f, w * 0.22f, h * 0.82f)
        lineTo(w * 0.12f, h * 0.84f)
        cubicTo(w * 0.06f, h * 0.68f, w * 0.03f, h * 0.42f, w * 0.05f, h * 0.20f)
        close()
    }
    drawPath(path = leftRamus, color = dentinColor.copy(alpha = 0.45f))

    val rightRamus = Path().apply {
        moveTo(w * 0.95f, h * 0.20f)
        lineTo(w * 0.92f, h * 0.22f)
        cubicTo(w * 0.90f, h * 0.45f, w * 0.86f, h * 0.70f, w * 0.78f, h * 0.82f)
        lineTo(w * 0.88f, h * 0.84f)
        cubicTo(w * 0.94f, h * 0.68f, w * 0.97f, h * 0.42f, w * 0.95f, h * 0.20f)
        close()
    }
    drawPath(path = rightRamus, color = dentinColor.copy(alpha = 0.45f))

    // 4. Panoramic Upper & Lower Teeth Arches (16 upper, 16 lower teeth silhouettes)
    val totalTeeth = 16
    val archCurveYUpper = h * 0.44f
    val archCurveYLower = h * 0.54f

    for (i in 0 until totalTeeth) {
        val tNorm = (i + 0.5f) / totalTeeth
        val toothX = w * (0.16f + 0.68f * tNorm)
        // Smile curve parabolic sag in center
        val centerSag = (1f - (2f * (tNorm - 0.5f) * (tNorm - 0.5f)).coerceIn(0f, 1f)) * 26f

        val isMolar = i < 3 || i >= 13
        val isPremolar = i in 3..4 || i in 11..12
        val toothWidth = if (isMolar) 26f else if (isPremolar) 18f else 14f

        // Upper Tooth
        val upperTop = archCurveYUpper - centerSag - (if (isMolar) 42f else 36f)
        val upperBottom = archCurveYUpper - centerSag

        drawRoundRect(
            color = dentinColor,
            topLeft = Offset(toothX - toothWidth / 2, upperTop),
            size = Size(toothWidth, upperBottom - upperTop),
            cornerRadius = CornerRadius(6f, 6f)
        )
        // Upper Enamel cap
        drawRoundRect(
            color = enamelColor,
            topLeft = Offset(toothX - toothWidth / 2, upperBottom - 12f),
            size = Size(toothWidth, 12f),
            cornerRadius = CornerRadius(3f, 3f)
        )

        // Lower Tooth
        val lowerTop = archCurveYLower + centerSag
        val lowerBottom = archCurveYLower + centerSag + (if (isMolar) 42f else 36f)

        drawRoundRect(
            color = dentinColor,
            topLeft = Offset(toothX - toothWidth / 2, lowerTop),
            size = Size(toothWidth, lowerBottom - lowerTop),
            cornerRadius = CornerRadius(6f, 6f)
        )
        // Lower Enamel cap
        drawRoundRect(
            color = enamelColor,
            topLeft = Offset(toothX - toothWidth / 2, lowerTop),
            size = Size(toothWidth, 12f),
            cornerRadius = CornerRadius(3f, 3f)
        )
        // Lower pulp line
        drawLine(
            color = pulpColor,
            start = Offset(toothX, lowerTop + 6f),
            end = Offset(toothX, lowerBottom - 8f),
            strokeWidth = 2f
        )
    }

    // 5. Horizontal Occlusal plane demarcation
    drawLine(
        color = sinusColor.copy(alpha = 0.6f),
        start = Offset(w * 0.15f, h * 0.49f),
        end = Offset(w * 0.85f, h * 0.49f),
        strokeWidth = 1.5f
    )
}
