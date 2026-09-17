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
import com.example.thornburydental.data.User
import com.example.thornburydental.theme.*
import com.example.thornburydental.util.ValidationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Brand-aligned SignInScreen for the Thornbury Clinical design system.
 * Features high-contrast dark ink typography, a clean clinical canvas,
 * demo quick logins, and accessible form controls.
 */
@Composable
fun SignInScreen(
    onSignInSuccess: (User) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    fun performLogin() {
        focusManager.clearFocus()
        val cleanEmail = email.trim()
        val cleanPassword = password

        if (cleanEmail.isBlank()) {
            errorMessage = "Please enter your email address"
            return
        }
        if (!ValidationUtils.isValidEmail(cleanEmail)) {
            errorMessage = "Please enter a valid email address with '@' and domain (e.g. name@example.com)"
            return
        }
        if (cleanPassword.isBlank()) {
            errorMessage = "Please enter your password"
            return
        }

        isLoading = true
        errorMessage = null

        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) {
                AuthRepository.login(cleanEmail, cleanPassword)
            }
            isLoading = false
            result.onSuccess { user ->
                onSignInSuccess(user)
            }.onFailure { error ->
                errorMessage = error.message ?: "Invalid email or password. Please try again."
            }
        }
    }

    fun performQuickLoginClinician() {
        focusManager.clearFocus()
        isLoading = true
        errorMessage = null

        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) {
                AuthRepository.quickLoginAsClinician()
            }
            isLoading = false
            result.onSuccess { user ->
                onSignInSuccess(user)
            }.onFailure { error ->
                errorMessage = error.message ?: "Failed to sign in as clinician demo account."
            }
        }
    }

    fun performQuickLoginPatient() {
        focusManager.clearFocus()
        isLoading = true
        errorMessage = null

        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) {
                AuthRepository.quickLoginAsPatient()
            }
            isLoading = false
            result.onSuccess { user ->
                onSignInSuccess(user)
            }.onFailure { error ->
                errorMessage = error.message ?: "Failed to sign in as patient demo account."
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ThornburyCanvas)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .adaptiveContentContainer(520.dp)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // --- Brand Logo ---
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(ThornburyPrimary, RoundedCornerShape(16.dp))
                    .semantics { contentDescription = "Dentara Brand Emblem" },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MedicalServices,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- Brand Header ---
            Text(
                text = "Dentara",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = ThornburyInk,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sign in to access your dental records or clinician operatory suite",
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = ThornburyMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- Error Banner ---
            if (errorMessage != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
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
                    // Email Field
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
                            Text("e.g. dr.halvorsen@dentara.com", color = ThornburyMutedSoft)
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

                    Spacer(modifier = Modifier.height(18.dp))

                    // Password Field
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
                            Text("Enter your password", color = ThornburyMutedSoft)
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
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { performLogin() }
                        ),
                        colors = thornburyTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Sign In Button
                    Button(
                        onClick = { performLogin() },
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
                                text = "Sign In",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // --- Quick One-Tap Demo Logins Section ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = ThornburySurfaceCard,
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = ThornburyAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Quick Access (Demo Accounts)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = ThornburyInk
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Instant 1-tap authentication for clinical operatory and patient portal testing",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThornburyMuted
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Chip/Button 1: Dr. Ingrid Halvorsen (Clinician)
                    OutlinedButton(
                        onClick = { performQuickLoginClinician() },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = ThornburyCanvas,
                            contentColor = ThornburyInk
                        ),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(ThornburyPrimaryWash, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalHospital,
                                    contentDescription = null,
                                    tint = ThornburyPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dr. Ingrid Halvorsen (Clinician)",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyInk
                                )
                                Text(
                                    text = "Lead Prosthodontist • Operatory Suite",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThornburyMuted
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Chip/Button 2: Rosalind Achebe (Patient)
                    OutlinedButton(
                        onClick = { performQuickLoginPatient() },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = ThornburyCanvas,
                            contentColor = ThornburyInk
                        ),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(ThornburyInfoWash, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = ThornburyAccentTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Rosalind Achebe (Patient)",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyInk
                                )
                                Text(
                                    text = "Patient Records & Appointment History",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThornburyMuted
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // --- Navigation Links ---
            TextButton(
                onClick = onNavigateToRegister,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "Don't have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThornburyBody
                )
                Text(
                    text = "Register here",
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
