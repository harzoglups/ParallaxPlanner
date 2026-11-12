package com.cussou.parallax.ui.quickcalc

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.cussou.parallax.data.MatchRepository
import com.cussou.parallax.data.PreferencesRepository
import com.cussou.parallax.domain.DistanceUnit
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

/** "Save as stage": the quick calc becomes a stage of an existing or a new match (F-64). */
@OptIn(ExperimentalCoroutinesApi::class)
class SaveAsStageTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    @BeforeTest
    fun setMain() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    private val cup = Match("cup", "Rimfire Cup", stages = listOf(Stage("s1", "Prone")))

    private fun withQuickCalc(
        block: suspend TestScope.(QuickCalcViewModel, MatchRepository, PreferencesRepository) -> Unit,
    ) = scope.runTest {
        val job = Job()
        val preferences = PreferencesRepository(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(dispatcher + job),
                produceFile = { File(folder.root, "test.preferences_pb") },
            ),
        )
        preferences.update { it.copy(advancedMode = true) }
        val matches = MatchRepository(File(folder.root, "matches"), dispatcher)
        matches.save(cup)
        var next = 0
        val viewModel = QuickCalcViewModel(
            preferences,
            locale = { Locale.ENGLISH },
            matchRepository = matches,
            newId = { "new${next++}" },
        )
        viewModel.uiState.filterNotNull().first()
        block(viewModel, matches, preferences)
        job.cancelAndJoin()
    }

    @Test
    fun `F-64 the existing matches are offered`() = withQuickCalc { viewModel, _, _ ->
        assertEquals(listOf(cup), viewModel.matches.first { it.isNotEmpty() })
    }

    @Test
    fun `F-64 F-62 the range and the fixed setting go into a stage added to an existing match`() =
        withQuickCalc { viewModel, matches, _ ->
            viewModel.onMinChange("100")
            viewModel.onMaxChange("200")
            viewModel.onCurrentSettingChange("150")
            viewModel.onSaveAsStage("  Barricade ", SaveTarget.Existing("cup"))
            advanceUntilIdle()
            val stages = matches.get("cup")!!.stages
            assertEquals(listOf("Prone", "Barricade"), stages.map { it.name })
            assertEquals(Stage("new0", "Barricade", rangeMinM = 100.0, rangeMaxM = 200.0, forcedSettingM = 150.0), stages[1])
        }

    @Test
    fun `F-64 targets go into a stage of a new match`() = withQuickCalc { viewModel, matches, _ ->
        val targets = listOf(Target(25.0, TargetSize(1.0, SizeUnit.MRAD), "T1"), Target(100.0))
        targets.forEach(viewModel::onTargetAdded)
        viewModel.onSaveAsStage("Tank", SaveTarget.New(" Club practice "))
        advanceUntilIdle()
        val created = matches.matches.first().single { it.id != "cup" }
        assertEquals("Club practice", created.name)
        assertEquals(Stage(created.stages.single().id, "Tank", targets = targets), created.stages.single())
        // The match and its stage get ids of their own.
        assertEquals(setOf("new0", "new1"), setOf(created.id, created.stages.single().id))
    }

    @Test
    fun `F-25 values shown rounded after a unit change are saved exact`() = withQuickCalc { viewModel, matches, prefs ->
        viewModel.onMinChange("100")
        viewModel.onMaxChange("200")
        prefs.update { it.copy(distanceUnit = DistanceUnit.YD) }
        viewModel.uiState.filterNotNull().first { it.minText == "109.4" }
        viewModel.onSaveAsStage("Prone 2", SaveTarget.Existing("cup"))
        advanceUntilIdle()
        val saved = matches.get("cup")!!.stages.last()
        assertEquals(100.0, saved.rangeMinM)
        assertEquals(200.0, saved.rangeMaxM)
    }

    @Test
    fun `F-64 the quick calc keeps its values after saving`() = withQuickCalc { viewModel, _, _ ->
        viewModel.onMinChange("100")
        viewModel.onSaveAsStage("Prone", SaveTarget.Existing("cup"))
        advanceUntilIdle()
        assertEquals("100", viewModel.uiState.filterNotNull().first().minText)
    }

    @Test
    fun `nothing is saved without a stage name, a match name or an existing match`() = withQuickCalc { viewModel, matches, _ ->
        viewModel.onMinChange("100")
        viewModel.onSaveAsStage("  ", SaveTarget.Existing("cup"))
        viewModel.onSaveAsStage("Prone", SaveTarget.New(" "))
        viewModel.onSaveAsStage("Prone", SaveTarget.Existing("gone"))
        advanceUntilIdle()
        assertEquals(listOf(cup), matches.matches.first())
    }
}
