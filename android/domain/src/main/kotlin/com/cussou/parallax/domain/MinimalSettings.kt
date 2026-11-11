package com.cussou.parallax.domain

// Minimum number of settings so that no target is red, docs/02-calculations.md §5 (F-55).

/** One suggested setting and the targets it covers, as indices into the list passed to [minimalSettings]. */
data class SettingGroup(val settingM: Double, val targetIndices: List<Int>)

/**
 * Fewest settings that keep every sized target out of red, sorted from the shortest to the longest setting.
 * Targets without size cannot be red and are left out of the groups.
 *
 * On the u = 1/s axis each target accepts the interval [x − b, x + b], with b = half-size / offset.
 * Stabbing all intervals with the fewest points is solved exactly by the greedy sweep on right bounds.
 * Each group then gets its own optimal setting, which leaves more margin than the stabbing point.
 */
fun minimalSettings(targets: List<Target>, offsetMm: Double): List<SettingGroup> {
    require(targets.isNotEmpty()) { "At least one target is required" }
    require(offsetMm > 0.0) { "Invalid offset: $offsetMm" }

    val intervals = targets.mapIndexedNotNull { index, target ->
        target.halfSizeMrad()?.let { halfSizeMrad ->
            val x = 1.0 / target.distanceM
            val tolerance = halfSizeMrad / offsetMm
            AdmissibleInterval(index, left = x - tolerance, right = x + tolerance)
        }
    }

    val groups = mutableListOf<MutableList<Int>>()
    var lastPoint = Double.NEGATIVE_INFINITY
    for (interval in intervals.sortedBy { it.right }) {
        if (interval.left > lastPoint) {
            groups += mutableListOf<Int>()
            lastPoint = interval.right
        }
        groups.last() += interval.targetIndex
    }

    return groups
        .map { indices -> SettingGroup(weightedMinimaxSettingM(indices.map { targets[it] }), indices.sorted()) }
        .sortedBy { it.settingM }
}

private class AdmissibleInterval(val targetIndex: Int, val left: Double, val right: Double)
