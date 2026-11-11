package com.cussou.parallax.ui.matches

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.cussou.parallax.data.MatchRepository
import com.cussou.parallax.data.PreferencesRepository
import com.cussou.parallax.domain.EyePosition
import com.cussou.parallax.domain.Match
import com.cussou.parallax.domain.Stage
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class MatchViewModelTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    @BeforeTest
    fun setMain() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    private val match = Match(
        id = "m",
        name = "Cup",
        stages = listOf(
            Stage("s1", "Prone", rangeMinM = 100.0, rangeMaxM = 200.0),
            Stage("s2", "Barricade"),
            Stage("s3", "Tank"),
        ),
    )

    private fun withViewModel(
        block: suspend (MatchViewModel, MatchRepository, PreferencesRepository) -> Unit,
    ) = scope.runTest {
        val job = Job()
        val preferences = PreferencesRepository(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(dispatcher + job),
                produceFile = { File(folder.root, "test.preferences_pb") },
            ),
        )
        val matches = MatchRepository(File(folder.root, "matches"), dispatcher)
        matches.save(match)
        var next = 0
        block(MatchViewModel("m", matches, preferences) { "new${next++}" }, matches, preferences)
        job.cancelAndJoin()
    }

    private suspend fun MatchViewModel.stateWhen(condition: (MatchUiState) -> Boolean) =
        uiState.filterNotNull().first(condition)

    private fun names(state: MatchUiState) = state.match!!.stages.map { it.name }

    @Test
    fun `F-63 each stage comes with its summary, following the preferences`() = withViewModel { viewModel, _, preferences ->
        val state = viewModel.stateWhen { it.match != null }
        assertEquals(3, state.summaries.size)
        // Default eye position (Good): a third of the worst-case error of V1.
        assertEquals(133.3333333333, state.summaries[0]!!.settingM, 1e-9)
        assertEquals(0.0625 / 3, state.summaries[0]!!.maxErrorMrad, 1e-12)
        assertNull(state.summaries[1])

        preferences.update { it.copy(eyePosition = EyePosition.WORST_CASE) }
        val worst = viewModel.stateWhen { it.preferences.eyePosition == EyePosition.WORST_CASE }
        assertEquals(0.0625, worst.summaries[0]!!.maxErrorMrad, 1e-12)
    }

    @Test
    fun `F-61 a stage is added at the end and stored`() = withViewModel { viewModel, matches, _ ->
        viewModel.onStageAdded("  Wall ")
        val state = viewModel.stateWhen { it.match?.stages?.size == 4 }
        assertEquals(listOf("Prone", "Barricade", "Tank", "Wall"), names(state))
        assertEquals("new0", state.match!!.stages.last().id)
        assertEquals(state.match, matches.get("m"))
    }

    @Test
    fun `F-61 a stage is renamed, duplicated right after itself, and moved`() = withViewModel { viewModel, _, _ ->
        viewModel.onStageRenamed("s2", "Wall")
        assertEquals(listOf("Prone", "Wall", "Tank"), names(viewModel.stateWhen { "Wall" in names(it) }))

        viewModel.onStageDuplicated("s1", "Prone (copy)")
        val duplicated = viewModel.stateWhen { it.match?.stages?.size == 4 }
        assertEquals(listOf("Prone", "Prone (copy)", "Wall", "Tank"), names(duplicated))
        val copy = duplicated.match!!.stages[1]
        assertNotEquals("s1", copy.id)
        assertEquals(100.0, copy.rangeMinM)

        viewModel.onStageMoved("s3", -1)
        assertEquals(
            listOf("Prone", "Prone (copy)", "Tank", "Wall"),
            names(viewModel.stateWhen { names(it).indexOf("Tank") == 2 }),
        )
    }

    @Test
    fun `F-61 F-66 a stage is deleted and can be put back where it was`() = withViewModel { viewModel, _, _ ->
        viewModel.onStageDeleted("s1")
        assertEquals(listOf("Barricade", "Tank"), names(viewModel.stateWhen { it.match?.stages?.size == 2 }))
        viewModel.onStageRestored(match.stages[0], 0)
        assertEquals(match, viewModel.stateWhen { it.match?.stages?.size == 3 }.match)
    }

    @Test
    fun `a match deleted elsewhere leaves the screen without a match`() = withViewModel { viewModel, matches, _ ->
        viewModel.stateWhen { it.match != null }
        matches.delete("m")
        assertNull(viewModel.stateWhen { it.match == null }.match)
    }
}
