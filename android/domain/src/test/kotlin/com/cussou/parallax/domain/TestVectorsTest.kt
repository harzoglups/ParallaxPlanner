package com.cussou.parallax.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Checks that the shared vectors file is found and readable before any domain code relies on it (NF-05). */
class TestVectorsTest {
    @Test
    fun `NF-05 vectors file exposes every section`() {
        assertEquals(listOf("V1", "V2", "V1b-equal"), TestVectors.section("range").map { it.string("id") })
        assertEquals(5, TestVectors.section("error").size)
        assertEquals(3, TestVectors.section("sizeConversion").size)
        assertEquals(2, TestVectors.section("targets").size)
    }

    @Test
    fun `NF-05 vectors file exposes the constants`() {
        val constants = TestVectors.root.obj("constants")
        assertEquals(0.9144, constants.double("yard_m"))
        assertEquals(0.290888, constants.double("moa_mrad"))
        assertEquals(1.0, constants.obj("eyeFraction").double("worstCase"))
    }

    @Test
    fun `NF-05 case tolerance defaults to 1e-6`() {
        assertEquals(1e-6, TestVectors.case("range", "V1").tolerance())
        assertEquals(1e-4, TestVectors.case("error", "V3-tappy").tolerance())
    }

    @Test
    fun `assertClose compares relatively`() {
        assertClose(1000.0, 1000.0005)
        assertClose(0.0, 0.0)
        assertFailsWith<AssertionError> { assertClose(1000.0, 1000.01) }
        assertFailsWith<AssertionError> { assertClose(0.0, 0.001) }
    }
}
