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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

val LightColorScheme = lightColorScheme(
    primary = LightThornburyPrimary,
    onPrimary = LightThornburyOnPrimary,
    primaryContainer = LightThornburySurfaceSoft,
    onPrimaryContainer = LightThornburyPrimaryText,
    inversePrimary = DarkThornburyPrimary,
    secondary = LightThornburyPrimaryActive,
    onSecondary = LightThornburyOnPrimary,
    secondaryContainer = LightThornburySurfaceCard,
    onSecondaryContainer = LightThornburyInk,
    tertiary = LightThornburyAccentTeal,
    onTertiary = Color.White,
    tertiaryContainer = LightThornburyInfoWash,
    onTertiaryContainer = LightThornburyTertiaryText,
    background = LightThornburyCanvas,
    onBackground = LightThornburyInk,
    surface = LightThornburyCanvas,
    onSurface = LightThornburyInk,
    surfaceVariant = LightThornburySurfaceCard,
    onSurfaceVariant = LightThornburyBody,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = LightThornburyCanvas,
    surfaceContainer = LightThornburySurfaceSoft,
    surfaceContainerHigh = LightThornburySurfaceCard,
    surfaceContainerHighest = LightThornburySurfaceCreamStrong,
    surfaceDim = LightThornburySurfaceCard,
    surfaceBright = Color.White,
    outline = LightThornburyHairline,
    outlineVariant = LightThornburyHairlineSoft,
    scrim = ThornburyScrim,
    error = LightThornburyError,
    onError = Color.White,
    errorContainer = LightThornburyErrorWash,
    onErrorContainer = LightThornburyError,
    inverseSurface = DarkThornburyCanvas,
    inverseOnSurface = DarkThornburyInk,
    surfaceTint = LightThornburyPrimary
)

val DarkColorScheme = darkColorScheme(
    primary = DarkThornburyPrimary,
    onPrimary = DarkThornburyOnPrimary,
    primaryContainer = DarkThornburySurfaceCard,
    onPrimaryContainer = DarkThornburyInk,
    inversePrimary = LightThornburyPrimary,
    secondary = DarkThornburyPrimaryActive,
    onSecondary = DarkThornburyOnPrimary,
    secondaryContainer = DarkThornburySurfaceSoft,
    onSecondaryContainer = DarkThornburyInk,
    tertiary = DarkThornburyAccentTeal,
    onTertiary = DarkThornburyOnPrimary,
    tertiaryContainer = DarkThornburyInfoWash,
    onTertiaryContainer = DarkThornburyTertiaryText,
    background = DarkThornburyCanvas,
    onBackground = DarkThornburyInk,
    surface = DarkThornburyCanvas,
    onSurface = DarkThornburyInk,
    surfaceVariant = DarkThornburySurfaceCard,
    onSurfaceVariant = DarkThornburyBody,
    surfaceContainerLowest = ThornburySurfaceDarkLowest,
    surfaceContainerLow = DarkThornburyCanvas,
    surfaceContainer = DarkThornburySurfaceSoft,
    surfaceContainerHigh = DarkThornburySurfaceCard,
    surfaceContainerHighest = DarkThornburySurfaceCreamStrong,
    surfaceDim = DarkThornburyCanvas,
    surfaceBright = DarkThornburySurfaceCreamStrong,
    outline = DarkThornburyHairline,
    outlineVariant = DarkThornburyHairlineSoft,
    scrim = ThornburyScrim,
    error = DarkThornburyError,
    onError = DarkThornburyOnPrimary,
    errorContainer = DarkThornburyErrorWash,
    onErrorContainer = DarkThornburyError,
    inverseSurface = LightThornburyCanvas,
    inverseOnSurface = LightThornburyInk,
    surfaceTint = DarkThornburyPrimary
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
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val palette = if (darkTheme) DarkThornburyPalette else LightThornburyPalette

    CompositionLocalProvider(LocalThornburyPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ThornburyTypography,
            shapes = ThornburyShapes,
            content = content
        )
    }
}
