package com.example.thornburydental.ui.clinic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thornburydental.data.Patient
import com.example.thornburydental.data.ToothCondition
import com.example.thornburydental.data.ToothRecord
import com.example.thornburydental.theme.*
import com.example.thornburydental.ui.components.AnatomicalToothView

// =============================================================================
// 1. Quadrant Filter Enum
// =============================================================================

/**
 * Filter mode for dental odontogram examination.
 * Allows switching between full-mouth inspection, individual arches, or specific quadrants.
 */
enum class QuadrantFilter(val label: String, val badge: String) {
    ALL("Full Mouth", "4 Quads"),
    UPPER("Upper Arch", "UR + UL"),
    LOWER("Lower Arch", "LL + LR"),
    UPPER_RIGHT("Upper Right", "UR 1-8"),
    UPPER_LEFT("Upper Left", "UL 9-16"),
    LOWER_LEFT("Lower Left", "LL 17-24"),
    LOWER_RIGHT("Lower Right", "LR 25-32");

    companion object {
        fun fromString(value: String?): QuadrantFilter {
            if (value.isNullOrBlank()) return ALL
            return when (value.trim().uppercase()) {
                "ALL", "FULL_MOUTH", "FULL MOUTH", "FULL" -> ALL
                "UPPER", "MAXILLARY" -> UPPER
                "LOWER", "MANDIBULAR" -> LOWER
                "UPPER_RIGHT", "UR", "Q1", "QUADRANT_1", "QUADRANT1" -> UPPER_RIGHT
                "UPPER_LEFT", "UL", "Q2", "QUADRANT_2", "QUADRANT2" -> UPPER_LEFT
                "LOWER_LEFT", "LL", "Q3", "QUADRANT_3", "QUADRANT3" -> LOWER_LEFT
                "LOWER_RIGHT", "LR", "Q4", "QUADRANT_4", "QUADRANT4" -> LOWER_RIGHT
                else -> values().firstOrNull {
                    it.name.equals(value, ignoreCase = true) ||
                    it.label.equals(value, ignoreCase = true) ||
                    it.badge.equals(value, ignoreCase = true)
                } ?: ALL
            }
        }
    }
}

// =============================================================================
// Helper: Default Maxillary Tooth Record Generator
// =============================================================================

fun defaultUpperToothRecord(number: Int): ToothRecord {
    val fdi = if (number <= 8) 19 - number else if (number <= 16) 20 + (number - 8) else number
    val namesUpper = listOf(
        "Third Molar (Wisdom)", "Second Molar", "First Molar", "Second Premolar",
        "First Premolar", "Canine (Cuspid)", "Lateral Incisor", "Central Incisor",
        "Central Incisor", "Lateral Incisor", "Canine (Cuspid)", "First Premolar",
        "Second Premolar", "First Molar", "Second Molar", "Third Molar (Wisdom)"
    )
    val name = if (number in 1..16) {
        "Maxillary " + (if (number <= 8) "Right " else "Left ") + namesUpper[number - 1]
    } else {
        "Maxillary Tooth #$number"
    }
    return ToothRecord(
        number = number,
        fdiNumber = fdi,
        name = name,
        arch = "Maxillary (Upper Arch)",
        condition = ToothCondition.SOUND
    )
}

fun defaultChildUpperToothRecord(fdi: Int): ToothRecord {
    val names = mapOf(
        55 to "Primary Maxillary Right Second Molar",
        54 to "Primary Maxillary Right First Molar",
        53 to "Primary Maxillary Right Canine",
        52 to "Primary Maxillary Right Lateral Incisor",
        51 to "Primary Maxillary Right Central Incisor",
        61 to "Primary Maxillary Left Central Incisor",
        62 to "Primary Maxillary Left Lateral Incisor",
        63 to "Primary Maxillary Left Canine",
        64 to "Primary Maxillary Left First Molar",
        65 to "Primary Maxillary Left Second Molar"
    )
    return ToothRecord(
        number = fdi,
        fdiNumber = fdi,
        name = names[fdi] ?: "Primary Maxillary Tooth $fdi",
        arch = "Maxillary (Upper Primary Arch)",
        condition = ToothCondition.SOUND
    )
}

// =============================================================================
// Helper: Quadrant Pathology Summary Badge
// =============================================================================

