package com.cussou.parallax

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
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

/** Targets on the quick calc in advanced mode, on a device (F-50, F-51, F-52, F-66). */
@RunWith(AndroidJUnit4::class)
class TargetListTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val repository get() = (compose.activity.application as ParallaxApp).preferencesRepository
    private lateinit var saved: UserPreferences

    /** Advanced mode, worst-case eye position (the one of vector V6), default units. */
    @Before
    fun setUp() = runBlocking {
        saved = repository.preferences.first()
        repository.update {
            UserPreferences(language = it.language, advancedMode = true, eyePosition = EyePosition.WORST_CASE)
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
    fun withoutTargetsAdvancedModeLooksLikeSimpleMode() {
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
}
