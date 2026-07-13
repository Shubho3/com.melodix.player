package com.melodix.player.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

enum class AppTheme {
    MONO, MIDNIGHT, SUNSHINE, AQUA, NATURE, TECHNO
}

/** Human-readable name for the theme picker. */
fun AppTheme.displayName(): String = when (this) {
    AppTheme.MONO -> "Monochrome"
    else -> name.lowercase().replaceFirstChar { it.uppercase() }
}

@Composable
fun MelodixTheme(
    appTheme: AppTheme = AppTheme.MONO,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = appTheme.toColorScheme(),
        typography = MelodixTypography,
        shapes = MelodixShapes,
        content = content,
    )
}
