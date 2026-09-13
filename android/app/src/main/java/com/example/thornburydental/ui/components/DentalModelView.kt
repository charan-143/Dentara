package com.example.thornburydental.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thornburydental.theme.*

enum class DentalLayer(val title: String, val subtitle: String, val description: String, val color: Color) {
    ENAMEL(
        title = "Enamel",
        subtitle = "Outer Protective Shield",
        description = "96% mineralised crystalline hydroxyapatite. Protects against masticatory wear and acid erosion.",
        color = Color(0xFFF0EBE1)
    ),
    DENTIN(
        title = "Dentin",
        subtitle = "Vital Elastic Cushion",
        description = "70% inorganic matrix perforated by millions of microscopic tubules communicating thermal changes to the nerve.",
        color = Color(0xFFE8C888)
    ),
    PULP(
        title = "Dental Pulp",
        subtitle = "Neurovascular Core",
        description = "Living core containing sensory nerves, arterioles, and odontoblasts responsible for tooth vitality and tertiary dentin defense.",
        color = ThornburyPrimary
    ),
    PERIODONTIUM(
        title = "Periodontium & Root",
        subtitle = "Anchor & Alveolar Bone",
        description = "Cementum, periodontal ligament fibers (PDL), and alveolar bone absorbing occlusal shock under mastication.",
        color = ThornburyAccentTeal
    )
}

