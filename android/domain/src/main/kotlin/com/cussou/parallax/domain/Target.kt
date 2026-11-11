package com.cussou.parallax.domain

/** One target of a stage: a distance and, optionally, its smallest dimension (F-50). */
data class Target(val distanceM: Double, val size: TargetSize? = null)

/** How much of a target's margin parallax uses up (F-53). NEUTRAL is a target without size. */
enum class TargetStatus { GREEN, ORANGE, RED, NEUTRAL }

/** Half of the target's angular size in mrad: the error beyond which a centered shot leaves the target. */
internal fun Target.halfSizeMrad(): Double? = size?.let { sizeToMrad(it, distanceM) / 2.0 }
