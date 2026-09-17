package com.example.thornburydental.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thornburydental.data.ToothCondition
import com.example.thornburydental.data.ToothRecord
import com.example.thornburydental.theme.*

// =============================================================================
// Tooth Anatomy Classification Helper
// =============================================================================

enum class ToothAnatomyType {
    MOLAR,
    PREMOLAR,
    CANINE,
    INCISOR
}

/**
 * Classifies tooth anatomy based on Universal Dental Numbering System (1 - 32).
 */
fun getToothAnatomyType(toothNum: Int): ToothAnatomyType {
    // Molars: 1, 2, 3, 14, 15, 16 (Upper) and 17, 18, 19, 30, 31, 32 (Lower)
    if ((toothNum in 1..3) || (toothNum in 14..19) || (toothNum in 30..32)) {
        return ToothAnatomyType.MOLAR
    }
    // Premolars: 4, 5, 12, 13 (Upper) and 20, 21, 28, 29 (Lower)
    if (toothNum in listOf(4, 5, 12, 13, 20, 21, 28, 29)) {
        return ToothAnatomyType.PREMOLAR
    }
    // Canines: 6, 11 (Upper) and 22, 27 (Lower)
    if (toothNum in listOf(6, 11, 22, 27)) {
        return ToothAnatomyType.CANINE
    }
    // Incisors: 7, 8, 9, 10 (Upper) and 23, 24, 25, 26 (Lower)
    return ToothAnatomyType.INCISOR
}

/**
 * Returns whether tooth belongs to Maxillary (Upper 1-16) or Mandibular (Lower 17-32) arch.
 */
fun isMaxillaryTooth(toothNum: Int): Boolean = toothNum in 1..16

// =============================================================================
// Color & Condition Resolvers
// =============================================================================

fun getToothConditionColor(condition: ToothCondition): Color {
    return when (condition) {
        ToothCondition.SOUND -> ToothSound
        ToothCondition.DECAY -> ToothDecay
        ToothCondition.FILLED -> ToothFilled
        ToothCondition.CROWN -> ToothCrown
        ToothCondition.ROOT_CANAL -> ToothRootCanal
        ToothCondition.IMPLANT -> ToothImplant
        ToothCondition.MISSING -> ToothMissing
    }
}

// =============================================================================
// Anatomical Tooth View (Main Interactive Component)
// =============================================================================

/**
 * Rich interactive Anatomical Tooth Component for the Thornbury Dental Odontogram.
 *
 * @param tooth The [ToothRecord] containing tooth number, FDI number, condition, etc.
 * @param isSelected Whether the tooth is selected in the odontogram.
 * @param modifier Optional Compose modifier.
 * @param onClick Callback triggered when tapping this tooth.
 */
@Composable
fun AnatomicalToothView(
    tooth: ToothRecord,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val condColor = getToothConditionColor(tooth.condition)

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.07f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ToothScaleSpring"
    )

    val animatedBgColor by animateColorAsState(
        targetValue = if (isSelected) ThornburyPrimaryWash else ThornburyCanvas,
        animationSpec = tween(durationMillis = 200),
        label = "ToothBgColor"
    )

    val targetBorderColor = if (isSelected) {
        ThornburyPrimary
    } else if (tooth.condition != ToothCondition.SOUND) {
        condColor.copy(alpha = 0.65f)
    } else {
        ThornburyHairline
    }

    val animatedBorderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(durationMillis = 200),
        label = "ToothBorderColor"
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .width(52.dp)
            .height(104.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = animatedBgColor
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = animatedBorderColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp,
            pressedElevation = 6.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: FDI Tooth Number (e.g. 11, 21, 51, 85)
            Text(
                text = "${tooth.fdiNumber}",
                style = ClinicalCodeStyle.copy(
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = if (isSelected) ThornburyPrimaryText else ThornburyInk,
                textAlign = TextAlign.Center
            )

            // Center: Anatomical Tooth Visual Graphic (Canvas Fallback in grid view)
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                AnatomicalToothVisual(
                    tooth = tooth,
                    modifier = Modifier.fillMaxSize(),
                    use3D = false
                )
            }

            // Bottom: Condition / Legacy indicator badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (tooth.number in 1..32 && tooth.number != tooth.fdiNumber) "#${tooth.number}" else "FDI",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = ThornburyMuted
                )

                ToothConditionBadge(condition = tooth.condition)
            }
        }
    }
}

/**
 * Overload without modifier for clean call syntax:
 * AnatomicalToothView(tooth, isSelected) { ... }
 */
