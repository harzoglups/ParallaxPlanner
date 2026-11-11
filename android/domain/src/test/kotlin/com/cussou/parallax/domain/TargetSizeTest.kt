package com.cussou.parallax.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/** Target size conversion to mrad, docs/02-calculations.md §1, against the "sizeConversion" vectors (NF-05). */
class TargetSizeTest {
    @Test
    fun `NF-05 size conversion vectors V7 V7b V7c`() {
        for (case in TestVectors.section("sizeConversion")) {
            val size = TargetSize(case.double("value"), sizeUnit(case.string("unit")))
            assertClose(
                case.double("expectedMrad"),
                sizeToMrad(size, case.double("distanceM")),
                case.tolerance(),
                case.string("id"),
            )
        }
    }

    @Test
    fun `F-50 angular sizes do not depend on the distance`() {
        assertEquals(1.5, sizeToMrad(TargetSize(1.5, SizeUnit.MRAD), distanceM = 25.0))
        assertEquals(1.5, sizeToMrad(TargetSize(1.5, SizeUnit.MRAD), distanceM = 400.0))
        assertEquals(
            sizeToMrad(TargetSize(2.0, SizeUnit.MOA), distanceM = 25.0),
            sizeToMrad(TargetSize(2.0, SizeUnit.MOA), distanceM = 400.0),
        )
    }

    @Test
    fun `F-50 a linear size looks half as big at twice the distance`() {
        val near = sizeToMrad(TargetSize(10.0, SizeUnit.CM), distanceM = 100.0)
        val far = sizeToMrad(TargetSize(10.0, SizeUnit.CM), distanceM = 200.0)
        assertClose(near / 2.0, far)
    }

    @Test
    fun `F-50 one inch is 2,54 cm`() {
        assertClose(
            sizeToMrad(TargetSize(2.54, SizeUnit.CM), distanceM = 75.0),
            sizeToMrad(TargetSize(1.0, SizeUnit.IN), distanceM = 75.0),
        )
    }
}
