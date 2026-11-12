package com.cussou.parallax.domain

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Optimal setting for sized targets, docs/02-calculations.md §4, against the "targets" vectors (NF-05). */
class TargetsSettingTest {
    @Test
    fun `NF-05 targets vector V6 optimal setting`() {
        val case = TestVectors.case("targets", "V6")
        val expected = case.obj("expected")

        val result = optimalSettingForTargets(case.targets(), case.offsetMm(), case.double("orangeThreshold"))

        assertClose(expected.double("settingM"), result.settingM, label = "V6 settingM")
        assertClose(expected.double("maxRatio"), result.maxRatio!!, label = "V6 maxRatio")
        assertPerTarget("V6", expected, result)
    }

    @Test
    fun `NF-05 targets vector V6b forced setting (F-56)`() {
        val case = TestVectors.case("targets", "V6b-forced")
        val forcedM = case.double("forcedSettingM")

        val result = evaluateSetting(case.targets(), case.offsetMm(), case.double("orangeThreshold"), forcedM)

        assertEquals(forcedM, result.settingM)
        assertPerTarget("V6b", case.obj("expected"), result)
    }

    @Test
    fun `F-53 status thresholds`() {
        // One 1 mrad target at 100 m, offset 25 mm: the ratio is driven by the forced setting alone.
        fun statusAtRatio(ratio: Double): TargetStatus {
            val u = 1.0 / 100.0 + ratio * 0.5 / 25.0 // setting closer than the target
            return evaluateSetting(listOf(sized(100.0, 1.0)), 25.0, 0.5, 1.0 / u).perTarget.single().status
        }
        assertEquals(TargetStatus.GREEN, statusAtRatio(0.0))
        assertEquals(TargetStatus.GREEN, statusAtRatio(0.49))
        assertEquals(TargetStatus.ORANGE, statusAtRatio(0.51))
        assertEquals(TargetStatus.ORANGE, statusAtRatio(0.99))
        assertEquals(TargetStatus.RED, statusAtRatio(1.01))
    }

    @Test
    fun `F-32 orange threshold changes the status but never the setting`() {
        val targets = listOf(sized(25.0, 1.0), sized(100.0, 1.0), sized(200.0, 0.5))
        val strict = optimalSettingForTargets(targets, 25.0, orangeThreshold = 0.1)
        val loose = optimalSettingForTargets(targets, 25.0, orangeThreshold = 0.9)
        assertEquals(strict.settingM, loose.settingM)
        assertEquals(TargetStatus.ORANGE, strict.perTarget[1].status)
        assertEquals(TargetStatus.GREEN, loose.perTarget[1].status)
    }

    @Test
    fun `F-52 a target without size falls back to the range calculation, with neutral status`() {
        val targets = listOf(sized(100.0, 1.0), Target(200.0), sized(150.0, 0.3))
        val result = optimalSettingForTargets(targets, 25.0, 0.5)
        val range = optimalSettingForRange(100.0, 200.0, 25.0)

        assertClose(range.settingM, result.settingM)
        val unsized = result.perTarget[1]
        assertEquals(TargetStatus.NEUTRAL, unsized.status)
        assertNull(unsized.halfSizeMrad)
        assertNull(unsized.ratio)
        assertClose(range.maxErrorMrad, unsized.errorMrad)
        assertTrue(result.perTarget[0].status != TargetStatus.NEUTRAL)
    }

    @Test
    fun `F-52 without any size there is no ratio`() {
        val result = optimalSettingForTargets(listOf(Target(100.0), Target(200.0)), 25.0, 0.5)
        assertNull(result.maxRatio)
        assertClose(133.3333333333, result.settingM)
    }

    @Test
    fun `a single target is set to its own distance`() {
        val result = optimalSettingForTargets(listOf(sized(75.0, 0.8)), 25.0, 0.5)
        assertClose(75.0, result.settingM)
        assertEquals(TargetStatus.GREEN, result.perTarget.single().status)
    }

