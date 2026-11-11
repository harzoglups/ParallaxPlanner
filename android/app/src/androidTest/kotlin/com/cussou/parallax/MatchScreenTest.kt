package com.cussou.parallax

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cussou.parallax.domain.EyePosition
import com.cussou.parallax.domain.Match
import com.cussou.parallax.domain.SizeUnit
import com.cussou.parallax.domain.Stage
import com.cussou.parallax.domain.Target
import com.cussou.parallax.domain.TargetSize
import com.cussou.parallax.domain.UserPreferences
import com.cussou.parallax.ui.NAVIGATION_TAG
import com.cussou.parallax.ui.Screen
import com.cussou.parallax.ui.matches.MatchListTags
import com.cussou.parallax.ui.matches.MatchTags
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** One match and its stages, on a device (F-61, F-63, F-66). */
@RunWith(AndroidJUnit4::class)
class MatchScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val app get() = compose.activity.application as ParallaxApp
    private lateinit var savedPreferences: UserPreferences
    private lateinit var savedMatches: List<Match>

    private val cup = Match(
        id = "cup",
        name = "Rimfire Cup",
        stages = listOf(
            // Vector V1.
            Stage("prone", "Prone", rangeMinM = 100.0, rangeMaxM = 200.0),
            // Vector V6.
            Stage(
                "barricade",
                "Barricade",
                targets = listOf(
                    Target(25.0, TargetSize(1.0, SizeUnit.MRAD), "T1"),
                    Target(100.0, TargetSize(1.0, SizeUnit.MRAD), "T2"),
                    Target(200.0, TargetSize(0.5, SizeUnit.MRAD), "T3"),
                ),
            ),
            Stage("tank", "Tank"),
        ),
    )

    /** Advanced mode, worst-case eye position, and one known match; the device gets its own data back afterwards. */
    @Before
    fun setUp() = runBlocking {
        savedPreferences = app.preferencesRepository.preferences.first()
        savedMatches = app.matchRepository.matches.first()
        savedMatches.forEach { app.matchRepository.delete(it.id) }
        app.matchRepository.save(cup)
        app.preferencesRepository.update {
            UserPreferences(language = it.language, advancedMode = true, eyePosition = EyePosition.WORST_CASE)
        }
        app.preferencesRepository.completeOnboarding()
    }

    @After
    fun restore() = runBlocking {
        app.matchRepository.matches.first().forEach { app.matchRepository.delete(it.id) }
        savedMatches.forEach { app.matchRepository.save(it) }
        app.preferencesRepository.update { savedPreferences }
    }

    private fun stage(index: Int) = "${MatchTags.STAGE}.$index"

    private fun exists(tag: String) = compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    private fun waitFor(tag: String) = compose.waitUntil(5_000) { exists(tag) }

    private fun waitUntilGone(tag: String) = compose.waitUntil(5_000) { !exists(tag) }

    private fun string(id: Int) = compose.activity.getString(id)

    private fun stageNames() = runBlocking { app.matchRepository.get("cup")!!.stages.map { it.name } }

    private fun openMatch() {
        val matches = "$NAVIGATION_TAG.${Screen.MATCHES}"
        waitFor(matches)
        compose.onNodeWithTag(matches).performClick()
        waitFor("${MatchListTags.ROW}.0")
        compose.onNodeWithTag("${MatchListTags.ROW}.0").performClick()
        waitFor(MatchTags.TITLE)
    }

    private fun chooseInMenu(index: Int, item: String) {
        compose.onNodeWithTag("${stage(index)}.menu").performScrollTo().performClick()
        waitFor(item)
        compose.onNodeWithTag(item).performClick()
    }

    @Test
    fun eachStageShowsItsSettingAndStatus() {
        openMatch()
        compose.onNodeWithTag(MatchTags.TITLE).assertTextEquals("Rimfire Cup")

        // V1: 133 m. No target size, so no status.
        compose.onNodeWithTag("${stage(0)}.name").assertTextEquals("Prone")
        compose.onNodeWithTag("${stage(0)}.setting").assertTextContains("133", substring = true)
        compose.onNodeWithTag("${stage(0)}.status").assertDoesNotExist()

        // V6: 60 m, two targets off target; the status is an icon with its text (F-54).
        compose.onNodeWithTag("${stage(1)}.setting").assertTextContains("60", substring = true)
        compose.onNode(
            hasTestTag(stage(1)) and hasAnyDescendant(hasContentDescription(string(R.string.status_red))),
            useUnmergedTree = true,
        ).assertExists()

        // An empty stage has nothing to show yet.
        compose.onNodeWithTag("${stage(2)}.setting").assertDoesNotExist()
    }

    @Test
    fun aStageIsAddedWithAProposedName() {
        openMatch()
        compose.onNodeWithTag(MatchTags.ADD_STAGE).performScrollTo().performClick()
        waitFor(MatchTags.NAME_FIELD)
        // "Stage 4" is proposed: saving at once is enough.
        compose.onNodeWithTag(MatchTags.NAME_SAVE).performClick()
        waitFor(stage(3))
        assert(stageNames().last() == string(R.string.default_stage_name).format(4)) { stageNames() }
    }

    @Test
    fun aStageIsRenamed() {
        openMatch()
        chooseInMenu(2, MatchTags.MENU_RENAME)
        waitFor(MatchTags.NAME_FIELD)
        compose.onNodeWithTag(MatchTags.NAME_FIELD).performTextReplacement("Tank trap")
        compose.onNodeWithTag(MatchTags.NAME_SAVE).performClick()
        waitUntilGone(MatchTags.NAME_FIELD)
        compose.onNodeWithTag("${stage(2)}.name").assertTextEquals("Tank trap")
    }

    @Test
    fun stagesAreReorderedAndDuplicated() {
        openMatch()
        chooseInMenu(0, MatchTags.MENU_DOWN)
        compose.waitUntil(5_000) { stageNames() == listOf("Barricade", "Prone", "Tank") }
        compose.onNodeWithTag("${stage(0)}.name").assertTextEquals("Barricade")

        // The first stage cannot go further up, the last one further down.
        compose.onNodeWithTag("${stage(0)}.menu").performClick()
        waitFor(MatchTags.MENU_DOWN)
        compose.onNodeWithTag(MatchTags.MENU_UP).assertDoesNotExist()
        compose.onNodeWithTag(MatchTags.MENU_DUPLICATE).performClick()

        val copy = string(R.string.copy_name).format("Barricade")
        compose.waitUntil(5_000) { stageNames() == listOf("Barricade", copy, "Prone", "Tank") }
        // The copy keeps the targets, hence the same setting.
        compose.onNodeWithTag("${stage(1)}.setting").assertTextContains("60", substring = true)
    }

    @Test
    fun aStageIsDeletedAndUndoBringsItBackInPlace() {
        openMatch()
        chooseInMenu(0, MatchTags.MENU_DELETE)
        compose.waitUntil(5_000) { stageNames() == listOf("Barricade", "Tank") }

        compose.onNodeWithText(string(R.string.undo)).performClick()
        compose.waitUntil(5_000) { stageNames() == listOf("Prone", "Barricade", "Tank") }
        compose.onNodeWithTag("${stage(0)}.name").assertTextEquals("Prone")
    }

    @Test
    fun backReturnsToTheMatchList() {
        openMatch()
        compose.onNodeWithTag(MatchTags.BACK).performClick()
        waitFor(MatchListTags.ADD)
        compose.onNodeWithTag("${MatchListTags.ROW}.0.name", useUnmergedTree = true).assertIsDisplayed()

        // The system back button does the same.
        compose.onNodeWithTag("${MatchListTags.ROW}.0").performClick()
        waitFor(MatchTags.TITLE)
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitFor(MatchListTags.ADD)
    }
}
