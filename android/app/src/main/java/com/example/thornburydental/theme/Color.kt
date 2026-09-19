package com.example.thornburydental.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =============================================================================
// Thornbury Clinical — a healthcare-grade Material 3 palette.
// Anchored on a deep clinical teal, cool neutral surfaces, and dark-mode tokens.
// =============================================================================

// --- Base Light Values ---
val LightThornburyPrimary = Color(0xFF0D7377)
val LightThornburyPrimaryActive = Color(0xFF0A5A5D)
val LightThornburyPrimaryDisabled = Color(0xFFDCE9E8)
val LightThornburyPrimaryText = Color(0xFF0A4F4F)
val LightThornburyOnPrimary = Color(0xFFFFFFFF)
val LightThornburyInk = Color(0xFF12181B)
val LightThornburyBody = Color(0xFF3C464A)
val LightThornburyBodyStrong = Color(0xFF232B2E)
val LightThornburyMuted = Color(0xFF5C666A)
val LightThornburyMutedSoft = Color(0xFF78878A)
val LightThornburyHairline = Color(0xFFDCE7E6)
val LightThornburyHairlineSoft = Color(0xFFE6EFEE)
val LightThornburyCanvas = Color(0xFFF6FAFA)
val LightThornburySurfaceSoft = Color(0xFFEEF4F3)
val LightThornburySurfaceCard = Color(0xFFE4EEED)
val LightThornburySurfaceCreamStrong = Color(0xFFD7E6E4)
val LightThornburyPrimaryWash = Color(0xFFE6F3F2)
val LightThornburySuccess = Color(0xFF227A4E)
val LightThornburySuccessWash = Color(0xFFE1F3E8)
val LightThornburyWarning = Color(0xFF8A6500)
val LightThornburyWarningWash = Color(0xFFFBF0D6)
val LightThornburyError = Color(0xFFB3261E)
val LightThornburyErrorWash = Color(0xFFFBE9E7)
val LightThornburyAccentTeal = Color(0xFF4FB8AC)
val LightThornburyAccentAmber = Color(0xFFC97A3D)
val LightThornburyInfoWash = Color(0xFFE3F3F1)
val LightThornburyTertiaryText = Color(0xFF0F5A52)

// --- Base Dark Values ---
val DarkThornburyPrimary = Color(0xFF6FDBD1)
val DarkThornburyPrimaryActive = Color(0xFF8FE6DC)
val DarkThornburyPrimaryDisabled = Color(0xFF1B3836)
val DarkThornburyPrimaryText = Color(0xFF7FE0D6)
val DarkThornburyOnPrimary = Color(0xFF00332E)
val DarkThornburyInk = Color(0xFFF2F8F7)
val DarkThornburyBody = Color(0xFFB5C8C5)
val DarkThornburyBodyStrong = Color(0xFFE2ECEB)
val DarkThornburyMuted = Color(0xFF8A9E9B)
val DarkThornburyMutedSoft = Color(0xFF5D716E)
val DarkThornburyHairline = Color(0xFF2A4341)
val DarkThornburyHairlineSoft = Color(0xFF223836)
val DarkThornburyCanvas = Color(0xFF0E1B1B)
val DarkThornburySurfaceSoft = Color(0xFF132523)
val DarkThornburySurfaceCard = Color(0xFF17302E)
val DarkThornburySurfaceCreamStrong = Color(0xFF1E3A37)
val DarkThornburyPrimaryWash = Color(0xFF123330)
val DarkThornburySuccess = Color(0xFF7FD9A6)
val DarkThornburySuccessWash = Color(0xFF163024)
val DarkThornburyWarning = Color(0xFFE3C15A)
val DarkThornburyWarningWash = Color(0xFF2E2712)
val DarkThornburyError = Color(0xFFE8756F)
val DarkThornburyErrorWash = Color(0xFF2E1C1B)
val DarkThornburyAccentTeal = Color(0xFF6ED9CB)
val DarkThornburyAccentAmber = Color(0xFFE3C15A)
val DarkThornburyInfoWash = Color(0xFF123330)
val DarkThornburyTertiaryText = Color(0xFFA9EEE3)

// Static surface constants
val ThornburySurfaceDark = Color(0xFF0E1B1B)
val ThornburySurfaceDarkElevated = Color(0xFF17302E)
val ThornburySurfaceDarkSoft = Color(0xFF132523)
val ThornburySurfaceDarkLowest = Color(0xFF081312)
val ThornburySurfaceDarkHighest = Color(0xFF1E3A37)
val ThornburyHairlineDark = Color(0xFF2A4341)
val ThornburyHairlineDarkSoft = Color(0xFF223836)
val ThornburyOnDark = Color(0xFFF2F8F7)
val ThornburyOnDarkSoft = Color(0xFF9FB3B0)
val ThornburyScrim = Color(0xFF000000)

