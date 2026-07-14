package com.melodix.player.core.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorMathTest {

    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()

    @Test
    fun `luminance of white is near 1 and black is near 0`() {
        assertEquals(1.0, ColorMath.luminance(white), 0.001)
        assertEquals(0.0, ColorMath.luminance(black), 0.001)
    }

    @Test
    fun `isLight distinguishes white from black`() {
        assertTrue(ColorMath.isLight(white))
        assertFalse(ColorMath.isLight(black))
    }

    @Test
    fun `contrast color is black on light and white on dark`() {
        assertEquals(black, ColorMath.contrastColor(white))
        assertEquals(white, ColorMath.contrastColor(black))
    }

    @Test
    fun `blending black and white halfway yields mid grey`() {
        val mid = ColorMath.blend(black, white, 0.5f)
        val channel = mid and 0xFF
        assertTrue("expected ~127, got $channel", channel in 126..128)
        // alpha preserved as opaque
        assertEquals(0xFF, (mid shr 24) and 0xFF)
    }
}
