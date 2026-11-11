package com.cussou.parallax.format

import com.cussou.parallax.domain.AngleUnit
import com.cussou.parallax.domain.DistanceUnit
import com.cussou.parallax.domain.MOA_MRAD
import com.cussou.parallax.domain.YARD_M

// Conversions between the domain's internal units (m, mrad, mm) and the units the user chose.
// They are used at input and display only: stored values stay in internal units (F-25).

private const val MM_PER_CM = 10.0
private const val MM_PER_INCH = 25.4

fun metersToDisplay(meters: Double, unit: DistanceUnit): Double = when (unit) {
    DistanceUnit.M -> meters
    DistanceUnit.YD -> meters / YARD_M
}

fun displayToMeters(value: Double, unit: DistanceUnit): Double = when (unit) {
    DistanceUnit.M -> value
    DistanceUnit.YD -> value * YARD_M
}

fun mradToDisplay(mrad: Double, unit: AngleUnit): Double = when (unit) {
    AngleUnit.MRAD -> mrad
    AngleUnit.MOA -> mrad / MOA_MRAD
}

/** Linear error in cm when distances are in meters, in inches when they are in yards (F-24). */
fun linearErrorToDisplay(errorMm: Double, distanceUnit: DistanceUnit): Double = when (distanceUnit) {
    DistanceUnit.M -> errorMm / MM_PER_CM
    DistanceUnit.YD -> errorMm / MM_PER_INCH
}
