package com.cussou.parallax.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.cussou.parallax.R

// Fonts of the style guide (docs/06-name-and-icons.md), bundled in res/font under the
// SIL Open Font License (texts in assets/licenses). Both have tabular figures ("tnum").

/** Headings and the large setting values. */
val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

/** Body text, labels and target rows. */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
)

private val Default = Typography()

private fun TextStyle.heading() = copy(fontFamily = SpaceGrotesk)
private fun TextStyle.body() = copy(fontFamily = Inter)

/** Material 3 type scale: Space Grotesk for display, headline and titleLarge, Inter for the rest. */
val AppTypography = Typography(
    displayLarge = Default.displayLarge.heading(),
    displayMedium = Default.displayMedium.heading(),
    displaySmall = Default.displaySmall.heading(),
    headlineLarge = Default.headlineLarge.heading(),
    headlineMedium = Default.headlineMedium.heading(),
    headlineSmall = Default.headlineSmall.heading(),
    titleLarge = Default.titleLarge.heading(),
    titleMedium = Default.titleMedium.body(),
    titleSmall = Default.titleSmall.body(),
    bodyLarge = Default.bodyLarge.body(),
    bodyMedium = Default.bodyMedium.body(),
    bodySmall = Default.bodySmall.body(),
    labelLarge = Default.labelLarge.body(),
    labelMedium = Default.labelMedium.body(),
    labelSmall = Default.labelSmall.body(),
)
