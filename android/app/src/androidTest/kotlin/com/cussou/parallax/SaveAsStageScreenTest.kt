package com.cussou.parallax

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cussou.parallax.domain.Match
import com.cussou.parallax.domain.Stage
import com.cussou.parallax.domain.UserPreferences
import com.cussou.parallax.ui.quickcalc.QuickCalcTags
import com.cussou.parallax.ui.quickcalc.SaveAsStageTags
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** "Save as stage" on the quick calc, on a device (F-64). */
@RunWith(AndroidJUnit4::class)
class SaveAsStageScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val app get() = compose.activity.application as ParallaxApp
    private lateinit var savedPreferences: UserPreferences
    private lateinit var savedMatches: List<Match>

    private val cup = Match("cup", "Rimfire Cup", stages = listOf(Stage("s1", "Prone")))

    /** One known match; the device gets its own data back afterwards. */
    @Before
    fun setUp() = runBlocking {
        savedPreferences = app.preferencesRepository.preferences.first()
        savedMatches = app.matchRepository.matches.first()
        savedMatches.forEach { app.matchRepository.delete(it.id) }
        app.matchRepository.save(cup)
        app.preferencesRepository.update { UserPreferences(language = it.language) }
        app.preferencesRepository.completeOnboarding()
    }

    @After
    fun restore() = runBlocking {
        app.matchRepository.matches.first().forEach { app.matchRepository.delete(it.id) }
        savedMatches.forEach { app.matchRepository.save(it) }
        app.preferencesRepository.update { savedPreferences }
    }

    private fun exists(tag: String) = compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    private fun waitFor(tag: String) = compose.waitUntil(5_000) { exists(tag) }

    private fun typeRangeAndOpenDialog() {
        waitFor(QuickCalcTags.MIN_FIELD)
        // Nothing to save while the quick calc is empty.
        compose.onNodeWithTag(QuickCalcTags.SAVE_AS_STAGE).performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag(QuickCalcTags.MIN_FIELD).performScrollTo().performTextInput("100")
        compose.onNodeWithTag(QuickCalcTags.MAX_FIELD).performTextInput("200")
        compose.onNodeWithTag(QuickCalcTags.SAVE_AS_STAGE).performScrollTo().assertIsEnabled().performClick()
        waitFor(SaveAsStageTags.STAGE_NAME)
    }

    @Test
    fun theQuickCalcIsAddedToAnExistingMatch() {
        typeRangeAndOpenDialog()
        // The first match is selected and the name proposed follows its stages: "Stage 2".
        compose.onNodeWithTag("${SaveAsStageTags.MATCH}.cup").assertIsSelected()
        compose.onNodeWithTag(SaveAsStageTags.STAGE_NAME)
            .assertTextContains(compose.activity.getString(R.string.default_stage_name, 2))
        compose.onNodeWithTag(SaveAsStageTags.STAGE_NAME).performTextReplacement("Barricade")
        compose.onNodeWithTag(SaveAsStageTags.SAVE).performClick()

        compose.waitUntil(5_000) { runBlocking { app.matchRepository.get("cup")!!.stages.size == 2 } }
        val stage = runBlocking { app.matchRepository.get("cup")!!.stages[1] }
        assert(stage.name == "Barricade" && stage.rangeMinM == 100.0 && stage.rangeMaxM == 200.0) { stage }
        // The quick calc keeps its values.
        compose.onNodeWithTag(QuickCalcTags.MIN_FIELD).performScrollTo().assertTextContains("100")
    }

    @Test
    fun theQuickCalcStartsANewMatch() {
        typeRangeAndOpenDialog()
        compose.onNodeWithTag(SaveAsStageTags.NEW_MATCH).performClick()
        // A new match needs a name.
        compose.onNodeWithTag(SaveAsStageTags.SAVE).assertIsNotEnabled()
        compose.onNodeWithTag(SaveAsStageTags.NEW_MATCH_NAME).performTextInput("Club practice")
        compose.onNodeWithTag(SaveAsStageTags.SAVE).assertIsEnabled().performClick()

        compose.waitUntil(5_000) { runBlocking { app.matchRepository.matches.first().size == 2 } }
        val created = runBlocking { app.matchRepository.matches.first().single { it.id != "cup" } }
        assert(created.name == "Club practice" && created.stages.single().rangeMinM == 100.0) { created }
    }
}
