package com.cussou.parallax

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cussou.parallax.ui.quickcalc.QuickCalcTags
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The real app on a device: typing a range shows the setting (F-01, F-02, F-04).
 * Assertions only use values that do not depend on the language or on the eye position.
 */
@RunWith(AndroidJUnit4::class)
class QuickCalcScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    /** These tests start on the quick calc: the first-launch screen is marked as seen. */
    @Before
    fun skipOnboarding() = runBlocking {
        (compose.activity.application as ParallaxApp).preferencesRepository.completeOnboarding()
    }

    @Test
    fun typingARangeShowsTheOptimalSetting() {
        compose.waitUntil(5_000) { compose.onAllNodes(androidx.compose.ui.test.hasTestTag(QuickCalcTags.EMPTY)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(QuickCalcTags.EMPTY).assertIsDisplayed()

        compose.onNodeWithTag(QuickCalcTags.MIN_FIELD).performTextInput("100")
        compose.onNodeWithTag(QuickCalcTags.MAX_FIELD).performTextInput("200")
        compose.onNodeWithTag(QuickCalcTags.SETTING).assertTextContains("133", substring = true)
        compose.onNodeWithTag(QuickCalcTags.MAX_ERROR).assertIsDisplayed()

        // Live update, no button (F-04).
        compose.onNodeWithTag(QuickCalcTags.MAX_FIELD).performTextClearance()
        compose.onNodeWithTag(QuickCalcTags.MAX_FIELD).performTextInput("100")
        compose.onNodeWithTag(QuickCalcTags.SETTING).assertTextContains("100", substring = true)

        compose.onNodeWithTag(QuickCalcTags.MAX_FIELD).performTextClearance()
        compose.onNodeWithTag(QuickCalcTags.EMPTY).assertIsDisplayed()
    }

    @Test
    fun comparingWithTheCurrentSettingIsOptional() {
        compose.waitUntil(5_000) { compose.onAllNodes(androidx.compose.ui.test.hasTestTag(QuickCalcTags.EMPTY)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(QuickCalcTags.MIN_FIELD).performTextInput("100")
        compose.onNodeWithTag(QuickCalcTags.MAX_FIELD).performTextInput("200")

        // Collapsed by default (F-06).
        compose.onNodeWithTag(QuickCalcTags.CURRENT_SETTING_FIELD).assertDoesNotExist()
        compose.onNodeWithTag(QuickCalcTags.COMPARE_TOGGLE).performScrollTo().performClick()
        compose.onNodeWithTag(QuickCalcTags.COMPARISON).assertDoesNotExist()

        compose.onNodeWithTag(QuickCalcTags.CURRENT_SETTING_FIELD).performScrollTo().performTextInput("150")
        compose.onNodeWithTag(QuickCalcTags.COMPARISON).performScrollTo().assertIsDisplayed()
        // The optimal setting stays the hero value.
        compose.onNodeWithTag(QuickCalcTags.SETTING).assertTextContains("133", substring = true)
    }

    @Test
    fun aReversedRangeStillGivesTheSetting() {
        compose.waitUntil(5_000) { compose.onAllNodes(androidx.compose.ui.test.hasTestTag(QuickCalcTags.EMPTY)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(QuickCalcTags.MIN_FIELD).performTextInput("100")
        compose.onNodeWithTag(QuickCalcTags.MAX_FIELD).performTextInput("25")
        compose.onNodeWithTag(QuickCalcTags.SETTING).assertTextContains("40", substring = true)
    }
}
