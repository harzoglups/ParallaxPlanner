package com.cussou.parallax.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Optimal setting for a range, docs/02-calculations.md §3, against the "range" vectors (NF-05). */
class RangeSettingTest {
    @Test
    fun `NF-05 range vectors V1 V2 V1b`() {
        for (case in TestVectors.section("range")) {
            val id = case.string("id")
            val tolerance = case.tolerance()
            val expected = case.obj("expected")
            val minM = case.double("minM")
            val maxM = case.double("maxM")
            val offsetMm = effectiveOffsetMm(case.double("objectiveMm"), eyePosition(case.string("eye")))

            val result = optimalSettingForRange(minM, maxM, offsetMm)

            assertClose(expected.double("settingM"), result.settingM, tolerance, "$id settingM")
            assertClose(expected.double("maxErrorMrad"), result.maxErrorMrad, tolerance, "$id maxErrorMrad")
            expected.doubleOrNull("linearErrorAtMinMm")?.let {
                assertClose(it, linearErrorMm(result.maxErrorMrad, minM), tolerance, "$id linearErrorAtMinMm")
            }
            expected.doubleOrNull("linearErrorAtMaxMm")?.let {
                assertClose(it, linearErrorMm(result.maxErrorMrad, maxM), tolerance, "$id linearErrorAtMaxMm")
            }
        }
    }

    @Test
    fun `F-05 equal distances give that distance and no error`() {
        val result = optimalSettingForRange(minM = 80.0, maxM = 80.0, offsetMm = 25.0)
        assertEquals(80.0, result.settingM)
        assertEquals(0.0, result.maxErrorMrad)
    }

    @Test
    fun `F-02 optimal setting lies between min and max, below the midpoint`() {
        for ((minM, maxM) in ranges) {
            val settingM = optimalSettingForRange(minM, maxM, offsetMm = 25.0).settingM
            assertTrue(settingM > minM && settingM < maxM, "$minM-$maxM gave $settingM")
            assertTrue(settingM < (minM + maxM) / 2.0, "$minM-$maxM gave $settingM")
        }
    }

    @Test
    fun `F-03 max error is reached at both ends of the range`() {
        for ((minM, maxM) in ranges) {
            val result = optimalSettingForRange(minM, maxM, offsetMm = 25.0)
            assertClose(result.maxErrorMrad, errorMrad(25.0, minM, result.settingM), label = "$minM-$maxM at min")
            assertClose(result.maxErrorMrad, errorMrad(25.0, maxM, result.settingM), label = "$minM-$maxM at max")
        }
    }

    @Test
    fun `F-02 any other setting has a larger worst error`() {
        for ((minM, maxM) in ranges) {
            val result = optimalSettingForRange(minM, maxM, offsetMm = 25.0)
            for (factor in listOf(0.5, 0.9, 0.99, 1.01, 1.1, 2.0)) {
                val otherM = result.settingM * factor
                val worst = maxOf(errorMrad(25.0, minM, otherM), errorMrad(25.0, maxM, otherM))
                assertTrue(worst > result.maxErrorMrad, "$minM-$maxM: setting $otherM is not worse")
            }
        }
    }

    @Test
    fun `invalid ranges are rejected`() {
        assertFailsWith<IllegalArgumentException> { optimalSettingForRange(200.0, 100.0, 25.0) }
        assertFailsWith<IllegalArgumentException> { optimalSettingForRange(0.0, 100.0, 25.0) }
        assertFailsWith<IllegalArgumentException> { optimalSettingForRange(-10.0, 100.0, 25.0) }
    }

    private val ranges = listOf(5.0 to 2000.0, 25.0 to 100.0, 100.0 to 200.0, 50.0 to 51.0, 300.0 to 1200.0)
}
