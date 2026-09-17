package com.example.thornburydental.ui.clinic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thornburydental.data.ToothCondition
import com.example.thornburydental.data.ToothRecord
import com.example.thornburydental.theme.*
import com.example.thornburydental.ui.components.AnatomicalToothView

// =============================================================================
// Quadrant Filter Parsing Helper
// =============================================================================

/**
 * Parses a string into a [QuadrantFilter] enum value.
 */
fun parseQuadrantFilter(value: String?): QuadrantFilter {
    if (value.isNullOrBlank()) return QuadrantFilter.ALL
    return when (value.trim().uppercase()) {
        "LOWER", "MANDIBULAR" -> QuadrantFilter.LOWER
        "LOWER_LEFT", "LL", "Q3", "QUADRANT_3", "QUADRANT3" -> QuadrantFilter.LOWER_LEFT
        "LOWER_RIGHT", "LR", "Q4", "QUADRANT_4", "QUADRANT4" -> QuadrantFilter.LOWER_RIGHT
        "UPPER", "MAXILLARY" -> QuadrantFilter.UPPER
        "UPPER_RIGHT", "UR", "Q1", "QUADRANT_1", "QUADRANT1" -> QuadrantFilter.UPPER_RIGHT
        "UPPER_LEFT", "UL", "Q2", "QUADRANT_2", "QUADRANT2" -> QuadrantFilter.UPPER_LEFT
        else -> QuadrantFilter.ALL
    }
}

// =============================================================================
// Mandibular Tooth Metadata Helpers
// =============================================================================

private val MandibularToothNames = mapOf(
    17 to "Third Molar (Wisdom)",
    18 to "Second Molar",
    19 to "First Molar",
    20 to "Second Premolar",
    21 to "First Premolar",
    22 to "Canine (Cuspid)",
    23 to "Lateral Incisor",
    24 to "Central Incisor",
    25 to "Central Incisor",
    26 to "Lateral Incisor",
    27 to "Canine (Cuspid)",
    28 to "First Premolar",
    29 to "Second Premolar",
    30 to "First Molar",
    31 to "Second Molar",
    32 to "Third Molar (Wisdom)"
)

/**
 * Resolves a [ToothRecord] from the provided map or synthesizes a canonical fallback
 * with standard FDI and anatomical tooth naming for the mandibular arch.
 */
fun getMandibularToothRecord(teeth: Map<Int, ToothRecord>, number: Int): ToothRecord {
    return teeth[number] ?: run {
        val fdi = if (number in 17..24) {
            30 + (25 - number) // FDI 38 to 31
        } else {
            40 + (number - 24) // FDI 41 to 48
        }
        val toothName = MandibularToothNames[number] ?: "Tooth $number"
        ToothRecord(
            number = number,
            fdiNumber = fdi,
            name = "Mandibular $toothName",
            arch = "Mandibular (Lower Arch)",
            condition = ToothCondition.SOUND
        )
    }
}

fun defaultChildLowerToothRecord(fdi: Int): ToothRecord {
    val names = mapOf(
        75 to "Primary Mandibular Left Second Molar",
        74 to "Primary Mandibular Left First Molar",
        73 to "Primary Mandibular Left Canine",
        72 to "Primary Mandibular Left Lateral Incisor",
        71 to "Primary Mandibular Left Central Incisor",
        81 to "Primary Mandibular Right Central Incisor",
        82 to "Primary Mandibular Right Lateral Incisor",
        83 to "Primary Mandibular Right Canine",
        84 to "Primary Mandibular Right First Molar",
        85 to "Primary Mandibular Right Second Molar"
    )
    return ToothRecord(
        number = fdi,
        fdiNumber = fdi,
        name = names[fdi] ?: "Primary Mandibular Tooth $fdi",
        arch = "Mandibular (Lower Primary Arch)",
        condition = ToothCondition.SOUND
    )
}

