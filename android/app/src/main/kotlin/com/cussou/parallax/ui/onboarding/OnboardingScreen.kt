package com.cussou.parallax.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.cussou.parallax.domain.Language
import com.cussou.parallax.domain.UserPreferences
import com.cussou.parallax.ui.settings.AngleUnitChoice
import com.cussou.parallax.ui.settings.DistanceUnitChoice
import com.cussou.parallax.ui.settings.LanguageChoice
import com.cussou.parallax.ui.settings.ObjectiveField
import com.cussou.parallax.ui.settings.SettingsUiState
import com.cussou.parallax.ui.settings.SettingsViewModel
import com.cussou.parallax.ui.settings.applyAppLanguage
import com.cussou.parallax.ui.theme.ParallaxPlannerTheme

/** Test tags of the first-launch screen; its fields use the tags of the settings screen. */
object OnboardingTags {
    const val TITLE = "onboarding.title"
    const val START = "onboarding.start"
}

/** The first-launch screen edits the same preferences as the settings, so it shares their view model. */
@Composable
fun OnboardingRoute(viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OnboardingScreen(
        state = state,
        onLanguageChange = { language ->
            viewModel.onLanguageChange(language)
            applyAppLanguage(language)
        },
        onDistanceUnitChange = viewModel::onDistanceUnitChange,
        onAngleUnitChange = viewModel::onAngleUnitChange,
        onObjectiveChange = viewModel::onObjectiveChange,
        onStart = viewModel::onOnboardingCompleted,
    )
}

/**
 * First launch (docs/03-ux.md §1): one page, everything prefilled from the device locales,
 * so a user in a hurry only has to tap the button.
 */
@Composable
fun OnboardingScreen(
    state: SettingsUiState?,
    onLanguageChange: (Language) -> Unit,
    onDistanceUnitChange: (DistanceUnit) -> Unit,
    onAngleUnitChange: (AngleUnit) -> Unit,
    onObjectiveChange: (String) -> Unit,
    onStart: () -> Unit,
) {
    Scaffold { innerPadding ->
        if (state == null) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.onboarding_welcome),
                modifier = Modifier.testTag(OnboardingTags.TITLE),
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                stringResource(R.string.onboarding_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LanguageChoice(state.preferences.language, onLanguageChange)
            DistanceUnitChoice(state.preferences.distanceUnit, onDistanceUnitChange)
            AngleUnitChoice(state.preferences.angleUnit, onAngleUnitChange)
            ObjectiveField(state.objectiveText, state.objectiveError, onObjectiveChange)
            Button(
                onClick = onStart,
                // An invalid diameter is not stored: starting now would silently keep the previous one.
                enabled = !state.objectiveError,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(OnboardingTags.START),
            ) {
                Text(stringResource(R.string.onboarding_start), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Preview
@Composable
private fun OnboardingScreenPreview() {
    ParallaxPlannerTheme {
        OnboardingScreen(
            state = SettingsUiState(UserPreferences(), "50", objectiveError = false),
            onLanguageChange = {},
            onDistanceUnitChange = {},
            onAngleUnitChange = {},
            onObjectiveChange = {},
            onStart = {},
        )
    }
}
