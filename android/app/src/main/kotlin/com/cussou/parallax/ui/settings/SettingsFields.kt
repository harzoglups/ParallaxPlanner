package com.cussou.parallax.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.os.LocaleListCompat
import com.cussou.parallax.R
import com.cussou.parallax.domain.AngleUnit
import com.cussou.parallax.domain.DistanceUnit
import com.cussou.parallax.domain.Language
import com.cussou.parallax.domain.UserPreferences
import com.cussou.parallax.ui.components.SegmentedChoice
import com.cussou.parallax.ui.components.angleUnitLabel
import com.cussou.parallax.ui.components.distanceUnitLabel

// Fields shared by the settings screen and the first-launch screen.

/** Applies [language] to the whole app; the activity is recreated in the new language (F-10). */
fun applyAppLanguage(language: Language) {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.code))
}

@Composable
fun LanguageChoice(selected: Language, onSelect: (Language) -> Unit) {
    SegmentedChoice(
        title = stringResource(R.string.language),
        options = listOf(
            Language.EN to stringResource(R.string.language_en),
            Language.FR to stringResource(R.string.language_fr),
        ),
        selected = selected,
        onSelect = onSelect,
        tagPrefix = SettingsTags.LANGUAGE,
    )
}

@Composable
fun DistanceUnitChoice(selected: DistanceUnit, onSelect: (DistanceUnit) -> Unit) {
    SegmentedChoice(
        title = stringResource(R.string.distance),
        options = DistanceUnit.entries.map { it to distanceUnitLabel(it) },
        selected = selected,
        onSelect = onSelect,
        tagPrefix = SettingsTags.DISTANCE_UNIT,
    )
}

@Composable
fun AngleUnitChoice(selected: AngleUnit, onSelect: (AngleUnit) -> Unit) {
    SegmentedChoice(
        title = stringResource(R.string.angles),
        options = AngleUnit.entries.map { it to angleUnitLabel(it) },
        selected = selected,
        onSelect = onSelect,
        tagPrefix = SettingsTags.ANGLE_UNIT,
    )
}

/** Objective diameter in mm, flagged when it is outside the valid range (F-30). */
@Composable
fun ObjectiveField(text: String, isError: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = text,
        onValueChange = { typed -> onChange(typed.filter { it.isDigit() || it == '.' || it == ',' }.take(MAX_INPUT_LENGTH)) },
        modifier = Modifier.fillMaxWidth().testTag(SettingsTags.OBJECTIVE_FIELD),
        label = { Text(stringResource(R.string.objective_diameter)) },
        suffix = { Text(stringResource(R.string.unit_mm)) },
        isError = isError,
        supportingText = {
            Text(
                if (isError) {
                    stringResource(
                        R.string.objective_out_of_range,
                        UserPreferences.OBJECTIVE_MM_RANGE.start.toInt().toString(),
                        UserPreferences.OBJECTIVE_MM_RANGE.endInclusive.toInt().toString(),
                    )
                } else {
                    stringResource(R.string.objective_hint)
                },
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
    )
}

private const val MAX_INPUT_LENGTH = 5