// =============================================================================
// 1. Occlusal Plane Divider (Coronal Bite Line)
// =============================================================================

/**
 * Clean divider representing the coronal bite line between upper (Maxillary)
 * and lower (Mandibular) dental arches.
 *
 * Consists of horizontal divider lines flanking a centered clinical pill chip:
 * - Icon: [Icons.Default.SwapVert] tinted with [ThornburyAccentTeal]
 * - Text: "OCCLUSAL PLANE (CORONAL BITE LINE)"
 * - Styled with [ThornburySurfaceSoft] and [ThornburyHairline].
 */
@Composable
fun OcclusalPlaneDivider(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = ThornburyHairline
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ThornburySurfaceSoft,
            border = BorderStroke(1.dp, ThornburyHairline),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = null,
                    tint = ThornburyAccentTeal,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "OCCLUSAL PLANE (CORONAL BITE LINE)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        fontSize = 10.sp
                    ),
                    color = ThornburyInk
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = ThornburyHairline
        )
    }
}

// =============================================================================
// Pathology Summary Badge
// =============================================================================

@Composable
fun PathologySummaryBadge(
    flaggedCount: Int,
    modifier: Modifier = Modifier
) {
    val isAllSound = flaggedCount == 0
    val containerColor = if (isAllSound) ThornburySuccessWash else ThornburyWarningWash
    val contentColor = if (isAllSound) ThornburySuccess else ThornburyWarning
    val text = if (isAllSound) "All Sound" else "$flaggedCount Flagged"
    val icon = if (isAllSound) Icons.Default.CheckCircle else Icons.Default.Warning

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = contentColor
            )
        }
    }
}

// =============================================================================
// 2. Lower Left Quadrant Card (LL • Q3 / Q7)
// =============================================================================

/**
 * Mandibular Lower Left Quadrant Composable (LL • Q3 for Adult, LL • Q7 for Child).
 */
@Composable
fun LowerLeftQuadrantCard(
    teeth: Map<Int, ToothRecord>,
    selectedToothId: Int? = null,
    onToothClick: (ToothRecord) -> Unit,
    isChild: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isChildTeeth = isChild || teeth.keys.any { it in 51..85 }
    val quadrantTeeth = if (isChildTeeth) {
        val fdiNumbers = listOf(75, 74, 73, 72, 71)
        fdiNumbers.map { fdi -> teeth[fdi] ?: defaultChildLowerToothRecord(fdi) }
    } else {
        (17..24).map { getMandibularToothRecord(teeth, it) }
    }
    val flaggedCount = quadrantTeeth.count { it.condition != ToothCondition.SOUND }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Quadrant indicator + Range + Pathology Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ThornburyInfoWash,
                        border = BorderStroke(1.dp, ThornburyAccentTeal.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(ThornburyAccentTeal, CircleShape)
                            )
                            Text(
                                text = if (isChildTeeth) "Lower Left Primary (LL • Q7)" else "Lower Left (LL • Q3)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = ThornburyTertiaryText
                            )
                        }
                    }
                    Text(
                        text = if (isChildTeeth) "Teeth FDI 75–71 (5 Primary Teeth)" else "Teeth FDI 38–31 (8 Permanent Teeth)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = ThornburyMuted
                    )
                }

                PathologySummaryBadge(flaggedCount = flaggedCount)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal Scrollable Teeth Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                quadrantTeeth.forEach { tooth ->
                    AnatomicalToothView(
                        tooth = tooth,
                        isSelected = selectedToothId == tooth.number || selectedToothId == tooth.fdiNumber,
                        onClick = { onToothClick(tooth) }
                    )
                }
            }
        }
    }
}

// =============================================================================
// 3. Lower Right Quadrant Card (LR • Q4 / Q8)
// =============================================================================

/**
 * Mandibular Lower Right Quadrant Composable (LR • Q4 for Adult, LR • Q8 for Child).
 */