/**
 * Clinical badge displaying pathology summary:
 * - "All Sound" in [ThornburySuccessWash] / [ThornburySuccess] if all teeth are sound.
 * - "N Flagged" in [ThornburyErrorWash] / [ThornburyError] if active caries/decay is present.
 * - "N Flagged" in [ThornburyWarningWash] / [ThornburyWarning] for other restorative conditions.
 */
@Composable
fun QuadrantPathologyBadge(
    teeth: List<ToothRecord>,
    modifier: Modifier = Modifier
) {
    val nonSoundTeeth = remember(teeth) { teeth.filter { it.condition != ToothCondition.SOUND } }
    val count = nonSoundTeeth.size
    val hasDecay = remember(nonSoundTeeth) { nonSoundTeeth.any { it.condition == ToothCondition.DECAY } }

    val isAllSound = count == 0
    val bgColor = when {
        isAllSound -> ThornburySuccessWash
        hasDecay -> ThornburyErrorWash
        else -> ThornburyWarningWash
    }
    val textColor = when {
        isAllSound -> ThornburySuccess
        hasDecay -> ThornburyError
        else -> ThornburyWarning
    }
    val borderColor = textColor.copy(alpha = 0.35f)
    val label = if (isAllSound) "All Sound" else "$count Flagged"

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(textColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = textColor
            )
        }
    }
}

// =============================================================================
// 2. Quadrant Filter Row Composable
// =============================================================================

/**
 * Horizontally scrollable row of clinical filter chips for switching between
 * ALL arches, UPPER, LOWER, or individual quadrants.
 */
