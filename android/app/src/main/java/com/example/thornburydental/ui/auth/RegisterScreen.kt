package com.example.thornburydental.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thornburydental.data.AuthRepository
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.User
import com.example.thornburydental.data.UserRole
import com.example.thornburydental.theme.*
import com.example.thornburydental.util.ValidationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Brand-aligned RegisterScreen for the Thornbury Clinical design system.
 * Allows new patients or clinicians/staff to create accounts with role selection,
 * comprehensive input validation, automatic clinic dental chart linking for patients,
 * and high-contrast accessible styling.
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: (User) -> Unit,
    onNavigateToSignIn: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRole by remember { mutableStateOf(UserRole.PATIENT) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    fun validateAndRegister() {
        focusManager.clearFocus()
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        val cleanPhone = phone.trim()
        val cleanPassword = password
        val cleanConfirm = confirmPassword

        // Non-empty validations
        if (cleanName.isBlank()) {
            errorMessage = "Please enter your full name"
            return
        }
        if (cleanEmail.isBlank()) {
            errorMessage = "Please enter your email address"
            return
        }
        // Valid email validation
        if (!ValidationUtils.isValidEmail(cleanEmail)) {
            errorMessage = "Please enter a valid email address with '@' and domain (e.g. name@example.com)"
            return
        }
        if (cleanPhone.isBlank()) {
            errorMessage = "Please enter your phone number"
            return
        }
        if (!ValidationUtils.isValidPhone(cleanPhone)) {
            errorMessage = "Please enter a valid phone number (7-15 digits). Letters like 'parrot' are not allowed."
            return
        }
        if (cleanPassword.isBlank()) {
            errorMessage = "Please enter a password"
            return
        }
        // Password length validation
        if (cleanPassword.length < 6) {
            errorMessage = "Password must be at least 6 characters long"
            return
        }
        if (cleanConfirm.isBlank()) {
            errorMessage = "Please confirm your password"
            return
        }
        // Passwords match validation
        if (cleanPassword != cleanConfirm) {
            errorMessage = "Passwords do not match"
            return
        }

        isLoading = true
        errorMessage = null

        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) {
                AuthRepository.register(
                    email = cleanEmail,
                    password = cleanPassword,
                    name = cleanName,
                    role = selectedRole,
                    phone = cleanPhone
                )
            }

            result.onSuccess { user ->
                // If patient registration succeeds, link a dental record in the clinic!
                if (user.role == UserRole.PATIENT) {
                    withContext(Dispatchers.IO) {
                        try {
                            DentalRepository.registerPatient(
                                name = user.name,
                                opNo = null,
                                dob = "Not specified",
                                phone = user.phone.ifBlank { "Not provided" },
                                email = user.email.ifBlank { "Not provided" },
                                address = "Portland, OR",
                                allergies = emptyList(),
                                medicalAlerts = emptyList(),
                                medicalHistory = "Self-registered patient account via mobile app"
                            )
                        } catch (_: Exception) {
                            // Non-critical if offline or already linked
                        }
                    }
                }
                isLoading = false
                onRegisterSuccess(user)
            }.onFailure { error ->
                isLoading = false
                errorMessage = error.message ?: "Registration failed. Please try again."
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ThornburyCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // --- Brand Logo ---
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(ThornburyPrimary, RoundedCornerShape(16.dp))
                    .semantics { contentDescription = "Dentara Brand Emblem" },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MedicalServices,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --- Brand Header ---
            Text(
                text = "Join Dentara",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = ThornburyInk,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Create an account to manage your dental appointments and clinical care",
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = ThornburyMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(26.dp))

            // --- Error Banner ---
            if (errorMessage != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = ThornburyErrorWash,
                    border = BorderStroke(1.dp, ThornburyError)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = ThornburyError,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = ThornburyError,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { errorMessage = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss error",
                                tint = ThornburyError,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // --- Form Inputs Card ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = ThornburySurfaceSoft,
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // --- Role Selector Header ---
                    Text(
                        text = "Account Role",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = ThornburyInk
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Role Selector: FilterChips Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = selectedRole == UserRole.PATIENT,
                            onClick = { selectedRole = UserRole.PATIENT },
                            label = {
                                Text(
                                    text = "Patient",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (selectedRole == UserRole.PATIENT) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ThornburyPrimary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                                containerColor = ThornburyCanvas,
                                labelColor = ThornburyInk,
                                iconColor = ThornburyMuted
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedRole == UserRole.PATIENT,
                                borderColor = ThornburyHairline,
                                selectedBorderColor = ThornburyPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = selectedRole == UserRole.CLINICIAN,
                            onClick = { selectedRole = UserRole.CLINICIAN },
                            label = {
                                Text(
                                    text = "Clinician / Staff",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (selectedRole == UserRole.CLINICIAN) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.MedicalServices,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ThornburyPrimary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                                containerColor = ThornburyCanvas,
                                labelColor = ThornburyInk,
                                iconColor = ThornburyMuted
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedRole == UserRole.CLINICIAN,
                                borderColor = ThornburyHairline,
                                selectedBorderColor = ThornburyPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (selectedRole == UserRole.PATIENT) {
                            "Links automatically to your clinic dental record & health history"
                        } else {
                            "Enables practitioner operatory suite, treatment plans & chart editing"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = ThornburyMuted
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = ThornburyHairlineSoft)
                    Spacer(modifier = Modifier.height(18.dp))

                    // Full Name
                    Text(
                        text = "Full Name",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (errorMessage != null) errorMessage = null
                        },
                        placeholder = {
                            Text("e.g. Eleanor Vance", color = ThornburyMutedSoft)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Name Icon",
                                tint = ThornburyMuted
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = thornburyTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Email Address
                    Text(
                        text = "Email Address",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (errorMessage != null) errorMessage = null
                        },
                        placeholder = {
                            Text("e.g. eleanor.vance@example.org", color = ThornburyMutedSoft)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email Icon",
                                tint = ThornburyMuted
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
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
                        colors = thornburyTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Phone Number
                    Text(
                        text = "Phone Number",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = ValidationUtils.filterPhoneInput(it)
                            if (errorMessage != null) errorMessage = null
                        },
                        placeholder = {
                            Text("e.g. +1 (503) 555-0142", color = ThornburyMutedSoft)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Phone Icon",
                                tint = ThornburyMuted
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
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
                        colors = thornburyTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password
                    Text(
                        text = "Password",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (errorMessage != null) errorMessage = null
                        },
                        placeholder = {
                            Text("Minimum 6 characters", color = ThornburyMutedSoft)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Password Icon",
                                tint = ThornburyMuted
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = ThornburyMuted
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = thornburyTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm Password
                    Text(
                        text = "Confirm Password",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            if (errorMessage != null) errorMessage = null
                        },
                        placeholder = {
                            Text("Re-enter password", color = ThornburyMutedSoft)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Confirm Password Icon",
                                tint = ThornburyMuted
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                    tint = ThornburyMuted
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { validateAndRegister() }
                        ),
                        colors = thornburyTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Create Account Button
                    Button(
                        onClick = { validateAndRegister() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThornburyPrimary,
                            contentColor = ThornburyOnPrimary,
                            disabledContainerColor = ThornburyPrimaryDisabled
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = ThornburyOnPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Create Account",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Navigation Links ---
            TextButton(
                onClick = onNavigateToSignIn,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThornburyBody
                )
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onNavigateToHome,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = ThornburyMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Back to Practice Home",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = ThornburyMuted
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