@Composable
fun LowerRightQuadrantCard(
    teeth: Map<Int, ToothRecord>,
    selectedToothId: Int? = null,
    onToothClick: (ToothRecord) -> Unit,
    isChild: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isChildTeeth = isChild || teeth.keys.any { it in 51..85 }
    val quadrantTeeth = if (isChildTeeth) {
        val fdiNumbers = listOf(81, 82, 83, 84, 85)
        fdiNumbers.map { fdi -> teeth[fdi] ?: defaultChildLowerToothRecord(fdi) }
    } else {
        (25..32).map { getMandibularToothRecord(teeth, it) }
    }
    val flaggedCount = quadrantTeeth.count { it.condition != ToothCondition.SOUND }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Quadrant indicator + Range + Pathology Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ThornburyInfoWash,
                        border = BorderStroke(1.dp, ThornburyAccentTeal.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(ThornburyAccentTeal, CircleShape)
                            )
                            Text(
                                text = if (isChildTeeth) "Lower Right Primary (LR • Q8)" else "Lower Right (LR • Q4)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = ThornburyTertiaryText
                            )
                        }
                    }
                    Text(
                        text = if (isChildTeeth) "Teeth FDI 81–85 (5 Primary Teeth)" else "Teeth FDI 41–48 (8 Permanent Teeth)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = ThornburyMuted
                    )
                }

                PathologySummaryBadge(flaggedCount = flaggedCount)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal Scrollable Teeth Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                quadrantTeeth.forEach { tooth ->
                    AnatomicalToothView(
                        tooth = tooth,
                        isSelected = selectedToothId == tooth.number || selectedToothId == tooth.fdiNumber,
                        onClick = { onToothClick(tooth) }
                    )
                }
            }
        }
    }
}

// =============================================================================
// 4. Mandibular Arch Container
// =============================================================================

/**
 * Mandibular Arch Container wrapping the lower dental arch.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MandibularArchContainer(
    teeth: Map<Int, ToothRecord>,
    selectedToothId: Int? = null,
    onToothClick: (ToothRecord) -> Unit,
    isChild: Boolean = false,
    modifier: Modifier = Modifier,
    activeFilter: QuadrantFilter = QuadrantFilter.ALL
) {
    val showLL = activeFilter == QuadrantFilter.ALL ||
            activeFilter == QuadrantFilter.LOWER ||
            activeFilter == QuadrantFilter.LOWER_LEFT

    val showLR = activeFilter == QuadrantFilter.ALL ||
            activeFilter == QuadrantFilter.LOWER ||
            activeFilter == QuadrantFilter.LOWER_RIGHT

    if (!showLL && !showLR) return

    val isChildTeeth = isChild || teeth.keys.any { it in 51..85 }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Arch Header Section
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(ThornburyAccentTeal, CircleShape)
                )
                Text(
                    text = if (isChildTeeth) "Mandibular Primary Arch (FDI 71–85 • 10 Teeth)" else "Mandibular Arch (FDI 31–48 • 16 Teeth)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = ThornburyInk
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ThornburySurfaceSoft,
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Text(
                    text = "Roots Point Down • Crown Points Up",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = ThornburyMuted,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        val isCompact = LocalWindowWidthSizeClass.current == WindowWidthSizeClass.Compact
        if (showLL && showLR && !isCompact) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    LowerLeftQuadrantCard(
                        teeth = teeth,
                        selectedToothId = selectedToothId,
                        onToothClick = onToothClick,
                        isChild = isChildTeeth
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    LowerRightQuadrantCard(
                        teeth = teeth,
                        selectedToothId = selectedToothId,
                        onToothClick = onToothClick,
                        isChild = isChildTeeth
                    )
                }
            }
        } else {
            // Lower Left Quadrant (LL • Q3 / Q7)
            if (showLL) {
                LowerLeftQuadrantCard(
                    teeth = teeth,
                    selectedToothId = selectedToothId,
                    onToothClick = onToothClick,
                    isChild = isChildTeeth
                )
            }

            if (showLL && showLR) {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Lower Right Quadrant (LR • Q4 / Q8)
            if (showLR) {
                LowerRightQuadrantCard(
                    teeth = teeth,
                    selectedToothId = selectedToothId,
                    onToothClick = onToothClick,
                    isChild = isChildTeeth
                )
            }
        }
    }
}

/**
 * Convenience overload of [MandibularArchContainer] accepting a string filter.
 */
