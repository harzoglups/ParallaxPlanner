package com.cussou.parallax.data

import com.cussou.parallax.domain.SizeUnit

// Text tokens of the stored formats (preferences and match files).
// They are part of those formats: never rename one without a migration.

internal val SIZE_UNIT_TOKENS = mapOf(
    SizeUnit.MRAD to "mrad",
    SizeUnit.MOA to "moa",
    SizeUnit.CM to "cm",
    SizeUnit.IN to "in",
)

internal fun <T> Map<T, String>.fromToken(token: String?): T? = entries.firstOrNull { it.value == token }?.key
