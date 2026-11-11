package com.cussou.parallax.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.cussou.parallax.R
import com.cussou.parallax.domain.AngleUnit
import com.cussou.parallax.domain.DistanceUnit
import com.cussou.parallax.domain.SizeUnit
import java.util.Locale

/** Locale of the language the app is displayed in, for number formats. */
@Composable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

@Composable
fun distanceUnitLabel(unit: DistanceUnit): String = stringResource(
    when (unit) {
        DistanceUnit.M -> R.string.unit_m
        DistanceUnit.YD -> R.string.unit_yd
    },
)

@Composable
fun angleUnitLabel(unit: AngleUnit): String = stringResource(
    when (unit) {
        AngleUnit.MRAD -> R.string.unit_mrad
        AngleUnit.MOA -> R.string.unit_moa
    },
)

/** Linear errors are in cm with meters and in inches with yards (F-24). */
@Composable
fun linearUnitLabel(distanceUnit: DistanceUnit): String = stringResource(
    when (distanceUnit) {
        DistanceUnit.M -> R.string.unit_cm
        DistanceUnit.YD -> R.string.unit_in
    },
)

@Composable
fun sizeUnitLabel(unit: SizeUnit): String = stringResource(
    when (unit) {
        SizeUnit.MRAD -> R.string.unit_mrad
        SizeUnit.MOA -> R.string.unit_moa
        SizeUnit.CM -> R.string.unit_cm
        SizeUnit.IN -> R.string.unit_in
    },
)
