package com.cussou.parallax.domain

import kotlin.math.abs

// Parallax error, docs/02-calculations.md §2. Thin-lens, small-angle model.

/**
 * Offset of the line of sight at the objective, in mm: y = f × D / 2.
 * Magnification cancels out, so only the objective diameter matters.
 */
fun effectiveOffsetMm(objectiveMm: Double, eyePosition: EyePosition): Double =
    eyePosition.fraction * objectiveMm / 2.0

/** Angular parallax error in mrad for a target at [distanceM] with the dial set to [settingM]. */
fun errorMrad(offsetMm: Double, distanceM: Double, settingM: Double): Double =
    offsetMm * abs(1.0 / distanceM - 1.0 / settingM)

/** Linear error on the target in mm for an angular error at [distanceM]. */
fun linearErrorMm(errorMrad: Double, distanceM: Double): Double = errorMrad * distanceM
