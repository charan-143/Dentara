package com.example.thornburydental.theme

import androidx.compose.ui.graphics.Color

// --- Brand: Modern Teal ---
val ThornburyPrimary = Color(0xFF0F6E63)          // Deep clinical teal
val ThornburyPrimaryActive = Color(0xFF0A4B44)    // Darker teal (active/pressed)
val ThornburyPrimaryDisabled = Color(0xFFD7E9E5)  // Disabled state
val ThornburyPrimaryText = Color(0xFF0A4B44)      // Darkened for AA text contrast (~9.5:1 on canvas)
val ThornburyOnPrimary = Color(0xFFFFFFFF)

// Dark-mode primary: lightened so it stays legible on near-black surfaces (~8.2:1 against
// ThornburyOnPrimaryOnDark), mirroring how the web app already brightens its accent for dark
// mode. Only wired into Theme.kt's DarkColorScheme — the plain ThornburyPrimary constant above
// is unchanged everywhere it's already used directly (most screens read it as a flat Color, not
// through MaterialTheme.colorScheme, so this fix only reaches Material3-driven chrome).
val ThornburyPrimaryOnDark = Color(0xFF6FC6BA)
val ThornburyOnPrimaryOnDark = Color(0xFF0A231F)

// Bright aqua, reserved for a standalone call-to-action moment (e.g. "Book a visit"). Defined
// here but not yet wired into any screen, so it has no visible effect until a screen opts in.
val ThornburyAccent = Color(0xFF4FD1C5)
val ThornburyOnAccent = Color(0xFF0A2E29)

// --- Ink & Text ---
val ThornburyInk = Color(0xFF12201E)              // Dominant headlines & high-contrast titles
val ThornburyBody = Color(0xFF47534F)             // Readable body copy
val ThornburyBodyStrong = Color(0xFF263330)       // Emphasized copy
val ThornburyMuted = Color(0xFF5B6E6A)            // Secondary captions & metadata
val ThornburyMutedSoft = Color(0xFF7A8E88)        // Subtle icons & disabled text

// --- Lines and Surfaces ---
val ThornburyHairline = Color(0xFFDCEAE7)         // Card borders & dividers
val ThornburyHairlineSoft = Color(0xFFE7F1EE)     // Subtle table & grid lines
val ThornburyCanvas = Color(0xFFF5FAF9)           // Cool mint-white canvas
val ThornburySurfaceSoft = Color(0xFFEEF6F4)      // Subtle elevated sections & wells
val ThornburySurfaceCard = Color(0xFFE3EFEC)      // Product & patient record cards
val ThornburySurfaceCreamStrong = Color(0xFFD7E9E5) // Name kept for compatibility; now a strong teal-tinted surface, not cream
val ThornburySurfaceDark = Color(0xFF0A1512)      // Dark clinical surfaces & callouts
val ThornburySurfaceDarkElevated = Color(0xFF142621)
val ThornburySurfaceDarkSoft = Color(0xFF0F1D19)
val ThornburySurfaceDarkLowest = Color(0xFF060D0B) // Deepest base surface in dark mode
val ThornburySurfaceDarkHighest = Color(0xFF1D322C) // Highest elevated surface in dark mode
val ThornburyHairlineDark = Color(0xFF2B403B)     // High-contrast outline in dark mode
val ThornburyHairlineDarkSoft = Color(0xFF192824) // Subtle outline/divider in dark mode
val ThornburyOnDark = Color(0xFFEAF3F1)           // Cool off-white text on dark surfaces
val ThornburyOnDarkSoft = Color(0xFF9FB4AF)
val ThornburyScrim = Color(0xFF000000)

// --- Clinical Status Signals ---
// Success / Warning / Error keep their existing, already-measured values unchanged (and still
// match the web app's tokens) — these are patient-safety signals, not brand color, so they
// aren't part of this rethink.
val ThornburyPrimaryWash = Color(0xFFE6F3F0)      // Subtle teal wash
val ThornburyBorder = ThornburyHairline           // Standard border alias
val ThornburySuccess = Color(0xFF357A45)          // Safe / Complete / Normal
val ThornburySuccessWash = Color(0xFFE8F2EA)
val ThornburyWarning = Color(0xFF7A5D0C)          // Alerts / Caution / Pending
val ThornburyWarningWash = Color(0xFFF8EFD8)
val ThornburyError = Color(0xFFB23A3A)            // Critical / Allergy Contraindication / Decay
val ThornburyErrorWash = Color(0xFFF7E6E4)
val ThornburyAccentTeal = Color(0xFF5B7FC4)       // Specialty badges / Crown / Implants — periwinkle now that teal is the brand color
val ThornburyAccentAmber = Color(0xFFE8A55A)      // Tooth restoration (unchanged)
val ThornburyInfoWash = Color(0xFFEBEFF9)
val ThornburyTertiaryText = Color(0xFF1E3A68)     // High contrast text on ThornburyInfoWash (~9.8:1)

// --- Dark Mode Status Tokens ---
val ThornburyTertiaryDark = Color(0xFF9CB8F0)     // Bright periwinkle for dark mode
val ThornburyOnTertiaryDark = Color(0xFF0E254A)
val ThornburyTertiaryContainerDark = Color(0xFF23395E)
val ThornburyOnTertiaryContainerDark = Color(0xFFD6E3FF)
val ThornburyErrorDark = Color(0xFFE8756F)        // Accessible dark mode error (mirrors web globals.css)
val ThornburyOnErrorDark = Color(0xFF3E0A08)
val ThornburyErrorContainerDark = Color(0xFF2E1C1B) // Accessible dark mode error wash (mirrors web globals.css)
val ThornburyOnErrorContainerDark = Color(0xFFFFDAD6)

// --- Odontogram Specific Tokens ---
val ToothSound = Color(0xFFFDFCFB)
val ToothDecay = Color(0xFFD32F2F)
val ToothFilled = Color(0xFF1976D2)
val ToothCrown = Color(0xFFE8A55A)
val ToothMissing = Color(0xFFBDBDBD)
val ToothImplant = Color(0xFF7B1FA2)
val ToothRootCanal = Color(0xFF0F6E63)            // Follows the brand primary (was terracotta, now teal)
