package com.example.thornburydental.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.UserProfilePreferences
import com.example.thornburydental.theme.*
import com.example.thornburydental.ui.components.ThornburyDatePickerField

/**
 * Minimal Clinician Setup Wizard - Only 4 essential questions (one per page)
 * to quickly configure the clinician's profile and operatory assignment.
 */
@Composable
fun OnboardingWizardScreen(
    onOnboardingFinished: () -> Unit,
    onCancelToWelcome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 4

    // Essential clinician setup states
    var clinicianName by remember { mutableStateOf("Dr. Ingrid Halvorsen") }
    var credentials by remember { mutableStateOf("BDS (Hons), MFDS RCSEd, MClinDent") }
    var specialty by remember { mutableStateOf("Periodontics & Microsurgery") }
    var surgeryRoom by remember { mutableStateOf("Surgery 1 (Main Surgical Operatory)") }
    var practiceDate by remember { mutableStateOf("") }

    // Intercept hardware/system back button
    BackHandler {
        if (currentStep > 0) {
            currentStep--
        } else {
            onCancelToWelcome()
        }
    }

    fun handleContinue() {
        if (currentStep < totalSteps - 1) {
            currentStep++
        } else {
            // Save clinician preferences to database
            val prefs = UserProfilePreferences(
                fullName = clinicianName.trim().ifBlank { "Dr. Dental Clinician" },
                pronouns = credentials.trim(),
                dob = practiceDate.trim(),
                phone = "",
                email = "",
                dentalGoals = listOf(specialty),
                anxietyLevel = specialty,
                comfortAmenities = emptyList(),
                anesthesiaPreference = "Standard Local Anesthetic",
                medicalAlerts = listOf("Room: $surgeryRoom"),
                lastVisit = practiceDate.trim().ifBlank { "Active" },
                schedulePreference = "Morning (8am - 12pm)",
                contactChannel = "Direct In-App Operatory Queue",
                additionalNotes = "Operatory: $surgeryRoom",
                isOnboardingCompleted = true
            )
            DentalRepository.completeOnboarding(prefs)
            onOnboardingFinished()
        }
    }

    Scaffold(
        containerColor = ThornburyCanvas,
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(
                color = ThornburyCanvas,
                border = BorderStroke(1.dp, ThornburyHairlineSoft)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                if (currentStep > 0) {
                                    currentStep--
                                } else {
                                    onCancelToWelcome()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = ThornburyInk
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Clinician Setup",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ThornburyInk
                                )
                            )
                            Text(
                                text = "Question ${currentStep + 1} of $totalSteps",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ThornburyPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        Box(modifier = Modifier.size(48.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { (currentStep + 1) / totalSteps.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape),
                        color = ThornburyPrimary,
                        trackColor = ThornburySurfaceSoft
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                color = ThornburyCanvas,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, ThornburyHairline)
                        ) {
                            Text("Back", color = ThornburyInk, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Button(
                        onClick = { handleContinue() },
                        modifier = Modifier
                            .weight(if (currentStep > 0) 2f else 1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThornburyPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (currentStep == totalSteps - 1) "Finish Setup & Enter Operatory" else "Continue",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (currentStep == totalSteps - 1) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "MinimalClinicianWizardTransition"
            ) { step ->
                when (step) {
                    0 -> StepClinicianIdentity(
                        name = clinicianName,
                        onNameChange = { clinicianName = it },
                        credentials = credentials,
                        onCredentialsChange = { credentials = it }
                    )
                    1 -> StepSpecialty(
                        selectedSpecialty = specialty,
                        onSelectSpecialty = { specialty = it }
                    )
                    2 -> StepSurgeryRoom(
                        selectedRoom = surgeryRoom,
                        onSelectRoom = { surgeryRoom = it }
                    )
                    3 -> StepPracticeDate(
                        date = practiceDate,
                        onDateChange = { practiceDate = it }
                    )
                }
            }
        }
    }
}

// =============================================================================
// Minimal Step Composables (Only 4 Questions)
// =============================================================================

@Composable
private fun ClinicianQuestionHeader(
    tag: String,
    title: String,
    subtitle: String
) {
    Text(
        text = tag.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            color = ThornburyPrimary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Light,
            fontSize = 26.sp,
            color = ThornburyInk,
            lineHeight = 32.sp
        )
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = ThornburyBody,
            lineHeight = 20.sp
        )
    )
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun StepClinicianIdentity(
    name: String,
    onNameChange: (String) -> Unit,
    credentials: String,
    onCredentialsChange: (String) -> Unit
) {
    Column {
        ClinicianQuestionHeader(
            tag = "Clinician Identity",
            title = "What is your clinical title & name?",
            subtitle = "This signs your digital treatment plans, prescriptions, and chart notes."
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Clinician Full Name") },
            placeholder = { Text("e.g. Dr. Ingrid Halvorsen") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = thornburyTextFieldColors(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedTextField(
            value = credentials,
            onValueChange = onCredentialsChange,
            label = { Text("Qualifications & Degrees (Optional)") },
            placeholder = { Text("e.g. BDS (Hons), MFDS RCSEd, MClinDent") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = thornburyTextFieldColors(),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun StepSpecialty(
    selectedSpecialty: String,
    onSelectSpecialty: (String) -> Unit
) {
    val specialties = listOf(
        "Periodontics & Microsurgery" to "Gum health, bone regeneration, recession and surgical implant maintenance",
        "Endodontics & Microscopy" to "Complex primary endodontics, retreatment and cracked-tooth surgery",
        "Restorative & Biomimetic Dentistry" to "Microscopic bonding, precision crowns, inlays and tooth preservation",
        "Implantology & Oral Surgery" to "Surgical implant fixtures, bone augmentation, and soft tissue grafting",
        "General & Preventive Dentistry" to "Comprehensive clinical examinations, hygiene therapy, and dental baseline"
    )

    Column {
        ClinicianQuestionHeader(
            tag = "Clinical Discipline",
            title = "What is your primary clinical discipline?",
            subtitle = "Customizes diagnostic templates and treatment plan presets."
        )

        specialties.forEach { (title, description) ->
            SingleSelectCard(
                title = title,
                description = description,
                isSelected = title == selectedSpecialty,
                onSelect = { onSelectSpecialty(title) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun StepSurgeryRoom(
    selectedRoom: String,
    onSelectRoom: (String) -> Unit
) {
    val rooms = listOf(
        "Surgery 1 (Main Surgical Operatory)" to "Equipped with high-magnification surgical ceiling microscope and surgical cart",
        "Surgery 2 (Endodontic Suite)" to "Specialized for root canal therapy with rotary motor and apex locator",
        "Surgery 3 (Restorative & Implant Surgery)" to "Designed for biomimetic bonding, intraoral scanning, and implant procedures",
        "Floating / Multi-Operatory" to "Practicing across multiple chairs and surgeries in the facility"
    )

    Column {
        ClinicianQuestionHeader(
            tag = "Surgery Assignment",
            title = "Which operatory room do you operate in?",
            subtitle = "Filters today's chairside queue and patient room callouts."
        )

        rooms.forEach { (title, description) ->
            SingleSelectCard(
                title = title,
                description = description,
                isSelected = title == selectedRoom,
                onSelect = { onSelectRoom(title) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun StepPracticeDate(
    date: String,
    onDateChange: (String) -> Unit
) {
    Column {
        ClinicianQuestionHeader(
            tag = "Practice Timeline",
            title = "Practice inception or registration date?",
            subtitle = "Optional. You can enter this manually or choose via the calendar picker toolbar."
        )

        ThornburyDatePickerField(
            value = date,
            onValueChange = onDateChange,
            label = "Registration / Inception Date",
            placeholder = "YYYY-MM-DD",
            isOptional = true,
            helperText = "Optional • Type manually or tap the calendar icon"
        )
    }
}

@Composable
private fun SingleSelectCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) ThornburyPrimary.copy(alpha = 0.08f) else Color.White,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) ThornburyPrimary else ThornburyHairlineSoft
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) ThornburyPrimary else ThornburySurfaceSoft),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) ThornburyPrimary else ThornburyInk
                    )
                )
                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ThornburyBody,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    )
                }
            }
        }
    }
}
