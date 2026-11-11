package com.cussou.parallax.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class MatchTest {
    private val match = Match(
        id = "m1",
        name = "Rimfire Cup #4",
        date = "2026-12-14",
        notes = "Bring the tripod",
        stages = listOf(
            Stage("s1", "Prone", rangeMinM = 100.0, rangeMaxM = 200.0),
            Stage("s2", "Barricade", forcedSettingM = 80.0, targets = listOf(Target(25.0, label = "T1"))),
        ),
    )

    @Test
    fun `F-60 a duplicated match is a separate match with the same content`() {
        var next = 0
        val copy = match.duplicate("Rimfire Cup #4 (copy)") { "new${next++}" }

        assertEquals("new0", copy.id)
        assertEquals("Rimfire Cup #4 (copy)", copy.name)
        assertEquals(match.date, copy.date)
        assertEquals(match.notes, copy.notes)
        // Stages get ids of their own, so the two matches never share a stage.
        assertEquals(listOf("new1", "new2"), copy.stages.map { it.id })
        assertEquals(match.stages.map { it.copy(id = "") }, copy.stages.map { it.copy(id = "") })
    }

    @Test
    fun `F-61 a duplicated stage is a separate stage with the same content`() {
        val copy = match.stages[1].duplicate("Barricade (copy)") { "new" }
        assertEquals(match.stages[1].copy(id = "new", name = "Barricade (copy)"), copy)
    }

    private fun names(match: Match) = match.stages.map { it.name }

    @Test
    fun `F-61 stages are added at the end, renamed, removed and put back`() {
        val added = match.addStage(Stage("s3", "Tank"))
        assertEquals(listOf("Prone", "Barricade", "Tank"), names(added))
        assertEquals(listOf("Prone", "Wall", "Tank"), names(added.renameStage("s2", "Wall")))

        val removed = added.removeStage("s1")
        assertEquals(listOf("Barricade", "Tank"), names(removed))
        // Undo of a deletion: back at the position it had (F-66).
        assertEquals(added, removed.insertStage(0, added.stages[0]))
        // An index beyond the end puts the stage last rather than failing.
        assertEquals(listOf("Barricade", "Tank", "Prone"), names(removed.insertStage(9, added.stages[0])))
    }

    @Test
    fun `F-61 stages are reordered one step at a time, within the list`() {
        val three = match.addStage(Stage("s3", "Tank"))
        assertEquals(listOf("Barricade", "Prone", "Tank"), names(three.moveStage("s1", 1)))
        assertEquals(listOf("Prone", "Tank", "Barricade"), names(three.moveStage("s3", -1)))
        // Already first or last: nothing moves.
        assertEquals(three, three.moveStage("s1", -1))
        assertEquals(three, three.moveStage("s3", 1))
        assertEquals(three, three.moveStage("unknown", 1))
    }

    @Test
    fun `F-62 the content of a stage is replaced as a whole`() {
        val updated = match.stages[0].copy(rangeMinM = 50.0, notes = "windy")
        assertEquals(listOf(updated, match.stages[1]), match.replaceStage(updated).stages)
    }
}
