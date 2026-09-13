package com.example.thornburydental.ui.publicsite

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.theme.*
import com.example.thornburydental.ui.components.DentalModelView

@Composable
fun PracticeHomeScreen(
    onNavigateToClinic: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ThornburyCanvas)
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        // --- Top Practice Brand Bar ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ThornburyCanvas,
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(ThornburyPrimary, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = "Thornbury Dental Brand",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Thornbury Dental",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            ),
                            color = ThornburyInk
                        )
                        Text(
                            text = "Portland, Oregon",
                            style = MaterialTheme.typography.labelSmall,
                            color = ThornburyMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = ThornburySurfaceSoft,
                    border = BorderStroke(1.dp, ThornburyHairline)
                ) {
                    Text(
                        text = "Clinical Practice",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = ThornburyPrimaryText
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(24.dp))

            // --- Hero Section ---
            Text(
                text = "Dentistry with the instructions written down.",
                style = MaterialTheme.typography.displayMedium,
                color = ThornburyInk
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "A practice in Portland treating gum disease, root canals and implants. To arrange a visit, ring the reception directly.",
                style = MaterialTheme.typography.bodyLarge,
                color = ThornburyBody
            )

            Spacer(modifier = Modifier.height(20.dp))

            // --- Emergency / Phone Call Action Banner ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:+15032247700")
                        }
                        context.startActivity(intent)
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ThornburySurfaceDark),
                border = BorderStroke(1.dp, ThornburySurfaceDarkElevated)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(ThornburySuccess, RoundedCornerShape(9999.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TWO SLOTS HELD OPEN TODAY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ThornburyAccentTeal
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "+1 (503) 224-7700",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = ThornburyOnDark
                        )
                        Text(
                            text = "Tap to call practice directly",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyOnDarkSoft
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(ThornburyAccent, RoundedCornerShape(9999.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call practice",
                            tint = ThornburyOnAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // --- Interactive Dental Model Component ---
            DentalModelView()

            Spacer(modifier = Modifier.height(32.dp))

            // --- The Care Pathway (Four Moves) ---
            Text(
                text = "The Care Pathway",
                style = MaterialTheme.typography.headlineLarge,
                color = ThornburyInk
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Four moves, and you always know which one you are in. Most dental anxiety is uncertainty — we remove that by writing it down before, during and after.",
                style = MaterialTheme.typography.bodyMedium,
                color = ThornburyMuted
            )

            Spacer(modifier = Modifier.height(16.dp))

            val pathway = listOf(
                PathwayStep("1. Book", "Ring the practice and we will find a time with the clinician you want to see.", Icons.Default.CalendarToday),
                PathwayStep("2. Prepare", "You get your preparation notes in writing before the appointment, not at the door.", Icons.Default.Description),
                PathwayStep("3. Treat", "Allergies and current medicines are checked on screen before anything is prescribed.", Icons.Default.HealthAndSafety),
                PathwayStep("4. Recover", "Written aftercare, and a direct line to call if something changes.", Icons.Default.CheckCircle)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                pathway.forEach { step ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ThornburySurfaceSoft),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(ThornburySurfaceCard, RoundedCornerShape(8.dp))
                                    .border(1.dp, ThornburyHairline, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = step.icon,
                                    contentDescription = null,
                                    tint = ThornburyPrimaryText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = step.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyInk
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = step.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThornburyBody
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Meet the Clinicians ---
            Text(
                text = "Three Clinicians",
                style = MaterialTheme.typography.headlineLarge,
                color = ThornburyInk
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Continuity is a clinical safety feature, not a courtesy. Your record follows you, and so does the person reading it.",
                style = MaterialTheme.typography.bodyMedium,
                color = ThornburyMuted
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DentalRepository.clinicians.forEach { clinician ->
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
                                Text(
                                    text = clinician.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = ThornburyInk
                                )
                                Surface(
                                    shape = RoundedCornerShape(9999.dp),
                                    color = ThornburySurfaceSoft,
                                    border = BorderStroke(1.dp, ThornburyHairline)
                                ) {
                                    Text(
                                        text = clinician.room,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ThornburyPrimaryText
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "${clinician.credentials} • ${clinician.specialty}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = ThornburyPrimaryText
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = clinician.bio,
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyBody
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Hours and Address ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ThornburySurfaceSoft),
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Practice Hours & Location",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HourRow("Monday – Thursday", "08:00 – 18:00")
                    HourRow("Friday", "08:00 – 15:30")
                    HourRow("Saturday", "Emergencies only")

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = ThornburyHairline)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = ThornburyPrimaryText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "18 Thornbury Row, Portland, Oregon 97210",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyBodyStrong
                        )
                    }
                }
            }
        }
    }
}

private data class PathwayStep(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
private fun HourRow(days: String, hours: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = days, style = MaterialTheme.typography.bodySmall, color = ThornburyBody)
        Text(
            text = hours,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = ThornburyInk
        )
    }
}
