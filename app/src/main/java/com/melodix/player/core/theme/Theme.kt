package com.melodix.player.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.melodix.player.model.CustomThemeColors

enum class AppTheme {
    MONO, MIDNIGHT, SUNSHINE, AQUA, NATURE, TECHNO, CUSTOM
}

/** Human-readable name for the theme picker. */
fun AppTheme.displayName(): String = when (this) {
    AppTheme.MONO -> "Monochrome"
    AppTheme.CUSTOM -> "Custom"
    else -> name.lowercase().replaceFirstChar { it.uppercase() }
}

@Composable
fun MelodixTheme(
    appTheme: AppTheme = AppTheme.MONO,
    customColors: CustomThemeColors? = null,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (appTheme == AppTheme.CUSTOM && customColors != null) {
        buildCustomScheme(customColors)
    } else {
        appTheme.toColorScheme()
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MelodixTypography,
        shapes = MelodixShapes,
        content = content,
    )
}
