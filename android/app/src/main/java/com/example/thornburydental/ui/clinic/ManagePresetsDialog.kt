package com.example.thornburydental.ui.clinic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.MedicationPreset
import com.example.thornburydental.theme.*

@Composable
fun ManagePresetsDialog(
    onDismiss: () -> Unit
) {
    val presets by DentalRepository.medicationPresets.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    var presetToEdit by remember { mutableStateOf<MedicationPreset?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var presetToDelete by remember { mutableStateOf<MedicationPreset?>(null) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val categories = remember(presets) {
        listOf("All") + presets.map { it.category }.filter { it.isNotBlank() }.distinct()
    }

    val filteredPresets = remember(presets, searchQuery, selectedCategory) {
        presets.filter { preset ->
            val matchesCategory = selectedCategory == "All" || preset.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    preset.name.contains(searchQuery, ignoreCase = true) ||
                    preset.dosage.contains(searchQuery, ignoreCase = true) ||
                    preset.category.contains(searchQuery, ignoreCase = true) ||
                    preset.instructions.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .adaptiveDialogWidth(720.dp)
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
                            text = "Medication Presets",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                        Text(
                            text = "Manage dental formulary presets and custom medications",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyMuted
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = ThornburyInk)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search presets by drug or category...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = ThornburyMuted)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = ThornburyMuted)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = thornburyTextFieldColors(containerColor = ThornburyCanvas),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = cat.equals(selectedCategory, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = if (isSelected) ThornburyPrimary else ThornburySurfaceSoft,
                            border = BorderStroke(1.dp, if (isSelected) ThornburyPrimary else ThornburyHairline),
                            modifier = Modifier.clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSelected) Color.White else ThornburyInk,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Presets List
                if (filteredPresets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = null,
                                tint = ThornburyMuted,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No presets match \"$searchQuery\"" else "No medication presets found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ThornburyMuted
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredPresets, key = { it.id }) { preset ->
                            PresetCard(
                                preset = preset,
                                onEdit = { presetToEdit = preset },
                                onDelete = { presetToDelete = preset }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = ThornburyHairlineSoft)
                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showResetConfirmDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ThornburyHairline),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore Defaults", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThornburyPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add New Preset", fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // Create / Edit Modal
    if (showCreateDialog || presetToEdit != null) {
        PresetEditDialog(
            preset = presetToEdit,
            onDismiss = {
                showCreateDialog = false
                presetToEdit = null
            },
            onSave = { name, dosage, frequency, duration, instructions, category ->
                if (presetToEdit != null) {
                    val updated = presetToEdit!!.copy(
                        name = name,
                        dosage = dosage,
                        frequency = frequency,
                        duration = duration,
                        instructions = instructions,
                        category = category
                    )
                    DentalRepository.updateMedicationPreset(updated)
                } else {
                    DentalRepository.addMedicationPreset(
                        name = name,
                        dosage = dosage,
                        frequency = frequency,
                        duration = duration,
                        instructions = instructions,
                        category = category
                    )
                }
                showCreateDialog = false
                presetToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    presetToDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { presetToDelete = null },
            title = { Text("Delete Preset?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete the preset for \"${preset.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        DentalRepository.deleteMedicationPreset(preset.id)
                        presetToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThornburyError, contentColor = Color.White)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { presetToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Restore Standard Formulary?", fontWeight = FontWeight.Bold) },
            text = { Text("This will reset all medication presets back to the default standard dental formulary. Custom presets will be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        DentalRepository.resetMedicationPresetsToDefaults()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary, contentColor = Color.White)
                ) {
                    Text("Restore Defaults")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PresetCard(
    preset: MedicationPreset,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard),
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (preset.isCustom) ThornburyAccentAmber.copy(alpha = 0.18f) else ThornburyInfoWash
                    ) {
                        Text(
                            text = if (preset.isCustom) "Custom" else preset.category,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                            color = if (preset.isCustom) ThornburyAccentAmber else ThornburyPrimaryText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Preset",
                            tint = ThornburyPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Preset",
                            tint = ThornburyError,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = ThornburySurfaceSoft,
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Text(
                    text = preset.dosage,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = ThornburyInk,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${preset.frequency} • ${preset.duration}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = ThornburyBodyStrong
            )

            if (preset.instructions.isNotBlank()) {
                Text(
                    text = preset.instructions,
                    style = MaterialTheme.typography.bodySmall,
                    color = ThornburyMuted,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun PresetEditDialog(
    preset: MedicationPreset?,
    onDismiss: () -> Unit,
    onSave: (name: String, dosage: String, frequency: String, duration: String, instructions: String, category: String) -> Unit
) {
    var name by remember { mutableStateOf(preset?.name ?: "") }
    var dosage by remember { mutableStateOf(preset?.dosage ?: "") }
    var frequency by remember { mutableStateOf(preset?.frequency ?: "") }
    var duration by remember { mutableStateOf(preset?.duration ?: "") }
    var instructions by remember { mutableStateOf(preset?.instructions ?: "") }
    var category by remember { mutableStateOf(preset?.category ?: "Custom") }

    var isNameError by remember { mutableStateOf(false) }
    var isDosageError by remember { mutableStateOf(false) }
    var isFrequencyError by remember { mutableStateOf(false) }

    val categorySuggestions = listOf("Antibiotics", "Analgesics", "Antiseptics", "Anti-inflammatory", "Custom")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (preset != null) "Edit Preset" else "Add Medication Preset",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = ThornburyInk)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Form Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Medication Name
                    OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) isNameError = false
                    },
                    label = { Text("Medication Name *") },
                    placeholder = { Text("e.g. Augmentin / Azithromycin") },
                    isError = isNameError,
                    supportingText = if (isNameError) { { Text("Medication name is required") } } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = thornburyTextFieldColors(containerColor = ThornburyCanvas)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category chips
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categorySuggestions.forEach { cat ->
                        val isSelected = category.equals(cat, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = if (isSelected) ThornburyPrimaryWash else ThornburySurfaceSoft,
                            border = BorderStroke(1.dp, if (isSelected) ThornburyPrimary else ThornburyHairline),
                            modifier = Modifier.clickable { category = cat }
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = if (isSelected) ThornburyPrimaryText else ThornburyInk,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dosage
                OutlinedTextField(
                    value = dosage,
                    onValueChange = {
                        dosage = it
                        if (it.isNotBlank()) isDosageError = false
                    },
                    label = { Text("Dosage / Formulation *") },
                    placeholder = { Text("e.g. 625 mg tablets / 250 mg capsules") },
                    isError = isDosageError,
                    supportingText = if (isDosageError) { { Text("Dosage is required") } } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = thornburyTextFieldColors(containerColor = ThornburyCanvas)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Frequency
                OutlinedTextField(
                    value = frequency,
                    onValueChange = {
                        frequency = it
                        if (it.isNotBlank()) isFrequencyError = false
                    },
                    label = { Text("Frequency *") },
                    placeholder = { Text("e.g. 1 tablet twice daily / every 8 hours") },
                    isError = isFrequencyError,
                    supportingText = if (isFrequencyError) { { Text("Frequency is required") } } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = thornburyTextFieldColors(containerColor = ThornburyCanvas)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Duration
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Duration") },
                    placeholder = { Text("e.g. 5 days / 7 days") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = thornburyTextFieldColors(containerColor = ThornburyCanvas)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Instructions
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Instructions") },
                    placeholder = { Text("e.g. Take with water after meals. Finish entire course.") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = thornburyTextFieldColors(containerColor = ThornburyCanvas)
                )

                Spacer(modifier = Modifier.height(12.dp))
            } // End of scrollable form body

            Spacer(modifier = Modifier.height(16.dp))

            // Actions (pinned at bottom)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            var hasError = false
                            if (name.isBlank()) {
                                isNameError = true
                                hasError = true
                            }
                            if (dosage.isBlank()) {
                                isDosageError = true
                                hasError = true
                            }
                            if (frequency.isBlank()) {
                                isFrequencyError = true
                                hasError = true
                            }
                            if (!hasError) {
                                onSave(
                                    name.trim(),
                                    dosage.trim(),
                                    frequency.trim(),
                                    if (duration.isBlank()) "5 days" else duration.trim(),
                                    instructions.trim(),
                                    category.trim().ifBlank { "Custom" }
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.3f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThornburyPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(if (preset != null) "Update Preset" else "Save Preset")
                    }
                }
            }
        }
    }
}
