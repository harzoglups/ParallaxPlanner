package com.cussou.parallax

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cussou.parallax.domain.EyePosition
import com.cussou.parallax.ui.components.EyePositionSelector
import com.cussou.parallax.ui.components.EyePositionTags
import com.cussou.parallax.ui.theme.ParallaxPlannerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The eye position control and its help (F-31, docs/03-ux.md §2). */
@RunWith(AndroidJUnit4::class)
class EyePositionSelectorTest {
    @get:Rule
    val compose = createComposeRule()

    private var selected by mutableStateOf(EyePosition.GOOD)

    private fun string(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun show() = compose.setContent {
        ParallaxPlannerTheme { EyePositionSelector(selected, onChange = { selected = it }) }
    }

    @Test
    fun oneTapSelectsALevel() {
        show()
        compose.onNodeWithTag("eyePosition.GOOD").assertIsSelected()
        compose.onNodeWithTag("eyePosition.WORST_CASE").performClick()
        assertEquals(EyePosition.WORST_CASE, selected)
        compose.onNodeWithTag("eyePosition.WORST_CASE").assertIsSelected()
    }

    @Test
    fun theInfoButtonExplainsTheThreeLevels() {
        show()
        compose.onNodeWithTag(EyePositionTags.HELP_DIALOG).assertDoesNotExist()

        compose.onNodeWithTag(EyePositionTags.HELP_BUTTON).performClick()
        compose.onNodeWithTag(EyePositionTags.HELP_DIALOG).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.eye_excellent_help)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.eye_good_help)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.eye_worst_help)).assertIsDisplayed()
        // NF-11: the disclaimer is part of the help too.
        compose.onNodeWithText(string(R.string.disclaimer)).assertIsDisplayed()

        compose.onNodeWithTag(EyePositionTags.HELP_CLOSE).performClick()
        compose.onNodeWithTag(EyePositionTags.HELP_DIALOG).assertDoesNotExist()
        // Reading the help changes nothing.
        assertEquals(EyePosition.GOOD, selected)
    }
}
