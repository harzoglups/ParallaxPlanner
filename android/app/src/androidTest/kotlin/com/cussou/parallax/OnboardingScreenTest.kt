package com.cussou.parallax

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cussou.parallax.domain.AngleUnit
import com.cussou.parallax.domain.DistanceUnit
import com.cussou.parallax.domain.Language
import com.cussou.parallax.domain.UserPreferences
import com.cussou.parallax.domain.detectDefaults
import com.cussou.parallax.ui.onboarding.OnboardingScreen
import com.cussou.parallax.ui.onboarding.OnboardingTags
import com.cussou.parallax.ui.settings.SettingsTags
import com.cussou.parallax.ui.settings.SettingsUiState
import com.cussou.parallax.ui.theme.ParallaxPlannerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The first-launch screen alone, without the app around it (docs/03-ux.md §1).
 * What it stores is covered by the unit tests of the repository and the view model.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var state by mutableStateOf(
        SettingsUiState(UserPreferences.from(detectDefaults(listOf("en-US"))), "50", objectiveError = false),
    )
    private var started = 0

    private fun show() = compose.setContent {
        ParallaxPlannerTheme {
            OnboardingScreen(
                state = state,
                onLanguageChange = { state = state.copy(preferences = state.preferences.copy(language = it)) },
                onDistanceUnitChange = { state = state.copy(preferences = state.preferences.copy(distanceUnit = it)) },
                onAngleUnitChange = { state = state.copy(preferences = state.preferences.copy(angleUnit = it)) },
                onObjectiveChange = {},
                onStart = { started++ },
            )
        }
    }

    @Test
    fun everythingIsPrefilledSoOneTapIsEnough() {
        show()
        // Detected for a US device: English, yards, MOA, 50 mm (F-11, F-21, F-30).
        compose.onNodeWithTag("${SettingsTags.LANGUAGE}.EN").assertIsSelected()
        compose.onNodeWithTag("${SettingsTags.DISTANCE_UNIT}.YD").assertIsSelected()
        compose.onNodeWithTag("${SettingsTags.ANGLE_UNIT}.MOA").assertIsSelected()
        compose.onNodeWithTag(SettingsTags.OBJECTIVE_FIELD).assertTextContains("50")

        compose.onNodeWithTag(OnboardingTags.START).performScrollTo().assertIsEnabled().performClick()
        assertEquals(1, started)
    }

    @Test
    fun choicesCanBeChangedBeforeStarting() {
        show()
        compose.onNodeWithTag("${SettingsTags.DISTANCE_UNIT}.M").performClick()
        compose.onNodeWithTag("${SettingsTags.ANGLE_UNIT}.MRAD").performClick()
        compose.onNodeWithTag("${SettingsTags.LANGUAGE}.FR").performClick()

        compose.onNodeWithTag("${SettingsTags.DISTANCE_UNIT}.M").assertIsSelected()
        compose.onNodeWithTag("${SettingsTags.ANGLE_UNIT}.MRAD").assertIsSelected()
        compose.onNodeWithTag("${SettingsTags.LANGUAGE}.FR").assertIsSelected()
        assertEquals(
            UserPreferences(language = Language.FR, distanceUnit = DistanceUnit.M, angleUnit = AngleUnit.MRAD),
            state.preferences,
        )
    }

    @Test
    fun anInvalidObjectiveBlocksTheStart() {
        state = state.copy(objectiveText = "500", objectiveError = true)
        show()
        compose.onNodeWithTag(OnboardingTags.START).performScrollTo().assertIsNotEnabled()
    }
}
