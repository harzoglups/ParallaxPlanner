package com.cussou.parallax.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cussou.parallax.domain.AngleUnit
import com.cussou.parallax.domain.DistanceUnit
import com.cussou.parallax.domain.EyePosition
import com.cussou.parallax.domain.Language
import com.cussou.parallax.domain.SizeUnit
import com.cussou.parallax.domain.ThemeMode
import com.cussou.parallax.domain.UserPreferences
import com.cussou.parallax.domain.detectDefaults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The user's preferences, stored locally in a DataStore (NF-04).
 * Keys and values follow the "Preferences" block of docs/04-architecture.md.
 */
class PreferencesRepository(private val dataStore: DataStore<Preferences>) {

    val preferences: Flow<UserPreferences> = dataStore.data.map(::read)

    /** False until the user has gone through the first-launch screen. */
    val onboardingDone: Flow<Boolean> = dataStore.data.map { it[ONBOARDING_DONE] == true }

    suspend fun completeOnboarding() {
        dataStore.edit { it[ONBOARDING_DONE] = true }
    }

    suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        dataStore.edit { stored -> write(stored, transform(read(stored))) }
    }

    /**
     * Stores the defaults detected from the device locales, the first time only (F-11).
     * On later launches the stored preferences win and this does nothing.
     */
    suspend fun initializeOnFirstLaunch(localeTags: List<String>) {
        dataStore.edit { stored ->
            if (stored[INITIALIZED] != true) {
                write(stored, UserPreferences.from(detectDefaults(localeTags)))
                stored[INITIALIZED] = true
            }
        }
    }

    private fun read(stored: Preferences): UserPreferences {
        val defaults = UserPreferences()
        return UserPreferences(
            language = LANGUAGES.fromToken(stored[LANGUAGE]) ?: defaults.language,
            distanceUnit = DISTANCE_UNITS.fromToken(stored[DISTANCE_UNIT]) ?: defaults.distanceUnit,
            angleUnit = ANGLE_UNITS.fromToken(stored[ANGLE_UNIT]) ?: defaults.angleUnit,
            targetSizeUnit = SIZE_UNITS.fromToken(stored[TARGET_SIZE_UNIT]) ?: defaults.targetSizeUnit,
            objectiveMm = (stored[OBJECTIVE_MM] ?: defaults.objectiveMm)
                .coerceIn(UserPreferences.OBJECTIVE_MM_RANGE),
            eyePosition = EYE_POSITIONS.fromToken(stored[EYE_POSITION]) ?: defaults.eyePosition,
            orangeThreshold = (stored[ORANGE_THRESHOLD] ?: defaults.orangeThreshold)
                .coerceIn(UserPreferences.ORANGE_THRESHOLD_RANGE),
            advancedMode = stored[ADVANCED_MODE] ?: defaults.advancedMode,
            theme = THEMES.fromToken(stored[THEME]) ?: defaults.theme,
        )
    }

    private fun write(stored: MutablePreferences, preferences: UserPreferences) {
        stored[LANGUAGE] = LANGUAGES.getValue(preferences.language)
        stored[DISTANCE_UNIT] = DISTANCE_UNITS.getValue(preferences.distanceUnit)
        stored[ANGLE_UNIT] = ANGLE_UNITS.getValue(preferences.angleUnit)
        stored[TARGET_SIZE_UNIT] = SIZE_UNITS.getValue(preferences.targetSizeUnit)
        stored[OBJECTIVE_MM] = preferences.objectiveMm
        stored[EYE_POSITION] = EYE_POSITIONS.getValue(preferences.eyePosition)
        stored[ORANGE_THRESHOLD] = preferences.orangeThreshold
        stored[ADVANCED_MODE] = preferences.advancedMode
        stored[THEME] = THEMES.getValue(preferences.theme)
    }

    private companion object {
        val INITIALIZED = booleanPreferencesKey("initialized")
        val ONBOARDING_DONE = booleanPreferencesKey("onboardingDone")
        val LANGUAGE = stringPreferencesKey("language")
        val DISTANCE_UNIT = stringPreferencesKey("distanceUnit")
        val ANGLE_UNIT = stringPreferencesKey("angleUnit")
        val TARGET_SIZE_UNIT = stringPreferencesKey("targetSizeUnit")
        val OBJECTIVE_MM = doublePreferencesKey("objectiveMm")
        val EYE_POSITION = stringPreferencesKey("eyePosition")
        val ORANGE_THRESHOLD = doublePreferencesKey("orangeThreshold")
        val ADVANCED_MODE = booleanPreferencesKey("advancedMode")
        val THEME = stringPreferencesKey("theme")

        // Stored tokens. They are part of the file format: never rename one without a migration.
        val LANGUAGES = mapOf(Language.EN to "en", Language.FR to "fr")
        val DISTANCE_UNITS = mapOf(DistanceUnit.M to "m", DistanceUnit.YD to "yd")
        val ANGLE_UNITS = mapOf(AngleUnit.MRAD to "mrad", AngleUnit.MOA to "moa")
        val SIZE_UNITS = mapOf(
            SizeUnit.MRAD to "mrad",
            SizeUnit.MOA to "moa",
            SizeUnit.CM to "cm",
            SizeUnit.IN to "in",
        )
        val EYE_POSITIONS = mapOf(
            EyePosition.EXCELLENT to "excellent",
            EyePosition.GOOD to "good",
            EyePosition.WORST_CASE to "worstCase",
        )
        val THEMES = mapOf(ThemeMode.SYSTEM to "system", ThemeMode.LIGHT to "light", ThemeMode.DARK to "dark")

        fun <T> Map<T, String>.fromToken(token: String?): T? = entries.firstOrNull { it.value == token }?.key
    }
}
