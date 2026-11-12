package com.cussou.parallax.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cussou.parallax.BuildConfig
import com.cussou.parallax.R
import com.cussou.parallax.domain.AccentColor
import com.cussou.parallax.domain.AngleUnit
import com.cussou.parallax.domain.DistanceUnit
import com.cussou.parallax.domain.EyePosition
import com.cussou.parallax.domain.Language
import com.cussou.parallax.domain.SizeUnit
import com.cussou.parallax.domain.ThemeMode
import com.cussou.parallax.domain.UserPreferences
import com.cussou.parallax.ui.components.Disclaimer
import com.cussou.parallax.ui.components.EyePositionSelector
import com.cussou.parallax.ui.components.SegmentedChoice
import com.cussou.parallax.ui.components.sizeUnitLabel
import com.cussou.parallax.ui.theme.ParallaxPlannerTheme
import com.cussou.parallax.ui.theme.shades
import kotlin.math.roundToInt

/** Test tags of the settings screen. Choices are tagged "[prefix].[OPTION]", e.g. "distanceUnit.YD". */
object SettingsTags {
    const val TITLE = "settings.title"
    const val OBJECTIVE_FIELD = "settings.objective"
    const val LANGUAGE = "language"
    const val THEME = "theme"
    const val ACCENT = "accent"
    const val DISTANCE_UNIT = "distanceUnit"
    const val ANGLE_UNIT = "angleUnit"
    const val TARGET_SIZE_UNIT = "targetSizeUnit"
    const val ORANGE_THRESHOLD = "settings.orangeThreshold"
    const val DISCLAIMER = "settings.disclaimer"
    const val VERSION = "settings.version"
}

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onLanguageChange = { language ->
            viewModel.onLanguageChange(language)
            applyAppLanguage(language)
        },
        onThemeChange = viewModel::onThemeChange,
        onAccentChange = viewModel::onAccentChange,
        onDistanceUnitChange = viewModel::onDistanceUnitChange,
        onAngleUnitChange = viewModel::onAngleUnitChange,
        onObjectiveChange = viewModel::onObjectiveChange,
        onEyePositionChange = viewModel::onEyePositionChange,
        onTargetSizeUnitChange = viewModel::onTargetSizeUnitChange,
        onOrangeThresholdChange = viewModel::onOrangeThresholdChange,
    )
}

