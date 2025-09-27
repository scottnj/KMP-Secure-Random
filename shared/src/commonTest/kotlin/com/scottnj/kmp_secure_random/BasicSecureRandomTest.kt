package com.scottnj.kmp_secure_random

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertEquals

/**
 * Basic cross-platform tests for SecureRandom implementation.
 * Tests core functionality that should work identically across all platforms.
 */
class BasicSecureRandomTest {

    private val secureRandom = createSecureRandom().getOrThrow()

    @Test
    fun testNextBytes() {
        // Test basic byte generation
        val result = secureRandom.nextBytes(32)
        assertTrue(result.isSuccess, "Should generate random bytes")

        val bytes = result.getOrNull()
        assertNotNull(bytes)
        assertEquals(32, bytes!!.size)

        // Verify not all zeros
        assertTrue(bytes.any { it != 0.toByte() }, "Should not generate all zeros")
    }

    @Test
    fun testNextBytesZeroSize() {
        val result = secureRandom.nextBytes(0)
        assertTrue(result.isSuccess, "Should handle zero size")

        val bytes = result.getOrNull()
        assertNotNull(bytes)
        assertEquals(0, bytes!!.size)
    }

    @Test
    fun testNextBytesNegativeSize() {
        val result = secureRandom.nextBytes(-1)
        assertTrue(result.isFailure, "Should fail with negative size")
        assertTrue(result.exceptionOrNull() is InvalidParameterException)
    }

    @Test
    fun testNextInt() {
        val result = secureRandom.nextInt()
        assertTrue(result.isSuccess, "Should generate random int")
        assertNotNull(result.getOrNull())
    }

    @Test
    fun testNextIntWithBound() {
        val bound = 100
        val result = secureRandom.nextInt(bound)
        assertTrue(result.isSuccess, "Should generate bounded int")

        val value = result.getOrNull()
        assertNotNull(value)
        assertTrue(value!! >= 0 && value < bound, "Value should be in range [0, $bound)")
    }

    @Test
    fun testNextIntWithInvalidBounds() {
        // Test zero bound
        val zeroResult = secureRandom.nextInt(0)
        assertTrue(zeroResult.isFailure, "Should fail with zero bound")

        // Test negative bound
        val negativeResult = secureRandom.nextInt(-10)
        assertTrue(negativeResult.isFailure, "Should fail with negative bound")
    }

    @Test
    fun testNextIntWithRange() {
        val min = 10
        val max = 20
        val result = secureRandom.nextInt(min, max)
        assertTrue(result.isSuccess, "Should generate ranged int")

        val value = result.getOrNull()
        assertNotNull(value)
        assertTrue(value!! >= min && value < max, "Value should be in range [$min, $max)")
    }

    @Test
    fun testNextIntWithInvalidRange() {
        // Test min >= max
        val invalidResult = secureRandom.nextInt(20, 10)
        assertTrue(invalidResult.isFailure, "Should fail with invalid range")

        // Test equal values
        val equalResult = secureRandom.nextInt(10, 10)
        assertTrue(equalResult.isFailure, "Should fail with equal range")
    }

    @Test
    fun testNextLong() {
        val result = secureRandom.nextLong()
        assertTrue(result.isSuccess, "Should generate random long")
        assertNotNull(result.getOrNull())
    }

    @Test
    fun testNextLongWithBound() {
        val bound = 1000L
        val result = secureRandom.nextLong(bound)
        assertTrue(result.isSuccess, "Should generate bounded long")

        val value = result.getOrNull()
        assertNotNull(value)
        assertTrue(value!! >= 0L && value < bound, "Value should be in range [0, $bound)")
    }

    @Test
    fun testNextLongWithRange() {
        val min = 100L
        val max = 200L
        val result = secureRandom.nextLong(min, max)
        assertTrue(result.isSuccess, "Should generate ranged long")

        val value = result.getOrNull()
        assertNotNull(value)
        assertTrue(value!! >= min && value < max, "Value should be in range [$min, $max)")
    }

    @Test
    fun testNextBoolean() {
        val result = secureRandom.nextBoolean()
        assertTrue(result.isSuccess, "Should generate random boolean")
        assertNotNull(result.getOrNull())
    }

    @Test
    fun testNextDouble() {
        val result = secureRandom.nextDouble()
        assertTrue(result.isSuccess, "Should generate random double")

        val value = result.getOrNull()
        assertNotNull(value)
        assertTrue(value!! >= 0.0 && value < 1.0, "Double should be in range [0, 1)")
    }

    @Test
    fun testNextFloat() {
        val result = secureRandom.nextFloat()
        assertTrue(result.isSuccess, "Should generate random float")

        val value = result.getOrNull()
        assertNotNull(value)
        assertTrue(value!! >= 0.0f && value < 1.0f, "Float should be in range [0, 1)")
    }

    @Test
    fun testBytesArrayMethod() {
        val array = ByteArray(16)
        val result = secureRandom.nextBytes(array)
        assertTrue(result.isSuccess, "Should fill byte array")

        // Verify array was modified (not all zeros)
        assertTrue(array.any { it != 0.toByte() }, "Array should be filled with random data")
    }
}