package com.cussou.parallax.domain

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Minimum number of settings, docs/02-calculations.md §5 (F-55), against vector V8 (NF-05). */
class MinimalSettingsTest {
    @Test
    fun `NF-05 vector V8 minimal settings of the V6 targets`() {
        val case = TestVectors.case("targets", "V6")
        val expected = case.obj("expected").array("minimalSettings").map { it.jsonObject }
        val offsetMm = effectiveOffsetMm(case.double("objectiveMm"), eyePosition(case.string("eye")))

        val groups = minimalSettings(case.targets(), offsetMm)

        assertEquals(expected.size, groups.size)
        expected.forEachIndexed { i, group ->
            assertEquals(group.array("targets").map { it.jsonPrimitive.int }, groups[i].targetIndices, "V8 group $i")
            assertClose(group.double("settingM"), groups[i].settingM, label = "V8 group $i settingM")
        }
    }

    @Test
    fun `F-55 targets that fit one setting give a single group with the optimal setting`() {
        val targets = listOf(sized(100.0, 1.0), sized(150.0, 1.0), sized(200.0, 1.0))
        val groups = minimalSettings(targets, 25.0)
        assertEquals(listOf(listOf(0, 1, 2)), groups.map { it.targetIndices })
        assertClose(optimalSettingForTargets(targets, 25.0, 0.5).settingM, groups.single().settingM)
    }

    @Test
    fun `F-55 groups are sorted from the shortest to the longest setting`() {
        val targets = listOf(sized(400.0, 0.05), sized(25.0, 0.05), sized(100.0, 0.05))
        val groups = minimalSettings(targets, 25.0)
        assertEquals(listOf(listOf(1), listOf(2), listOf(0)), groups.map { it.targetIndices })
        assertEquals(groups.map { it.settingM }.sorted(), groups.map { it.settingM })
    }

    @Test
    fun `F-55 targets without size are left out of the groups`() {
        val groups = minimalSettings(listOf(sized(100.0, 1.0), Target(25.0), sized(110.0, 1.0)), 25.0)
        assertEquals(listOf(listOf(0, 2)), groups.map { it.targetIndices })
        assertEquals(emptyList(), minimalSettings(listOf(Target(25.0)), 25.0))
    }

    @Test
    fun `F-55 a far target whose tolerance reaches infinity still gets a finite setting`() {
        // Interval [1/2000 − 0.02, 1/2000 + 0.02] goes below u = 0.
        val group = minimalSettings(listOf(sized(2000.0, 1.0)), 25.0).single()
        assertClose(2000.0, group.settingM)
    }

    @Test
    fun `property - every sized target is in exactly one group and is not red there`() {
        repeat(300) {
            val targets = randomTargets()
            val groups = minimalSettings(targets, 25.0)
            assertEquals(targets.indices.toList(), groups.flatMap { it.targetIndices }.sorted(), "$targets")
            for (group in groups) {
                val result = evaluateSetting(group.targetIndices.map { targets[it] }, 25.0, 0.5, group.settingM)
                assertTrue(result.maxRatio!! <= 1.0 + 1e-9, "$targets: group $group has ratio ${result.maxRatio}")
            }
        }
    }

    @Test
    fun `property - no smaller number of settings keeps every target out of red`() {
        repeat(300) {
            val targets = randomTargets(maxCount = 7)
            val groupCount = minimalSettings(targets, 25.0).size
            assertTrue(!canCoverWith(groupCount - 1, targets, 25.0), "$targets can be covered with fewer than $groupCount")
        }
    }

    @Test
    fun `invalid inputs are rejected`() {
        assertFailsWith<IllegalArgumentException> { minimalSettings(emptyList(), 25.0) }
        assertFailsWith<IllegalArgumentException> { minimalSettings(listOf(sized(100.0, 1.0)), 0.0) }
    }

    private val random = Random(20261111)

    private fun sized(distanceM: Double, sizeMrad: Double) = Target(distanceM, TargetSize(sizeMrad, SizeUnit.MRAD))

    private fun randomTargets(maxCount: Int = 12): List<Target> =
        List(random.nextInt(1, maxCount + 1)) { sized(random.nextDouble(10.0, 400.0), random.nextDouble(0.05, 1.5)) }

    /**
     * Brute force: can [count] settings keep every target out of red? If a solution exists, one exists
     * that only uses right bounds of the admissible intervals, so trying every subset of them is enough.
     */
    private fun canCoverWith(count: Int, targets: List<Target>, offsetMm: Double): Boolean {
        val intervals = targets.map {
            val x = 1.0 / it.distanceM
            val b = sizeToMrad(it.size!!, it.distanceM) / 2.0 / offsetMm
            (x - b)..(x + b)
        }
        val points = intervals.map { it.endInclusive }
        return (0 until (1 shl points.size)).any { mask ->
            Integer.bitCount(mask) <= count && intervals.all { interval ->
                points.indices.any { mask and (1 shl it) != 0 && points[it] in interval }
            }
        }
    }
}