/** Settings (docs/03-ux.md §5), a tab of the bottom bar. [state] is null while the preferences load. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState?,
    onLanguageChange: (Language) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onAccentChange: (AccentColor) -> Unit,
    onDistanceUnitChange: (DistanceUnit) -> Unit,
    onAngleUnitChange: (AngleUnit) -> Unit,
    onObjectiveChange: (String) -> Unit,
    onEyePositionChange: (EyePosition) -> Unit,
    onTargetSizeUnitChange: (SizeUnit) -> Unit,
    onOrangeThresholdChange: (Double) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), Modifier.testTag(SettingsTags.TITLE)) },
            )
        },
    ) { innerPadding ->
        if (state == null) return@Scaffold
        val preferences = state.preferences
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Leaves room for the keyboard, so the focused field can scroll above it.
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionTitle(R.string.settings_general)
            LanguageChoice(preferences.language, onLanguageChange)
            SegmentedChoice(
                title = stringResource(R.string.theme),
                options = listOf(
                    ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                    ThemeMode.LIGHT to stringResource(R.string.theme_light),
                    ThemeMode.DARK to stringResource(R.string.theme_dark),
                ),
                selected = preferences.theme,
                onSelect = onThemeChange,
                tagPrefix = SettingsTags.THEME,
            )
            AccentChoice(preferences.accent, onAccentChange)

            SectionTitle(R.string.settings_units)
            DistanceUnitChoice(preferences.distanceUnit, onDistanceUnitChange)
            AngleUnitChoice(preferences.angleUnit, onAngleUnitChange)

            SectionTitle(R.string.settings_scope)
            ObjectiveField(state.objectiveText, state.objectiveError, onObjectiveChange)

            SectionTitle(R.string.settings_assumptions)
            EyePositionSelector(preferences.eyePosition, onEyePositionChange)

            // The settings that only matter with targets.
            SectionTitle(R.string.targets)
            SegmentedChoice(
                title = stringResource(R.string.target_size),
                options = SizeUnit.entries.map { it to sizeUnitLabel(it) },
                selected = preferences.targetSizeUnit,
                onSelect = onTargetSizeUnitChange,
                tagPrefix = SettingsTags.TARGET_SIZE_UNIT,
            )
            OrangeThreshold(preferences.orangeThreshold, onOrangeThresholdChange)

            SectionTitle(R.string.about)
            AboutSection()
        }
    }
}

/**
 * One round swatch per accent, in the shade of the current theme (F-15). The chosen one carries a
 * check mark and its name is written above, so the choice never relies on color alone.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentChoice(selected: AccentColor, onSelect: (AccentColor) -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.accent_color_value, stringResource(selected.label)),
            style = MaterialTheme.typography.titleSmall,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AccentColor.entries.forEach { accent ->
                val name = stringResource(accent.label)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accent.shades().let { if (dark) it.dark else it.light })
                        .selectable(selected = accent == selected, role = Role.RadioButton) { onSelect(accent) }
                        .semantics { contentDescription = name }
                        .testTag("${SettingsTags.ACCENT}.$accent"),
                    contentAlignment = Alignment.Center,
                ) {
                    if (accent == selected) {
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

private val AccentColor.label: Int
    get() = when (this) {
        AccentColor.AZURE -> R.string.accent_azure
        AccentColor.SKY_BLUE -> R.string.accent_sky_blue
        AccentColor.ELECTRIC_BLUE -> R.string.accent_electric_blue
        AccentColor.ROYAL_BLUE -> R.string.accent_royal_blue
        AccentColor.GLACIER_BLUE -> R.string.accent_glacier_blue
        AccentColor.CYAN -> R.string.accent_cyan
        AccentColor.VIOLET -> R.string.accent_violet
        AccentColor.ORANGE -> R.string.accent_orange
        AccentColor.BRIGHT_ORANGE -> R.string.accent_bright_orange
        AccentColor.LIME -> R.string.accent_lime
    }

/** Orange threshold from 10 to 90 %, in steps of 5 % (F-32). */
@Composable
private fun OrangeThreshold(threshold: Double, onChange: (Double) -> Unit) {
    val range = UserPreferences.ORANGE_THRESHOLD_RANGE
    val percent = (threshold * 100).roundToInt()
    Column {
        Text(
            stringResource(R.string.orange_threshold_value, stringResource(R.string.percent, percent.toString())),
            style = MaterialTheme.typography.titleSmall,
        )
        Slider(
            value = threshold.toFloat(),
            // Snapped to whole steps, so the stored value is exactly 0.30 and not 0.30000001.
            onValueChange = { onChange((it * THRESHOLD_STEPS_PER_UNIT).roundToInt() / THRESHOLD_STEPS_PER_UNIT) },
            valueRange = range.start.toFloat()..range.endInclusive.toFloat(),
            steps = ((range.endInclusive - range.start) * THRESHOLD_STEPS_PER_UNIT).roundToInt() - 1,
            modifier = Modifier.testTag(SettingsTags.ORANGE_THRESHOLD),
        )
        Text(
            stringResource(R.string.orange_threshold_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 20 steps per unit: the threshold moves by 5 %. */
private const val THRESHOLD_STEPS_PER_UNIT = 20.0

/** Short explanation of parallax, the formulas, the disclaimer and the version (docs/03-ux.md §5). */
@Composable
private fun AboutSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.about_parallax), style = MaterialTheme.typography.bodyMedium)
        Column {
            Text(
                stringResource(R.string.about_error_formula),
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                stringResource(R.string.about_error_formula_legend),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column {
            Text(stringResource(R.string.about_optimal), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.about_optimal_formula),
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
            )
        }
        Disclaimer(textTag = SettingsTags.DISCLAIMER)
        Text(
            stringResource(R.string.about_version, stringResource(R.string.app_name), BuildConfig.VERSION_NAME),
            modifier = Modifier.testTag(SettingsTags.VERSION),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.about_fonts),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionTitle(title: Int) {
    Text(
        stringResource(title),
        modifier = Modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    ParallaxPlannerTheme {
        SettingsScreen(
            state = SettingsUiState(UserPreferences(), "50", objectiveError = false),
            onLanguageChange = {},
            onThemeChange = {},
            onAccentChange = {},
            onDistanceUnitChange = {},
            onAngleUnitChange = {},
            onObjectiveChange = {},
            onEyePositionChange = {},
            onTargetSizeUnitChange = {},
            onOrangeThresholdChange = {},
        )
    }
}