@Composable
fun DentalModelView(
    modifier: Modifier = Modifier
) {
    var selectedLayer by remember { mutableStateOf(DentalLayer.PULP) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ThornburySurfaceCard
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(ThornburyHairline)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Interactive Anatomy Model",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif
                    ),
                    color = ThornburyInk
                )
                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = ThornburySurfaceSoft
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = ThornburyPrimaryText,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tap layers to inspect",
                            style = MaterialTheme.typography.labelSmall,
                            color = ThornburyPrimaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas drawing of anatomical molar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(ThornburyCanvas, RoundedCornerShape(8.dp))
                    .border(1.dp, ThornburyHairlineSoft, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                val animatedColor by animateColorAsState(
                    targetValue = selectedLayer.color,
                    animationSpec = tween(400),
                    label = "layerColor"
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f

                    // 1. Draw Alveolar Bone / Periodontium outline
                    val boneLevel = cy + 15.dp.toPx()
                    drawRect(
                        color = if (selectedLayer == DentalLayer.PERIODONTIUM) ThornburyAccentTeal.copy(alpha = 0.25f) else Color(0xFFEAD8C4).copy(alpha = 0.35f),
                        topLeft = Offset(cx - 100.dp.toPx(), boneLevel),
                        size = Size(200.dp.toPx(), h - boneLevel)
                    )
                    drawLine(
                        color = if (selectedLayer == DentalLayer.PERIODONTIUM) ThornburyAccentTeal else ThornburyMutedSoft,
                        start = Offset(cx - 100.dp.toPx(), boneLevel),
                        end = Offset(cx + 100.dp.toPx(), boneLevel),
                        strokeWidth = 2.dp.toPx()
                    )

                    // 2. Crown & Root Outer Contour (Enamel)
                    val outerPath = Path().apply {
                        moveTo(cx - 50.dp.toPx(), cy - 40.dp.toPx())
                        // Occlusal cusps
                        cubicTo(cx - 40.dp.toPx(), cy - 65.dp.toPx(), cx - 15.dp.toPx(), cy - 50.dp.toPx(), cx - 10.dp.toPx(), cy - 60.dp.toPx())
                        cubicTo(cx + 5.dp.toPx(), cy - 50.dp.toPx(), cx + 35.dp.toPx(), cy - 65.dp.toPx(), cx + 50.dp.toPx(), cy - 40.dp.toPx())
                        // Crown to CEJ (cervical line)
                        cubicTo(cx + 52.dp.toPx(), cy, cx + 45.dp.toPx(), boneLevel, cx + 38.dp.toPx(), boneLevel)
                        // Distal Root
                        cubicTo(cx + 42.dp.toPx(), cy + 65.dp.toPx(), cx + 22.dp.toPx(), cy + 85.dp.toPx(), cx + 18.dp.toPx(), cy + 88.dp.toPx())
                        cubicTo(cx + 14.dp.toPx(), cy + 85.dp.toPx(), cx + 10.dp.toPx(), cy + 50.dp.toPx(), cx, cy + 30.dp.toPx()) // Furcation
                        // Mesial Root
                        cubicTo(cx - 10.dp.toPx(), cy + 50.dp.toPx(), cx - 14.dp.toPx(), cy + 85.dp.toPx(), cx - 18.dp.toPx(), cy + 88.dp.toPx())
                        cubicTo(cx - 22.dp.toPx(), cy + 85.dp.toPx(), cx - 42.dp.toPx(), cy + 65.dp.toPx(), cx - 38.dp.toPx(), boneLevel)
                        cubicTo(cx - 45.dp.toPx(), boneLevel, cx - 52.dp.toPx(), cy, cx - 50.dp.toPx(), cy - 40.dp.toPx())
                        close()
                    }

                    // Fill Enamel
                    drawPath(
                        path = outerPath,
                        color = if (selectedLayer == DentalLayer.ENAMEL) Color(0xFFF7F2EB) else Color(0xFFFAF7F2)
                    )
                    drawPath(
                        path = outerPath,
                        color = if (selectedLayer == DentalLayer.ENAMEL) ThornburyPrimary else ThornburyMutedSoft,
                        style = Stroke(width = if (selectedLayer == DentalLayer.ENAMEL) 3.dp.toPx() else 1.5.dp.toPx())
                    )

                    // 3. Dentin Core
                    val dentinPath = Path().apply {
                        moveTo(cx - 38.dp.toPx(), cy - 30.dp.toPx())
                        cubicTo(cx - 30.dp.toPx(), cy - 48.dp.toPx(), cx - 10.dp.toPx(), cy - 40.dp.toPx(), cx, cy - 45.dp.toPx())
                        cubicTo(cx + 10.dp.toPx(), cy - 40.dp.toPx(), cx + 30.dp.toPx(), cy - 48.dp.toPx(), cx + 38.dp.toPx(), cy - 30.dp.toPx())
                        cubicTo(cx + 38.dp.toPx(), cy, cx + 32.dp.toPx(), boneLevel, cx + 28.dp.toPx(), boneLevel)
                        // Distal canal
                        lineTo(cx + 16.dp.toPx(), cy + 80.dp.toPx())
                        lineTo(cx, cy + 28.dp.toPx()) // Furcation
                        // Mesial canal
                        lineTo(cx - 16.dp.toPx(), cy + 80.dp.toPx())
                        lineTo(cx - 28.dp.toPx(), boneLevel)
                        cubicTo(cx - 32.dp.toPx(), boneLevel, cx - 38.dp.toPx(), cy, cx - 38.dp.toPx(), cy - 30.dp.toPx())
                        close()
                    }

                    drawPath(
                        path = dentinPath,
                        color = if (selectedLayer == DentalLayer.DENTIN) Color(0xFFF2DDB0) else Color(0xFFF7EBD4)
                    )
                    drawPath(
                        path = dentinPath,
                        color = if (selectedLayer == DentalLayer.DENTIN) ThornburyPrimaryActive else ThornburyHairline,
                        style = Stroke(width = if (selectedLayer == DentalLayer.DENTIN) 2.5.dp.toPx() else 1.dp.toPx())
                    )

                    // 4. Pulp Chamber & Root Canals
                    val pulpPath = Path().apply {
                        moveTo(cx - 16.dp.toPx(), cy - 20.dp.toPx())
                        cubicTo(cx - 14.dp.toPx(), cy - 32.dp.toPx(), cx - 4.dp.toPx(), cy - 26.dp.toPx(), cx, cy - 28.dp.toPx())
                        cubicTo(cx + 4.dp.toPx(), cy - 26.dp.toPx(), cx + 14.dp.toPx(), cy - 32.dp.toPx(), cx + 16.dp.toPx(), cy - 20.dp.toPx())
                        cubicTo(cx + 14.dp.toPx(), cy, cx + 10.dp.toPx(), cy + 10.dp.toPx(), cx + 8.dp.toPx(), cy + 15.dp.toPx())
                        // Right canal
                        lineTo(cx + 14.dp.toPx(), cy + 78.dp.toPx())
                        lineTo(cx + 10.dp.toPx(), cy + 78.dp.toPx())
                        lineTo(cx + 4.dp.toPx(), cy + 20.dp.toPx())
                        lineTo(cx - 4.dp.toPx(), cy + 20.dp.toPx())
                        // Left canal
                        lineTo(cx - 10.dp.toPx(), cy + 78.dp.toPx())
                        lineTo(cx - 14.dp.toPx(), cy + 78.dp.toPx())
                        lineTo(cx - 8.dp.toPx(), cy + 15.dp.toPx())
                        close()
                    }

                    drawPath(
                        path = pulpPath,
                        color = if (selectedLayer == DentalLayer.PULP) ThornburyPrimary else ThornburyPrimary.copy(alpha = 0.5f)
                    )
                    drawPath(
                        path = pulpPath,
                        color = if (selectedLayer == DentalLayer.PULP) ThornburyPrimaryActive else ThornburyPrimaryDisabled,
                        style = Stroke(width = if (selectedLayer == DentalLayer.PULP) 2.dp.toPx() else 1.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Layer Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DentalLayer.values().forEach { layer ->
                    val isSelected = layer == selectedLayer
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedLayer = layer },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) ThornburyPrimary else ThornburySurfaceSoft,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) ThornburyPrimaryActive else ThornburyHairline
                        )
                    ) {
                        Text(
                            text = layer.title,
                            modifier = Modifier.padding(vertical = 8.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (isSelected) Color.White else ThornburyBody,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Explanation Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ThornburyCanvas
                ),
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(selectedLayer.color, RoundedCornerShape(9999.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${selectedLayer.title} - ${selectedLayer.subtitle}",
                            style = MaterialTheme.typography.titleSmall,
                            color = ThornburyInk
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedLayer.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = ThornburyBody
                    )
                }
            }
        }
    }
}
