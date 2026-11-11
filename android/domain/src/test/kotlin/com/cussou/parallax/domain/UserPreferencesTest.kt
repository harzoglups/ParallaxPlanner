package com.cussou.parallax.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/** Preference defaults, docs/01-requirements.md §2. */
class UserPreferencesTest {
    @Test
    fun `F-30 F-31 F-32 F-40 static defaults`() {
        val preferences = UserPreferences()
        assertEquals(50.0, preferences.objectiveMm)
        assertEquals(EyePosition.GOOD, preferences.eyePosition)
        assertEquals(0.5, preferences.orangeThreshold)
        assertEquals(false, preferences.advancedMode)
        assertEquals(ThemeMode.SYSTEM, preferences.theme)
    }

    @Test
    fun `F-12 F-22 F-23 without detection the defaults are English, meters and mrad`() {
        val preferences = UserPreferences()
        assertEquals(Language.EN, preferences.language)
        assertEquals(DistanceUnit.M, preferences.distanceUnit)
        assertEquals(AngleUnit.MRAD, preferences.angleUnit)
        assertEquals(SizeUnit.MRAD, preferences.targetSizeUnit)
    }

    @Test
    fun `F-11 F-21 detected defaults only set language and units`() {
        val preferences = UserPreferences.from(detectDefaults(listOf("fr-US")))
        assertEquals(
            UserPreferences(
                language = Language.FR,
                distanceUnit = DistanceUnit.YD,
                angleUnit = AngleUnit.MOA,
                targetSizeUnit = SizeUnit.MRAD,
            ),
            preferences,
        )
    }

    @Test
    fun `F-30 F-32 valid ranges`() {
        assertEquals(20.0..80.0, UserPreferences.OBJECTIVE_MM_RANGE)
        assertEquals(0.1..0.9, UserPreferences.ORANGE_THRESHOLD_RANGE)
    }
}