@Composable
fun QuadrantFilterRow(
    selectedFilter: QuadrantFilter,
    onFilterSelected: (QuadrantFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuadrantFilter.values().forEach { filter ->
            val isSelected = filter == selectedFilter
            Surface(
                modifier = Modifier
                    .clickable { onFilterSelected(filter) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) ThornburyPrimary else ThornburySurfaceSoft,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) ThornburyPrimaryActive else ThornburyHairline
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) Color.White else ThornburyInk
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color.White.copy(alpha = 0.22f) else ThornburyCanvas,
                        border = if (isSelected) null else BorderStroke(1.dp, ThornburyHairlineSoft)
                    ) {
                        Text(
                            text = filter.badge,
                            style = ClinicalCodeStyle.copy(
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (isSelected) Color.White else ThornburyMuted,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// 3. Upper Right Quadrant Card Composable (UR • Q1)
// =============================================================================

/**
 * Upper Right Quadrant Card covering Teeth 1 to 8 (FDI 18 to 11).
 * Features:
 * - Thornbury theme styling (ThornburyCanvas, ThornburyHairline, ThornburyPrimary accent).
 * - Header with:
 *   - Quadrant indicator badge: "Upper Right (UR • Q1)"
 *   - Subtitle / range: "Teeth 1–8 (FDI 18–11)"
 *   - Pathology summary badge ("All Sound" in ThornburySuccess or "N Flagged" in ThornburyWarning).
 * - Horizontal scrollable teeth strip rendering [AnatomicalToothView] for teeth 1..8 with selection state and click handler.
 */
@Composable
fun UpperRightQuadrantCard(
    teeth: Map<Int, ToothRecord>,
    selectedToothId: Int? = null,
    onToothClick: (ToothRecord) -> Unit,
    isChild: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isChildTeeth = isChild || teeth.keys.any { it in 51..85 }
    val quadrantTeeth = remember(teeth, isChildTeeth) {
        if (isChildTeeth) {
            val fdiNumbers = listOf(55, 54, 53, 52, 51)
            fdiNumbers.map { fdi -> teeth[fdi] ?: defaultChildUpperToothRecord(fdi) }
        } else {
            val fdiNumbers = listOf(18, 17, 16, 15, 14, 13, 12, 11)
            val universalNumbers = (1..8).toList()
            fdiNumbers.mapIndexed { idx, fdi ->
                teeth[fdi] ?: teeth[universalNumbers[idx]] ?: defaultUpperToothRecord(universalNumbers[idx])
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(28.dp)
                            .background(ThornburyPrimary, RoundedCornerShape(2.dp))
                    )
                    Column {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ThornburyPrimaryWash,
                            border = BorderStroke(1.dp, ThornburyPrimary.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = if (isChildTeeth) "Upper Right Primary (UR • Q5)" else "Upper Right (UR • Q1)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ThornburyPrimaryText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isChildTeeth) "Teeth FDI 55–51 (5 Primary Teeth)" else "Teeth FDI 18–11 (8 Permanent Teeth)",
                            style = ClinicalCodeStyle.copy(
                                fontSize = 11.sp,
                                color = ThornburyMuted
                            )
                        )
                    }
                }

                // Pathology summary badge
                QuadrantPathologyBadge(teeth = quadrantTeeth)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Horizontal Scrollable Teeth Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (tooth in quadrantTeeth) {
                    AnatomicalToothView(
                        tooth = tooth,
                        isSelected = selectedToothId == tooth.number || selectedToothId == tooth.fdiNumber,
                        onClick = { onToothClick(tooth) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Anatomical Orientation Guide
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isChildTeeth) "◀ Distal (FDI 55)" else "◀ Distal (FDI 18)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = ThornburyMutedSoft
                )
                Text(
                    text = if (isChildTeeth) "Mesial (FDI 51) ▶ Midline" else "Mesial (FDI 11) ▶ Midline",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = ThornburyMutedSoft
                )
            }
        }
    }
}

/**
 * Overload accepting [Patient] directly for convenience.
 */
@Composable
fun UpperRightQuadrantCard(
    patient: Patient,
    selectedToothId: Int? = null,
    onToothClick: (ToothRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    UpperRightQuadrantCard(
        teeth = patient.teeth,
        selectedToothId = selectedToothId,
        onToothClick = onToothClick,
        isChild = patient.isChild,
        modifier = modifier
    )
}

// =============================================================================
// 4. Upper Left Quadrant Card Composable (UL • Q2 / Q6)
// =============================================================================

@Composable
fun UpperLeftQuadrantCard(
    teeth: Map<Int, ToothRecord>,
    selectedToothId: Int? = null,
    onToothClick: (ToothRecord) -> Unit,
    isChild: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isChildTeeth = isChild || teeth.keys.any { it in 51..85 }
    val quadrantTeeth = remember(teeth, isChildTeeth) {
        if (isChildTeeth) {
            val fdiNumbers = listOf(61, 62, 63, 64, 65)
            fdiNumbers.map { fdi -> teeth[fdi] ?: defaultChildUpperToothRecord(fdi) }
        } else {
            val fdiNumbers = listOf(21, 22, 23, 24, 25, 26, 27, 28)
            val universalNumbers = (9..16).toList()
            fdiNumbers.mapIndexed { idx, fdi ->
                teeth[fdi] ?: teeth[universalNumbers[idx]] ?: defaultUpperToothRecord(universalNumbers[idx])
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(28.dp)
                            .background(ThornburyPrimary, RoundedCornerShape(2.dp))
                    )
                    Column {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ThornburyPrimaryWash,
                            border = BorderStroke(1.dp, ThornburyPrimary.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = if (isChildTeeth) "Upper Left Primary (UL • Q6)" else "Upper Left (UL • Q2)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ThornburyPrimaryText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isChildTeeth) "Teeth FDI 61–65 (5 Primary Teeth)" else "Teeth FDI 21–28 (8 Permanent Teeth)",
                            style = ClinicalCodeStyle.copy(
                                fontSize = 11.sp,
                                color = ThornburyMuted
                            )
                        )
                    }
                }

                // Pathology summary badge
                QuadrantPathologyBadge(teeth = quadrantTeeth)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Horizontal Scrollable Teeth Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (tooth in quadrantTeeth) {
                    AnatomicalToothView(
                        tooth = tooth,
                        isSelected = selectedToothId == tooth.number || selectedToothId == tooth.fdiNumber,
                        onClick = { onToothClick(tooth) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Anatomical Orientation Guide
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isChildTeeth) "Midline ◀ Mesial (FDI 61)" else "Midline ◀ Mesial (FDI 21)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = ThornburyMutedSoft
                )
                Text(
                    text = if (isChildTeeth) "Distal (FDI 65) ▶" else "Distal (FDI 28) ▶",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = ThornburyMutedSoft
                )
            }
        }
    }
}

/**
 * Overload accepting [Patient] directly for convenience.
 */
@Composable
fun UpperLeftQuadrantCard(
    patient: Patient,
    selectedToothId: Int? = null,
    onToothClick: (ToothRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    UpperLeftQuadrantCard(
        teeth = patient.teeth,
        selectedToothId = selectedToothId,
        onToothClick = onToothClick,
        isChild = patient.isChild,
        modifier = modifier
    )
}

// =============================================================================
// 5. Maxillary Arch Container Composable
// =============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MaxillaryArchContainer(
    teeth: Map<Int, ToothRecord>,
    activeFilter: QuadrantFilter = QuadrantFilter.ALL,
    selectedToothId: Int? = null,
    onToothClick: (ToothRecord) -> Unit,
    isChild: Boolean = false,
    modifier: Modifier = Modifier
) {
    val showUpperRight = activeFilter in listOf(
        QuadrantFilter.ALL,
        QuadrantFilter.UPPER,
        QuadrantFilter.UPPER_RIGHT
    )
    val showUpperLeft = activeFilter in listOf(
        QuadrantFilter.ALL,
        QuadrantFilter.UPPER,
        QuadrantFilter.UPPER_LEFT
    )

    if (!showUpperRight && !showUpperLeft) return

    val isChildTeeth = isChild || teeth.keys.any { it in 51..85 }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Arch Main Header
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(ThornburyPrimary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isChildTeeth) "Maxillary Primary Arch (FDI 51–65 • 10 Teeth)" else "Maxillary Arch (FDI 11–28 • 16 Teeth)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = ThornburyInk
                    )
                }

                // Anatomical Orientation Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ThornburyPrimaryWash,
                    border = BorderStroke(1.dp, ThornburyPrimary.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "Roots Point Up • Crown Points Down",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = ThornburyPrimaryText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val isCompact = LocalWindowWidthSizeClass.current == WindowWidthSizeClass.Compact
            if (showUpperRight && showUpperLeft && !isCompact) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        UpperRightQuadrantCard(
                            teeth = teeth,
                            selectedToothId = selectedToothId,
                            onToothClick = onToothClick,
                            isChild = isChildTeeth
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        UpperLeftQuadrantCard(
                            teeth = teeth,
                            selectedToothId = selectedToothId,
                            onToothClick = onToothClick,
                            isChild = isChildTeeth
                        )
                    }
                }
            } else {
                if (showUpperRight) {
                    UpperRightQuadrantCard(
                        teeth = teeth,
                        selectedToothId = selectedToothId,
                        onToothClick = onToothClick,
                        isChild = isChildTeeth
                    )
                }

                if (showUpperRight && showUpperLeft) {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (showUpperLeft) {
                    UpperLeftQuadrantCard(
                        teeth = teeth,
                        selectedToothId = selectedToothId,
                        onToothClick = onToothClick,
                        isChild = isChildTeeth
                    )
                }
            }
        }
    }
}

