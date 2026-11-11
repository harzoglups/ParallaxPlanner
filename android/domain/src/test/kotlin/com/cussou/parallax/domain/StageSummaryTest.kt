package com.cussou.parallax.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** One-line summary of a stage in the match view (F-63). */
class StageSummaryTest {
    private val worstCaseOffsetMm = effectiveOffsetMm(50.0, EyePosition.WORST_CASE)

    private fun summary(stage: Stage, orangeThreshold: Double = 0.5) =
        stageSummary(stage, worstCaseOffsetMm, orangeThreshold)

    private val v6Targets = listOf(
        Target(25.0, TargetSize(1.0, SizeUnit.MRAD)),
        Target(100.0, TargetSize(1.0, SizeUnit.MRAD)),
        Target(200.0, TargetSize(0.5, SizeUnit.MRAD)),
    )

    @Test
    fun `F-63 vector V1 a stage with a range gets the setting of the range`() {
        val summary = summary(Stage("s", "Prone", rangeMinM = 100.0, rangeMaxM = 200.0))!!
        assertClose(133.3333333333, summary.settingM)
        assertClose(0.0625, summary.maxErrorMrad)
        // No target size, so nothing to judge.
        assertEquals(TargetStatus.NEUTRAL, summary.status)
        assertEquals(false, summary.forced)
    }

    @Test
    fun `F-63 vector V6 a stage with targets gets the weighted setting and its worst status`() {
        val summary = summary(Stage("s", "Barricade", targets = v6Targets))!!
        assertClose(60.0, summary.settingM)
        assertClose(0.5833333333, summary.maxErrorMrad)
        assertEquals(TargetStatus.RED, summary.status)
        assertEquals(2, summary.statusCount)
    }

    @Test
    fun `F-56 F-63 vector V6b a fixed setting is the one summed up`() {
        val summary = summary(Stage("s", "Barricade", forcedSettingM = 80.0, targets = v6Targets))!!
        assertEquals(80.0, summary.settingM)
        assertEquals(true, summary.forced)
        assertClose(0.6875, summary.maxErrorMrad)
        assertEquals(TargetStatus.RED, summary.status)
        assertEquals(1, summary.statusCount)
    }

    @Test
    fun `F-56 F-63 vector V4 a fixed setting on a range gives the worse of the two ends`() {
        val summary = summary(Stage("s", "Prone", rangeMinM = 100.0, rangeMaxM = 200.0, forcedSettingM = 150.0))!!
        assertEquals(150.0, summary.settingM)
        assertClose(0.0833333333, summary.maxErrorMrad)
        assertEquals(TargetStatus.NEUTRAL, summary.status)
    }

    @Test
    fun `F-63 the status is the worst one, with the number of targets that have it`() {
        // Ratios of V6 at 80 m: 1.375 / 0.125 / 0.75.
        val orange = summary(Stage("s", "x", forcedSettingM = 80.0, targets = v6Targets.drop(1)))!!
        assertEquals(TargetStatus.ORANGE, orange.status)
        assertEquals(1, orange.statusCount)
        val green = summary(Stage("s", "x", targets = v6Targets.drop(1)))!!
        assertEquals(TargetStatus.GREEN, green.status)
        assertEquals(2, green.statusCount)
    }

    @Test
    fun `F-51 targets take over a range left in the stage`() {
        val summary = summary(Stage("s", "x", rangeMinM = 300.0, rangeMaxM = 400.0, targets = v6Targets))!!
        assertClose(60.0, summary.settingM)
    }

    @Test
    fun `targets without size have a setting but no status`() {
        val summary = summary(Stage("s", "x", targets = listOf(Target(100.0), Target(200.0))))!!
        assertClose(133.3333333333, summary.settingM)
        assertEquals(TargetStatus.NEUTRAL, summary.status)
        assertEquals(0, summary.statusCount)
    }

    @Test
    fun `a stage without targets nor a complete range has no summary`() {
        assertNull(summary(Stage("s", "Empty")))
        assertNull(summary(Stage("s", "Half", rangeMinM = 100.0)))
        assertNull(summary(Stage("s", "Only fixed", forcedSettingM = 100.0)))
    }

    @Test
    fun `a range stored in the wrong order is still summed up`() {
        val summary = summary(Stage("s", "x", rangeMinM = 200.0, rangeMaxM = 100.0))!!
        assertClose(133.3333333333, summary.settingM)
    }
}
