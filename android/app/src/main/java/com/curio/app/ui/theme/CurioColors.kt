package com.curio.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.curio.app.CurioApp

/**
 * Reactive color bundle that switches between dark and light palettes
 * based on CurioApp.darkThemeEnabled.
 */
data class CurioColors(
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outlineVariant: Color,
    val scrim: Color,
    val primary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val error: Color,
    val cardGradientStart: Color,
    val cardGradientEnd: Color,
    val accentGradientStart: Color,
    val accentGradientMid: Color,
    val accentGradientEnd: Color,
    val bookmarkActive: Color
)

val DarkCurioColors = CurioColors(
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    outlineVariant = OutlineVariant,
    scrim = Scrim,
    primary = Primary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    error = Error,
    cardGradientStart = CardGradientStart,
    cardGradientEnd = CardGradientEnd,
    accentGradientStart = AccentGradientStart,
    accentGradientMid = AccentGradientMid,
    accentGradientEnd = AccentGradientEnd,
    bookmarkActive = BookmarkActive
)

val LightCurioColors = CurioColors(
    surface = LightSurface,
    surfaceVariant = LightSurfaceContainerHighest,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    outlineVariant = LightOutlineVariant,
    scrim = Scrim,
    primary = LightPrimary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    error = LightError,
    cardGradientStart = LightCardGradientStart,
    cardGradientEnd = LightCardGradientEnd,
    accentGradientStart = LightAccentGradientStart,
    accentGradientMid = LightAccentGradientMid,
    accentGradientEnd = LightAccentGradientEnd,
    bookmarkActive = LightBookmarkActive
)

/**
 * Returns the correct color set based on the current theme setting.
 */
@Composable
fun curioColors(): CurioColors {
    return if (CurioApp.darkThemeEnabled) DarkCurioColors else LightCurioColors
}
