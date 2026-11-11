package com.cussou.parallax.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/** Default language and units from the device locales (F-11, F-12, F-21, F-22, F-23). */
class DefaultsTest {
    private val metricEnglish = Defaults(Language.EN, DistanceUnit.M, AngleUnit.MRAD, SizeUnit.MRAD)
    private val imperialEnglish = Defaults(Language.EN, DistanceUnit.YD, AngleUnit.MOA, SizeUnit.MRAD)
    private val metricFrench = Defaults(Language.FR, DistanceUnit.M, AngleUnit.MRAD, SizeUnit.MRAD)

    @Test
    fun `F-11 language comes from the device language`() {
        assertEquals(metricFrench, detectDefaults(listOf("fr-FR")))
        assertEquals(metricEnglish, detectDefaults(listOf("en-GB")))
        assertEquals(Language.FR, detectDefaults(listOf("fr-CA")).language)
    }

    @Test
    fun `F-10 language of a locale tag, when the app is translated into it`() {
        assertEquals(Language.FR, languageOf("fr-CA"))
        assertEquals(Language.EN, languageOf("en"))
        assertEquals(Language.FR, languageOf("FR_fr"))
        assertEquals(null, languageOf("de-DE"))
        assertEquals(null, languageOf(""))
    }

    @Test
    fun `F-12 unsupported language falls back to English`() {
        assertEquals(metricEnglish, detectDefaults(listOf("de-DE")))
        assertEquals(metricEnglish, detectDefaults(listOf("ja")))
    }

    @Test
    fun `F-11 the first supported language of the list wins`() {
        assertEquals(Language.FR, detectDefaults(listOf("de-DE", "fr-FR", "en-US")).language)
        assertEquals(Language.EN, detectDefaults(listOf("de-DE", "en-GB", "fr-FR")).language)
    }

    @Test
    fun `F-21 US, LR and MM default to yards and MOA`() {
        assertEquals(imperialEnglish, detectDefaults(listOf("en-US")))
        assertEquals(imperialEnglish, detectDefaults(listOf("en-LR")))
        assertEquals(imperialEnglish, detectDefaults(listOf("my-MM")))
    }

    @Test
    fun `F-21 every other region defaults to meters and mrad`() {
        for (tag in listOf("en-GB", "en-CA", "en-AU", "fr-FR", "de-CH", "es-MX")) {
            val defaults = detectDefaults(listOf(tag))
            assertEquals(DistanceUnit.M, defaults.distanceUnit, tag)
            assertEquals(AngleUnit.MRAD, defaults.angleUnit, tag)
        }
    }

    @Test
    fun `F-21 units follow the region of the first locale, whatever the language`() {
        assertEquals(imperialEnglish, detectDefaults(listOf("es-US", "en-GB")))
        assertEquals(Defaults(Language.FR, DistanceUnit.YD, AngleUnit.MOA, SizeUnit.MRAD), detectDefaults(listOf("fr-US")))
        assertEquals(metricEnglish, detectDefaults(listOf("de-DE", "en-US")))
    }

    @Test
    fun `F-22 detection failure defaults to English, meters and mrad`() {
        assertEquals(metricEnglish, detectDefaults(emptyList()))
        assertEquals(metricEnglish, detectDefaults(listOf("")))
        assertEquals(metricEnglish, detectDefaults(listOf("???")))
        assertEquals(metricEnglish, detectDefaults(listOf("en")))
        assertEquals(metricFrench, detectDefaults(listOf("fr")))
    }

    @Test
    fun `F-23 target size unit defaults to mrad in every region`() {
        for (tag in listOf("en-US", "fr-FR", "en-GB", "my-MM")) {
            assertEquals(SizeUnit.MRAD, detectDefaults(listOf(tag)).targetSizeUnit, tag)
        }
    }

    @Test
    fun `locale tags are read whatever their case, separator, script or extension`() {
        assertEquals(imperialEnglish, detectDefaults(listOf("en_US")))
        assertEquals(imperialEnglish, detectDefaults(listOf("EN-us")))
        assertEquals(imperialEnglish, detectDefaults(listOf("en-Latn-US")))
        assertEquals(imperialEnglish, detectDefaults(listOf("en-US-u-ca-gregory")))
        // "mm" here is an extension value, not the Myanmar region.
        assertEquals(metricEnglish, detectDefaults(listOf("en-GB-u-xx-mm")))
        assertEquals(metricEnglish, detectDefaults(listOf("en-u-xx-us")))
    }
}