    @Test
    fun `property - with equal tolerances the result for targets equals the result for their range`() {
        repeat(200) {
            val targets = randomTargets(sizeMrad = { 0.7 })
            val result = optimalSettingForTargets(targets, 25.0, 0.5)
            val range = optimalSettingForRange(targets.minOf { it.distanceM }, targets.maxOf { it.distanceM }, 25.0)
            assertClose(range.settingM, result.settingM, label = "$targets")
        }
    }

    @Test
    fun `property - the optimal setting lies between the nearest and the farthest target`() {
        repeat(200) {
            val targets = randomTargets()
            val settingM = optimalSettingForTargets(targets, 25.0, 0.5).settingM
            val minM = targets.minOf { it.distanceM }
            val maxM = targets.maxOf { it.distanceM }
            assertTrue(settingM >= minM * (1 - 1e-9) && settingM <= maxM * (1 + 1e-9), "$targets gave $settingM")
        }
    }

    @Test
    fun `property - the result does not depend on the order of the targets`() {
        repeat(200) {
            val targets = randomTargets()
            val result = optimalSettingForTargets(targets, 25.0, 0.5)
            val shuffled = optimalSettingForTargets(targets.shuffled(random), 25.0, 0.5)
            assertClose(result.settingM, shuffled.settingM, label = "$targets")
            assertClose(result.maxRatio!!, shuffled.maxRatio!!, label = "$targets")
        }
    }

    @Test
    fun `property - no other setting gives a smaller worst ratio`() {
        repeat(100) {
            val targets = randomTargets()
            val best = optimalSettingForTargets(targets, 25.0, 0.5)
            val minU = 1.0 / targets.maxOf { it.distanceM }
            val maxU = 1.0 / targets.minOf { it.distanceM }
            for (step in 0..400) {
                val u = minU + (maxU - minU) * step / 400.0
                val other = evaluateSetting(targets, 25.0, 0.5, 1.0 / u)
                assertTrue(other.maxRatio!! >= best.maxRatio!! * (1 - 1e-9), "$targets: 1/$u beats ${best.settingM}")
            }
        }
    }

    @Test
    fun `invalid inputs are rejected`() {
        assertFailsWith<IllegalArgumentException> { optimalSettingForTargets(emptyList(), 25.0, 0.5) }
        assertFailsWith<IllegalArgumentException> { optimalSettingForTargets(listOf(sized(0.0, 1.0)), 25.0, 0.5) }
        assertFailsWith<IllegalArgumentException> { optimalSettingForTargets(listOf(sized(100.0, 0.0)), 25.0, 0.5) }
        assertFailsWith<IllegalArgumentException> { evaluateSetting(listOf(sized(100.0, 1.0)), 25.0, 0.5, 0.0) }
    }

    private val random = Random(20261111)

    private fun sized(distanceM: Double, sizeMrad: Double) = Target(distanceM, TargetSize(sizeMrad, SizeUnit.MRAD))

    private fun randomTargets(sizeMrad: () -> Double = { random.nextDouble(0.1, 3.0) }): List<Target> =
        List(random.nextInt(2, 9)) { sized(random.nextDouble(5.0, 2000.0), sizeMrad()) }

    private fun kotlinx.serialization.json.JsonObject.offsetMm(): Double =
        effectiveOffsetMm(double("objectiveMm"), eyePosition(string("eye")))

    private fun assertPerTarget(id: String, expected: kotlinx.serialization.json.JsonObject, result: TargetsSetting) {
        val errors = expected.doubles("errorsMrad")
        val halfSizes = expected.doubles("halfSizeMrad")
        val statuses = expected.strings("status")
        assertEquals(errors.size, result.perTarget.size)
        result.perTarget.forEachIndexed { i, target ->
            assertClose(errors[i], target.errorMrad, label = "$id errorsMrad[$i]")
            assertClose(halfSizes[i], target.halfSizeMrad!!, label = "$id halfSizeMrad[$i]")
            assertEquals(statuses[i], target.status.name.lowercase(), "$id status[$i]")
        }
    }
}
