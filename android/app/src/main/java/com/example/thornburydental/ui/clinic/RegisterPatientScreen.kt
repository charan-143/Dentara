package com.example.thornburydental.ui.clinic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.Patient
import com.example.thornburydental.speech.AndroidSpeechRecognizerManager
import com.example.thornburydental.speech.ClinicalVoiceExtractor
import com.example.thornburydental.speech.DemographicsFinding
import com.example.thornburydental.speech.rememberMicrophonePermissionState
import com.example.thornburydental.theme.*
import com.example.thornburydental.ui.components.ThornburyDatePickerField
import com.example.thornburydental.util.ValidationUtils
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Dedicated Full-Page Patient Registration View.
 * Provides clean, spacious, and perfectly aligned section cards for onboarding new patients.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterPatientScreen(
    onBack: () -> Unit,
    onRegistered: (Patient) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var isChild by remember { mutableStateOf(false) }
    var selectedGender by remember { mutableStateOf("Female") }
    var dob by remember { mutableStateOf("") }
    var opNo by remember { mutableStateOf("OP-${(40000..49999).random()}") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var emergencyName by remember { mutableStateOf("") }
    var emergencyPhone by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showVoiceIntakeDialog by remember { mutableStateOf(false) }

    val genders = listOf("Female", "Male", "Other")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Register New Patient",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = ThornburyInk
                        )
                        Text(
                            text = "Create electronic health record & assign OP number",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ThornburyInk
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { showVoiceIntakeDialog = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ThornburyPrimaryWash,
                            contentColor = ThornburyPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Auto-Fill",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Voice Auto-Fill", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThornburyCanvas)
            )
        },
        containerColor = ThornburyCanvas,
        modifier = modifier
    ) { innerPadding ->
        val isCompact = LocalWindowWidthSizeClass.current == WindowWidthSizeClass.Compact

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .adaptiveContentContainer(860.dp)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(10.dp))

            errorMessage?.let { err ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ThornburyErrorWash,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ThornburyError.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = ThornburyError,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = err,
                            color = ThornburyError,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 1. Personal Details Card
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft),
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = ThornburyPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Personal Information",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Full Name *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("e.g. Arthur Pendelton") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Patient Category (Dentition Type) *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Adult Option
                        Surface(
                            onClick = { isChild = false },
                            shape = RoundedCornerShape(10.dp),
                            color = if (!isChild) ThornburyPrimaryWash else ThornburyCanvas,
                            border = BorderStroke(1.dp, if (!isChild) ThornburyPrimary else ThornburyHairline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = !isChild,
                                    onClick = { isChild = false },
                                    colors = RadioButtonDefaults.colors(selectedColor = ThornburyPrimary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Adult Patient",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = ThornburyInk
                                    )
                                    Text(
                                        text = "Permanent Dentition • 32 Teeth (FDI 11–48)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ThornburyMuted
                                    )
                                }
                            }
                        }

                        // Child Option
                        Surface(
                            onClick = { isChild = true },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isChild) ThornburyPrimaryWash else ThornburyCanvas,
                            border = BorderStroke(1.dp, if (isChild) ThornburyPrimary else ThornburyHairline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isChild,
                                    onClick = { isChild = true },
                                    colors = RadioButtonDefaults.colors(selectedColor = ThornburyPrimary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Child / Pediatric Patient",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = ThornburyInk
                                    )
                                    Text(
                                        text = "Primary Dentition • 20 Teeth (FDI 51–85)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ThornburyMuted
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    ThornburyDatePickerField(
                        value = dob,
                        onValueChange = { dob = it },
                        label = "Date of Birth",
                        placeholder = "DD/MM/YYYY",
                        isOptional = true,
                        helperText = "Format: DD/MM/YYYY"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Gender",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        genders.forEach { gender ->
                            val isSelected = gender == selectedGender
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedGender = gender },
                                label = { Text(gender, style = MaterialTheme.typography.labelMedium) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ThornburyPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Clinical Indexing Card
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft),
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = ThornburyPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Clinical Registration Number",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Outpatient (OP) Number",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = opNo,
                        onValueChange = { opNo = it },
                        placeholder = { Text("OP-40192") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Contact Details Card
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft),
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = null,
                            tint = ThornburyPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Contact Information",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Phone Number (Optional)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = ValidationUtils.filterPhoneInput(it) },
                        placeholder = { Text("+44 7700 900123") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = phone.isNotBlank() && !ValidationUtils.isValidPhone(phone),
                        supportingText = {
                            if (phone.isNotBlank() && !ValidationUtils.isValidPhone(phone)) {
                                Text(
                                    text = "Invalid phone number (must be 7-15 digits, no letters)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ThornburyError
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Email Address",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("patient@example.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = email.isNotBlank() && !ValidationUtils.isValidEmail(email),
                        supportingText = {
                            if (email.isNotBlank() && !ValidationUtils.isValidEmail(email)) {
                                Text(
                                    text = "Must be a valid email (e.g. name@example.com with '@' and domain)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ThornburyError
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Residential Address",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        placeholder = { Text("House no., Street name, City, Postcode") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Emergency Contact Card
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = ThornburySurfaceSoft),
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = ThornburyPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Emergency Contact (Optional)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Contact Name",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = emergencyName,
                        onValueChange = { emergencyName = it },
                        placeholder = { Text("Next of Kin / Relative full name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Contact Phone",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = emergencyPhone,
                        onValueChange = { emergencyPhone = ValidationUtils.filterPhoneInput(it) },
                        placeholder = { Text("+44 7700 900999") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = emergencyPhone.isNotBlank() && !ValidationUtils.isValidPhone(emergencyPhone),
                        supportingText = {
                            if (emergencyPhone.isNotBlank() && !ValidationUtils.isValidPhone(emergencyPhone)) {
                                Text(
                                    text = "Invalid phone number (must be 7-15 digits, no letters)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ThornburyError
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    enabled = !isSubmitting,
                    onClick = {
                        if (isSubmitting) return@Button
                        isSubmitting = true
                        
                        if (name.isBlank()) {
                            errorMessage = "Please enter patient's full name."
                            isSubmitting = false
                            return@Button
                        }
                        if (phone.isNotBlank() && !ValidationUtils.isValidPhone(phone)) {
                            errorMessage = "Please enter a valid phone number (7-15 digits). Letters like 'parrot' are not allowed."
                            isSubmitting = false
                            return@Button
                        }
                        if (email.isNotBlank() && !ValidationUtils.isValidEmail(email)) {
                            errorMessage = "Please enter a valid email address with '@' and domain (e.g. name@example.com)."
                            isSubmitting = false
                            return@Button
                        }
                        if (emergencyPhone.isNotBlank() && !ValidationUtils.isValidPhone(emergencyPhone)) {
                            errorMessage = "Please enter a valid emergency contact phone number (7-15 digits)."
                            isSubmitting = false
                            return@Button
                        }

                        val created = DentalRepository.registerPatient(
                            name = name.trim(),
                            dob = dob.trim().ifBlank { "Not specified" },
                            opNo = if (opNo.isNotBlank()) opNo.trim() else "OP-${(40000..49999).random()}",
                            phone = phone.trim(),
                            email = email.trim(),
                            address = address.trim(),
                            allergies = emptyList(),
                            medicalAlerts = emptyList(),
                            medicalHistory = "",
                            isChild = isChild
                        )
                        if (created != null) {
                            onRegistered(created)
                        } else {
                            isSubmitting = false
                        }
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Register Patient", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    }

    if (showVoiceIntakeDialog) {
        VoiceDemographicIntakeDialog(
            onDismiss = { showVoiceIntakeDialog = false },
            onApplyDemographics = { demo ->
                if (demo.name.isNotBlank()) name = demo.name
                if (demo.dob.isNotBlank()) {
                    dob = demo.dob
                } else if (demo.ageYears != null) {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.YEAR, -demo.ageYears)
                    dob = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
                }
                if (demo.phone.isNotBlank()) phone = ValidationUtils.filterPhoneInput(demo.phone)
                if (demo.email.isNotBlank()) email = demo.email
                if (demo.address.isNotBlank()) address = demo.address
                if (demo.isChild != null) isChild = demo.isChild
            }
        )
    }
}

@Composable
fun VoiceDemographicIntakeDialog(
    onDismiss: () -> Unit,
    onApplyDemographics: (DemographicsFinding) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var extractedFinding by remember { mutableStateOf<DemographicsFinding?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val recognizer = remember {
        if (AndroidSpeechRecognizerManager.isAvailable(context)) {
            AndroidSpeechRecognizerManager(context)
        } else null
    }

    val micPermission = rememberMicrophonePermissionState(
        onGranted = {
            isListening = true
            recognizer?.startListening(
                continuous = false,
                onPartialResult = { partial -> recognizedText = partial },
                onFinalResult = { full ->
                    recognizedText = full
                    isListening = false
                    isProcessing = true
                    coroutineScope.launch {
                        val findings = ClinicalVoiceExtractor.extract(full, context)
                        extractedFinding = findings.demographics
                        isProcessing = false
                    }
                },
                onError = { isListening = false }
            )
        }
    )

    AlertDialog(
        onDismissRequest = {
            if (isListening) recognizer?.stopListening()
            onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = ThornburyPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Voice Intake Scribe",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThornburyInk
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Speak patient details hands-free (name, DOB or age, phone, email, address, adult or pediatric):",
                    style = MaterialTheme.typography.bodySmall,
                    color = ThornburyMuted
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = ThornburyCanvas,
                    border = BorderStroke(1.dp, ThornburyHairline)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (recognizedText.isNotBlank()) "\"$recognizedText\"" else if (isListening) "Listening for clinician voice..." else "Tap 'Start Recording' and dictate patient demographics.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (recognizedText.isNotBlank()) ThornburyInk else ThornburyMuted
                        )
                    }
                }

                if (isProcessing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                extractedFinding?.let { demo ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = ThornburyPrimaryWash
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Extracted Demographic Fields:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ThornburyPrimary)
                            if (demo.name.isNotBlank()) Text("• Name: ${demo.name}", style = MaterialTheme.typography.bodySmall, color = ThornburyInk)
                            if (demo.dob.isNotBlank() || demo.ageYears != null) Text("• DOB/Age: ${demo.dob.ifBlank { "${demo.ageYears} yrs" }}", style = MaterialTheme.typography.bodySmall, color = ThornburyInk)
                            if (demo.phone.isNotBlank()) Text("• Phone: ${demo.phone}", style = MaterialTheme.typography.bodySmall, color = ThornburyInk)
                            if (demo.email.isNotBlank()) Text("• Email: ${demo.email}", style = MaterialTheme.typography.bodySmall, color = ThornburyInk)
                            if (demo.address.isNotBlank()) Text("• Address: ${demo.address}", style = MaterialTheme.typography.bodySmall, color = ThornburyInk)
                            if (demo.isChild != null) Text("• Type: ${if (demo.isChild == true) "Child / Pediatric" else "Adult"}", style = MaterialTheme.typography.bodySmall, color = ThornburyInk)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isListening && extractedFinding == null) {
                    Button(
                        onClick = {
                            if (micPermission.isGranted) {
                                isListening = true
                                recognizer?.startListening(
                                    continuous = false,
                                    onPartialResult = { partial -> recognizedText = partial },
                                    onFinalResult = { full ->
                                        recognizedText = full
                                        isListening = false
                                        isProcessing = true
                                        coroutineScope.launch {
                                            val findings = ClinicalVoiceExtractor.extract(full, context)
                                            extractedFinding = findings.demographics
                                            isProcessing = false
                                        }
                                    },
                                    onError = { isListening = false }
                                )
                            } else {
                                micPermission.request()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Recording")
                    }
                } else if (isListening) {
                    Button(
                        onClick = {
                            isListening = false
                            recognizer?.stopListening()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThornburyError)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop")
                    }
                } else if (extractedFinding != null) {
                    Button(
                        onClick = {
                            extractedFinding?.let { onApplyDemographics(it) }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary)
                    ) {
                        Text("Apply to Form")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
