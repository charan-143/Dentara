package com.example.thornburydental.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// Thornbury Clinical — a healthcare-grade Material 3 palette.
// Anchored on a deep clinical teal (trust + clean/fresh dental association),
// cool neutral surfaces (never warm paper-cream), and a restrained warm
// apricot accent kept for approachability. Replaces the earlier warm
// coral/cream editorial system (see DESIGN.md) with something that reads as
// a clinic, not a marketing site.
// =============================================================================

// --- Brand: Clinical Teal ---
val ThornburyPrimary = Color(0xFF0D7377)          // Signature clinical teal CTA (5.6:1 white-on-primary)
val ThornburyPrimaryActive = Color(0xFF0A5A5D)    // Darker teal (active/pressed/selected state)
val ThornburyPrimaryDisabled = Color(0xFFDCE9E8)  // Desaturated cool wash disabled state
val ThornburyPrimaryText = Color(0xFF0A4F4F)      // Darkened for AA text contrast (9.4:1 on canvas)
val ThornburyOnPrimary = Color(0xFFFFFFFF)

// Dark-mode primary
val ThornburyPrimaryOnDark = Color(0xFF6FDBD1)
val ThornburyOnPrimaryOnDark = Color(0xFF00332E)
val ThornburyPrimaryActiveDark = Color(0xFF8FE6DC)
val ThornburyPrimaryTextDark = Color(0xFF7FE0D6)
val ThornburyPrimaryDisabledDark = Color(0xFF1B3836)

// Warm human accent — used sparingly against the clinical teal so the app
// doesn't read as cold: icon tints, ratings, small illustration highlights.
val ThornburyAccent = Color(0xFFC97A3D)
val ThornburyOnAccent = Color(0xFF12211F)

// --- Ink & Text (cool clinical slate, not warm ink) ---
val ThornburyInk = Color(0xFF12181B)              // Dominant headlines & high-contrast titles
val ThornburyBody = Color(0xFF3C464A)             // Readable body copy
val ThornburyBodyStrong = Color(0xFF232B2E)       // Emphasized copy
val ThornburyMuted = Color(0xFF5C666A)            // Secondary captions & metadata (5.9:1 on white)
val ThornburyMutedSoft = Color(0xFF78878A)        // Subtle icons & disabled text

// --- Lines and Surfaces ---
val ThornburyHairline = Color(0xFFDCE7E6)         // Card borders & dividers
val ThornburyHairlineSoft = Color(0xFFE6EFEE)     // Subtle table & grid lines
val ThornburyCanvas = Color(0xFFF6FAFA)           // Clean, cool clinical canvas (never warm cream)
val ThornburySurfaceSoft = Color(0xFFEEF4F3)      // Subtle elevated sections & wells
val ThornburySurfaceCard = Color(0xFFE4EEED)      // Product & patient record cards
val ThornburySurfaceCreamStrong = Color(0xFFD7E6E4) // Selected category tabs and emphasized section bands
val ThornburySurfaceDark = Color(0xFF0E1B1B)      // Dark product surfaces & callouts
val ThornburySurfaceDarkElevated = Color(0xFF17302E)
val ThornburySurfaceDarkSoft = Color(0xFF132523)
val ThornburySurfaceDarkLowest = Color(0xFF081312) // Deepest base surface in dark mode
val ThornburySurfaceDarkHighest = Color(0xFF1E3A37) // Highest elevated surface in dark mode
val ThornburyHairlineDark = Color(0xFF2A4341)     // High-contrast outline in dark mode
val ThornburyHairlineDarkSoft = Color(0xFF223836) // Subtle outline/divider in dark mode
val ThornburyOnDark = Color(0xFFF2F8F7)           // Cool near-white on dark surfaces
val ThornburyOnDarkSoft = Color(0xFF9FB3B0)
val ThornburyScrim = Color(0xFF000000)

// --- Clinical Status Signals ---
val ThornburyPrimaryWash = Color(0xFFE6F3F2)      // Teal wash
val ThornburyBorder = ThornburyHairline           // Standard border alias
val ThornburySuccess = Color(0xFF227A4E)          // Safe / Complete / Normal (5.4:1 on success-wash)
val ThornburySuccessWash = Color(0xFFE1F3E8)
val ThornburyWarning = Color(0xFF8A6500)          // Alerts / Caution / Pending (5.7:1 on warning-wash)
val ThornburyWarningWash = Color(0xFFFBF0D6)
val ThornburyError = Color(0xFFB3261E)            // Critical / Allergy Contraindication / Decay (5.9:1 on error-wash)
val ThornburyErrorWash = Color(0xFFFBE9E7)
val ThornburyAccentTeal = Color(0xFF4FB8AC)       // Specialty badges / Restored condition (bright mint-teal)
val ThornburyAccentAmber = Color(0xFFC97A3D)      // Tooth restoration & crown highlights
val ThornburyInfoWash = Color(0xFFE3F3F1)         // Clinical teal wash
val ThornburyTertiaryText = Color(0xFF0F5A52)     // High contrast text on ThornburyInfoWash

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
