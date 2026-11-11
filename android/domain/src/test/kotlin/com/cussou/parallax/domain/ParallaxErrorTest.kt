package com.cussou.parallax.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/** Parallax error formula, docs/02-calculations.md §1 and §2, against the "error" vectors (NF-05). */
class ParallaxErrorTest {
    @Test
    fun `NF-05 unit constants match the vectors file`() {
        val constants = TestVectors.root.obj("constants")
        assertEquals(constants.double("yard_m"), YARD_M)
        assertEquals(constants.double("moa_mrad"), MOA_MRAD)
    }

    @Test
    fun `F-31 eye position fractions match the vectors file`() {
        val fractions = TestVectors.root.obj("constants").obj("eyeFraction")
        assertEquals(fractions.double("excellent"), EyePosition.EXCELLENT.fraction)
        assertEquals(fractions.double("good"), EyePosition.GOOD.fraction)
        assertEquals(fractions.double("worstCase"), EyePosition.WORST_CASE.fraction)
    }

    @Test
    fun `F-31 worst case offset is the objective radius`() {
        assertEquals(25.0, effectiveOffsetMm(50.0, EyePosition.WORST_CASE))
    }

    @Test
    fun `NF-05 error vectors V3 V4 V5 V5b`() {
        for (case in TestVectors.section("error")) {
            val id = case.string("id")
            val tolerance = case.tolerance()
            val expected = case.obj("expected")
            val distanceM = case.double("distanceM")

            val offsetMm = effectiveOffsetMm(case.double("objectiveMm"), eyePosition(case.string("eye")))
            val errorMrad = errorMrad(offsetMm, distanceM, case.double("settingM"))

            assertClose(expected.double("errorMrad"), errorMrad, tolerance, "$id errorMrad")
            expected.doubleOrNull("effectiveOffsetMm")?.let {
                assertClose(it, offsetMm, tolerance, "$id effectiveOffsetMm")
            }
            expected.doubleOrNull("linearErrorMm")?.let {
                assertClose(it, linearErrorMm(errorMrad, distanceM), tolerance, "$id linearErrorMm")
            }
            expected.doubleOrNull("errorMoa")?.let {
                assertClose(it, errorMrad / MOA_MRAD, tolerance, "$id errorMoa")
            }
        }
    }

    @Test
    fun `F-05 error is zero when the setting equals the distance`() {
        assertEquals(0.0, errorMrad(offsetMm = 25.0, distanceM = 80.0, settingM = 80.0))
    }

    @Test
    fun `error is the same on either side of the setting in inverse distance`() {
        // 1/100 - 1/150 = 1/150 - 1/300
        assertClose(
            errorMrad(offsetMm = 25.0, distanceM = 100.0, settingM = 150.0),
            errorMrad(offsetMm = 25.0, distanceM = 300.0, settingM = 150.0),
        )
    }
}
