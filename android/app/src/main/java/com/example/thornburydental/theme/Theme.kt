package com.example.thornburydental.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

val LightColorScheme = lightColorScheme(
    primary = ThornburyPrimary,
    onPrimary = ThornburyOnPrimary,
    primaryContainer = ThornburySurfaceSoft,
    onPrimaryContainer = ThornburyPrimaryText,
    inversePrimary = ThornburyPrimaryOnDark,
    secondary = ThornburyPrimaryActive,
    onSecondary = ThornburyOnPrimary,
    secondaryContainer = ThornburySurfaceCard,
    onSecondaryContainer = ThornburyInk,
    tertiary = ThornburyAccentTeal,
    onTertiary = Color.White,
    tertiaryContainer = ThornburyInfoWash,
    onTertiaryContainer = ThornburyTertiaryText,
    background = ThornburyCanvas,
    onBackground = ThornburyInk,
    surface = ThornburyCanvas,
    onSurface = ThornburyInk,
    surfaceVariant = ThornburySurfaceCard,
    onSurfaceVariant = ThornburyBody,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = ThornburyCanvas,
    surfaceContainer = ThornburySurfaceSoft,
    surfaceContainerHigh = ThornburySurfaceCard,
    surfaceContainerHighest = ThornburySurfaceCreamStrong,
    surfaceDim = ThornburySurfaceCard,
    surfaceBright = Color.White,
    outline = ThornburyHairline,
    outlineVariant = ThornburyHairlineSoft,
    scrim = ThornburyScrim,
    error = ThornburyError,
    onError = Color.White,
    errorContainer = ThornburyErrorWash,
    onErrorContainer = ThornburyError,
    inverseSurface = ThornburySurfaceDark,
    inverseOnSurface = ThornburyOnDark,
    surfaceTint = ThornburyPrimary
)

val DarkColorScheme = darkColorScheme(
    primary = ThornburyPrimaryOnDark,
    onPrimary = ThornburyOnPrimaryOnDark,
    primaryContainer = ThornburySurfaceDarkElevated,
    onPrimaryContainer = ThornburyOnDark,
    inversePrimary = ThornburyPrimary,
    secondary = ThornburyPrimaryActiveDark,
    onSecondary = ThornburyOnPrimaryOnDark,
    secondaryContainer = ThornburySurfaceDarkSoft,
    onSecondaryContainer = ThornburyOnDark,
    tertiary = ThornburyTertiaryDark,
    onTertiary = ThornburyOnTertiaryDark,
    tertiaryContainer = ThornburyTertiaryContainerDark,
    onTertiaryContainer = ThornburyOnTertiaryContainerDark,
    background = ThornburySurfaceDarkLowest,
    onBackground = ThornburyOnDark,
    surface = ThornburySurfaceDark,
    onSurface = ThornburyOnDark,
    surfaceVariant = ThornburySurfaceDarkElevated,
    onSurfaceVariant = ThornburyOnDarkSoft,
    surfaceContainerLowest = ThornburySurfaceDarkLowest,
    surfaceContainerLow = ThornburySurfaceDark,
    surfaceContainer = ThornburySurfaceDarkSoft,
    surfaceContainerHigh = ThornburySurfaceDarkElevated,
    surfaceContainerHighest = ThornburySurfaceDarkHighest,
    surfaceDim = ThornburySurfaceDark,
    surfaceBright = ThornburySurfaceDarkHighest,
    outline = ThornburyHairlineDark,
    outlineVariant = ThornburyHairlineDarkSoft,
    scrim = ThornburyScrim,
    error = ThornburyErrorDark,
    onError = ThornburyOnErrorDark,
    errorContainer = ThornburyErrorContainerDark,
    onErrorContainer = ThornburyOnErrorContainerDark,
    inverseSurface = ThornburyCanvas,
    inverseOnSurface = ThornburyInk,
    surfaceTint = ThornburyPrimaryOnDark
)

// A softer, friendlier rounding scale than stock M3 — modern clinical apps
// lean into generous corner radii (approachable, non-clinical-cold) while
// keeping small controls crisp enough to read as precise/clinical.
val ThornburyShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Standard text field color styling for Thornbury Dental.
 * Ensures input font color is always legible ThornburyInk (never white-on-white).
 */
@Composable
fun thornburyTextFieldColors(
    containerColor: Color = ThornburyCanvas,
    focusedBorderColor: Color = ThornburyPrimary,
    unfocusedBorderColor: Color = ThornburyHairline,
    textColor: Color = ThornburyInk,
    placeholderColor: Color = ThornburyMutedSoft,
    labelColor: Color = ThornburyMuted,
    cursorColor: Color = ThornburyPrimary
): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = textColor,
    unfocusedTextColor = textColor,
    focusedContainerColor = containerColor,
    unfocusedContainerColor = containerColor,
    focusedBorderColor = focusedBorderColor,
    unfocusedBorderColor = unfocusedBorderColor,
    focusedPlaceholderColor = placeholderColor,
    unfocusedPlaceholderColor = placeholderColor,
    focusedLabelColor = focusedBorderColor,
    unfocusedLabelColor = labelColor,
    cursorColor = cursorColor
)

@Composable
fun ThornburyDentalTheme(
    darkTheme: Boolean = false, // Thornbury Clinical anchors on a clean, cool clinical-canvas system
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ThornburyTypography,
        shapes = ThornburyShapes,
        content = content
    )
}
