package com.cussou.parallax.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cussou.parallax.R
import com.cussou.parallax.domain.AngleUnit
import com.cussou.parallax.domain.DistanceUnit
import com.cussou.parallax.domain.EyePosition
import com.cussou.parallax.domain.Language
import com.cussou.parallax.domain.ThemeMode
import com.cussou.parallax.domain.UserPreferences
import com.cussou.parallax.ui.components.EyePositionSelector
import com.cussou.parallax.ui.components.SegmentedChoice
import com.cussou.parallax.ui.theme.ParallaxPlannerTheme

/** Test tags of the settings screen. Choices are tagged "[prefix].[OPTION]", e.g. "distanceUnit.YD". */
object SettingsTags {
    const val TITLE = "settings.title"
    const val BACK = "settings.back"
    const val OBJECTIVE_FIELD = "settings.objective"
    const val LANGUAGE = "language"
    const val THEME = "theme"
    const val DISTANCE_UNIT = "distanceUnit"
    const val ANGLE_UNIT = "angleUnit"
}

@Composable
fun SettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onBack = onBack,
        onLanguageChange = { language ->
            viewModel.onLanguageChange(language)
            applyAppLanguage(language)
        },
        onThemeChange = viewModel::onThemeChange,
        onDistanceUnitChange = viewModel::onDistanceUnitChange,
        onAngleUnitChange = viewModel::onAngleUnitChange,
        onObjectiveChange = viewModel::onObjectiveChange,
        onEyePositionChange = viewModel::onEyePositionChange,
    )
}

/** Settings of simple mode (docs/03-ux.md §5). [state] is null while the preferences load. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState?,
    onBack: () -> Unit,
    onLanguageChange: (Language) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onDistanceUnitChange: (DistanceUnit) -> Unit,
    onAngleUnitChange: (AngleUnit) -> Unit,
    onObjectiveChange: (String) -> Unit,
    onEyePositionChange: (EyePosition) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), Modifier.testTag(SettingsTags.TITLE)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag(SettingsTags.BACK)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state == null) return@Scaffold
        val preferences = state.preferences
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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

            SectionTitle(R.string.settings_units)
            DistanceUnitChoice(preferences.distanceUnit, onDistanceUnitChange)
            AngleUnitChoice(preferences.angleUnit, onAngleUnitChange)

            SectionTitle(R.string.settings_scope)
            ObjectiveField(state.objectiveText, state.objectiveError, onObjectiveChange)

            SectionTitle(R.string.settings_assumptions)
            EyePositionSelector(preferences.eyePosition, onEyePositionChange)
        }
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
            onBack = {},
            onLanguageChange = {},
            onThemeChange = {},
            onDistanceUnitChange = {},
            onAngleUnitChange = {},
            onObjectiveChange = {},
            onEyePositionChange = {},
        )
    }
}
