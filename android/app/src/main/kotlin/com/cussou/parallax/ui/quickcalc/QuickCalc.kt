package com.cussou.parallax.ui.quickcalc

import com.cussou.parallax.domain.DistanceUnit
import com.cussou.parallax.domain.UserPreferences
import com.cussou.parallax.domain.effectiveOffsetMm
import com.cussou.parallax.domain.linearErrorMm
import com.cussou.parallax.domain.optimalSettingForRange
import com.cussou.parallax.format.displayToMeters
import com.cussou.parallax.format.parseDecimal

/** Limits of a typed distance, in the unit it is typed in (F-05). */
const val MIN_DISTANCE = 5.0
const val MAX_DISTANCE = 2000.0

/** Result of the quick calc for a range, in internal units. */
data class QuickCalcResult(
    val minM: Double,
    val maxM: Double,
    val settingM: Double,
    val maxErrorMrad: Double,
    val linearErrorAtMinMm: Double,
    val linearErrorAtMaxMm: Double,
    /** True when the user typed min > max and the range was swapped (F-05). */
    val swapped: Boolean,
)

/**
 * What the screen shows for the typed range: a field can be flagged, and there is a result
 * only once both fields hold a valid distance.
 */
data class QuickCalcOutcome(
    val minError: Boolean = false,
    val maxError: Boolean = false,
    val result: QuickCalcResult? = null,
)

/** From the typed range to the result (F-01 to F-05). [unit] is the unit the texts were typed in. */
fun quickCalcOutcome(
    minText: String,
    maxText: String,
    unit: DistanceUnit,
    preferences: UserPreferences,
): QuickCalcOutcome {
    val min = typedDistance(minText)
    val max = typedDistance(maxText)
    if (min is Typed.Invalid || max is Typed.Invalid) {
        return QuickCalcOutcome(minError = min is Typed.Invalid, maxError = max is Typed.Invalid)
    }
    if (min !is Typed.Valid || max !is Typed.Valid) return QuickCalcOutcome()

    val first = displayToMeters(min.value, unit)
    val second = displayToMeters(max.value, unit)
    val minM = minOf(first, second)
    val maxM = maxOf(first, second)
    val offsetMm = effectiveOffsetMm(preferences.objectiveMm, preferences.eyePosition)
    val setting = optimalSettingForRange(minM, maxM, offsetMm)
    return QuickCalcOutcome(
        result = QuickCalcResult(
            minM = minM,
            maxM = maxM,
            settingM = setting.settingM,
            maxErrorMrad = setting.maxErrorMrad,
            linearErrorAtMinMm = linearErrorMm(setting.maxErrorMrad, minM),
            linearErrorAtMaxMm = linearErrorMm(setting.maxErrorMrad, maxM),
            swapped = first > second,
        ),
    )
}

private sealed interface Typed {
    data object Empty : Typed
    data object Invalid : Typed
    data class Valid(val value: Double) : Typed
}

private fun typedDistance(text: String): Typed {
    if (text.isBlank()) return Typed.Empty
    val value = parseDecimal(text) ?: return Typed.Invalid
    return if (value in MIN_DISTANCE..MAX_DISTANCE) Typed.Valid(value) else Typed.Invalid
}
