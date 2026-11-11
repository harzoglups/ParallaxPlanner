package com.cussou.parallax.ui.quickcalc

import com.cussou.parallax.domain.DistanceUnit
import com.cussou.parallax.domain.EyePosition
import com.cussou.parallax.domain.SizeUnit
import com.cussou.parallax.domain.Target
import com.cussou.parallax.domain.TargetSize
import com.cussou.parallax.domain.TargetStatus
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
    fun `F-06 vector V4 error when keeping the current setting`() {
        val outcome = quickCalcOutcome("100", "200", DistanceUnit.M, worstCase, currentSettingText = "150")
        val comparison = assertNotNull(outcome.comparison)
        assertEquals(150.0, comparison.settingM)
        // 0.0833 mrad at 100 m, 0.0417 mrad at 200 m, 8.33 mm at both.
        assertEquals(0.0833333333, comparison.maxErrorMrad, 1e-9)
        assertEquals(8.3333333333, comparison.linearErrorAtMinMm, 1e-9)
        assertEquals(8.3333333333, comparison.linearErrorAtMaxMm, 1e-9)
        assertNotNull(outcome.result)
    }

    @Test
    fun `F-06 the current setting is optional`() {
        val outcome = quickCalcOutcome("100", "200", DistanceUnit.M, worstCase, currentSettingText = "")
        assertNull(outcome.comparison)
        assertFalse(outcome.currentSettingError)
        assertNotNull(outcome.result)
    }

    @Test
    fun `F-06 keeping the optimal setting gives the optimal error, any other setting a larger one`() {
        val optimal = quickCalcOutcome("100", "200", DistanceUnit.M, worstCase, currentSettingText = "133.3333333333")
        assertEquals(optimal.result!!.maxErrorMrad, optimal.comparison!!.maxErrorMrad, 1e-9)
        for (setting in listOf("100", "120", "150", "200", "50", "1000")) {
            val other = quickCalcOutcome("100", "200", DistanceUnit.M, worstCase, currentSettingText = setting)
            assertTrue(other.comparison!!.maxErrorMrad > other.result!!.maxErrorMrad, setting)
        }
    }

    @Test
    fun `F-06 the current setting is typed in the distance unit`() {
        val outcome = quickCalcOutcome("100", "200", DistanceUnit.YD, worstCase, currentSettingText = "150")
        assertEquals(137.16, outcome.comparison!!.settingM, 1e-9)
    }

    @Test
    fun `F-06 an invalid current setting is flagged without hiding the result`() {
        for (setting in listOf("3", "2001", "1.2.3")) {
            val outcome = quickCalcOutcome("100", "200", DistanceUnit.M, worstCase, currentSettingText = setting)
            assertTrue(outcome.currentSettingError, setting)
            assertNull(outcome.comparison, setting)
            assertNotNull(outcome.result, setting)
        }
    }

    @Test
    fun `F-06 there is no comparison without a range`() {
        val outcome = quickCalcOutcome("100", "", DistanceUnit.M, worstCase, currentSettingText = "150")
        assertNull(outcome.comparison)
        assertFalse(outcome.currentSettingError)
    }

    private val advanced = worstCase.copy(advancedMode = true)
    private val v6Targets = listOf(
        Target(25.0, TargetSize(1.0, SizeUnit.MRAD)),
        Target(100.0, TargetSize(1.0, SizeUnit.MRAD)),
        Target(200.0, TargetSize(0.5, SizeUnit.MRAD)),
    )

    @Test
    fun `F-52 vector V6 with sized targets the setting is the weighted one`() {
        val outcome = quickCalcOutcome("", "", DistanceUnit.M, advanced, targets = v6Targets)
        val targetsResult = assertNotNull(outcome.targetsResult)
        assertEquals(60.0, targetsResult.settingM, 1e-9)
        assertEquals(3, targetsResult.perTarget.size)
        // The range fields play no part once there are targets (F-51).
        assertNull(outcome.result)
        assertNull(outcome.comparison)
    }

    @Test
    fun `F-51 targets take over a typed range`() {
        val outcome = quickCalcOutcome("300", "400", DistanceUnit.M, advanced, targets = v6Targets)
        assertEquals(60.0, outcome.targetsResult!!.settingM, 1e-9)
        assertNull(outcome.result)
        assertFalse(outcome.minError || outcome.maxError)
    }

    @Test
    fun `F-52 a target without size falls back to the range of the targets`() {
        val targets = listOf(Target(100.0, TargetSize(1.0, SizeUnit.MRAD)), Target(200.0))
        val outcome = quickCalcOutcome("", "", DistanceUnit.M, advanced, targets = targets)
        assertEquals(133.3333333333, outcome.targetsResult!!.settingM, 1e-9)
    }

    @Test
    fun `F-32 the orange threshold of the preferences drives the statuses`() {
        val strict = quickCalcOutcome("", "", DistanceUnit.M, advanced.copy(orangeThreshold = 0.1), targets = v6Targets)
        val loose = quickCalcOutcome("", "", DistanceUnit.M, advanced.copy(orangeThreshold = 0.9), targets = v6Targets)
        assertEquals(TargetStatus.ORANGE, strict.targetsResult!!.perTarget[1].status)
        assertEquals(TargetStatus.GREEN, loose.targetsResult!!.perTarget[1].status)
    }

    @Test
    fun `F-51 without targets advanced mode behaves like simple mode`() {
        val outcome = quickCalcOutcome("100", "200", DistanceUnit.M, advanced, targets = emptyList())
        assertNull(outcome.targetsResult)
        assertEquals(133.3333333333, outcome.result!!.settingM, 1e-9)
    }

    @Test
    fun `F-41 with advanced mode off the targets are ignored, not lost`() {
        val outcome = quickCalcOutcome("100", "200", DistanceUnit.M, worstCase, targets = v6Targets)
        assertNull(outcome.targetsResult)
        assertEquals(133.3333333333, outcome.result!!.settingM, 1e-9)
    }

    private fun summaryOf(targets: List<Target>, preferences: UserPreferences = advanced) =
        statusSummary(quickCalcOutcome("", "", DistanceUnit.M, preferences, targets = targets).targetsResult!!)

    @Test
    fun `F-53 the summary counts the red targets first (vector V6)`() {
        assertEquals(StatusSummary(TargetStatus.RED, 2), summaryOf(v6Targets))
    }

    @Test
    fun `F-53 without red target the summary counts the orange ones`() {
        // Good eye position: errors are a third of the worst case, ratios 0.39 / 0.11 / 0.39.
        val good = advanced.copy(eyePosition = EyePosition.GOOD)
        assertEquals(StatusSummary(TargetStatus.GREEN, 3), summaryOf(v6Targets, good))
        assertEquals(StatusSummary(TargetStatus.ORANGE, 2), summaryOf(v6Targets, good.copy(orangeThreshold = 0.3)))
    }

    @Test
    fun `F-53 targets without size are left out of the summary`() {
        val mixed = listOf(Target(100.0, TargetSize(1.0, SizeUnit.MRAD)), Target(200.0))
        assertEquals(1, summaryOf(mixed)!!.count)
        assertNull(summaryOf(listOf(Target(100.0), Target(200.0))))
    }

    @Test
    fun `F-55 vector V8 red targets come with the fewest settings that clear them`() {
        val outcome = quickCalcOutcome("", "", DistanceUnit.M, advanced, targets = v6Targets)
        val groups = outcome.suggestedSettings
        assertEquals(listOf(listOf(0), listOf(1, 2)), groups.map { it.targetIndices })
        assertEquals(25.0, groups[0].settingM, 1e-9)
        assertEquals(150.0, groups[1].settingM, 1e-9)
    }

    @Test
    fun `F-55 nothing is suggested while no target is red`() {
        val good = advanced.copy(eyePosition = EyePosition.GOOD)
        assertTrue(quickCalcOutcome("", "", DistanceUnit.M, good, targets = v6Targets).suggestedSettings.isEmpty())
        // Orange targets are not a reason to suggest more settings.
        val strict = good.copy(orangeThreshold = 0.3)
        assertTrue(quickCalcOutcome("", "", DistanceUnit.M, strict, targets = v6Targets).suggestedSettings.isEmpty())
        assertTrue(quickCalcOutcome("100", "200", DistanceUnit.M, advanced).suggestedSettings.isEmpty())
    }

    @Test
    fun `F-55 each suggested setting keeps its own targets out of red`() {
        val outcome = quickCalcOutcome("", "", DistanceUnit.M, advanced, targets = v6Targets)
        for (group in outcome.suggestedSettings) {
            val alone = group.targetIndices.map { v6Targets[it] }
            val check = quickCalcOutcome("", "", DistanceUnit.M, advanced, targets = alone)
            assertTrue(check.targetsResult!!.perTarget.none { it.status == TargetStatus.RED })
        }
    }

    @Test
    fun `decimal comma is accepted`() {
        val result = assertNotNull(outcome("22,86", "91,44").result)
        assertEquals(22.86, result.minM)
        assertEquals(91.44, result.maxM)
    }
}
