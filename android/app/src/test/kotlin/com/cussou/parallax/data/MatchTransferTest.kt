package com.cussou.parallax.data

import com.cussou.parallax.domain.Match
import com.cussou.parallax.domain.SizeUnit
import com.cussou.parallax.domain.Stage
import com.cussou.parallax.domain.Target
import com.cussou.parallax.domain.TargetSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

/** Export and import of a match as a JSON file (F-65). */
class MatchTransferTest {
    private val match = Match(
        id = "m1",
        name = "Rimfire Cup #4",
        date = "2026-12-14",
        notes = "Bring the tripod",
        stages = listOf(
            Stage("s1", "Prone", rangeMinM = 100.0, rangeMaxM = 200.0),
            Stage("s2", "Barricade", targets = listOf(Target(25.0, TargetSize(1.0, SizeUnit.MRAD), "T1"))),
        ),
    )

    private fun ids(): () -> String {
        var next = 0
        return { "new${next++}" }
    }

    @Test
    fun `F-65 an exported match is imported with the same content`() {
        val imported = assertIs<ImportResult.Imported>(MatchTransfer.import(MatchTransfer.export(match), ids())).match
        assertEquals(match.copy(id = imported.id, stages = match.stages.mapIndexed { i, s -> s.copy(id = imported.stages[i].id) }), imported)
    }

    @Test
    fun `F-65 an imported match gets new ids, so it never replaces a match already there`() {
        val imported = assertIs<ImportResult.Imported>(MatchTransfer.import(MatchTransfer.export(match), ids())).match
        assertNotEquals(match.id, imported.id)
        assertEquals(setOf("new0", "new1", "new2"), (imported.stages.map { it.id } + imported.id).toSet())
    }

    @Test
    fun `a file that is not a match is reported as such`() {
        assertEquals(ImportResult.NotAMatch, MatchTransfer.import("not json", ids()))
        assertEquals(ImportResult.NotAMatch, MatchTransfer.import("", ids()))
    }

    @Test
    fun `a file from a newer version of the app is reported as such`() {
        val newer = MatchTransfer.export(match).replace("\"schemaVersion\": 1", "\"schemaVersion\": 7")
        assertEquals(ImportResult.TooNew, MatchTransfer.import(newer, ids()))
    }

    @Test
    fun `the exported file is named after the match, with characters any system accepts`() {
        assertEquals("Rimfire Cup 4.json", MatchTransfer.fileName(match))
        assertEquals("Été à Toulon.json", MatchTransfer.fileName(match.copy(name = "Été à Toulon")))
        assertEquals("a b c.json", MatchTransfer.fileName(match.copy(name = " a/b\\\\c: ")))
        assertEquals("match.json", MatchTransfer.fileName(match.copy(name = "???")))
    }
}
