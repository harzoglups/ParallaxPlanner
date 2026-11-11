package com.cussou.parallax.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Colors of the provisional style guide (docs/06-name-and-icons.md) and their contrast (NF-02). */
class ColorContrastTest {
    @Test
    fun `dark theme uses the colors of the style guide`() {
        assertEquals(Color(0xFFF28C28), DarkColors.primary)
        assertEquals(Color(0xFF1F2630), DarkColors.background)
        assertEquals(Color(0xFF1F2630), DarkColors.surface)
        assertEquals(Color(0xFFE6EDF3), DarkColors.onSurface)
        assertEquals(Color(0xFF9DA7B3), DarkColors.onSurfaceVariant)
    }

    @Test
    fun `NF-02 text reaches AA contrast in the dark theme`() = assertTextContrast("dark", DarkColors)

    @Test
    fun `NF-02 text reaches AA contrast in the light theme`() = assertTextContrast("light", LightColors)

    @Test
    fun `NF-02 field outlines are visible in both themes`() {
        // 3:1 is the WCAG minimum for the boundary of an input control.
        assertTrue(contrast(DarkColors.outline, DarkColors.surface) >= 3.0)
        assertTrue(contrast(LightColors.outline, LightColors.surface) >= 3.0)
    }

    /** Every text / background pair the screens use must reach 4.5:1 (WCAG AA). */
    private fun assertTextContrast(theme: String, colors: ColorScheme) {
        val pairs = mapOf(
            "text on background" to (colors.onBackground to colors.background),
            "text on surface" to (colors.onSurface to colors.surface),
            "secondary text on surface" to (colors.onSurfaceVariant to colors.surface),
            "accent text on surface" to (colors.primary to colors.surface),
            "text on accent button" to (colors.onPrimary to colors.primary),
            "text on selected choice" to (colors.onSecondaryContainer to colors.secondaryContainer),
            "text on result card and dialogs" to (colors.onSurface to colors.surfaceContainerHigh),
            "secondary text on result card and dialogs" to (colors.onSurfaceVariant to colors.surfaceContainerHigh),
            "setting on result card" to (colors.primary to colors.surfaceContainerHigh),
            "error text on surface" to (colors.error to colors.surface),
        )
        for ((name, pair) in pairs) {
            val ratio = contrast(pair.first, pair.second)
            assertTrue(ratio >= 4.5, "$theme: $name has contrast %.2f".format(ratio))
        }
    }

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** WCAG relative luminance of an opaque sRGB color. */
    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double =
            if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }
}
