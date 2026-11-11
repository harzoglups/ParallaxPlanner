package com.cussou.parallax.domain

/** Languages the app is translated into (F-13). [code] is the ISO 639-1 language code. */
enum class Language(val code: String) { EN("en"), FR("fr") }

enum class DistanceUnit { M, YD }

enum class AngleUnit { MRAD, MOA }

/** Preferences proposed on first launch. */
data class Defaults(
    val language: Language,
    val distanceUnit: DistanceUnit,
    val angleUnit: AngleUnit,
    val targetSizeUnit: SizeUnit,
)

/** Regions where shooters use yards and MOA by default (F-21). */
private val IMPERIAL_REGIONS = setOf("US", "LR", "MM")

/**
 * Defaults for the device locales, given as language tags ("fr-FR") in order of preference.
 *
 * Language: the first supported one in the list, otherwise English (F-11, F-12).
 * Units: from the region of the first locale; metric when it is missing or unreadable (F-21, F-22).
 * The target size unit is always mrad (F-23).
 */
fun detectDefaults(localeTags: List<String>): Defaults {
    val language = localeTags.firstNotNullOfOrNull(::languageOf) ?: Language.EN
    val imperial = localeTags.firstOrNull()?.let(::region) in IMPERIAL_REGIONS
    return Defaults(
        language = language,
        distanceUnit = if (imperial) DistanceUnit.YD else DistanceUnit.M,
        angleUnit = if (imperial) AngleUnit.MOA else AngleUnit.MRAD,
        targetSizeUnit = SizeUnit.MRAD,
    )
}

/** Language of a locale tag ("fr-CA"), or null when the app is not translated into it. */
fun languageOf(localeTag: String): Language? {
    val code = subtags(localeTag).firstOrNull()?.lowercase()
    return Language.entries.firstOrNull { it.code == code }
}

private fun subtags(tag: String): List<String> = tag.split('-', '_')

/** Two-letter region of a language tag, upper case. Stops at extensions (single-letter subtags). */
private fun region(tag: String): String? =
    subtags(tag).drop(1)
        .takeWhile { it.length > 1 }
        .firstOrNull { it.length == 2 && it.all(Char::isLetter) }
        ?.uppercase()
