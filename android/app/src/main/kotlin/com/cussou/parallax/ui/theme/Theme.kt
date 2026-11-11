package com.cussou.parallax.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colors of the provisional style guide (docs/06-name-and-icons.md): "gong steel" orange accent
// on a dark slate. Every text / background pair used on screen reaches AA contrast (NF-02),
// which ColorContrastTest checks.

private val GongOrange = Color(0xFFF28C28)
private val Slate = Color(0xFF1F2630)
private val LightStroke = Color(0xFFE6EDF3)
private val SecondaryStroke = Color(0xFF9DA7B3)

val DarkColors: ColorScheme = darkColorScheme(
    primary = GongOrange,
    onPrimary = Slate,
    primaryContainer = Color(0xFF5A3A10),
    onPrimaryContainer = Color(0xFFFFE0BF),
    secondary = SecondaryStroke,
    onSecondary = Slate,
    secondaryContainer = Color(0xFF3A4553),
    onSecondaryContainer = LightStroke,
    tertiary = GongOrange,
    onTertiary = Slate,
    background = Slate,
    onBackground = LightStroke,
    surface = Slate,
    onSurface = LightStroke,
    surfaceVariant = Color(0xFF2B3542),
    onSurfaceVariant = SecondaryStroke,
    surfaceDim = Color(0xFF1A2028),
    surfaceBright = Color(0xFF37424F),
    surfaceContainerLowest = Color(0xFF171D25),
    surfaceContainerLow = Color(0xFF222A35),
    surfaceContainer = Color(0xFF252E3A),
    surfaceContainerHigh = Color(0xFF2B3542),
    surfaceContainerHighest = Color(0xFF313C4A),
    outline = SecondaryStroke,
    outlineVariant = Color(0xFF3A4553),
)

// The guide's orange is too light for text on a light background (2.3:1), so the light theme
// uses a darker shade of the same hue.
private val DeepOrange = Color(0xFF9A4F00)

val LightColors: ColorScheme = lightColorScheme(
    primary = DeepOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCBE),
    onPrimaryContainer = Color(0xFF3A1D00),
    secondary = Color(0xFF4A5562),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE3EA),
    onSecondaryContainer = Slate,
    tertiary = DeepOrange,
    onTertiary = Color.White,
    background = Color(0xFFF7F9FB),
    onBackground = Slate,
    surface = Color(0xFFF7F9FB),
    onSurface = Slate,
    surfaceVariant = Color(0xFFE5EAEF),
    onSurfaceVariant = Color(0xFF4A5562),
    surfaceDim = Color(0xFFD9DFE5),
    surfaceBright = Color(0xFFF7F9FB),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF1F4F7),
    surfaceContainer = Color(0xFFEBEFF3),
    surfaceContainerHigh = Color(0xFFE5EAEF),
    surfaceContainerHighest = Color(0xFFDFE5EB),
    outline = Color(0xFF6B7785),
    outlineVariant = Color(0xFFC4CCD5),
)

/** App theme: Material 3 with the style guide colors, light or dark (NF-02). */
@Composable
fun ParallaxPlannerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
