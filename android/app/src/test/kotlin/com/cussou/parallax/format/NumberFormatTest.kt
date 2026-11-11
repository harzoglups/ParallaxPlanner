package com.cussou.parallax.format

import com.cussou.parallax.domain.AngleUnit
import com.cussou.parallax.domain.DistanceUnit
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Display rounding (NF-07) and number input. */
class NumberFormatTest {
    private val en = Locale.ENGLISH
    private val fr = Locale.FRENCH

    @Test
    fun `NF-07 setting is rounded to the unit`() {
        // Vector V1: 133.33 m.
        assertEquals("133", formatSetting(133.3333333333, DistanceUnit.M, en))
        assertEquals("146", formatSetting(133.3333333333, DistanceUnit.YD, en))
        assertEquals("134", formatSetting(133.5, DistanceUnit.M, en))
        assertEquals("40", formatSetting(40.0, DistanceUnit.M, en))
    }

    @Test
    fun `NF-07 setting has no thousands separator`() {
        assertEquals("1234", formatSetting(1234.4, DistanceUnit.M, en))
        assertEquals("1234", formatSetting(1234.4, DistanceUnit.M, fr))
    }

    @Test
    fun `NF-07 angular error has 2 decimals in mrad and 1 in MOA`() {
        // Vector V3: 0.6562 mrad = 2.256 MOA.
        assertEquals("0.66", formatAngularError(0.656167979, AngleUnit.MRAD, en))
        assertEquals("2.3", formatAngularError(0.656167979, AngleUnit.MOA, en))
        // Vector V1: 0.0625 mrad.
        assertEquals("0.06", formatAngularError(0.0625, AngleUnit.MRAD, en))
        assertEquals("0.2", formatAngularError(0.0625, AngleUnit.MOA, en))
        assertEquals("0.00", formatAngularError(0.0, AngleUnit.MRAD, en))
        assertEquals("0.0", formatAngularError(0.0, AngleUnit.MOA, en))
    }

    @Test
    fun `NF-07 linear error has 1 decimal`() {
        // Vector V1: 6.25 mm at 100 m, 12.5 mm at 200 m.
        assertEquals("0.6", formatLinearError(6.25, DistanceUnit.M, en))
        assertEquals("1.3", formatLinearError(12.5, DistanceUnit.M, en))
        assertEquals("0.2", formatLinearError(6.25, DistanceUnit.YD, en))
        assertEquals("0.0", formatLinearError(0.0, DistanceUnit.M, en))
    }

    @Test
    fun `distances and objective diameters show at most 1 decimal, without trailing zero`() {
        assertEquals("100", formatDistance(100.0, DistanceUnit.M, en))
        assertEquals("109.4", formatDistance(100.0, DistanceUnit.YD, en))
        assertEquals("109,4", formatDistance(100.0, DistanceUnit.YD, fr))
        assertEquals("22.9", formatDistance(22.86, DistanceUnit.M, en))
        assertEquals("2000", formatDistance(2000.0, DistanceUnit.M, en))
        assertEquals("50", formatObjective(50.0, en))
        assertEquals("44,5", formatObjective(44.5, fr))
    }

    @Test
    fun `target sizes show at most 2 decimals, without trailing zero`() {
        assertEquals("1", formatSize(1.0, en))
        assertEquals("0.5", formatSize(0.5, en))
        assertEquals("0,25", formatSize(0.25, fr))
        assertEquals("30", formatSize(30.0, en))
        assertEquals("1.13", formatSize(1.125, en))
    }

    @Test
    fun `NF-07 halves round up`() {
        assertEquals("0.13", formatAngularError(0.125, AngleUnit.MRAD, en))
        assertEquals("0.3", formatLinearError(2.5, DistanceUnit.M, en))
    }

    @Test
    fun `numbers use the decimal separator of the locale`() {
        assertEquals("0,06", formatAngularError(0.0625, AngleUnit.MRAD, fr))
        assertEquals("2,3", formatAngularError(0.656167979, AngleUnit.MOA, fr))
        assertEquals("1,3", formatLinearError(12.5, DistanceUnit.M, fr))
    }

    @Test
    fun `entered numbers accept a dot or a comma`() {
        assertEquals(100.0, parseDecimal("100"))
        assertEquals(100.5, parseDecimal("100.5"))
        assertEquals(100.5, parseDecimal("100,5"))
        assertEquals(100.0, parseDecimal(" 100 "))
        assertEquals(100.0, parseDecimal("100."))
        assertEquals(0.5, parseDecimal(",5"))
    }

    @Test
    fun `anything that is not a plain positive number is not a number`() {
        for (text in listOf("", " ", ".", "abc", "1,2,3", "1.2.3", "-5", "1e3", "NaN", "Infinity", "1f", "0x10")) {
            assertNull(parseDecimal(text), "\"$text\"")
        }
    }
}