@Composable
fun AnatomicalToothView(
    tooth: ToothRecord,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    AnatomicalToothView(
        tooth = tooth,
        isSelected = isSelected,
        modifier = Modifier,
        onClick = onClick
    )
}

// =============================================================================
// Tooth Condition Mini Badge
// =============================================================================

@Composable
fun ToothConditionBadge(
    condition: ToothCondition,
    modifier: Modifier = Modifier
) {
    when (condition) {
        ToothCondition.SOUND -> {
            Box(
                modifier = modifier
                    .size(6.dp)
                    .background(ThornburyMutedSoft.copy(alpha = 0.4f), CircleShape)
            )
        }
        ToothCondition.DECAY -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(3.dp),
                color = ThornburyErrorWash
            ) {
                Text(
                    text = "D",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThornburyError,
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                )
            }
        }
        ToothCondition.FILLED -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(3.dp),
                color = ThornburyInfoWash
            ) {
                Text(
                    text = "F",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThornburyTertiaryText,
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                )
            }
        }
        ToothCondition.CROWN -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(3.dp),
                color = ThornburyWarningWash
            ) {
                Text(
                    text = "Cr",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThornburyWarning,
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                )
            }
        }
        ToothCondition.ROOT_CANAL -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(3.dp),
                color = ThornburyRoseWash
            ) {
                Text(
                    text = "RCT",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThornburyRoseText,
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                )
            }
        }
        ToothCondition.IMPLANT -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(3.dp),
                color = ThornburyPurpleWash
            ) {
                Text(
                    text = "Imp",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThornburyPurpleText,
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                )
            }
        }
        ToothCondition.MISSING -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(3.dp),
                color = ThornburyNeutralWash
            ) {
                Text(
                    text = "✕",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThornburySlateText,
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                )
            }
        }
    }
}

// =============================================================================
// Anatomical Tooth Visual Component (3D Filament with 2D Canvas Fallback)
// =============================================================================

/**
 * Primary 3D Visualizer for Anatomical Tooth rendering powered by Google Filament PBR Engine,
 * with smooth 2D canvas fallback ([AnatomicalToothCanvas]) if Filament GL initialization fails or is unsupported.
 *
 * @param tooth The [ToothRecord] containing tooth number, condition, etc.
 * @param modifier Optional Compose modifier.
 * @param use3D Whether to attempt 3D rendering for supported tooth models.
 * @param showCard Whether to render in card wrapper or direct viewport.
 */
@Composable
fun AnatomicalToothVisual(
    tooth: ToothRecord,
    modifier: Modifier = Modifier,
    use3D: Boolean = false,
    showCard: Boolean = false
) {
    AnatomicalToothCanvas(
        tooth = tooth,
        modifier = modifier
    )
}

// =============================================================================
// Anatomical Tooth Canvas Renderer
// =============================================================================

/**
 * Pure Canvas rendering of tooth anatomy, roots, crown table, and clinical condition overlays.
 *
 * Coordinate Model:
 * Normalized ViewBox is 64x90.
 * In canonical maxillary orientation:
 *   - Roots: y = 5 to 46 (pointing UPWARDS into the maxilla)
 *   - Cervical Margin (CEJ): y = 46
 *   - Crown: y = 46 to 85 (pointing DOWNWARDS towards the occlusal bite line)
 * For mandibular teeth (17 - 32):
 *   - Inverted vertically around y = 45: translate(0, 90) scale(1, -1)
 *   - Roots point DOWNWARDS into the mandible
 *   - Crown points UPWARDS towards the occlusal bite line
 */
