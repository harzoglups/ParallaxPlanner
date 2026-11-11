package com.cussou.parallax.format

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.util.Locale

// Match dates are stored as ISO 8601 text ("2026-12-14"); these functions are the only place
// where they are turned into something else.

/** The date the way [locale] writes it ("Dec 14, 2026"). Text that is not a date is returned as it is. */
fun formatDate(isoDate: String, locale: Locale): String = try {
    LocalDate.parse(isoDate).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
} catch (e: DateTimeParseException) {
    isoDate
}

/** The date picker works with UTC milliseconds at midnight. */
fun isoDateFromUtcMillis(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()

fun utcMillisFromIsoDate(isoDate: String): Long? = try {
    LocalDate.parse(isoDate).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
} catch (e: DateTimeParseException) {
    null
}
