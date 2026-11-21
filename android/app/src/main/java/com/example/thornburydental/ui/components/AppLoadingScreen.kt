package com.example.thornburydental.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dedicated clinical loading screen displayed during app launch and database preparation,
 * preventing blank or black screens on startup.
 */
@Composable
fun AppLoadingScreen(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Branded Tooth Mark with smooth breathing pulse
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(54.dp)) {
                        val toothPath = Path().apply {
                            moveTo(size.width * 0.5f, size.height * 0.24f)
                            cubicTo(
                                size.width * 0.38f, size.height * 0.24f,
                                size.width * 0.29f, size.height * 0.31f,
                                size.width * 0.28f, size.height * 0.41f
                            )
                            cubicTo(
                                size.width * 0.27f, size.height * 0.48f,
                                size.width * 0.28f, size.height * 0.53f,
                                size.width * 0.30f, size.height * 0.59f
                            )
                            cubicTo(
                                size.width * 0.32f, size.height * 0.64f,
                                size.width * 0.33f, size.height * 0.69f,
                                size.width * 0.34f, size.height * 0.74f
                            )
                            cubicTo(
                                size.width * 0.34f, size.height * 0.77f,
                                size.width * 0.37f, size.height * 0.80f,
                                size.width * 0.40f, size.height * 0.80f
                            )
                            cubicTo(
                                size.width * 0.43f, size.height * 0.80f,
                                size.width * 0.45f, size.height * 0.77f,
                                size.width * 0.46f, size.height * 0.74f
                            )
                            cubicTo(
                                size.width * 0.47f, size.height * 0.70f,
                                size.width * 0.48f, size.height * 0.65f,
                                size.width * 0.49f, size.height * 0.62f
                            )
                            cubicTo(
                                size.width * 0.49f, size.height * 0.60f,
                                size.width * 0.50f, size.height * 0.59f,
                                size.width * 0.50f, size.height * 0.59f
                            )
                            cubicTo(
                                size.width * 0.50f, size.height * 0.59f,
                                size.width * 0.51f, size.height * 0.60f,
                                size.width * 0.51f, size.height * 0.62f
                            )
                            cubicTo(
                                size.width * 0.52f, size.height * 0.65f,
                                size.width * 0.53f, size.height * 0.70f,
                                size.width * 0.54f, size.height * 0.74f
                            )
                            cubicTo(
                                size.width * 0.55f, size.height * 0.77f,
                                size.width * 0.57f, size.height * 0.80f,
                                size.width * 0.60f, size.height * 0.80f
                            )
                            cubicTo(
                                size.width * 0.63f, size.height * 0.80f,
                                size.width * 0.66f, size.height * 0.77f,
                                size.width * 0.66f, size.height * 0.74f
                            )
                            cubicTo(
                                size.width * 0.67f, size.height * 0.69f,
                                size.width * 0.68f, size.height * 0.64f,
                                size.width * 0.70f, size.height * 0.59f
                            )
                            cubicTo(
                                size.width * 0.72f, size.height * 0.53f,
                                size.width * 0.73f, size.height * 0.48f,
                                size.width * 0.72f, size.height * 0.41f
                            )
                            cubicTo(
                                size.width * 0.71f, size.height * 0.31f,
                                size.width * 0.62f, size.height * 0.24f,
                                size.width * 0.50f, size.height * 0.24f
                            )
                            close()
                        }
                        drawPath(toothPath, brush = SolidColor(primaryColor))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Dentara",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Dental Practice & Clinical Operatory Suite",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                LinearProgressIndicator(
                    modifier = Modifier
                        .width(160.dp)
                        .height(4.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Preparing clinical workspace...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
