package com.example.thornburydental.theme

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3 window width size classes for responsive layout switching.
 */
enum class WindowWidthSizeClass {
    COMPACT,  // Phones (< 600dp)
    MEDIUM,   // Foldables / Small Tablets (600dp - 839dp)
    EXPANDED; // Large Tablets / Desktop (>= 840dp)

    companion object {
        val Compact get() = COMPACT
        val Medium get() = MEDIUM
        val Expanded get() = EXPANDED
    }
}

val LocalWindowWidthSizeClass = compositionLocalOf { WindowWidthSizeClass.COMPACT }

@Composable
fun rememberWindowWidthSizeClass(): WindowWidthSizeClass {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    return when {
        screenWidth < 600.dp -> WindowWidthSizeClass.COMPACT
        screenWidth < 840.dp -> WindowWidthSizeClass.MEDIUM
        else -> WindowWidthSizeClass.EXPANDED
    }
}

/**
 * Constrains dialogs and modals to appropriate readable widths based on screen class.
 */
fun Modifier.adaptiveDialogWidth(maxWidth: Dp = 600.dp): Modifier = this.widthIn(min = 300.dp, max = maxWidth)

/**
 * Constrains single-pane content containers so text lines don't stretch uncomfortably on wide screens.
 */
fun Modifier.adaptiveContentContainer(maxWidth: Dp = 900.dp): Modifier = this.widthIn(max = maxWidth)
