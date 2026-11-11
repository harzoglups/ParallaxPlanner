package com.cussou.parallax.format

import com.cussou.parallax.domain.AngleUnit
import com.cussou.parallax.domain.DistanceUnit
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// Display rounding (NF-07). This is the only place where values are rounded.
// The functions return the number alone; the screen puts the unit next to it.

/** Setting rounded to the unit (m or yd). */
fun formatSetting(settingM: Double, unit: DistanceUnit, locale: Locale): String =
    formatNumber(metersToDisplay(settingM, unit), decimals = 0, locale)

/** Angular error with 2 decimals in mrad, 1 decimal in MOA. */
fun formatAngularError(errorMrad: Double, unit: AngleUnit, locale: Locale): String {
    val decimals = when (unit) {
        AngleUnit.MRAD -> 2
        AngleUnit.MOA -> 1
    }
    return formatNumber(mradToDisplay(errorMrad, unit), decimals, locale)
}

/** Linear error with 1 decimal, in cm or inches depending on the distance unit (F-24). */
fun formatLinearError(errorMm: Double, distanceUnit: DistanceUnit, locale: Locale): String =
    formatNumber(linearErrorToDisplay(errorMm, distanceUnit), decimals = 1, locale)

private fun formatNumber(value: Double, decimals: Int, locale: Locale): String {
    val pattern = if (decimals == 0) "0" else "0." + "0".repeat(decimals)
    return DecimalFormat(pattern, DecimalFormatSymbols.getInstance(locale))
        .apply { roundingMode = RoundingMode.HALF_UP }
        .format(value)
}

private val DECIMAL_INPUT = Regex("""\d+[.,]?\d*|[.,]\d+""")

/**
 * Number typed by the user, with a dot or a comma as decimal separator whatever the locale.
 * Returns null for anything else, including an empty field and negative numbers.
 */
fun parseDecimal(text: String): Double? {
    val trimmed = text.trim()
    if (!DECIMAL_INPUT.matches(trimmed)) return null
    return trimmed.replace(',', '.').toDoubleOrNull()
}
