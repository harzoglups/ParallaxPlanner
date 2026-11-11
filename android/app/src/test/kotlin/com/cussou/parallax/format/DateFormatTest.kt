package com.cussou.parallax.format

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DateFormatTest {
    @Test
    fun `match dates are written the way the language writes them`() {
        val english = formatDate("2026-12-14", Locale.US)
        val french = formatDate("2026-12-14", Locale.FRANCE)
        assertTrue("Dec" in english && "14" in english && "2026" in english, english)
        assertTrue("déc" in french && "14" in french && "2026" in french, french)
        assertTrue(french.indexOf("14") < french.indexOf("déc"), french)
    }

    @Test
    fun `an unreadable date is shown as it is rather than hidden`() {
        assertEquals("someday", formatDate("someday", Locale.US))
    }

    @Test
    fun `a day picked in the date picker becomes an ISO date and back`() {
        // The picker works in UTC milliseconds at midnight.
        val millis = 1_797_206_400_000L // 2026-12-14T00:00:00Z
        assertEquals("2026-12-14", isoDateFromUtcMillis(millis))
        assertEquals(millis, utcMillisFromIsoDate("2026-12-14"))
        assertEquals(null, utcMillisFromIsoDate("someday"))
    }
}