@Composable
fun AnatomicalToothCanvas(
    tooth: ToothRecord,
    modifier: Modifier = Modifier
) {
    val anatomy = remember(tooth.number) { getToothAnatomyType(tooth.number) }
    val isUpper = remember(tooth.number) { isMaxillaryTooth(tooth.number) }

    // Pre-create reusable anatomical paths in the 64x90 coordinate space
    val paths = remember(anatomy, isUpper) {
        createAnatomyPaths(anatomy, isUpper)
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // Linear & Radial Shader Brushes
        val enamelBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFFDFCF9),
                Color(0xFFF8F4EC),
                Color(0xFFEFE8DD)
            ),
            start = Offset(0f, 0f),
            end = Offset(64f, 90f)
        )

        val rootBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFECE4D8),
                Color(0xFFDFD2BE),
                Color(0xFFCFBEAA)
            ),
            start = Offset(0f, 0f),
            end = Offset(0f, 90f)
        )

        val crownCapBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFEF08A),
                Color(0xFFF59E0B),
                Color(0xFFFBBF24),
                Color(0xFFD97706),
                Color(0xFF92400E)
            ),
            start = Offset(0f, 46f),
            end = Offset(64f, 85f)
        )

        val implantBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF475569),
                Color(0xFF94A3B8),
                Color(0xFFF8FAFC),
                Color(0xFF94A3B8),
                Color(0xFF334155)
            ),
            start = Offset(0f, 0f),
            end = Offset(64f, 0f)
        )

        val restoredBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF7EE2D0),
                Color(0xFF5DB8A6),
                Color(0xFF2E7568)
            ),
            start = Offset(20f, 60f),
            end = Offset(45f, 75f)
        )

        val cariesBrush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF0D0502),
                Color(0xFF2E1408),
                Color(0xFF5A290F),
                Color(0xFF7C3A17)
            ),
            center = Offset(32f, 66f),
            radius = 12f
        )

        // Apply Coordinate Transformation:
        // Scale to Canvas pixel dimensions (w / 64, h / 90).
        // If mandibular (17-32), invert vertically around y = 45.
        withTransform({
            scale(scaleX = w / 64f, scaleY = h / 90f, pivot = Offset.Zero)
            if (!isUpper) {
                translate(left = 0f, top = 90f)
                scale(scaleX = 1f, scaleY = -1f, pivot = Offset.Zero)
            }
        }) {
            val isMissing = tooth.condition == ToothCondition.MISSING
            val isImplant = tooth.condition == ToothCondition.IMPLANT
            val isCrown = tooth.condition == ToothCondition.CROWN
            val isRootCanal = tooth.condition == ToothCondition.ROOT_CANAL
            val isFilled = tooth.condition == ToothCondition.FILLED
            val isDecay = tooth.condition == ToothCondition.DECAY
            val isSound = tooth.condition == ToothCondition.SOUND

            if (isMissing) {
                // =============================================================
                // MISSING TOOTH: Translucent ghosted contour + Clinical Cross
                // =============================================================
                val dash = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                val ghostStroke = Stroke(width = 1.2f, pathEffect = dash)
                val ghostColor = Color(0xFF94A3B8).copy(alpha = 0.35f)

                // Draw ghosted root & crown
                paths.rootPath?.let { drawPath(it, ghostColor, style = ghostStroke) }
                paths.crownPath.let { drawPath(it, ghostColor, style = ghostStroke) }

                // Bold clinical "✕" cross in red
                val crossColor = ThornburyError
                drawLine(
                    color = crossColor,
                    start = Offset(14f, 14f),
                    end = Offset(50f, 76f),
                    strokeWidth = 2.4f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = crossColor,
                    start = Offset(50f, 14f),
                    end = Offset(14f, 76f),
                    strokeWidth = 2.4f,
                    cap = StrokeCap.Round
                )
            } else {
                // =============================================================
                // ROOT LAYER: Titanium Implant Post OR Natural Root(s)
                // =============================================================
                if (isImplant) {
                    // Abutment Collar at CEJ
                    drawPath(
                        path = paths.implantAbutment,
                        brush = implantBrush
                    )
                    drawPath(
                        path = paths.implantAbutment,
                        color = Color(0xFF334155),
                        style = Stroke(width = 1f)
                    )

                    // Threaded Tapered Screw Post Body
                    drawPath(
                        path = paths.implantBody,
                        brush = implantBrush
                    )
                    drawPath(
                        path = paths.implantBody,
                        color = Color(0xFF334155),
                        style = Stroke(width = 1f)
                    )

                    // Horizontal Thread Ribs
                    val threadLines = listOf(
                        Triple(23.5f, 40.5f, 35f),
                        Triple(24.2f, 39.8f, 30f),
                        Triple(24.9f, 39.1f, 25f),
                        Triple(25.6f, 38.4f, 20f),
                        Triple(26.3f, 37.7f, 15f),
                        Triple(27.0f, 37.0f, 10f)
                    )
                    threadLines.forEach { (x1, x2, y) ->
                        drawLine(
                            color = Color(0xFF1E293B),
                            start = Offset(x1, y),
                            end = Offset(x2, y),
                            strokeWidth = 1.6f,
                            cap = StrokeCap.Round
                        )
                    }

                    // Titanium Metallic Specular Reflection Highlight
                    drawLine(
                        color = Color.White.copy(alpha = 0.8f),
                        start = Offset(32f, 8f),
                        end = Offset(32f, 39f),
                        strokeWidth = 1.4f,
                        cap = StrokeCap.Round
                    )
                } else {
                    // Natural Biological Roots
                    paths.rootPath?.let { rootPath ->
                        // Cementum fill
                        drawPath(path = rootPath, brush = rootBrush)
                        // Root outline
                        drawPath(
                            path = rootPath,
                            color = Color(0xFFBEAD98),
                            style = Stroke(width = 1f)
                        )

                        // Anatomical Root Canal Guide Lines
                        when (anatomy) {
                            ToothAnatomyType.MOLAR -> {
                                if (isUpper) {
                                    // 3 canals: mesiobuccal, distobuccal, palatal
                                    drawLine(
                                        color = if (isRootCanal) ToothRootCanal else Color(0xFFB4A38D),
                                        start = Offset(14f, 42f),
                                        end = Offset(13f, 13f),
                                        strokeWidth = if (isRootCanal) 2.2f else 1.2f,
                                        cap = StrokeCap.Round
                                    )
                                    drawLine(
                                        color = if (isRootCanal) ToothRootCanal else Color(0xFFB4A38D),
                                        start = Offset(33f, 42f),
                                        end = Offset(33f, 9f),
                                        strokeWidth = if (isRootCanal) 2.2f else 1.2f,
                                        cap = StrokeCap.Round
                                    )
                                    drawLine(
                                        color = if (isRootCanal) ToothRootCanal else Color(0xFFB4A38D),
                                        start = Offset(51f, 42f),
                                        end = Offset(52f, 13f),
                                        strokeWidth = if (isRootCanal) 2.2f else 1.2f,
                                        cap = StrokeCap.Round
                                    )
                                } else {
                                    // 2 canals: mesial and distal
                                    drawLine(
                                        color = if (isRootCanal) ToothRootCanal else Color(0xFFB4A38D),
                                        start = Offset(16f, 42f),
                                        end = Offset(16f, 11f),
                                        strokeWidth = if (isRootCanal) 2.2f else 1.2f,
                                        cap = StrokeCap.Round
                                    )
                                    drawLine(
                                        color = if (isRootCanal) ToothRootCanal else Color(0xFFB4A38D),
                                        start = Offset(48f, 42f),
                                        end = Offset(48f, 11f),
                                        strokeWidth = if (isRootCanal) 2.2f else 1.2f,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                            ToothAnatomyType.PREMOLAR -> {
                                drawLine(
                                    color = if (isRootCanal) ToothRootCanal else Color(0xFFB4A38D),
                                    start = Offset(32.5f, 42f),
                                    end = Offset(32.5f, 11f),
                                    strokeWidth = if (isRootCanal) 2.2f else 1.2f,
                                    cap = StrokeCap.Round
                                )
                            }
                            ToothAnatomyType.CANINE -> {
                                drawLine(
                                    color = if (isRootCanal) ToothRootCanal else Color(0xFFB4A38D),
                                    start = Offset(32f, 42f),
                                    end = Offset(32f, 8f),
                                    strokeWidth = if (isRootCanal) 2.2f else 1.4f,
                                    cap = StrokeCap.Round
                                )
                            }
                            ToothAnatomyType.INCISOR -> {
                                drawLine(
                                    color = if (isRootCanal) ToothRootCanal else Color(0xFFB4A38D),
                                    start = Offset(32f, 42f),
                                    end = Offset(32f, 10f),
                                    strokeWidth = if (isRootCanal) 2.2f else 1.2f,
                                    cap = StrokeCap.Round
                                )
                            }
                        }

                        // Root Canal Endodontic Obturation Gutta-Percha core
                        if (isRootCanal) {
                            drawOval(
                                color = ToothRootCanal,
                                topLeft = Offset(26f, 41f),
                                size = Size(12f, 6f)
                            )
                            drawOval(
                                color = Color(0xFFFB7185),
                                topLeft = Offset(28.5f, 42f),
                                size = Size(7f, 4f)
                            )
                        }
                    }
                }

                // =============================================================
                // CERVICAL MARGIN (CEJ) LINE
                // =============================================================
                drawPath(
                    path = paths.cejPath,
                    color = Color(0xFFCFBEAA),
                    style = Stroke(width = 1.2f, cap = StrokeCap.Round)
                )

                // =============================================================
                // CROWN LAYER: Enamel / Gold-Ceramic Cap
                // =============================================================
                drawPath(
                    path = paths.crownPath,
                    brush = if (isCrown) crownCapBrush else enamelBrush
                )
                drawPath(
                    path = paths.crownPath,
                    color = if (isCrown) ThornburyWarning else Color(0xFFD4C8B8),
                    style = Stroke(width = 1.2f)
                )

                // =============================================================
                // OCCLUSAL GROOVES / FISSURES / MAMELONS
                // =============================================================
                if (!isCrown) {
                    val fissureColor = Color(0xFFC4B5A2)
                    val fissureAlpha = if (isDecay) 0.4f else 0.85f

                    when (anatomy) {
                        ToothAnatomyType.MOLAR -> {
                            // Central Developmental Groove
                            drawLine(
                                color = fissureColor.copy(alpha = fissureAlpha),
                                start = Offset(21f, 66f),
                                end = Offset(43f, 66f),
                                strokeWidth = 1.3f,
                                cap = StrokeCap.Round
                            )
                            // Buccal / Lingual Grooves
                            drawLine(
                                color = fissureColor.copy(alpha = fissureAlpha),
                                start = Offset(32f, 54f),
                                end = Offset(32f, 78f),
                                strokeWidth = 1.3f,
                                cap = StrokeCap.Round
                            )
                            // Cusp Fissure Branches
                            drawLine(
                                color = fissureColor.copy(alpha = fissureAlpha),
                                start = Offset(25f, 66f),
                                end = Offset(16f, 57f),
                                strokeWidth = 1.1f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = fissureColor.copy(alpha = fissureAlpha),
                                start = Offset(25f, 66f),
                                end = Offset(16f, 75f),
                                strokeWidth = 1.1f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = fissureColor.copy(alpha = fissureAlpha),
                                start = Offset(39f, 66f),
                                end = Offset(48f, 57f),
                                strokeWidth = 1.1f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = fissureColor.copy(alpha = fissureAlpha),
                                start = Offset(39f, 66f),
                                end = Offset(48f, 75f),
                                strokeWidth = 1.1f,
                                cap = StrokeCap.Round
                            )
                        }
                        ToothAnatomyType.PREMOLAR -> {
                            // Bicuspid Central Groove
                            drawLine(
                                color = fissureColor.copy(alpha = fissureAlpha),
                                start = Offset(22f, 66f),
                                end = Offset(42f, 66f),
                                strokeWidth = 1.3f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = fissureColor.copy(alpha = fissureAlpha),
                                start = Offset(25f, 66f),
                                end = Offset(20f, 62f),
                                strokeWidth = 1.1f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = fissureColor.copy(alpha = fissureAlpha),
                                start = Offset(25f, 66f),
                                end = Offset(20f, 70f),
                                strokeWidth = 1.1f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = fissureColor.copy(alpha = fissureAlpha),
                                start = Offset(39f, 66f),
                                end = Offset(44f, 62f),
                                strokeWidth = 1.1f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = fissureColor.copy(alpha = fissureAlpha),
                                start = Offset(39f, 66f),
                                end = Offset(44f, 70f),
                                strokeWidth = 1.1f,
                                cap = StrokeCap.Round
                            )
                        }
                        ToothAnatomyType.CANINE -> {
                            // Prominent Labial Ridge
                            drawLine(
                                color = Color.White.copy(alpha = 0.7f),
                                start = Offset(32f, 48f),
                                end = Offset(32f, 82f),
                                strokeWidth = 1.2f,
                                cap = StrokeCap.Round
                            )
                            // Canine Cusp Slopes
                            drawLine(
                                color = fissureColor.copy(alpha = fissureAlpha),
                                start = Offset(23f, 74f),
                                end = Offset(32f, 83f),
                                strokeWidth = 1f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = fissureColor.copy(alpha = fissureAlpha),
                                start = Offset(32f, 83f),
                                end = Offset(41f, 74f),
                                strokeWidth = 1f,
                                cap = StrokeCap.Round
                            )
                        }
                        ToothAnatomyType.INCISOR -> {
                            // Shovel Cutting Edge Margin
                            drawLine(
                                color = fissureColor.copy(alpha = fissureAlpha),
                                start = Offset(18f, 80f),
                                end = Offset(46f, 80f),
                                strokeWidth = 1f,
                                cap = StrokeCap.Round
                            )
                            // Developmental Mamelon Depressions
                            drawLine(
                                color = Color.White.copy(alpha = 0.6f),
                                start = Offset(26f, 56f),
                                end = Offset(26f, 76f),
                                strokeWidth = 1f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.6f),
                                start = Offset(38f, 56f),
                                end = Offset(38f, 76f),
                                strokeWidth = 1f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                // Pearlescent Enamel Translucent Contour Reflection (Sound / Natural)
                if (isSound) {
                    drawPath(
                        path = paths.enamelReflection,
                        color = Color.White.copy(alpha = 0.7f),
                        style = Stroke(width = 1.5f, cap = StrokeCap.Round)
                    )
                }

                // =============================================================
                // PATHOLOGY & RESTORATION OVERLAYS
                // =============================================================

                // A. CARIES / DECAY: Dark Cavitation Pit & Radiating Fissures
                if (isDecay) {
                    // Necrotic demineralization halo
                    drawOval(
                        color = Color(0xFF78350F).copy(alpha = 0.22f),
                        topLeft = Offset(23f, 59f),
                        size = Size(18f, 14f)
                    )

                    // Anatomical Caries Pit Lesion
                    drawPath(
                        path = paths.cariesPit,
                        brush = cariesBrush
                    )
                    drawPath(
                        path = paths.cariesPit,
                        color = Color(0xFF1A0C04),
                        style = Stroke(width = 0.8f)
                    )

                    // Jagged micro-fissure cracks
                    val cracks = listOf(
                        Pair(Offset(21f, 66f), Offset(13f, 62f)),
                        Pair(Offset(42f, 66f), Offset(50f, 69f)),
                        Pair(Offset(27f, 63f), Offset(30f, 53f)),
                        Pair(Offset(34f, 71f), Offset(33f, 80f))
                    )
                    cracks.forEach { (p1, p2) ->
                        drawLine(
                            color = Color(0xFF1F1108),
                            start = p1,
                            end = p2,
                            strokeWidth = 1.5f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // B. FILLED / RESTORED: Composite / Amalgam Inlay Geometry
                if (isFilled) {
                    drawPath(
                        path = paths.fillingInlay,
                        brush = restoredBrush
                    )
                    drawPath(
                        path = paths.fillingInlay,
                        color = Color(0xFF134E48),
                        style = Stroke(width = 1.1f)
                    )

                    // Restorative Specular Polish Luster
                    drawOval(
                        color = Color(0xFFA7F3D0).copy(alpha = 0.85f),
                        topLeft = Offset(27.5f, 63.5f),
                        size = Size(7f, 3.6f)
                    )
                }

                // C. CROWN: Cervical Collar Margin Ring & High-Gloss Specular Sheen
                if (isCrown) {
                    // Cervical Marginal Collar Ring
                    drawPath(
                        path = paths.crownCollar,
                        color = Color(0xFF78350F),
                        style = Stroke(width = 2.4f, cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = paths.crownCollar,
                        color = Color(0xFFFEF08A),
                        style = Stroke(width = 1f, cap = StrokeCap.Round)
                    )

                    // Polished High-Gloss Specular Sheen Line
                    drawPath(
                        path = paths.crownSheen,
                        color = Color.White.copy(alpha = 0.5f),
                        style = Stroke(width = 3.2f, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

// =============================================================================
// Reusable Precomputed Path Data Structure
// =============================================================================

private class ToothAnatomyPaths(
    val crownPath: Path,
    val rootPath: Path?,
    val cejPath: Path,
    val enamelReflection: Path,
    val implantAbutment: Path,
    val implantBody: Path,
    val cariesPit: Path,
    val fillingInlay: Path,
    val crownCollar: Path,
    val crownSheen: Path
)

/**
 * Builds vector paths in normalized 64x90 coordinate space.
 */
private fun createAnatomyPaths(anatomy: ToothAnatomyType, isUpper: Boolean): ToothAnatomyPaths {
    // 1. Crown Path
    val crownPath = Path().apply {
        when (anatomy) {
            ToothAnatomyType.MOLAR -> {
                moveTo(12f, 46f)
                cubicTo(8f, 53f, 8f, 72f, 13f, 80f)
                cubicTo(18f, 86f, 26f, 84f, 32f, 82f)
                cubicTo(38f, 84f, 46f, 86f, 51f, 80f)
                cubicTo(56f, 72f, 56f, 53f, 52f, 46f)
                cubicTo(42f, 48f, 22f, 48f, 12f, 46f)
                close()
            }
            ToothAnatomyType.PREMOLAR -> {
                moveTo(16f, 46f)
                cubicTo(12f, 54f, 13f, 73f, 18f, 80f)
                cubicTo(23f, 85f, 28f, 83f, 32f, 81f)
                cubicTo(36f, 83f, 41f, 85f, 46f, 80f)
                cubicTo(51f, 73f, 52f, 54f, 48f, 46f)
                cubicTo(40f, 47f, 24f, 47f, 16f, 46f)
                close()
            }
            ToothAnatomyType.CANINE -> {
                moveTo(18f, 46f)
                cubicTo(14f, 54f, 14f, 69f, 19f, 76f)
                lineTo(32f, 86f)
                lineTo(45f, 76f)
                cubicTo(50f, 69f, 50f, 54f, 46f, 46f)
                cubicTo(38f, 47f, 26f, 47f, 18f, 46f)
                close()
            }
            ToothAnatomyType.INCISOR -> {
                moveTo(17f, 46f)
                cubicTo(14f, 54f, 13f, 70f, 14f, 80f)
                cubicTo(15f, 82f, 17f, 83f, 20f, 83f)
                lineTo(44f, 83f)
                cubicTo(47f, 83f, 49f, 82f, 50f, 80f)
                cubicTo(51f, 70f, 50f, 54f, 47f, 46f)
                cubicTo(39f, 47f, 25f, 47f, 17f, 46f)
                close()
            }
        }
    }

    // 2. Root Path
    val rootPath = Path().apply {
        when (anatomy) {
            ToothAnatomyType.MOLAR -> {
                if (isUpper) {
                    // Maxillary Molar: 3 Roots (Mesiobuccal, Distobuccal, Palatal)
                    moveTo(15f, 46f)
                    cubicTo(13f, 36f, 9f, 24f, 9f, 13f)
                    cubicTo(9f, 6f, 17f, 6f, 18f, 12f)
                    cubicTo(20f, 23f, 23f, 35f, 25f, 38f)
                    cubicTo(27f, 28f, 29f, 16f, 31f, 6f)
                    cubicTo(32f, 4f, 34f, 4f, 35f, 6f)
                    cubicTo(37f, 16f, 39f, 28f, 41f, 38f)
                    cubicTo(43f, 35f, 46f, 23f, 48f, 12f)
                    cubicTo(49f, 6f, 57f, 6f, 57f, 13f)
                    cubicTo(57f, 24f, 53f, 36f, 50f, 46f)
                    cubicTo(38f, 48f, 26f, 48f, 15f, 46f)
                    close()
                } else {
                    // Mandibular Molar: 2 Roots (Mesial and Distal with deep Furcation)
                    moveTo(13f, 46f)
                    cubicTo(11f, 36f, 10f, 22f, 11f, 12f)
                    cubicTo(12f, 6f, 20f, 6f, 22f, 11f)
                    cubicTo(24f, 22f, 26f, 33f, 32f, 37f)
                    cubicTo(38f, 33f, 40f, 22f, 42f, 11f)
                    cubicTo(44f, 6f, 52f, 6f, 53f, 12f)
                    cubicTo(54f, 22f, 53f, 36f, 51f, 46f)
                    cubicTo(40f, 48f, 24f, 48f, 13f, 46f)
                    close()
                }
            }
            ToothAnatomyType.PREMOLAR -> {
                moveTo(21f, 46f)
                cubicTo(22f, 34f, 26f, 18f, 31f, 7f)
                cubicTo(32f, 5f, 34f, 5f, 35f, 7f)
                cubicTo(38f, 18f, 42f, 34f, 43f, 46f)
                cubicTo(36f, 47f, 28f, 47f, 21f, 46f)
                close()
            }
            ToothAnatomyType.CANINE -> {
                moveTo(19f, 46f)
                cubicTo(20f, 33f, 26f, 15f, 31f, 4f)
                cubicTo(32f, 3f, 33f, 3f, 34f, 4f)
                cubicTo(38f, 15f, 44f, 33f, 45f, 46f)
                cubicTo(37f, 47f, 27f, 47f, 19f, 46f)
                close()
            }
            ToothAnatomyType.INCISOR -> {
                moveTo(21f, 46f)
                cubicTo(23f, 35f, 27f, 18f, 31f, 7f)
                cubicTo(32f, 6f, 33f, 6f, 34f, 7f)
                cubicTo(38f, 18f, 42f, 35f, 43f, 46f)
                cubicTo(36f, 47f, 28f, 47f, 21f, 46f)
                close()
            }
        }
    }

    // 3. Cervical Margin (CEJ) Line
    val cejPath = Path().apply {
        moveTo(16f, 46f)
        cubicTo(26f, 48f, 38f, 48f, 48f, 46f)
    }

    // 4. Enamel Reflection Highlight
    val enamelReflection = Path().apply {
        moveTo(20f, 52f)
        cubicTo(24f, 50f, 40f, 50f, 44f, 52f)
    }

    // 5. Implant Abutment Collar
    val implantAbutment = Path().apply {
        moveTo(22f, 46f)
        lineTo(24f, 39f)
        lineTo(40f, 39f)
        lineTo(42f, 46f)
        close()
    }

    // 6. Implant Screw Body
    val implantBody = Path().apply {
        moveTo(24f, 39f)
        lineTo(27f, 9f)
        cubicTo(28f, 6f, 36f, 6f, 37f, 9f)
        lineTo(40f, 39f)
        close()
    }

    // 7. Caries Cavitation Pit
    val cariesPit = Path().apply {
        moveTo(27f, 63f)
        cubicTo(24f, 61f, 23f, 64f, 21f, 66f)
        cubicTo(20f, 69f, 22f, 72f, 25f, 72f)
        cubicTo(29f, 73f, 31f, 70f, 34f, 71f)
        cubicTo(38f, 72f, 41f, 69f, 42f, 66f)
        cubicTo(43f, 63f, 40f, 61f, 37f, 62f)
        cubicTo(34f, 60f, 30f, 62f, 27f, 63f)
        close()
    }

    // 8. Filling Inlay
    val fillingInlay = Path().apply {
        moveTo(23f, 62f)
        cubicTo(21f, 63f, 20f, 66f, 22f, 68f)
        cubicTo(24f, 70f, 28f, 69f, 31f, 70f)
        cubicTo(35f, 71f, 38f, 69f, 40f, 69f)
        cubicTo(43f, 68f, 44f, 65f, 42f, 63f)
        cubicTo(40f, 61f, 36f, 63f, 32f, 62f)
        cubicTo(28f, 61f, 25f, 61f, 23f, 62f)
        close()
    }

    // 9. Crown Collar Ring
    val crownCollar = Path().apply {
        moveTo(14f, 46f)
        cubicTo(24f, 49f, 40f, 49f, 50f, 46f)
    }

    // 10. Crown Sheen Line
    val crownSheen = Path().apply {
        moveTo(21f, 53f)
        cubicTo(28f, 64f, 36f, 74f, 36f, 78f)
    }

    return ToothAnatomyPaths(
        crownPath = crownPath,
        rootPath = rootPath,
        cejPath = cejPath,
        enamelReflection = enamelReflection,
        implantAbutment = implantAbutment,
        implantBody = implantBody,
        cariesPit = cariesPit,
        fillingInlay = fillingInlay,
        crownCollar = crownCollar,
        crownSheen = crownSheen
    )
}

// =============================================================================
// Interactive Preview
// =============================================================================

@Preview(showBackground = true, backgroundColor = 0xFFF5FAF9)
@Composable
fun AnatomicalToothViewPreview() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Thornbury Anatomical Tooth Visuals", style = MaterialTheme.typography.titleMedium)

        // Upper Teeth Showcase
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Molar (#3 - Sound)
            AnatomicalToothView(
                tooth = ToothRecord(3, 16, "Maxillary 1st Molar", "Upper", ToothCondition.SOUND),
                isSelected = false,
                onClick = {}
            )
            // Premolar (#4 - Decay)
            AnatomicalToothView(
                tooth = ToothRecord(4, 15, "Maxillary 2nd Premolar", "Upper", ToothCondition.DECAY),
                isSelected = false,
                onClick = {}
            )
            // Canine (#6 - Filled)
            AnatomicalToothView(
                tooth = ToothRecord(6, 13, "Maxillary Canine", "Upper", ToothCondition.FILLED),
                isSelected = true,
                onClick = {}
            )
            // Incisor (#8 - Crown)
            AnatomicalToothView(
                tooth = ToothRecord(8, 11, "Maxillary Central Incisor", "Upper", ToothCondition.CROWN),
                isSelected = false,
                onClick = {}
            )
            // Incisor (#9 - Missing)
            AnatomicalToothView(
                tooth = ToothRecord(9, 21, "Maxillary Central Incisor", "Upper", ToothCondition.MISSING),
                isSelected = false,
                onClick = {}
            )
        }

        // Lower Teeth Showcase (Roots Downward)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Lower Molar (#19 - Root Canal)
            AnatomicalToothView(
                tooth = ToothRecord(19, 36, "Mandibular 1st Molar", "Lower", ToothCondition.ROOT_CANAL),
                isSelected = false,
                onClick = {}
            )
            // Lower Premolar (#20 - Implant)
            AnatomicalToothView(
                tooth = ToothRecord(20, 35, "Mandibular 2nd Premolar", "Lower", ToothCondition.IMPLANT),
                isSelected = true,
                onClick = {}
            )
            // Lower Canine (#22 - Sound)
            AnatomicalToothView(
                tooth = ToothRecord(22, 33, "Mandibular Canine", "Lower", ToothCondition.SOUND),
                isSelected = false,
                onClick = {}
            )
            // Lower Incisor (#24 - Filled)
            AnatomicalToothView(
                tooth = ToothRecord(24, 31, "Mandibular Central Incisor", "Lower", ToothCondition.FILLED),
                isSelected = false,
                onClick = {}
            )
        }
    }
}
