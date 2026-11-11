package com.cussou.parallax.domain

/**
 * One target of a stage (F-50): a distance and, optionally, its smallest dimension and a label
 * such as "T3" or "red gong". The label plays no part in the calculations.
 */
data class Target(val distanceM: Double, val size: TargetSize? = null, val label: String = "")

/** How much of a target's margin parallax uses up (F-53). NEUTRAL is a target without size. */
enum class TargetStatus { GREEN, ORANGE, RED, NEUTRAL }

/** Half of the target's angular size in mrad: the error beyond which a centered shot leaves the target. */
internal fun Target.halfSizeMrad(): Double? = size?.let { sizeToMrad(it, distanceM) / 2.0 }
