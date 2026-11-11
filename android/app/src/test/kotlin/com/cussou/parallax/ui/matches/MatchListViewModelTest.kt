package com.cussou.parallax.ui.matches

import com.cussou.parallax.data.MatchRepository
import com.cussou.parallax.domain.Match
import com.cussou.parallax.domain.Stage
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class MatchListViewModelTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    @BeforeTest
    fun setMain() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    private fun withViewModel(block: suspend (MatchListViewModel, MatchRepository) -> Unit) = scope.runTest {
        val repository = MatchRepository(File(folder.root, "matches"), dispatcher)
        var next = 0
        block(MatchListViewModel(repository) { "id${next++}" }, repository)
    }

    private suspend fun MatchListViewModel.matchesWhen(condition: (List<Match>) -> Boolean) =
        matches.filterNotNull().first(condition)

    @Test
    fun `F-60 a match is created with a name, an optional date and notes`() = withViewModel { viewModel, repository ->
        assertEquals(emptyList(), viewModel.matchesWhen { true })
        viewModel.onMatchCreated("  Rimfire Cup #4 ", "2026-12-14", "Bring the tripod")
        val created = viewModel.matchesWhen { it.size == 1 }.single()
        assertEquals(Match("id0", "Rimfire Cup #4", "2026-12-14", "Bring the tripod"), created)
        assertEquals(listOf(created), repository.matches.first())
    }

    @Test
    fun `F-60 a match is renamed without touching its stages`() = withViewModel { viewModel, repository ->
        val match = Match("m", "Old name", stages = listOf(Stage("s", "Prone")))
        repository.save(match)
        viewModel.onMatchEdited(match, "New name", null, "notes")
        val edited = viewModel.matchesWhen { it.singleOrNull()?.name == "New name" }.single()
        assertEquals(match.copy(name = "New name", notes = "notes"), edited)
    }

    @Test
    fun `F-60 a match is duplicated with its stages, under new ids`() = withViewModel { viewModel, repository ->
        val match = Match("m", "Cup", "2026-12-14", stages = listOf(Stage("s", "Prone", rangeMinM = 100.0, rangeMaxM = 200.0)))
        repository.save(match)
        viewModel.onMatchDuplicated(match, "Cup (copy)")
        val copy = viewModel.matchesWhen { it.size == 2 }.single { it.name == "Cup (copy)" }
        assertNotEquals(match.id, copy.id)
        assertNotEquals(match.stages.single().id, copy.stages.single().id)
        assertEquals(match.stages.single().copy(id = ""), copy.stages.single().copy(id = ""))
    }

    @Test
    fun `F-60 F-66 a match is deleted, and the deletion can be undone`() = withViewModel { viewModel, repository ->
        val match = Match("m", "Cup", stages = listOf(Stage("s", "Prone")))
        repository.save(match)
        viewModel.matchesWhen { it.size == 1 }

        viewModel.onMatchDeleted(match)
        assertTrue(viewModel.matchesWhen { it.isEmpty() }.isEmpty())
        assertEquals(emptyList(), repository.matches.first())

        viewModel.onMatchRestored(match)
        assertEquals(listOf(match), viewModel.matchesWhen { it.size == 1 })
    }
}