/**
 * Overload accepting [Patient] directly for convenience.
 */
@Composable
fun MaxillaryArchContainer(
    patient: Patient,
    activeFilter: QuadrantFilter = QuadrantFilter.ALL,
    selectedToothId: Int? = null,
    onToothClick: (ToothRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    MaxillaryArchContainer(
        teeth = patient.teeth,
        activeFilter = activeFilter,
        selectedToothId = selectedToothId,
        onToothClick = onToothClick,
        isChild = patient.isChild,
        modifier = modifier
    )
}

// =============================================================================
// Previews
// =============================================================================

@Preview(showBackground = true, backgroundColor = 0xFFF5FAF9)
@Composable
fun MaxillaryQuadrantViewsPreview() {
    ThornburyDentalTheme {
        var filter by remember { mutableStateOf(QuadrantFilter.ALL) }
        var selectedId by remember { mutableStateOf<Int?>(3) }
        val sampleTeeth = remember {
            (1..16).associateWith { num ->
                val cond = when (num) {
                    3 -> ToothCondition.FILLED
                    6 -> ToothCondition.CROWN
                    14 -> ToothCondition.DECAY
                    else -> ToothCondition.SOUND
                }
                defaultUpperToothRecord(num).copy(condition = cond)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            QuadrantFilterRow(
                selectedFilter = filter,
                onFilterSelected = { filter = it }
            )

            MaxillaryArchContainer(
                teeth = sampleTeeth,
                activeFilter = filter,
                selectedToothId = selectedId,
                onToothClick = { selectedId = it.number }
            )
        }
    }
}
