package com.example.thornburydental.ui.cds

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thornburydental.data.cds.PatientVitalsContext
import com.example.thornburydental.data.cds.SymptomItem
import com.example.thornburydental.theme.*

val availableSymptomsList = listOf(
    SymptomItem("s1", "Severe Spontaneous Toothache", "Maxillofacial & Dental"),
    SymptomItem("s2", "Lingering Thermal Sensitivity", "Maxillofacial & Dental"),
    SymptomItem("s3", "Facial Swelling / Submandibular Edema", "Maxillofacial & Dental", isRedFlagTrigger = true),
    SymptomItem("s4", "Trismus / Difficulty Opening Mouth", "Maxillofacial & Dental", isRedFlagTrigger = true),
    SymptomItem("s5", "Bleeding on Probing (BOP)", "Maxillofacial & Dental"),
    SymptomItem("s6", "Purulent Discharge / Pus Exudate", "Maxillofacial & Dental"),
    SymptomItem("s7", "Tooth Avulsion / Dental Trauma", "Maxillofacial & Dental"),
    SymptomItem("s8", "Interdental Papilla Necrosis", "Maxillofacial & Dental"),
    SymptomItem("s9", "Dysphagia / Difficulty Swallowing", "Airway & Swallowing", isRedFlagTrigger = true),
    SymptomItem("s10", "Stridor / Airway Compromise", "Airway & Swallowing", isRedFlagTrigger = true),
    SymptomItem("s11", "Floor of Mouth Elevation", "Airway & Swallowing", isRedFlagTrigger = true),
    SymptomItem("s12", "Fever (≥ 38.5°C)", "Systemic & Pain", isRedFlagTrigger = true),
    SymptomItem("s13", "Uncontrolled Post-Extraction Bleeding", "Systemic & Pain", isRedFlagTrigger = true),
    SymptomItem("s14", "Anaphylaxis / Lip-Throat Tightness", "Systemic & Pain", isRedFlagTrigger = true),
    SymptomItem("s15", "Throbbing Jaw Pain", "Maxillofacial & Dental")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SymptomPickerComponent(
    selectedSymptoms: List<SymptomItem>,
    onSymptomToggle: (SymptomItem) -> Unit,
    freeTextDescription: String,
    onFreeTextChange: (String) -> Unit,
    vitalsContext: PatientVitalsContext,
    onVitalsChange: (PatientVitalsContext) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showVitalsSection by remember { mutableStateOf(false) }

    val filteredSymptoms = remember(searchQuery) {
        if (searchQuery.isBlank()) availableSymptomsList
        else availableSymptomsList.filter { it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ThornburySurface),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Patient Symptoms & Clinical Input",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = ThornburyInk
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select structured clinical indicators or type narrative symptoms for offline evaluation.",
                style = MaterialTheme.typography.bodySmall,
                color = ThornburyMuted
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar for Symptoms
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search symptoms (e.g. Swelling, Trismus, Pain)...", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = ThornburyMuted) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThornburyPrimary,
                    unfocusedBorderColor = ThornburyHairline
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Symptom Chips Layout
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                filteredSymptoms.forEach { symptom ->
                    val isSelected = selectedSymptoms.any { it.id == symptom.id }
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSymptomToggle(symptom) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (symptom.isRedFlagTrigger) {
                                    Text("⚠️ ", fontSize = 11.sp)
                                }
                                Text(
                                    text = symptom.name,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            }
                        },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (symptom.isRedFlagTrigger) Color(0xFFFFEBEE) else ThornburyPrimaryWash,
                            selectedLabelColor = if (symptom.isRedFlagTrigger) Color(0xFFC62828) else ThornburyPrimaryText,
                            containerColor = ThornburyCanvas,
                            labelColor = ThornburyInk
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (symptom.isRedFlagTrigger) Color(0xFFEF9A9A) else ThornburyHairline,
                            selectedBorderColor = if (symptom.isRedFlagTrigger) Color(0xFFC62828) else ThornburyPrimary,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Free Text Narrative Input
            OutlinedTextField(
                value = freeTextDescription,
                onValueChange = onFreeTextChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Clinical Narrative & History (Optional)") },
                placeholder = { Text("e.g., Patient reports 3-day history of throbbing tooth #19 pain with progressive facial swelling and low-grade fever...") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThornburyPrimary,
                    unfocusedBorderColor = ThornburyHairline
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Expandable Patient Vitals & Demographics Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ThornburyCanvas)
                    .clickable { showVitalsSection = !showVitalsSection }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Optional Patient Vitals & Context (${if (vitalsContext.temperatureCelsius != null || vitalsContext.heartRateBpm != null) "Vitals Set" else "None"})",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyPrimaryText
                )
                Icon(
                    imageVector = if (showVitalsSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle Vitals",
                    tint = ThornburyPrimary
                )
            }

            AnimatedVisibility(visible = showVitalsSection) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = vitalsContext.ageYears?.toString() ?: "",
                            onValueChange = { onVitalsChange(vitalsContext.copy(ageYears = it.toIntOrNull())) },
                            label = { Text("Age (yrs)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = vitalsContext.painSeverity?.toString() ?: "",
                            onValueChange = { onVitalsChange(vitalsContext.copy(painSeverity = it.toIntOrNull())) },
                            label = { Text("Pain (1-10)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = vitalsContext.temperatureCelsius?.toString() ?: "",
                            onValueChange = { onVitalsChange(vitalsContext.copy(temperatureCelsius = it.toFloatOrNull())) },
                            label = { Text("Temp (°C)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = vitalsContext.bloodPressureSystolic?.toString() ?: "",
                            onValueChange = { onVitalsChange(vitalsContext.copy(bloodPressureSystolic = it.toIntOrNull())) },
                            label = { Text("Systolic BP", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = vitalsContext.heartRateBpm?.toString() ?: "",
                            onValueChange = { onVitalsChange(vitalsContext.copy(heartRateBpm = it.toIntOrNull())) },
                            label = { Text("Heart Rate (bpm)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}
