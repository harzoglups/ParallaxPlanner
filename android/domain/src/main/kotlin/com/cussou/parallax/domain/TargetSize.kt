package com.cussou.parallax.domain

/** Unit a target size is entered in (F-20). MRAD and MOA are angular, CM and IN are linear. */
enum class SizeUnit { MRAD, MOA, CM, IN }

/**
 * Smallest dimension of a target (F-50), kept in the unit it was entered in:
 * a linear size only becomes an angle once the target's distance is known.
 */
data class TargetSize(val value: Double, val unit: SizeUnit)

private const val MM_PER_CM = 10.0
private const val MM_PER_INCH = 25.4

/** Angular size in mrad of a target at [distanceM] (docs/02-calculations.md §1). */
fun sizeToMrad(size: TargetSize, distanceM: Double): Double = when (size.unit) {
    SizeUnit.MRAD -> size.value
    SizeUnit.MOA -> size.value * MOA_MRAD
    SizeUnit.CM -> size.value * MM_PER_CM / distanceM
    SizeUnit.IN -> size.value * MM_PER_INCH / distanceM
}