val ThornburyAccent = Color(0xFFC97A3D)
val ThornburyOnAccent = Color(0xFF12211F)

val ThornburyPrimaryOnDark = DarkThornburyPrimary
val ThornburyOnPrimaryOnDark = DarkThornburyOnPrimary
val ThornburyPrimaryActiveDark = DarkThornburyPrimaryActive
val ThornburyPrimaryTextDark = DarkThornburyPrimaryText
val ThornburyPrimaryDisabledDark = DarkThornburyPrimaryDisabled

/**
 * Data class representing the active semantic clinical color palette.
 */
data class ThornburyPalette(
    val isDark: Boolean,
    val primary: Color,
    val primaryActive: Color,
    val primaryDisabled: Color,
    val primaryText: Color,
    val onPrimary: Color,
    val ink: Color,
    val body: Color,
    val bodyStrong: Color,
    val muted: Color,
    val mutedSoft: Color,
    val hairline: Color,
    val hairlineSoft: Color,
    val canvas: Color,
    val surfaceSoft: Color,
    val surfaceCard: Color,
    val surfaceCreamStrong: Color,
    val primaryWash: Color,
    val border: Color,
    val success: Color,
    val successWash: Color,
    val warning: Color,
    val warningWash: Color,
    val error: Color,
    val errorWash: Color,
    val accentTeal: Color,
    val accentAmber: Color,
    val infoWash: Color,
    val tertiaryText: Color
)

val LightThornburyPalette = ThornburyPalette(
    isDark = false,
    primary = LightThornburyPrimary,
    primaryActive = LightThornburyPrimaryActive,
    primaryDisabled = LightThornburyPrimaryDisabled,
    primaryText = LightThornburyPrimaryText,
    onPrimary = LightThornburyOnPrimary,
    ink = LightThornburyInk,
    body = LightThornburyBody,
    bodyStrong = LightThornburyBodyStrong,
    muted = LightThornburyMuted,
    mutedSoft = LightThornburyMutedSoft,
    hairline = LightThornburyHairline,
    hairlineSoft = LightThornburyHairlineSoft,
    canvas = LightThornburyCanvas,
    surfaceSoft = LightThornburySurfaceSoft,
    surfaceCard = LightThornburySurfaceCard,
    surfaceCreamStrong = LightThornburySurfaceCreamStrong,
    primaryWash = LightThornburyPrimaryWash,
    border = LightThornburyHairline,
    success = LightThornburySuccess,
    successWash = LightThornburySuccessWash,
    warning = LightThornburyWarning,
    warningWash = LightThornburyWarningWash,
    error = LightThornburyError,
    errorWash = LightThornburyErrorWash,
    accentTeal = LightThornburyAccentTeal,
    accentAmber = LightThornburyAccentAmber,
    infoWash = LightThornburyInfoWash,
    tertiaryText = LightThornburyTertiaryText
)

val DarkThornburyPalette = ThornburyPalette(
    isDark = true,
    primary = DarkThornburyPrimary,
    primaryActive = DarkThornburyPrimaryActive,
    primaryDisabled = DarkThornburyPrimaryDisabled,
    primaryText = DarkThornburyPrimaryText,
    onPrimary = DarkThornburyOnPrimary,
    ink = DarkThornburyInk,
    body = DarkThornburyBody,
    bodyStrong = DarkThornburyBodyStrong,
    muted = DarkThornburyMuted,
    mutedSoft = DarkThornburyMutedSoft,
    hairline = DarkThornburyHairline,
    hairlineSoft = DarkThornburyHairlineSoft,
    canvas = DarkThornburyCanvas,
    surfaceSoft = DarkThornburySurfaceSoft,
    surfaceCard = DarkThornburySurfaceCard,
    surfaceCreamStrong = DarkThornburySurfaceCreamStrong,
    primaryWash = DarkThornburyPrimaryWash,
    border = DarkThornburyHairline,
    success = DarkThornburySuccess,
    successWash = DarkThornburySuccessWash,
    warning = DarkThornburyWarning,
    warningWash = DarkThornburyWarningWash,
    error = DarkThornburyError,
    errorWash = DarkThornburyErrorWash,
    accentTeal = DarkThornburyAccentTeal,
    accentAmber = DarkThornburyAccentAmber,
    infoWash = DarkThornburyInfoWash,
    tertiaryText = DarkThornburyTertiaryText
)

val LocalThornburyPalette = staticCompositionLocalOf { LightThornburyPalette }

