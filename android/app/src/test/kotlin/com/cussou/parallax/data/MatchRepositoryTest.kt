package com.cussou.parallax.data

import com.cussou.parallax.domain.Match
import com.cussou.parallax.domain.Stage
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder

/** Matches stored locally, one JSON file each (NF-04, F-60). */
class MatchRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val directory: File by lazy { File(folder.root, "matches") }

    private fun TestScope.repository() = MatchRepository(directory, StandardTestDispatcher(testScheduler))

    private val cup = Match(id = "cup", name = "Rimfire Cup #4", date = "2026-12-14", stages = listOf(Stage("s1", "Prone")))
    private val club = Match(id = "club", name = "Club practice")

    @Test
    fun `there is no match at first`() = runTest {
        assertEquals(emptyList(), repository().matches.first())
        assertNull(repository().get("cup"))
    }

    @Test
    fun `NF-04 saved matches are still there after a restart`() = runTest {
        repository().apply {
            save(cup)
            save(club)
        }
        val reopened = repository()
        assertEquals(setOf(cup, club), reopened.matches.first().toSet())
        assertEquals(cup, reopened.get("cup"))
    }

    @Test
    fun `F-60 saving a match again replaces it`() = runTest {
        val repository = repository()
        repository.save(cup)
        repository.save(cup.copy(name = "Renamed"))
        assertEquals(listOf("Renamed"), repository.matches.first().map { it.name })
        assertEquals(listOf("Renamed"), repository().matches.first().map { it.name })
    }

    @Test
    fun `F-60 a deleted match is gone for good`() = runTest {
        val repository = repository()
        repository.save(cup)
        repository.save(club)
        repository.delete("cup")
        assertEquals(listOf(club), repository.matches.first())
        assertEquals(listOf(club), repository().matches.first())
        // Deleting what is not there is not an error.
        repository.delete("cup")
    }

    @Test
    fun `matches are listed by date, most recent first, then undated ones by name`() = runTest {
        val repository = repository()
        repository.save(Match(id = "b", name = "Beta"))
        repository.save(Match(id = "old", name = "Old", date = "2026-01-10"))
        repository.save(Match(id = "a", name = "alpha"))
        repository.save(Match(id = "new", name = "New", date = "2026-12-14"))
        assertEquals(listOf("New", "Old", "alpha", "Beta"), repository.matches.first().map { it.name })
    }

    @Test
    fun `an unreadable file is left alone and does not hide the other matches`() = runTest {
        repository().save(cup)
        val broken = File(directory, "broken.json").apply { writeText("not a match") }
        assertEquals(listOf(cup), repository().matches.first())
        assertTrue(broken.exists())
    }

    @Test
    fun `the list follows every change`() = runTest {
        val repository = repository()
        assertEquals(0, repository.matches.first().size)
        repository.save(cup)
        assertEquals(1, repository.matches.first().size)
        repository.delete("cup")
        assertEquals(0, repository.matches.first().size)
    }
}
