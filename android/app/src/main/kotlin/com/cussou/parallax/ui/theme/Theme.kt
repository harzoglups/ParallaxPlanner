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
import androidx.compose.ui.graphics.lerp
import com.cussou.parallax.domain.AccentColor

// Colors of the provisional style guide (docs/06-name-and-icons.md): an accent on a dark slate,
// azure by default. Every text / background pair used on screen reaches AA contrast (NF-02)
// with every accent, which ColorContrastTest checks.

/**
 * The two shades of an accent: [dark] for the dark theme, [light] for the light theme. The
 * guide's light shades are too pale for text on a light background (azure: 2.2:1), so the
 * light theme uses a darker shade of the same hue.
 */
data class AccentShades(val dark: Color, val light: Color)

/** The accent colors the user can choose (F-15), azure first. */
fun AccentColor.shades(): AccentShades = when (this) {
    AccentColor.AZURE -> AccentShades(Color(0xFF2FB4FF), Color(0xFF006A9E))
    AccentColor.SKY_BLUE -> AccentShades(Color(0xFF4DA3FF), Color(0xFF1F5FAD))
    AccentColor.ELECTRIC_BLUE -> AccentShades(Color(0xFF5C9DFF), Color(0xFF1A56C4))
    AccentColor.ROYAL_BLUE -> AccentShades(Color(0xFF7F9BFF), Color(0xFF3046C2))
    AccentColor.GLACIER_BLUE -> AccentShades(Color(0xFF8CC4FF), Color(0xFF2A5F94))
    AccentColor.CYAN -> AccentShades(Color(0xFF2CD3E1), Color(0xFF00727D))
    AccentColor.VIOLET -> AccentShades(Color(0xFFA78BFA), Color(0xFF6240C9))
    AccentColor.ORANGE -> AccentShades(Color(0xFFF28C28), Color(0xFF9A4F00))
    AccentColor.BRIGHT_ORANGE -> AccentShades(Color(0xFFFF7B1C), Color(0xFFA34200))
    AccentColor.LIME -> AccentShades(Color(0xFFB8F04A), Color(0xFF4A6B00))
}

private val Slate = Color(0xFF1F2630)
private val LightStroke = Color(0xFFE6EDF3)
private val SecondaryStroke = Color(0xFF9DA7B3)

/** Dark theme with the given accent. */
fun darkColors(accent: AccentColor): ColorScheme = accent.shades().dark.let { primary ->
    DarkBase.copy(
        primary = primary,
        primaryContainer = lerp(Slate, primary, 0.35f),
        onPrimaryContainer = lerp(primary, Color.White, 0.75f),
        tertiary = primary,
    )
}

private val DarkBase: ColorScheme = darkColorScheme(
    onPrimary = Slate,
    secondary = SecondaryStroke,
    onSecondary = Slate,
    secondaryContainer = Color(0xFF3A4553),
    onSecondaryContainer = LightStroke,
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

/** Light theme with the given accent. */
fun lightColors(accent: AccentColor): ColorScheme = accent.shades().light.let { primary ->
    LightBase.copy(
        primary = primary,
        primaryContainer = lerp(Color.White, primary, 0.18f),
        onPrimaryContainer = lerp(primary, Color.Black, 0.6f),
        tertiary = primary,
    )
}

private val LightBase: ColorScheme = lightColorScheme(
    onPrimary = Color.White,
    secondary = Color(0xFF4A5562),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE3EA),
    onSecondaryContainer = Slate,
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
fun ParallaxPlannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: AccentColor = AccentColor.AZURE,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkColors(accent) else lightColors(accent),
            typography = AppTypography,
            content = content,
        )
    }
}
