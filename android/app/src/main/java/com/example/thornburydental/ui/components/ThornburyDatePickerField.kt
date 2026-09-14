package com.example.thornburydental.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thornburydental.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Reusable Date Input Field that supports:
 * 1. Typing the date manually with the keyboard.
 * 2. Selecting a date visually via a calendar popup dialog.
 * 3. Optional status - does not strictly enforce filling unless explicitly configured.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThornburyDatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Date",
    placeholder: String = "YYYY-MM-DD",
    modifier: Modifier = Modifier,
    isOptional: Boolean = true,
    helperText: String? = "Enter manually or select from calendar"
) {
    var showDatePickerDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    text = if (isOptional) "$label (Optional)" else label,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            placeholder = {
                Text(
                    text = placeholder,
                    color = ThornburyMuted
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = thornburyTextFieldColors(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                Row {
                    if (value.isNotBlank()) {
                        IconButton(
                            onClick = { onValueChange("") },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear date",
                                tint = ThornburyMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = { showDatePickerDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Open calendar picker",
                            tint = ThornburyPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        )

        if (!helperText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = helperText,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = ThornburyMuted,
                    fontSize = 11.sp
                ),
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState()

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            onValueChange(sdf.format(Date(millis)))
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text(
                        text = "Select Date",
                        color = ThornburyPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text(text = "Cancel", color = ThornburyBody)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = ThornburyCanvas
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    titleContentColor = ThornburyInk,
                    headlineContentColor = ThornburyInk,
                    weekdayContentColor = ThornburyMuted,
                    subheadContentColor = ThornburyMuted,
                    yearContentColor = ThornburyInk,
                    currentYearContentColor = ThornburyPrimary,
                    selectedYearContentColor = Color.White,
                    selectedYearContainerColor = ThornburyPrimary,
                    dayContentColor = ThornburyInk,
                    disabledDayContentColor = ThornburyMuted.copy(alpha = 0.3f),
                    selectedDayContentColor = Color.White,
                    selectedDayContainerColor = ThornburyPrimary,
                    todayDateBorderColor = ThornburyPrimary,
                    todayContentColor = ThornburyPrimary
                )
            )
        }
    }
}
