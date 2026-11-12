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
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cussou.parallax.domain.Match
import com.cussou.parallax.domain.Stage
import com.cussou.parallax.domain.UserPreferences
import com.cussou.parallax.ui.NAVIGATION_TAG
import com.cussou.parallax.ui.Screen
import com.cussou.parallax.ui.matches.MatchListTags
import com.cussou.parallax.ui.quickcalc.QuickCalcTags
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The bottom bar of advanced mode and the list of matches, on a device (F-41, F-60, F-66). */
@RunWith(AndroidJUnit4::class)
class MatchListTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val app get() = compose.activity.application as ParallaxApp
    private lateinit var savedPreferences: UserPreferences
    private lateinit var savedMatches: List<Match>

    /** Advanced mode and no match; what was on the device comes back afterwards. */
    @Before
    fun setUp() = runBlocking {
        savedPreferences = app.preferencesRepository.preferences.first()
        savedMatches = app.matchRepository.matches.first()
        savedMatches.forEach { app.matchRepository.delete(it.id) }
        app.preferencesRepository.update { UserPreferences(language = it.language) }
        app.preferencesRepository.completeOnboarding()
    }

    @After
    fun restore() = runBlocking {
        app.matchRepository.matches.first().forEach { app.matchRepository.delete(it.id) }
        savedMatches.forEach { app.matchRepository.save(it) }
        app.preferencesRepository.update { savedPreferences }
    }

    private fun nav(screen: Screen) = "$NAVIGATION_TAG.$screen"

    private fun row(index: Int) = "${MatchListTags.ROW}.$index"

    private fun exists(tag: String) = compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    private fun waitFor(tag: String) = compose.waitUntil(5_000) { exists(tag) }

    private fun waitUntilGone(tag: String) = compose.waitUntil(5_000) { !exists(tag) }

    private fun string(id: Int) = compose.activity.getString(id)

    private fun openMatches() {
        waitFor(nav(Screen.MATCHES))
        compose.onNodeWithTag(nav(Screen.MATCHES)).performClick()
        waitFor(MatchListTags.ADD)
    }

    private fun createMatch(name: String) {
        compose.onNodeWithTag(MatchListTags.ADD).performClick()
        waitFor(MatchListTags.EDITOR_NAME)
        compose.onNodeWithTag(MatchListTags.EDITOR_NAME).performTextInput(name)
        compose.onNodeWithTag(MatchListTags.EDITOR_SAVE).performClick()
        waitUntilGone(MatchListTags.EDITOR_NAME)
    }

    private fun chooseInMenu(index: Int, item: String) {
        compose.onNodeWithTag("${row(index)}.menu").performClick()
        waitFor(item)
        compose.onNodeWithTag(item).performClick()
    }

    @Test
    fun theBottomBarSwitchesBetweenTheThreeScreens() {
        waitFor(nav(Screen.CALC))
        compose.onNodeWithTag(nav(Screen.SETTINGS)).performClick()
        waitFor("settings.title")
        compose.onNodeWithTag("settings.back").assertDoesNotExist()

        compose.onNodeWithTag(nav(Screen.MATCHES)).performClick()
        waitFor(MatchListTags.EMPTY)

        compose.onNodeWithTag(nav(Screen.CALC)).performClick()
        waitFor(QuickCalcTags.EMPTY)
    }

    @Test
    fun aMatchIsCreatedWithAName() {
        openMatches()
        compose.onNodeWithTag(MatchListTags.EMPTY).assertIsDisplayed()

        compose.onNodeWithTag(MatchListTags.ADD).performClick()
        waitFor(MatchListTags.EDITOR_NAME)
        // The name is required (F-60).
        compose.onNodeWithTag(MatchListTags.EDITOR_SAVE).assertIsNotEnabled()
        compose.onNodeWithTag(MatchListTags.EDITOR_NAME).performTextInput("Rimfire Cup")
        compose.onNodeWithTag(MatchListTags.EDITOR_NOTES).performTextInput("Bring the tripod")
        compose.onNodeWithTag(MatchListTags.EDITOR_SAVE).assertIsEnabled().performClick()

        waitFor(row(0))
        compose.onNodeWithTag("${row(0)}.name", useUnmergedTree = true).assertTextContains("Rimfire Cup")
        val stored = runBlocking { app.matchRepository.matches.first().single() }
        assert(stored.name == "Rimfire Cup" && stored.notes == "Bring the tripod" && stored.date == null)
    }

    @Test
    fun aMatchIsRenamed() {
        runBlocking { app.matchRepository.save(Match("m", "Old name", stages = listOf(Stage("s", "Prone")))) }
        openMatches()
        waitFor(row(0))
        chooseInMenu(0, MatchListTags.MENU_EDIT)
        waitFor(MatchListTags.EDITOR_NAME)
        compose.onNodeWithTag(MatchListTags.EDITOR_NAME).performTextReplacement("New name")
        compose.onNodeWithTag(MatchListTags.EDITOR_SAVE).performClick()
        waitUntilGone(MatchListTags.EDITOR_NAME)

        compose.onNodeWithTag("${row(0)}.name", useUnmergedTree = true).assertTextContains("New name")
        // Its stages are untouched.
        assert(runBlocking { app.matchRepository.get("m") }?.stages?.single()?.name == "Prone")
    }

    @Test
    fun aMatchIsDuplicated() {
        openMatches()
        createMatch("Rimfire Cup")
        waitFor(row(0))
        chooseInMenu(0, MatchListTags.MENU_DUPLICATE)
        waitFor(row(1))
        val names = runBlocking { app.matchRepository.matches.first().map { it.name } }
        assert(names.toSet() == setOf("Rimfire Cup", string(R.string.copy_name).format("Rimfire Cup"))) { names }
    }

    @Test
    fun aMatchIsDeletedAndUndoBringsItBack() {
        openMatches()
        createMatch("Rimfire Cup")
        waitFor(row(0))
        chooseInMenu(0, MatchListTags.MENU_DELETE)
        waitFor(MatchListTags.EMPTY)

        // F-66: the deletion can be undone.
        compose.onNodeWithText(string(R.string.undo)).performClick()
        waitFor(row(0))
        compose.onNodeWithTag("${row(0)}.name", useUnmergedTree = true).assertTextContains("Rimfire Cup")
    }
}
