package com.cussou.parallax.ui.quickcalc

import com.cussou.parallax.domain.DistanceUnit
import com.cussou.parallax.domain.SizeUnit
import com.cussou.parallax.domain.Target
import com.cussou.parallax.domain.TargetSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** From the fields of the target editor to a target (F-50). */
class TargetDraftTest {
    private fun draft(distance: String = "", size: String = "", unit: SizeUnit = SizeUnit.MRAD, label: String = "") =
        TargetDraft(distance, size, unit, label)

    @Test
    fun `F-50 a distance alone makes a target without size`() {
        val check = checkTargetDraft(draft("100"), DistanceUnit.M)
        assertEquals(Target(100.0), check.target)
        assertFalse(check.distanceError || check.sizeError)
    }

    @Test
    fun `F-50 size keeps the unit it was typed in, label is trimmed`() {
        val check = checkTargetDraft(draft("50", "5", SizeUnit.CM, "  red gong "), DistanceUnit.M)
        assertEquals(Target(50.0, TargetSize(5.0, SizeUnit.CM), "red gong"), check.target)
    }

    @Test
    fun `F-20 the distance is typed in the distance unit`() {
        val check = checkTargetDraft(draft("100", "1"), DistanceUnit.YD)
        assertEquals(91.44, check.target!!.distanceM, 1e-9)
    }

    @Test
    fun `F-50 the distance is required`() {
        val check = checkTargetDraft(draft("", "1"), DistanceUnit.M)
        assertNull(check.target)
        // An empty field is not an error yet, only a target that cannot be saved.
        assertFalse(check.distanceError)
    }

    @Test
    fun `F-05 the distance must be between 5 and 2000`() {
        for (distance in listOf("4", "2001", "abc")) {
            val check = checkTargetDraft(draft(distance), DistanceUnit.M)
            assertNull(check.target, distance)
            assertTrue(check.distanceError, distance)
        }
        assertEquals(5.0, checkTargetDraft(draft("5"), DistanceUnit.M).target!!.distanceM)
        assertEquals(2000.0, checkTargetDraft(draft("2000"), DistanceUnit.M).target!!.distanceM)
    }

    @Test
    fun `F-50 a size must be a number above zero`() {
        for (size in listOf("0", "0,0", "abc", "1.2.3")) {
            val check = checkTargetDraft(draft("100", size), DistanceUnit.M)
            assertNull(check.target, size)
            assertTrue(check.sizeError, size)
            assertFalse(check.distanceError, size)
        }
        assertEquals(0.5, checkTargetDraft(draft("100", "0,5"), DistanceUnit.M).target!!.size!!.value)
    }

    @Test
    fun `F-25 an edited target keeps its exact distance when the distance field was not touched`() {
        // 100 m shows as 109.4 yd; saving that text again would give 100.04 m.
        val check = checkTargetDraft(draft("109.4", "2"), DistanceUnit.YD, untouchedDistanceM = 100.0)
        assertEquals(100.0, check.target!!.distanceM)
    }

    @Test
    fun `labels are limited to a short text`() {
        val check = checkTargetDraft(draft("100", label = "x".repeat(100)), DistanceUnit.M)
        assertEquals(MAX_LABEL_LENGTH, check.target!!.label.length)
    }
}
