package com.cussou.parallax.format

import com.cussou.parallax.domain.AngleUnit
import com.cussou.parallax.domain.DistanceUnit
import kotlin.test.Test
import kotlin.test.assertEquals

/** Display conversions, docs/02-calculations.md §1. Internal values are never modified (F-25). */
class UnitConversionTest {
    @Test
    fun `F-20 meters are displayed as they are, or converted to yards`() {
        assertEquals(100.0, metersToDisplay(100.0, DistanceUnit.M))
        assertEquals(100.0, metersToDisplay(91.44, DistanceUnit.YD), 1e-9)
    }

    @Test
    fun `F-20 entered distances are converted to meters`() {
        assertEquals(100.0, displayToMeters(100.0, DistanceUnit.M))
        assertEquals(91.44, displayToMeters(100.0, DistanceUnit.YD), 1e-9)
    }

    @Test
    fun `F-25 a distance survives a round trip through yards`() {
        for (meters in listOf(5.0, 22.86, 133.3333333333, 2000.0)) {
            assertEquals(meters, displayToMeters(metersToDisplay(meters, DistanceUnit.YD), DistanceUnit.YD), 1e-9)
        }
    }

    @Test
    fun `F-20 angles are displayed in mrad or MOA`() {
        assertEquals(0.656167979, mradToDisplay(0.656167979, AngleUnit.MRAD))
        // Vector V3: 0.656167979 mrad = 2.255740969 MOA.
        assertEquals(2.255740969, mradToDisplay(0.656167979, AngleUnit.MOA), 1e-6)
    }

    @Test
    fun `F-24 linear error is in cm with meters and in inches with yards`() {
        // Vector V3: 15 mm.
        assertEquals(1.5, linearErrorToDisplay(15.0, DistanceUnit.M), 1e-9)
        assertEquals(15.0 / 25.4, linearErrorToDisplay(15.0, DistanceUnit.YD), 1e-9)
    }
}
