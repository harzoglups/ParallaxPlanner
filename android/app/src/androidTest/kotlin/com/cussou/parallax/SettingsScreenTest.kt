package com.cussou.parallax

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cussou.parallax.domain.AccentColor
import com.cussou.parallax.domain.Language
import com.cussou.parallax.domain.SizeUnit
import com.cussou.parallax.domain.UserPreferences
import com.cussou.parallax.ui.NAVIGATION_TAG
import com.cussou.parallax.ui.Screen
import com.cussou.parallax.ui.quickcalc.QuickCalcTags
import com.cussou.parallax.ui.settings.SettingsTags
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Settings on a device: each choice shows on the quick calc at once (F-10, F-20, F-25, F-30). */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val repository get() = (compose.activity.application as ParallaxApp).preferencesRepository
    private lateinit var saved: UserPreferences

    /** Units and scope start from the defaults, past the first-launch screen; the language is left as it is. */
    @Before
    fun resetPreferences() = runBlocking {
        saved = repository.preferences.first()
        repository.update { UserPreferences(language = it.language) }
        repository.completeOnboarding()
    }

    @After
    fun restorePreferences() = runBlocking {
        repository.update { saved }
    }

    private fun waitFor(tag: String) {
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun typeRange() {
        waitFor(QuickCalcTags.MIN_FIELD)
        compose.onNodeWithTag(QuickCalcTags.MIN_FIELD).performTextInput("100")
        compose.onNodeWithTag(QuickCalcTags.MAX_FIELD).performTextInput("200")
    }

    private fun tab(screen: Screen) = "$NAVIGATION_TAG.$screen"

    private fun openSettings() {
        waitFor(tab(Screen.SETTINGS))
        compose.onNodeWithTag(tab(Screen.SETTINGS)).performClick()
        waitFor(SettingsTags.TITLE)
    }

    private fun backToCalc() = compose.onNodeWithTag(tab(Screen.CALC)).performClick()

    private fun choose(tag: String) = compose.onNodeWithTag(tag).performScrollTo().performClick()

    @Test
    fun switchingToYardsConvertsTheTypedRange() {
        typeRange()
        compose.onNodeWithTag(QuickCalcTags.SETTING).assertTextContains("133", substring = true)
        compose.onNodeWithTag(QuickCalcTags.SETTING).assertTextContains("m", substring = true)

        openSettings()
        choose("${SettingsTags.DISTANCE_UNIT}.YD")
        backToCalc()

        waitFor(QuickCalcTags.SETTING)
        // Same range, now shown in yards: 133.33 m = 145.8 yd (F-25).
        compose.onNodeWithTag(QuickCalcTags.SETTING).assertTextContains("146", substring = true)
        compose.onNodeWithTag(QuickCalcTags.SETTING).assertTextContains("yd", substring = true)
    }

    @Test
    fun switchingToMoaChangesTheErrorUnit() {
        typeRange()
        compose.onNodeWithTag(QuickCalcTags.MAX_ERROR).assertTextContains("mrad", substring = true)

        openSettings()
        choose("${SettingsTags.ANGLE_UNIT}.MOA")
        backToCalc()

        waitFor(QuickCalcTags.MAX_ERROR)
        compose.onNodeWithTag(QuickCalcTags.MAX_ERROR).assertTextContains("MOA", substring = true)
    }

    @Test
    fun objectiveDiameterIsValidatedAndStored() {
        openSettings()
        compose.onNodeWithTag(SettingsTags.OBJECTIVE_FIELD).performScrollTo().performTextReplacement("56")
        compose.waitUntil(5_000) { runBlocking { repository.preferences.first().objectiveMm == 56.0 } }

        // Out of the 20-80 mm range: flagged, and the stored value does not move (F-30).
        compose.onNodeWithTag(SettingsTags.OBJECTIVE_FIELD).performTextReplacement("500")
        compose.waitForIdle()
        assert(runBlocking { repository.preferences.first().objectiveMm } == 56.0)
    }

    @Test
    fun aboutShowsTheDisclaimerAndTheVersion() {
        openSettings()
        // NF-11: the model is simplified, the head-movement test remains the reference.
        compose.onNodeWithTag(SettingsTags.DISCLAIMER).performScrollTo().assertIsDisplayed()
            .assertTextEquals(compose.activity.getString(R.string.disclaimer))
        compose.onNodeWithTag(SettingsTags.VERSION).performScrollTo()
            .assertTextContains(BuildConfig.VERSION_NAME, substring = true)
    }

    @Test
    fun anAccentColorIsChosenFromItsSwatch() {
        openSettings()
        // F-15: the chosen swatch is selected, and its name is written, not only shown in color.
        choose("${SettingsTags.ACCENT}.VIOLET")
        compose.waitUntil(5_000) { runBlocking { repository.preferences.first().accent == AccentColor.VIOLET } }
        compose.onNodeWithTag("${SettingsTags.ACCENT}.VIOLET").assertIsSelected()
        compose.onNodeWithText(compose.activity.getString(R.string.accent_color_value, compose.activity.getString(R.string.accent_violet)))
            .assertIsDisplayed()
    }

    @Test
    fun targetSettingsAreStored() {
        openSettings()
        choose("${SettingsTags.TARGET_SIZE_UNIT}.CM")
        compose.waitUntil(5_000) { runBlocking { repository.preferences.first().targetSizeUnit == SizeUnit.CM } }

        compose.onNodeWithTag(SettingsTags.ORANGE_THRESHOLD).performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.3f) }
        compose.waitUntil(5_000) { runBlocking { repository.preferences.first().orangeThreshold == 0.3 } }
    }

    @Test
    fun choosingALanguageTranslatesTheApp() {
        val original = runBlocking { repository.preferences.first().language }
        openSettings()
        try {
            choose("${SettingsTags.LANGUAGE}.EN")
            compose.waitUntil(10_000) { titleIs("Settings") }
            compose.onNodeWithTag("${SettingsTags.LANGUAGE}.EN").assertIsSelected()

            choose("${SettingsTags.LANGUAGE}.FR")
            compose.waitUntil(10_000) { titleIs("Réglages") }
            compose.onNodeWithTag(SettingsTags.TITLE).assertTextEquals("Réglages")
        } finally {
            // Leave the app in the language it had (F-10 is a user choice).
            choose("${SettingsTags.LANGUAGE}.${original.name}")
            compose.waitUntil(10_000) { titleIs(if (original == Language.FR) "Réglages" else "Settings") }
        }
    }

    private fun titleIs(text: String): Boolean = runCatching {
        compose.onNodeWithTag(SettingsTags.TITLE).assertTextEquals(text)
    }.isSuccess
}
