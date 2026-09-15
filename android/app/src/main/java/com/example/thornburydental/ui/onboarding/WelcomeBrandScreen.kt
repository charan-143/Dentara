package com.example.thornburydental.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thornburydental.theme.*
import com.example.thornburydental.ui.components.molar3d.Molar3DView

/**
 * Minimal Brand Page - Clean, cardless presentation featuring the brand identity
 * and the floating 3D Mandibular First Molar directly on the canvas.
 */
@Composable
fun WelcomeBrandScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        containerColor = ThornburyCanvas,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Subtle brand establishment mark
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(ThornburyPrimary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "EST. 2012 • PORTLAND, OR",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp,
                        color = ThornburyPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Practice Title
            Text(
                text = "Dentara",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Light,
                    fontSize = 42.sp,
                    color = ThornburyInk,
                    letterSpacing = 0.5.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Dental Practice & Clinical Operatory Suite",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = ThornburyBody,
                    fontSize = 15.sp,
                    letterSpacing = 0.4.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3D Mandibular Molar - Cardless, directly on canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Molar3DView(
                    modifier = Modifier.fillMaxSize(),
                    showCard = false
                )
            }

            Text(
                text = "3D Mandibular Molar • Drag to rotate 360°",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = ThornburyMuted,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Bottom Action Area - Clean and uncarded
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThornburyPrimary,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = "Get Started",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Initial Clinician Setup • Takes 1 minute",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = ThornburyMuted,
                    fontSize = 12.sp
                )
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