@Composable
fun MandibularArchContainer(
    teeth: Map<Int, ToothRecord>,
    selectedToothId: Int? = null,
    onToothClick: (ToothRecord) -> Unit,
    isChild: Boolean = false,
    modifier: Modifier = Modifier,
    activeFilter: String
) {
    MandibularArchContainer(
        teeth = teeth,
        selectedToothId = selectedToothId,
        onToothClick = onToothClick,
        isChild = isChild,
        modifier = modifier,
        activeFilter = parseQuadrantFilter(activeFilter)
    )
}

/**
 * Convenience overload of [MandibularArchContainer] accepting a [Patient] model.
 */
@Composable
fun MandibularArchContainer(
    patient: com.example.thornburydental.data.Patient,
    selectedToothId: Int? = null,
    onToothClick: (ToothRecord) -> Unit,
    modifier: Modifier = Modifier,
    activeFilter: QuadrantFilter = QuadrantFilter.ALL
) {
    MandibularArchContainer(
        teeth = patient.teeth,
        selectedToothId = selectedToothId,
        onToothClick = onToothClick,
        isChild = patient.isChild,
        modifier = modifier,
        activeFilter = activeFilter
    )
}

// =============================================================================
// Compose Previews
// =============================================================================

@Preview(showBackground = true, backgroundColor = 0xFFF5FAF9)
@Composable
private fun OcclusalPlaneDividerPreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            OcclusalPlaneDivider()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5FAF9)
@Composable
private fun LowerLeftQuadrantCardPreview() {
    val sampleTeeth = (17..24).associateWith { num ->
        val fdi = 30 + (25 - num)
        val condition = if (num == 19) ToothCondition.ROOT_CANAL else ToothCondition.SOUND
        ToothRecord(
            number = num,
            fdiNumber = fdi,
            name = "Mandibular Tooth $num",
            arch = "Mandibular (Lower Arch)",
            condition = condition
        )
    }

    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            LowerLeftQuadrantCard(
                teeth = sampleTeeth,
                selectedToothId = 19,
                onToothClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5FAF9)
@Composable
private fun LowerRightQuadrantCardPreview() {
    val sampleTeeth = (25..32).associateWith { num ->
        val fdi = 40 + (num - 24)
        val condition = if (num == 30) ToothCondition.DECAY else ToothCondition.SOUND
        ToothRecord(
            number = num,
            fdiNumber = fdi,
            name = "Mandibular Tooth $num",
            arch = "Mandibular (Lower Arch)",
            condition = condition
        )
    }

    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            LowerRightQuadrantCard(
                teeth = sampleTeeth,
                selectedToothId = 30,
                onToothClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5FAF9)
@Composable
private fun MandibularArchContainerPreview() {
    val sampleTeeth = (17..32).associateWith { num ->
        val fdi = if (num <= 24) 30 + (25 - num) else 40 + (num - 24)
        val condition = when (num) {
            19 -> ToothCondition.ROOT_CANAL
            20 -> ToothCondition.FILLED
            30 -> ToothCondition.DECAY
            31 -> ToothCondition.CROWN
            else -> ToothCondition.SOUND
        }
        ToothRecord(
            number = num,
            fdiNumber = fdi,
            name = "Mandibular Tooth $num",
            arch = "Mandibular (Lower Arch)",
            condition = condition
        )
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OcclusalPlaneDivider()
            MandibularArchContainer(
                teeth = sampleTeeth,
                selectedToothId = 19,
                onToothClick = {}
            )
        }
    }
}

