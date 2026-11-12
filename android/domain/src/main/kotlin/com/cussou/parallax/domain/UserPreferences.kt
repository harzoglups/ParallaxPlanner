package com.cussou.parallax.domain

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Accent color of the app (F-15). The launcher icon stays azure whatever the choice. */
enum class AccentColor {
    AZURE, SKY_BLUE, ELECTRIC_BLUE, ROYAL_BLUE, GLACIER_BLUE, CYAN, VIOLET, ORANGE, BRIGHT_ORANGE, LIME,
}

/**
 * The user's preferences (docs/01-requirements.md §2). The default values are the ones used
 * when locale detection gives nothing: English, meters and mrad (F-12, F-22, F-23).
 */
data class UserPreferences(
    val language: Language = Language.EN,
    val distanceUnit: DistanceUnit = DistanceUnit.M,
    val angleUnit: AngleUnit = AngleUnit.MRAD,
    val targetSizeUnit: SizeUnit = SizeUnit.MRAD,
    val objectiveMm: Double = 50.0,
    val eyePosition: EyePosition = EyePosition.GOOD,
    val orangeThreshold: Double = 0.5,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentColor = AccentColor.AZURE,
) {
    companion object {
        /** Valid objective diameters in mm (F-30). */
        val OBJECTIVE_MM_RANGE = 20.0..80.0

        /** Valid orange thresholds, as a fraction of the target half-size (F-32). */
        val ORANGE_THRESHOLD_RANGE = 0.1..0.9

        /** Preferences for a first launch: detected language and units, static defaults for the rest. */
        fun from(defaults: Defaults) = UserPreferences(
            language = defaults.language,
            distanceUnit = defaults.distanceUnit,
            angleUnit = defaults.angleUnit,
            targetSizeUnit = defaults.targetSizeUnit,
        )
    }
}