// =============================================================================
// Dynamic Composable Getters — seamlessly adapt all existing UI to Dark Theme!
// =============================================================================

val ThornburyPrimary: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburyPrimary else LightThornburyPrimary

val ThornburyPrimaryActive: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburyPrimaryActive else LightThornburyPrimaryActive

val ThornburyPrimaryDisabled: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburyPrimaryDisabled else LightThornburyPrimaryDisabled

val ThornburyPrimaryText: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburyPrimaryText else LightThornburyPrimaryText

val ThornburyOnPrimary: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburyOnPrimary else LightThornburyOnPrimary

val ThornburyInk: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburyInk else LightThornburyInk

val ThornburyBody: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburyBody else LightThornburyBody

val ThornburyBodyStrong: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburyBodyStrong else LightThornburyBodyStrong

val ThornburyMuted: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburyMuted else LightThornburyMuted

val ThornburyMutedSoft: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburyMutedSoft else LightThornburyMutedSoft

val ThornburyHairline: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburyHairline else LightThornburyHairline

val ThornburyHairlineSoft: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburyHairlineSoft else LightThornburyHairlineSoft

val ThornburyCanvas: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburyCanvas else LightThornburyCanvas

val ThornburySurfaceSoft: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburySurfaceSoft else LightThornburySurfaceSoft

val ThornburySurfaceCard: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburySurfaceCard else LightThornburySurfaceCard

val ThornburySurface: Color
    get() = ThornburySurfaceCard

val ThornburySurfaceCreamStrong: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburySurfaceCreamStrong else LightThornburySurfaceCreamStrong

val ThornburyPrimaryWash: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburyPrimaryWash else LightThornburyPrimaryWash

val ThornburyBorder: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburyHairline else LightThornburyHairline

val ThornburySuccess: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburySuccess else LightThornburySuccess

val ThornburySuccessWash: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburySuccessWash else LightThornburySuccessWash

val ThornburyWarning: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburyWarning else LightThornburyWarning

val ThornburyWarningWash: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburyWarningWash else LightThornburyWarningWash

val ThornburyError: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburyError else LightThornburyError

val ThornburyErrorWash: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburyErrorWash else LightThornburyErrorWash

val ThornburyAccentTeal: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburyAccentTeal else LightThornburyAccentTeal

val ThornburyAccentAmber: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburyAccentAmber else LightThornburyAccentAmber

val ThornburyInfoWash: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburyInfoWash else LightThornburyInfoWash

val ThornburyTertiaryText: Color
    get() = if (com.example.thornburydental.data.DentalRepository.isDarkModeEnabled.value) DarkThornburyTertiaryText else LightThornburyTertiaryText


// --- Dark Mode Status Tokens ---
val ThornburyTertiaryDark = Color(0xFF6ED9CB)     // Lighter clinical teal for dark mode
val ThornburyOnTertiaryDark = Color(0xFF04302B)
val ThornburyTertiaryContainerDark = Color(0xFF123330)
val ThornburyOnTertiaryContainerDark = Color(0xFFA9EEE3)
val ThornburySuccessDark = Color(0xFF7FD9A6)
val ThornburySuccessWashDark = Color(0xFF163024)
val ThornburyWarningDark = Color(0xFFE3C15A)
val ThornburyWarningWashDark = Color(0xFF2E2712)
val ThornburyErrorDark = Color(0xFFE8756F)        // Accessible dark mode error
val ThornburyOnErrorDark = Color(0xFF3E0A08)
val ThornburyErrorContainerDark = Color(0xFF2E1C1B) // Accessible dark mode error wash
val ThornburyOnErrorContainerDark = Color(0xFFFFDAD6)

// --- Odontogram Specific Tokens ---
val ToothSound = Color(0xFFFDFEFE)
val ToothDecay = Color(0xFFD32F2F)
val ToothFilled = Color(0xFF1976D2)
val ToothCrown = Color(0xFFC9932F)
val ToothMissing = Color(0xFF9AA6A5)
val ToothImplant = Color(0xFF7B1FA2)
val ToothRootCanal = Color(0xFFAD1457)            // Deep clinical rose — distinct from decay-red and primary-teal

// --- Clinical Condition Badge Tokens (mini legend chips) ---
val ThornburyRoseWash = Color(0xFFFCE4EC)
val ThornburyRoseText = Color(0xFFAD1457)
val ThornburyPurpleWash = Color(0xFFF3E8FB)
val ThornburyPurpleText = Color(0xFF7B1FA2)
val ThornburyNeutralWash = Color(0xFFEEF2F2)
val ThornburySlateText = Color(0xFF5C6A6D)
