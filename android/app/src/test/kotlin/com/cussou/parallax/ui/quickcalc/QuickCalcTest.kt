package com.cussou.parallax.ui.quickcalc

import com.cussou.parallax.domain.DistanceUnit
import com.cussou.parallax.domain.EyePosition
import com.cussou.parallax.domain.UserPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Quick calc in simple mode: from the typed range to the result (F-01 to F-05). */
class QuickCalcTest {
    private val worstCase = UserPreferences(eyePosition = EyePosition.WORST_CASE)

    private fun outcome(
        min: String,
        max: String,
        unit: DistanceUnit = DistanceUnit.M,
        preferences: UserPreferences = worstCase,
    ) = quickCalcOutcome(min, max, unit, preferences)

    @Test
    fun `F-02 F-03 vector V1 from typed text`() {
        val result = assertNotNull(outcome("100", "200").result)
        assertEquals(100.0, result.minM)
        assertEquals(200.0, result.maxM)
        assertEquals(133.3333333333, result.settingM, 1e-9)
        assertEquals(0.0625, result.maxErrorMrad, 1e-12)
        assertEquals(6.25, result.linearErrorAtMinMm, 1e-9)
        assertEquals(12.5, result.linearErrorAtMaxMm, 1e-9)
        assertFalse(result.swapped)
    }

    @Test
    fun `F-30 F-31 the result uses the objective and eye position of the preferences`() {
        val good = assertNotNull(outcome("100", "200", preferences = UserPreferences()).result)
        assertEquals(0.0625 / 3.0, good.maxErrorMrad, 1e-12)
        val small = assertNotNull(outcome("100", "200", preferences = worstCase.copy(objectiveMm = 40.0)).result)
        assertEquals(0.05, small.maxErrorMrad, 1e-12)
        assertEquals(good.settingM, small.settingM)
    }

    @Test
    fun `F-20 distances typed in yards are converted to meters`() {
        val result = assertNotNull(outcome("100", "200", DistanceUnit.YD).result)
        assertEquals(91.44, result.minM, 1e-9)
        assertEquals(182.88, result.maxM, 1e-9)
        assertEquals(133.3333333333 * 0.9144, result.settingM, 1e-9)
    }

    @Test
    fun `F-05 a reversed range is swapped and flagged`() {
        val result = assertNotNull(outcome("200", "100").result)
        assertEquals(100.0, result.minM)
        assertEquals(200.0, result.maxM)
        assertEquals(133.3333333333, result.settingM, 1e-9)
        assertTrue(result.swapped)
    }

    @Test
    fun `F-05 equal distances give that distance and no error`() {
        val result = assertNotNull(outcome("80", "80").result)
        assertEquals(80.0, result.settingM)
        assertEquals(0.0, result.maxErrorMrad)
        assertFalse(result.swapped)
    }

    @Test
    fun `F-05 distances must be between 5 and 2000`() {
        assertTrue(outcome("4.9", "200").minError)
        assertTrue(outcome("100", "2000.1").maxError)
        assertNull(outcome("4.9", "200").result)
        for ((min, max) in listOf("5" to "2000", "5" to "5", "2000" to "2000")) {
            val valid = outcome(min, max)
            assertFalse(valid.minError || valid.maxError, "$min-$max")
            assertNotNull(valid.result, "$min-$max")
        }
    }

    @Test
    fun `F-05 the limits apply to the number as typed, in yards too`() {
        assertNotNull(outcome("5", "2000", DistanceUnit.YD).result)
        assertTrue(outcome("4", "200", DistanceUnit.YD).minError)
    }

    @Test
    fun `F-04 an incomplete range gives no result and no error`() {
        for ((min, max) in listOf("" to "", "100" to "", "" to "200", " " to "200")) {
            val incomplete = outcome(min, max)
            assertNull(incomplete.result, "\"$min\"-\"$max\"")
            assertFalse(incomplete.minError || incomplete.maxError, "\"$min\"-\"$max\"")
        }
    }

    @Test
    fun `a field is flagged alone, even while the other one is empty`() {
        val invalid = outcome("3", "")
        assertTrue(invalid.minError)
        assertFalse(invalid.maxError)
        assertTrue(outcome("1.2.3", "200").minError)
    }

    @Test
    fun `decimal comma is accepted`() {
        val result = assertNotNull(outcome("22,86", "91,44").result)
        assertEquals(22.86, result.minM)
        assertEquals(91.44, result.maxM)
    }
}
