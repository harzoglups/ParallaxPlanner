package com.cussou.parallax.ui.quickcalc

import com.cussou.parallax.domain.DistanceUnit
import com.cussou.parallax.domain.SettingGroup
import com.cussou.parallax.domain.Target
import com.cussou.parallax.domain.TargetStatus
import com.cussou.parallax.domain.TargetsSetting
import com.cussou.parallax.domain.UserPreferences
import com.cussou.parallax.domain.effectiveOffsetMm
import com.cussou.parallax.domain.errorMrad
import com.cussou.parallax.domain.evaluateSetting
import com.cussou.parallax.domain.linearErrorMm
import com.cussou.parallax.domain.minimalSettings
import com.cussou.parallax.domain.optimalSettingForRange
import com.cussou.parallax.domain.optimalSettingForTargets
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
    val currentSettingError: Boolean = false,
    val comparison: SettingComparison? = null,
    /** Set instead of [result] when advanced mode is on and there are targets (F-51, F-52). */
    val targetsResult: TargetsSetting? = null,
    /** The targets evaluated at the fixed setting, when the user typed a valid one (F-56). */
    val forcedResult: TargetsSetting? = null,
    /** The fewest settings that leave no target red; empty unless a target is red (F-55). */
    val suggestedSettings: List<SettingGroup> = emptyList(),
) {
    /** What the target rows and the card show: the fixed setting if there is one, otherwise the optimal one. */
    val shownTargetsResult: TargetsSetting? get() = forcedResult ?: targetsResult
}

/** What the range gives if the user keeps the setting the dial is on now (F-06). */
data class SettingComparison(
    val settingM: Double,
    /** The worse of the errors at the two ends of the range. */
    val maxErrorMrad: Double,
    val linearErrorAtMinMm: Double,
    val linearErrorAtMaxMm: Double,
)

/**
 * From the typed range to the result (F-01 to F-05), with the optional comparison against
 * the current setting (F-06). [unit] is the unit the texts were typed in.
 * In advanced mode, [targets] replace the range as soon as there is one; [currentSettingText] is
 * then the fixed setting the targets are evaluated at (F-56).
 */
fun quickCalcOutcome(
    minText: String,
    maxText: String,
    unit: DistanceUnit,
    preferences: UserPreferences,
    currentSettingText: String = "",
    targets: List<Target> = emptyList(),
): QuickCalcOutcome {
    val offsetMm = effectiveOffsetMm(preferences.objectiveMm, preferences.eyePosition)
    // With targets, the range comes from them and the range fields play no part (F-51).
    // Targets are ignored, not lost, while advanced mode is off (F-41).
    if (preferences.advancedMode && targets.isNotEmpty()) {
        val targetsResult = optimalSettingForTargets(targets, offsetMm, preferences.orangeThreshold)
        val fixed = typedDistance(currentSettingText)
        val forcedResult = (fixed as? Typed.Valid)?.let {
            evaluateSetting(targets, offsetMm, preferences.orangeThreshold, displayToMeters(it.value, unit))
        }
        // Suggestions follow the setting actually used.
        val anyRed = (forcedResult ?: targetsResult).perTarget.any { it.status == TargetStatus.RED }
        return QuickCalcOutcome(
            targetsResult = targetsResult,
            currentSettingError = fixed is Typed.Invalid,
            forcedResult = forcedResult,
            suggestedSettings = if (anyRed) minimalSettings(targets, offsetMm) else emptyList(),
        )
    }
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
    val setting = optimalSettingForRange(minM, maxM, offsetMm)
    val current = typedDistance(currentSettingText)
    return QuickCalcOutcome(
        currentSettingError = current is Typed.Invalid,
        comparison = (current as? Typed.Valid)?.let { comparison(displayToMeters(it.value, unit), minM, maxM, offsetMm) },
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

/** The worst status among the sized targets, and how many targets have it. */
data class StatusSummary(val status: TargetStatus, val count: Int)

/**
 * One-line summary for the result card (docs/03-ux.md §3): red targets if any, otherwise orange
 * ones, otherwise the green ones. Null when no target has a size, as there is nothing to judge.
 */
fun statusSummary(result: TargetsSetting): StatusSummary? =
    listOf(TargetStatus.RED, TargetStatus.ORANGE, TargetStatus.GREEN).firstNotNullOfOrNull { status ->
        val count = result.perTarget.count { it.status == status }
        if (count > 0) StatusSummary(status, count) else null
    }

private fun comparison(settingM: Double, minM: Double, maxM: Double, offsetMm: Double): SettingComparison {
    val errorAtMinMrad = errorMrad(offsetMm, minM, settingM)
    val errorAtMaxMrad = errorMrad(offsetMm, maxM, settingM)
    return SettingComparison(
        settingM = settingM,
        maxErrorMrad = maxOf(errorAtMinMrad, errorAtMaxMrad),
        linearErrorAtMinMm = linearErrorMm(errorAtMinMrad, minM),
        linearErrorAtMaxMm = linearErrorMm(errorAtMaxMrad, maxM),
    )
}

internal sealed interface Typed {
    data object Empty : Typed
    data object Invalid : Typed
    data class Valid(val value: Double) : Typed
}

internal fun typedDistance(text: String): Typed {
    if (text.isBlank()) return Typed.Empty
    val value = parseDecimal(text) ?: return Typed.Invalid
    return if (value in MIN_DISTANCE..MAX_DISTANCE) Typed.Valid(value) else Typed.Invalid
}
