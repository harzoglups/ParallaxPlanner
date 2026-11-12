package com.cussou.parallax.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Colors of the provisional style guide (docs/06-name-and-icons.md): azure blue accent
// on a dark slate. Every text / background pair used on screen reaches AA contrast (NF-02),
// which ColorContrastTest checks.

private val Azure = Color(0xFF2FB4FF)
private val Slate = Color(0xFF1F2630)
private val LightStroke = Color(0xFFE6EDF3)
private val SecondaryStroke = Color(0xFF9DA7B3)

val DarkColors: ColorScheme = darkColorScheme(
    primary = Azure,
    onPrimary = Slate,
    primaryContainer = Color(0xFF0B3A57),
    onPrimaryContainer = Color(0xFFCDEBFF),
    secondary = SecondaryStroke,
    onSecondary = Slate,
    secondaryContainer = Color(0xFF3A4553),
    onSecondaryContainer = LightStroke,
    tertiary = Azure,
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

// The guide's azure is too light for text on a light background (2.2:1), so the light theme
// uses a darker shade of the same hue.
private val DeepAzure = Color(0xFF006A9E)

val LightColors: ColorScheme = lightColorScheme(
    primary = DeepAzure,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDE9FF),
    onPrimaryContainer = Color(0xFF001E30),
    secondary = Color(0xFF4A5562),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE3EA),
    onSecondaryContainer = Slate,
    tertiary = DeepAzure,
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

/**
 * Colors of the green / orange / red target statuses (F-53). They always come with an icon and
 * a text (F-54); they reach AA contrast and differ in luminance, which ColorContrastTest checks.
 * The "orange" status is a yellow-amber in the dark theme, so it reads as a warning, not as red.
 */
data class StatusColors(val green: Color, val orange: Color, val red: Color)

val DarkStatusColors = StatusColors(green = Color(0xFF5FCB80), orange = Color(0xFFFFDD7A), red = Color(0xFFFF7A6E))
val LightStatusColors = StatusColors(green = Color(0xFF0F5A24), orange = Color(0xFF8A5A00), red = Color(0xFF7F1712))

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

/** App theme: Material 3 with the style guide colors and fonts, light or dark (NF-02). */
@Composable
fun ParallaxPlannerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            content = content,
        )
    }
}
