package com.cussou.parallax

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cussou.parallax.domain.EyePosition
import com.cussou.parallax.domain.UserPreferences
import com.cussou.parallax.ui.quickcalc.QuickCalcTags
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Targets on the quick calc, on a device (F-50, F-51, F-52, F-66). */
@RunWith(AndroidJUnit4::class)
class TargetListTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val repository get() = (compose.activity.application as ParallaxApp).preferencesRepository
    private lateinit var saved: UserPreferences

    /** Worst-case eye position (the one of vector V6), default units. */
    @Before
    fun setUp() = runBlocking {
        saved = repository.preferences.first()
        repository.update {
            UserPreferences(language = it.language, eyePosition = EyePosition.WORST_CASE)
        }
        repository.completeOnboarding()
    }

    @After
    fun restorePreferences() = runBlocking {
        repository.update { saved }
    }

    private fun row(index: Int) = "${QuickCalcTags.TARGET_ROW}.$index"

    private fun exists(tag: String) = compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    private fun waitFor(tag: String) = compose.waitUntil(5_000) { exists(tag) }

    private fun waitUntilGone(tag: String) = compose.waitUntil(5_000) { !exists(tag) }

    private fun openEditor() {
        waitFor(QuickCalcTags.TARGET_ADD)
        compose.onNodeWithTag(QuickCalcTags.TARGET_ADD).performScrollTo().performClick()
        waitFor(QuickCalcTags.EDITOR_DISTANCE)
    }

    private fun fillEditor(distance: String, size: String = "", label: String = "") {
        compose.onNodeWithTag(QuickCalcTags.EDITOR_DISTANCE).performTextInput(distance)
        if (size.isNotEmpty()) compose.onNodeWithTag(QuickCalcTags.EDITOR_SIZE).performTextInput(size)
        if (label.isNotEmpty()) compose.onNodeWithTag(QuickCalcTags.EDITOR_LABEL).performTextInput(label)
    }

    private fun addTarget(distance: String, size: String = "", label: String = "") {
        openEditor()
        fillEditor(distance, size, label)
        compose.onNodeWithTag(QuickCalcTags.EDITOR_SAVE).performClick()
        waitUntilGone(QuickCalcTags.EDITOR_DISTANCE)
    }

    @Test
    fun withoutTargetsTheSettingComesFromTheRange() {
        waitFor(QuickCalcTags.TARGET_ADD)
        compose.onNodeWithTag(QuickCalcTags.MIN_FIELD).performTextInput("100")
        compose.onNodeWithTag(QuickCalcTags.MAX_FIELD).performTextInput("200")
        compose.onNodeWithTag(QuickCalcTags.SETTING).assertTextContains("133", substring = true)
    }

    @Test
    fun targetsReplaceTheRangeAndGiveTheWeightedSetting() {
        // Vector V6: 25 m / 1 mrad, 100 m / 1 mrad, 200 m / 0.5 mrad -> 60 m.
        addTarget("200", "0.5", "T3")
        addTarget("25", "1", "T1")
        addTarget("100", "1", "T2")

        compose.onNodeWithTag(QuickCalcTags.SETTING).assertTextContains("60", substring = true)
        // The range comes from the targets (F-51).
        compose.onNodeWithTag(QuickCalcTags.MIN_FIELD).assertDoesNotExist()
        // Rows are sorted by distance, whatever the order they were entered in.
        compose.onNodeWithTag(row(0)).performScrollTo().assertTextContains("T1")
        compose.onNodeWithTag(row(1)).assertTextContains("T2")
        compose.onNodeWithTag(row(2)).assertTextContains("T3")
    }

    private fun string(id: Int) = compose.activity.getString(id)

    private fun plural(id: Int, count: Int) = compose.activity.resources.getQuantityString(id, count, count)

    @Test
    fun eachTargetShowsItsErrorAndStatus() {
        // Vector V6, worst-case eye position: red / green / red, errors 0.58 / 0.17 / 0.29 mrad.
        addTarget("25", "1", "T1")
        addTarget("100", "1", "T2")
        addTarget("200", "0.5", "T3")

        // Status as an icon with its text, not as a color alone (F-54).
        compose.onNodeWithTag("${row(0)}.status", useUnmergedTree = true).performScrollTo()
        compose.onNode(hasTestTag(row(0)) and hasAnyDescendant(hasContentDescription(string(R.string.status_red))), true).assertExists()
        compose.onNode(hasTestTag(row(1)) and hasAnyDescendant(hasContentDescription(string(R.string.status_green))), true).assertExists()
        compose.onNode(hasTestTag(row(2)) and hasAnyDescendant(hasContentDescription(string(R.string.status_red))), true).assertExists()
        compose.onNodeWithTag("${row(0)}.error", useUnmergedTree = true).assertTextContains("58", substring = true)
        compose.onNodeWithTag("${row(1)}.error", useUnmergedTree = true).assertTextContains("17", substring = true)
        compose.onNodeWithTag("${row(2)}.error", useUnmergedTree = true).assertTextContains("29", substring = true)

        compose.onNodeWithTag(QuickCalcTags.STATUS_SUMMARY).performScrollTo()
            .assertTextContains(plural(R.plurals.targets_off_target, 2))

        // A better eye position divides the errors by three: every target is on target (F-31).
        compose.onNodeWithTag("eyePosition.GOOD").performScrollTo().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasText(string(R.string.targets_all_on_target))).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasTestTag(row(0)) and hasAnyDescendant(hasContentDescription(string(R.string.status_green))), true).assertExists()
    }

    @Test
    fun redTargetsComeWithSuggestedSettings() {
        // Vector V8: {25 m} -> 25 m and {100 m, 200 m} -> 150 m.
        addTarget("25", "1", "T1")
        addTarget("100", "1", "T2")
        addTarget("200", "0.5")

        compose.onNodeWithTag(QuickCalcTags.SUGGESTED_TOGGLE).performScrollTo()
            .assertTextContains(plural(R.plurals.suggested_settings, 2))
        // Collapsed until asked for.
        compose.onNodeWithTag("${QuickCalcTags.SUGGESTED_SETTING}.0").assertDoesNotExist()
        compose.onNodeWithTag(QuickCalcTags.SUGGESTED_TOGGLE).performClick()

        // Shortest setting first; a target without label is named by its distance.
        compose.onNodeWithTag("${QuickCalcTags.SUGGESTED_SETTING}.0").performScrollTo()
            .assertTextContains("25", substring = true).assertTextContains("T1", substring = true)
        compose.onNodeWithTag("${QuickCalcTags.SUGGESTED_SETTING}.1")
            .assertTextContains("150", substring = true).assertTextContains("T2", substring = true)
            .assertTextContains("200", substring = true)

        // No red target left: nothing to suggest (F-55).
        compose.onNodeWithTag("eyePosition.GOOD").performScrollTo().performClick()
        compose.waitUntil(5_000) { !exists(QuickCalcTags.SUGGESTED_TOGGLE) }
    }

    @Test
    fun aFixedSettingShowsItsOwnErrors() {
        // Vector V6b: fixed at 80 m, errors 0.69 / 0.06 / 0.19 mrad, red / green / orange.
        addTarget("25", "1", "T1")
        addTarget("100", "1", "T2")
        addTarget("200", "0.5", "T3")
        compose.onNodeWithTag(QuickCalcTags.SETTING).assertTextContains("60", substring = true)
        compose.onNodeWithTag(QuickCalcTags.OPTIMAL_SETTING).assertDoesNotExist()

        compose.onNodeWithTag(QuickCalcTags.COMPARE_TOGGLE).performScrollTo().performClick()
        compose.onNodeWithTag(QuickCalcTags.CURRENT_SETTING_FIELD).performScrollTo().performTextInput("80")

        compose.onNodeWithTag(QuickCalcTags.SETTING).performScrollTo().assertTextContains("80", substring = true)
        // The optimal setting stays visible next to the fixed one.
        compose.onNodeWithTag(QuickCalcTags.OPTIMAL_SETTING).assertTextContains("60", substring = true)
        compose.onNodeWithTag("${row(0)}.error", useUnmergedTree = true).performScrollTo()
            .assertTextContains("69", substring = true)
        compose.onNodeWithTag("${row(2)}.error", useUnmergedTree = true).assertTextContains("19", substring = true)
        compose.onNode(hasTestTag(row(2)) and hasAnyDescendant(hasContentDescription(string(R.string.status_orange))), true).assertExists()

        // Emptying the field goes back to the optimal setting.
        compose.onNodeWithTag(QuickCalcTags.CURRENT_SETTING_FIELD).performScrollTo().performTextReplacement("")
        compose.onNodeWithTag(QuickCalcTags.SETTING).performScrollTo().assertTextContains("60", substring = true)
        compose.onNodeWithTag(QuickCalcTags.OPTIMAL_SETTING).assertDoesNotExist()
    }

    @Test
    fun aTargetWithoutSizeHasAnErrorButNoStatus() {
        addTarget("100")
        addTarget("200")
        compose.onNodeWithTag("${row(0)}.error", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(QuickCalcTags.STATUS_SUMMARY).assertDoesNotExist()
        for (status in listOf(R.string.status_green, R.string.status_orange, R.string.status_red)) {
            compose.onNode(hasContentDescription(string(status)), useUnmergedTree = true).assertDoesNotExist()
        }
    }

    @Test
    fun addAndNextKeepsTheEditorOpen() {
        openEditor()
        fillEditor("100")
        compose.onNodeWithTag(QuickCalcTags.EDITOR_SAVE_AND_NEXT).performClick()
        // Still open, and empty again.
        compose.onNodeWithTag(QuickCalcTags.EDITOR_SAVE).assertIsNotEnabled()
        fillEditor("200")
        compose.onNodeWithTag(QuickCalcTags.EDITOR_SAVE).performClick()
        waitUntilGone(QuickCalcTags.EDITOR_DISTANCE)

        compose.onNodeWithTag(row(1)).performScrollTo().assertIsDisplayed()
        // Two targets without size: the setting of the range 100-200 m (F-52).
        compose.onNodeWithTag(QuickCalcTags.SETTING).assertTextContains("133", substring = true)
    }

    @Test
    fun theDistanceIsRequiredAndValidated() {
        openEditor()
        compose.onNodeWithTag(QuickCalcTags.EDITOR_SAVE).assertIsNotEnabled()
        compose.onNodeWithTag(QuickCalcTags.EDITOR_DISTANCE).performTextInput("3")
        compose.onNodeWithTag(QuickCalcTags.EDITOR_SAVE).assertIsNotEnabled()
        compose.onNodeWithTag(QuickCalcTags.EDITOR_DISTANCE).performTextReplacement("30")
        compose.onNodeWithTag(QuickCalcTags.EDITOR_SAVE).assertIsEnabled()
        // A size of zero is refused (F-50).
        compose.onNodeWithTag(QuickCalcTags.EDITOR_SIZE).performTextInput("0")
        compose.onNodeWithTag(QuickCalcTags.EDITOR_SAVE).assertIsNotEnabled()
    }

    @Test
    fun tappingARowEditsTheTarget() {
        addTarget("100", "1", "T1")
        compose.onNodeWithTag(row(0)).performScrollTo().performClick()
        waitFor(QuickCalcTags.EDITOR_DISTANCE)
        compose.onNodeWithTag(QuickCalcTags.EDITOR_DISTANCE).assertTextContains("100")
        compose.onNodeWithTag(QuickCalcTags.EDITOR_DISTANCE).performTextReplacement("150")
        compose.onNodeWithTag(QuickCalcTags.EDITOR_SAVE).performClick()
        waitUntilGone(QuickCalcTags.EDITOR_DISTANCE)

        compose.onNodeWithTag(row(0)).performScrollTo().assertTextContains("150", substring = true)
        compose.onNodeWithTag(row(1)).assertDoesNotExist()
        compose.onNodeWithTag(QuickCalcTags.SETTING).assertTextContains("150", substring = true)
    }

    @Test
    fun swipingARowDeletesItAndUndoBringsItBack() {
        addTarget("100", "1", "T1")
        addTarget("200", "1", "T2")

        compose.onNodeWithTag(row(0)).performScrollTo().performTouchInput { swipeLeft() }
        waitUntilGone(row(1))
        compose.onNodeWithTag(row(0)).assertTextContains("T2")

        // F-66: the deletion can be undone.
        compose.onNodeWithText(compose.activity.getString(R.string.undo)).performClick()
        waitFor(row(1))
        compose.onNodeWithTag(row(0)).assertTextContains("T1")
        compose.onNodeWithTag(row(1)).assertTextContains("T2")
    }

    @Test
    fun clearAllBringsTheRangeBackAndUndoBringsTheTargetsBack() {
        waitFor(QuickCalcTags.MIN_FIELD)
        compose.onNodeWithTag(QuickCalcTags.MIN_FIELD).performTextInput("100")
        compose.onNodeWithTag(QuickCalcTags.MAX_FIELD).performTextInput("200")
        addTarget("25", "1", "T1")
        addTarget("100", "1", "T2")
        compose.onNodeWithTag(QuickCalcTags.MIN_FIELD).assertDoesNotExist()

        // Back to a plain range in one tap: the typed range is still there.
        compose.onNodeWithTag(QuickCalcTags.TARGETS_CLEAR).performClick()
        waitFor(QuickCalcTags.MIN_FIELD)
        compose.onNodeWithTag(QuickCalcTags.SETTING).assertTextContains("133", substring = true)
        compose.onNodeWithTag(QuickCalcTags.TARGETS_CLEAR).assertDoesNotExist()

        // F-66: clearing can be undone.
        compose.onNodeWithText(compose.activity.getString(R.string.undo)).performClick()
        waitFor(row(1))
        compose.onNodeWithTag(row(0)).assertTextContains("T1")
        compose.onNodeWithTag(row(1)).assertTextContains("T2")
    }
}
