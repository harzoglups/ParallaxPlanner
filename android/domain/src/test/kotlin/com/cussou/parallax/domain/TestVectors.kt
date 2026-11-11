package com.cussou.parallax.domain

import java.io.File
import kotlin.math.abs
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Access to spec/test-vectors.json, the shared reference cases of the domain (NF-05). */
object TestVectors {
    const val DEFAULT_TOLERANCE = 1e-6

    val root: JsonObject by lazy {
        val path = checkNotNull(System.getProperty("testVectors")) { "testVectors system property is not set" }
        Json.parseToJsonElement(File(path).readText()).jsonObject
    }

    /** Cases of one section ("simple", "error", "sizeConversion", "advanced"). */
    fun section(name: String): List<JsonObject> = root.getValue(name).jsonArray.map { it.jsonObject }

    fun case(section: String, id: String): JsonObject = section(section).single { it.string("id") == id }
}

fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content

fun JsonObject.double(key: String): Double = getValue(key).jsonPrimitive.double

fun JsonObject.doubleOrNull(key: String): Double? = get(key)?.jsonPrimitive?.double

fun JsonObject.obj(key: String): JsonObject = getValue(key).jsonObject

fun JsonObject.array(key: String): JsonArray = getValue(key).jsonArray

/** Maps the "eye" field of a case to the domain enum. */
fun eyePosition(name: String): EyePosition = when (name) {
    "excellent" -> EyePosition.EXCELLENT
    "good" -> EyePosition.GOOD
    "worstCase" -> EyePosition.WORST_CASE
    else -> error("Unknown eye position: $name")
}

/** Maps the "unit" field of a size to the domain enum. */
fun sizeUnit(name: String): SizeUnit = when (name) {
    "mrad" -> SizeUnit.MRAD
    "moa" -> SizeUnit.MOA
    "cm" -> SizeUnit.CM
    "in" -> SizeUnit.IN
    else -> error("Unknown size unit: $name")
}

/** Tolerance of a case: its own "tolerance" field, otherwise the default 1e-6. */
fun JsonObject.tolerance(): Double = doubleOrNull("tolerance") ?: TestVectors.DEFAULT_TOLERANCE

/** Relative comparison; falls back to an absolute one when the expected value is zero. */
fun assertClose(expected: Double, actual: Double, tolerance: Double = TestVectors.DEFAULT_TOLERANCE, label: String = "") {
    val allowed = if (expected == 0.0) tolerance else tolerance * abs(expected)
    assertTrue(abs(actual - expected) <= allowed, "$label expected $expected but was $actual (tolerance $tolerance)")
}
