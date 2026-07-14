package com.melodix.player.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.melodix.player.model.CustomThemeColors

/**
 * Derives a full Material 3 [ColorScheme] from three user-picked colors (accent, background, text).
 * Light vs dark is decided by the background's luminance. The color math lives in [ColorMath] (and
 * is unit-tested); this function only assembles Compose [Color]s onto a light/dark base.
 */
fun buildCustomScheme(colors: CustomThemeColors): ColorScheme {
    val accent = colors.accent
    val background = colors.background
    val text = colors.text
    val light = ColorMath.isLight(background)

    fun col(argb: Int) = Color(argb)
    // Subtle elevation tints: blend background toward the text color (works in light and dark).
    fun container(ratio: Float) = col(ColorMath.blend(background, text, ratio))

    val primaryContainerArgb = ColorMath.blend(accent, background, 0.55f)
    val secondaryArgb = ColorMath.blend(accent, text, 0.15f)
    val tertiaryArgb = ColorMath.blend(accent, background, 0.30f)

    val base = if (light) lightColorScheme() else darkColorScheme()
    return base.copy(
        primary = col(accent),
        onPrimary = col(ColorMath.contrastColor(accent)),
        primaryContainer = col(primaryContainerArgb),
        onPrimaryContainer = col(ColorMath.contrastColor(primaryContainerArgb)),
        secondary = col(secondaryArgb),
        onSecondary = col(ColorMath.contrastColor(secondaryArgb)),
        secondaryContainer = container(0.12f),
        onSecondaryContainer = col(text),
        tertiary = col(tertiaryArgb),
        onTertiary = col(ColorMath.contrastColor(tertiaryArgb)),
        background = col(background),
        onBackground = col(text),
        surface = col(background),
        onSurface = col(text),
        surfaceVariant = container(0.10f),
        onSurfaceVariant = col(ColorMath.blend(text, background, 0.25f)),
        outline = col(ColorMath.blend(text, background, 0.45f)),
        outlineVariant = col(ColorMath.blend(text, background, 0.72f)),
        surfaceTint = col(accent),
        surfaceDim = container(0.06f),
        surfaceBright = container(0.0f),
        surfaceContainerLowest = container(0.0f),
        surfaceContainerLow = container(0.03f),
        surfaceContainer = container(0.05f),
        surfaceContainerHigh = container(0.08f),
        surfaceContainerHighest = container(0.11f),
    )
}
