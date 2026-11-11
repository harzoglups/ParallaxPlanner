package com.cussou.parallax.data

import com.cussou.parallax.domain.Match
import com.cussou.parallax.domain.SizeUnit
import com.cussou.parallax.domain.Stage
import com.cussou.parallax.domain.Target
import com.cussou.parallax.domain.TargetSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** The match file format of docs/04-architecture.md, shared by storage and export (F-65). */
class MatchJsonTest {
    private val match = Match(
        id = "m1",
        name = "Rimfire Cup #4",
        date = "2026-12-14",
        notes = "Bring the tripod",
        stages = listOf(
            Stage(id = "s1", name = "Prone", rangeMinM = 100.0, rangeMaxM = 200.0),
            Stage(
                id = "s2",
                name = "Barricade",
                notes = "Left side",
                forcedSettingM = 80.0,
                targets = listOf(
                    Target(25.0, TargetSize(1.0, SizeUnit.MRAD), "T1"),
                    Target(100.0, TargetSize(30.0, SizeUnit.CM), "T2"),
                    Target(200.0),
                ),
            ),
        ),
    )

    @Test
    fun `F-65 a match survives encoding and decoding unchanged`() {
        assertEquals(match, MatchJson.decode(MatchJson.encode(match)))
    }

    @Test
    fun `the encoded file follows the documented schema`() {
        val root = Json.parseToJsonElement(MatchJson.encode(match)).jsonObject
        assertEquals(1, root.getValue("schemaVersion").jsonPrimitive.int)
        val json = root.getValue("match").jsonObject
        assertEquals("m1", json.getValue("id").jsonPrimitive.content)
        assertEquals("Rimfire Cup #4", json.getValue("name").jsonPrimitive.content)
        assertEquals("2026-12-14", json.getValue("date").jsonPrimitive.content)

        val stages = json.getValue("stages").jsonArray.map { it.jsonObject }
        // "order" is the 1-based position of the stage in the match (F-61).
        assertEquals(listOf(1, 2), stages.map { it.getValue("order").jsonPrimitive.int })
        assertEquals(100.0, stages[0].getValue("rangeMinM").jsonPrimitive.double)
        assertEquals(JsonNull, stages[0].getValue("forcedSettingM"))
        assertEquals(JsonNull, stages[1].getValue("rangeMinM"))
        assertEquals(80.0, stages[1].getValue("forcedSettingM").jsonPrimitive.double)

        val targets = stages[1].getValue("targets").jsonArray.map { it.jsonObject }
        assertEquals("T1", targets[0].getValue("label").jsonPrimitive.content)
        assertEquals(25.0, targets[0].getValue("distanceM").jsonPrimitive.double)
        // A size keeps the unit it was entered in.
        val size = targets[1].getValue("size").jsonObject
        assertEquals(30.0, size.getValue("value").jsonPrimitive.double)
        assertEquals("cm", size.getValue("unit").jsonPrimitive.content)
        assertEquals(JsonNull, targets[2].getValue("size"))
    }

    @Test
    fun `the example of the architecture doc can be read`() {
        val decoded = MatchJson.decode(
            """
            {
              "schemaVersion": 1,
              "match": {
                "id": "uuid", "name": "Rimfire Cup #4", "date": "2026-12-14", "notes": "",
                "stages": [
                  { "id": "uuid", "name": "Barricade", "order": 2, "notes": "",
                    "rangeMinM": null, "rangeMaxM": null, "forcedSettingM": null,
                    "targets": [
                      { "label": "T1", "distanceM": 25.0, "size": { "value": 1.0, "unit": "mrad" } }
                    ] }
                ]
              }
            }
            """.trimIndent(),
        )
        assertEquals("Rimfire Cup #4", decoded.name)
        assertEquals(Target(25.0, TargetSize(1.0, SizeUnit.MRAD), "T1"), decoded.stages.single().targets.single())
    }

    @Test
    fun `F-61 stages are read in the order given by their order field`() {
        val text = MatchJson.encode(match)
            .replace("\"order\": 1", "\"order\": 9")
        assertEquals(listOf("Barricade", "Prone"), MatchJson.decode(text).stages.map { it.name })
    }

    @Test
    fun `optional fields can be missing, unknown fields are ignored`() {
        val decoded = MatchJson.decode(
            """
            { "schemaVersion": 1, "futureField": true,
              "match": { "id": "m", "name": "Club", "stages": [
                { "id": "s", "name": "One", "order": 1, "targets": [ { "distanceM": 50, "id": "legacy" } ] }
              ] } }
            """.trimIndent(),
        )
        assertNull(decoded.date)
        assertEquals("", decoded.notes)
        assertEquals(Target(50.0), decoded.stages.single().targets.single())
    }

    @Test
    fun `a file that is not a match is rejected`() {
        for (text in listOf(
            "",
            "not json",
            "[]",
            """{ "schemaVersion": 1 }""",
            """{ "schemaVersion": 1, "match": { "name": "no id", "stages": [] } }""",
            """{ "schemaVersion": 1, "match": { "id": "m", "name": "n", "stages": [
                 { "id": "s", "name": "bad unit", "order": 1, "targets": [
                   { "distanceM": 50, "size": { "value": 1, "unit": "furlong" } } ] } ] } }""",
            """{ "schemaVersion": 1, "match": { "id": "m", "name": "n", "stages": [
                 { "id": "s", "name": "bad distance", "order": 1, "targets": [ { "distanceM": -5 } ] } ] } }""",
            """{ "schemaVersion": 1, "match": { "id": "m", "name": "n", "date": "14/12/2026", "stages": [] } }""",
        )) {
            assertFailsWith<MatchFormatException>(text.take(60)) { MatchJson.decode(text) }
        }
    }

    @Test
    fun `a file written by a newer version of the format is rejected with its version`() {
        val error = assertFailsWith<MatchFormatException> {
            MatchJson.decode("""{ "schemaVersion": 2, "match": { "id": "m", "name": "n", "stages": [] } }""")
        }
        assertTrue("2" in error.message.orEmpty())
    }
}
