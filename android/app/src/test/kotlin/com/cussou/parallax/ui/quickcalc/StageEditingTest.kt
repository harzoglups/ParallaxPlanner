package com.cussou.parallax.ui.quickcalc

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.cussou.parallax.data.MatchRepository
import com.cussou.parallax.data.PreferencesRepository
import com.cussou.parallax.domain.DistanceUnit
import com.cussou.parallax.domain.EyePosition
import com.cussou.parallax.domain.Match
import com.cussou.parallax.domain.SizeUnit
import com.cussou.parallax.domain.Stage
import com.cussou.parallax.domain.Target
import com.cussou.parallax.domain.TargetSize
import java.io.File
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.rules.TemporaryFolder

/** A stage of a match edited on the quick calc screen (F-62): loaded from the match, saved on every change. */
@OptIn(ExperimentalCoroutinesApi::class)
class StageEditingTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    @BeforeTest
    fun setMain() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    private val v6Targets = listOf(
        Target(25.0, TargetSize(1.0, SizeUnit.MRAD), "T1"),
        Target(100.0, TargetSize(1.0, SizeUnit.MRAD), "T2"),
        Target(200.0, TargetSize(0.5, SizeUnit.MRAD), "T3"),
    )
    private val match = Match(
        id = "m",
        name = "Cup",
        stages = listOf(
            Stage("prone", "Prone", notes = "windy", rangeMinM = 100.0, rangeMaxM = 200.0, forcedSettingM = 150.0),
            Stage("barricade", "Barricade", targets = v6Targets),
            Stage("empty", "Empty"),
        ),
    )

    private fun withStage(
        stageId: String,
        locale: Locale = Locale.ENGLISH,
        before: suspend (PreferencesRepository) -> Unit = {},
        block: suspend TestScope.(QuickCalcViewModel, MatchRepository, PreferencesRepository) -> Unit,
    ) = scope.runTest {
        val job = Job()
        val preferences = PreferencesRepository(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(dispatcher + job),
                produceFile = { File(folder.root, "test.preferences_pb") },
            ),
        )
        preferences.update { it.copy(eyePosition = EyePosition.WORST_CASE) }
        before(preferences)
        val matches = MatchRepository(File(folder.root, "matches"), dispatcher)
        matches.save(match)
        val viewModel = QuickCalcViewModel(preferences, { locale }, StageBinding(matches, "m", stageId))
        block(viewModel, matches, preferences)
        job.cancelAndJoin()
    }

    private suspend fun QuickCalcViewModel.stateWhen(condition: (QuickCalcUiState) -> Boolean) =
        uiState.filterNotNull().first(condition)

    private suspend fun MatchRepository.stage(id: String) = get("m")!!.stages.single { it.id == id }

    @Test
    fun `F-62 the stage is loaded into the screen`() = withStage("prone") { viewModel, _, _ ->
        val state = viewModel.stateWhen { true }
        assertEquals("Prone", state.stageName)
        assertEquals("100", state.minText)
        assertEquals("200", state.maxText)
        assertEquals("150", state.currentSettingText)
        assertEquals("windy", state.notes)
        // Vector V4: fixed at 150 m on 100-200 m.
        assertEquals(0.0833333333, state.outcome.comparison!!.maxErrorMrad, 1e-9)
    }

    @Test
    fun `F-52 a stage with targets shows their weighted setting`() = withStage("barricade") { viewModel, _, _ ->
        val state = viewModel.stateWhen { true }
        assertEquals(v6Targets, state.targets)
        assertEquals(60.0, state.outcome.targetsResult!!.settingM, 1e-9)
    }

    @Test
    fun `F-20 the stage is shown in the distance unit of the preferences`() = withStage(
        "prone",
        before = { it.update { prefs -> prefs.copy(distanceUnit = DistanceUnit.YD) } },
    ) { viewModel, _, _ ->
        val state = viewModel.stateWhen { true }
        assertEquals("109.4", state.minText)
        assertEquals("218.7", state.maxText)
    }

    @Test
    fun `F-62 every change is saved in the match`() = withStage("empty") { viewModel, matches, _ ->
        viewModel.stateWhen { true }
        viewModel.onMinChange("50")
        viewModel.onMaxChange("150")
        viewModel.onCurrentSettingChange("80")
        viewModel.onNotesChange("left side")
        advanceUntilIdle()
        val saved = matches.stage("empty")
        assertEquals(50.0, saved.rangeMinM)
        assertEquals(150.0, saved.rangeMaxM)
        assertEquals(80.0, saved.forcedSettingM)
        assertEquals("left side", saved.notes)
        assertEquals("Empty", saved.name)

        viewModel.onTargetAdded(Target(30.0, label = "T1"))
        advanceUntilIdle()
        assertEquals(listOf(Target(30.0, label = "T1")), matches.stage("empty").targets)

        // Emptied or invalid fields are stored as missing values.
        viewModel.onCurrentSettingChange("")
        viewModel.onMinChange("abc")
        advanceUntilIdle()
        assertNull(matches.stage("empty").forcedSettingM)
        assertNull(matches.stage("empty").rangeMinM)
        // The other stages are untouched.
        assertEquals(match.stages[0], matches.stage("prone"))
    }

    @Test
    fun `F-25 changing the distance unit never changes the stored stage`() = withStage("prone") { viewModel, matches, prefs ->
        viewModel.stateWhen { true }
        prefs.update { it.copy(distanceUnit = DistanceUnit.YD) }
        viewModel.stateWhen { it.minText == "109.4" }
        // An edit after the switch must not save the rounded yard values of the other fields.
        viewModel.onNotesChange("still windy")
        advanceUntilIdle()
        assertEquals(match.stages[0].copy(notes = "still windy"), matches.stage("prone"))

        // A field typed in yards is stored in meters.
        viewModel.onMinChange("100")
        advanceUntilIdle()
        assertEquals(91.44, matches.stage("prone").rangeMinM!!, 1e-9)
        assertEquals(200.0, matches.stage("prone").rangeMaxM)
    }

    @Test
    fun `a stage that no longer exists is reported`() = withStage("unknown") { viewModel, _, _ ->
        assertTrue(viewModel.stageMissing.first { it })
    }

    @Test
    fun `the quick calc itself has no stage and saves nothing`() = scope.runTest {
        val job = Job()
        val preferences = PreferencesRepository(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(dispatcher + job),
                produceFile = { File(folder.root, "quick.preferences_pb") },
            ),
        )
        val viewModel = QuickCalcViewModel(preferences)
        val state = viewModel.uiState.filterNotNull().first()
        assertNull(state.stageName)
        assertNull(state.notes)
        job.cancelAndJoin()
    }
}
