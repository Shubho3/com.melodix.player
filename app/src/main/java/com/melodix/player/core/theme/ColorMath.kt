package com.melodix.player.core.theme

/**
 * Pure ARGB color math used to derive a full Material 3 palette from a few user-picked colors.
 * Deliberately free of Android/Compose types so it is unit-testable on the JVM.
 */
object ColorMath {

    /** WCAG relative luminance in 0..1 for an opaque ARGB int (alpha ignored). */
    fun luminance(argb: Int): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    fun isLight(argb: Int): Boolean = luminance(argb) > 0.5

    /** Black on light colors, white on dark ones — for readable on-color text. */
    fun contrastColor(argb: Int): Int =
        if (isLight(argb)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()

    /** Linear per-channel blend of two opaque colors; [ratio] 0 → [a], 1 → [b]. Result is opaque. */
    fun blend(a: Int, b: Int, ratio: Float): Int {
        fun mix(shift: Int): Int {
            val ca = (a shr shift) and 0xFF
            val cb = (b shr shift) and 0xFF
            return (ca * (1 - ratio) + cb * ratio).toInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }
}
