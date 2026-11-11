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
}
