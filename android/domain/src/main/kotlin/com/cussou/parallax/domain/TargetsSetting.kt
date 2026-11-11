package com.cussou.parallax.domain

import kotlin.math.abs

// Setting for a set of targets, docs/02-calculations.md §4.

/**
 * Result for one target at a given setting.
 *
 * @property ratio error / half-size; null, like [halfSizeMrad], when the target has no size.
 */
data class TargetResult(
    val errorMrad: Double,
    val halfSizeMrad: Double?,
    val ratio: Double?,
    val status: TargetStatus,
)

/**
 * A setting and what it gives on each target, in the order the targets were passed.
 *
 * @property maxRatio worst ratio among the sized targets; null when no target has a size.
 */
data class TargetsSetting(
    val settingM: Double,
    val maxRatio: Double?,
    val perTarget: List<TargetResult>,
)

/**
 * Best setting for [targets] (F-52): the weighted minimax when every target has a size,
 * otherwise the range calculation over all the targets.
 */
fun optimalSettingForTargets(targets: List<Target>, offsetMm: Double, orangeThreshold: Double): TargetsSetting {
    requireValid(targets)
    val settingM = if (targets.all { it.size != null }) {
        weightedMinimaxSettingM(targets)
    } else {
        optimalSettingForRange(targets.minOf { it.distanceM }, targets.maxOf { it.distanceM }, offsetMm).settingM
    }
    return evaluateSetting(targets, offsetMm, orangeThreshold, settingM)
}

/** Errors and statuses of [targets] for a given setting, for example a forced one (F-56). */
fun evaluateSetting(
    targets: List<Target>,
    offsetMm: Double,
    orangeThreshold: Double,
    settingM: Double,
): TargetsSetting {
    requireValid(targets)
    require(settingM > 0.0) { "Invalid setting: $settingM" }
    val perTarget = targets.map { target ->
        val errorMrad = errorMrad(offsetMm, target.distanceM, settingM)
        val halfSizeMrad = target.halfSizeMrad()
        val ratio = halfSizeMrad?.let { errorMrad / it }
        TargetResult(errorMrad, halfSizeMrad, ratio, status(ratio, orangeThreshold))
    }
    return TargetsSetting(settingM, perTarget.mapNotNull { it.ratio }.maxOrNull(), perTarget)
}

private fun status(ratio: Double?, orangeThreshold: Double): TargetStatus = when {
    ratio == null -> TargetStatus.NEUTRAL
    ratio <= orangeThreshold -> TargetStatus.GREEN
    ratio <= 1.0 -> TargetStatus.ORANGE
    else -> TargetStatus.RED
}

/**
 * Setting that minimizes the worst error / half-size ratio. Every target must have a size.
 *
 * Works on u = 1/s, where each ratio is |x_i − u| / a_i with x_i = 1/d_i and a_i the half-size.
 * The worst ratio is convex and piecewise linear in u, so its minimum is at a target (x_i)
 * or where two targets have the same ratio (u_ij). Checking every candidate is exact, and
 * O(n³) is fine for the few targets of a stage.
 *
 * The doc writes the tolerance as b_i = a_i / y; the offset y scales every ratio alike,
 * so it does not move the minimum and a_i is used directly.
 */
internal fun weightedMinimaxSettingM(targets: List<Target>): Double {
    val points = targets.map { InversePoint(x = 1.0 / it.distanceM, halfSizeMrad = it.halfSizeMrad()!!) }
    val candidates = points.map { it.x } + points.flatMap { i ->
        points.map { j -> (i.x * j.halfSizeMrad + j.x * i.halfSizeMrad) / (i.halfSizeMrad + j.halfSizeMrad) }
    }
    // Sorted ascending and minBy keeps the first minimum: on a tie the smallest u wins (farthest setting).
    val bestU = candidates.sorted().minBy { u -> points.maxOf { abs(it.x - u) / it.halfSizeMrad } }
    return 1.0 / bestU
}

private class InversePoint(val x: Double, val halfSizeMrad: Double)

private fun requireValid(targets: List<Target>) {
    require(targets.isNotEmpty()) { "At least one target is required" }
    for (target in targets) {
        require(target.distanceM > 0.0) { "Invalid target distance: ${target.distanceM}" }
        require(target.size == null || target.size.value > 0.0) { "Invalid target size: ${target.size}" }
    }
}
