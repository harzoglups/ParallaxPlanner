package com.cussou.parallax.domain

/** Optimal parallax setting for a distance range, with the worst angular error it leaves. */
data class RangeSetting(val settingM: Double, val maxErrorMrad: Double)

/**
 * Setting that minimizes the maximum angular error over [minM, maxM] (docs/02-calculations.md §3).
 *
 * The error is linear in 1/s, so the optimum makes both ends equal: the harmonic mean of the bounds.
 * Requires 0 < minM <= maxM; swapping a reversed range is the caller's job, because it must be shown (F-05).
 */
fun optimalSettingForRange(minM: Double, maxM: Double, offsetMm: Double): RangeSetting {
    require(minM > 0.0 && minM <= maxM) { "Invalid range: $minM..$maxM" }
    return RangeSetting(
        settingM = 2.0 * minM * maxM / (minM + maxM),
        maxErrorMrad = offsetMm * (1.0 / minM - 1.0 / maxM) / 2.0,
    )
}
