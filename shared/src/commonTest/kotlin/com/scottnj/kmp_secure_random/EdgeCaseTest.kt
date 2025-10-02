package com.scottnj.kmp_secure_random

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertEquals

/**
 * Edge case tests for SecureRandom that work across all platforms.
 */
class EdgeCaseTest {

    private val secureRandom = createSecureRandom().getOrThrow()

    @Test
    fun testBoundaryValues() {
        // Test nextInt with max possible bound
        val maxIntResult = secureRandom.nextInt(Int.MAX_VALUE)
        assertTrue(maxIntResult.isSuccess)
        val maxIntValue = maxIntResult.getOrNull()!!
        assertTrue(maxIntValue >= 0 && maxIntValue < Int.MAX_VALUE)

        // Test nextInt with bound 1 (should always return 0)
        val oneResult = secureRandom.nextInt(1)
        assertTrue(oneResult.isSuccess)
        assertEquals(0, oneResult.getOrNull())

        // Test nextLong with bound 1
        val oneLongResult = secureRandom.nextLong(1L)
        assertTrue(oneLongResult.isSuccess)
        assertEquals(0L, oneLongResult.getOrNull())
    }

    @Test
    fun testLargeByteArrays() {
        // Test moderately large byte array
        val largeSize = 64 * 1024 // 64KB
        val result = secureRandom.nextBytes(largeSize)
        assertTrue(result.isSuccess, "Should handle 64KB allocation")

        val bytes = result.getOrNull()!!
        assertEquals(largeSize, bytes.size)

        // Verify it's not all zeros
        assertTrue(bytes.any { it != 0.toByte() }, "Large array should contain non-zero bytes")

        // Check randomness across the array (sample different parts)
        val samples = listOf(
            bytes.sliceArray(0..999),
            bytes.sliceArray(largeSize/2 until largeSize/2 + 1000),
            bytes.sliceArray(largeSize - 1000 until largeSize)
        )

        // Each sample should be different
        for (i in samples.indices) {
            for (j in i + 1 until samples.size) {
                assertTrue(
                    !samples[i].contentEquals(samples[j]),
                    "Different parts of large array should not be identical"
                )
            }
        }
    }

    @Test
    fun testRapidGeneration() {
        val iterations = 1000
        val values = mutableSetOf<String>()

        // Generate many values quickly
        for (i in 0 until iterations) {
            val result = secureRandom.nextBytes(8)
            assertTrue(result.isSuccess, "Rapid generation iteration $i failed")
            values.add(result.getOrNull()!!.contentToString())
        }

        // Should maintain high uniqueness even with rapid generation
        val uniquenessRatio = values.size.toDouble() / iterations
        assertTrue(uniquenessRatio > 0.98, "Rapid generation uniqueness too low: $uniquenessRatio")
    }

    @Test
    fun testMultipleInstances() {
        // Create multiple instances
        val instances = List(10) { createSecureRandom().getOrThrow() }
        val allValues = mutableSetOf<String>()

        // Generate values from each instance
        instances.forEach { instance ->
            val result = instance.nextBytes(16)
            assertTrue(result.isSuccess)
            allValues.add(result.getOrNull()!!.contentToString())
        }

        // All instances should produce different values
        assertEquals(10, allValues.size, "All instances should produce unique values")
    }

    @Test
    fun testParameterValidation() {
        // Test various invalid parameters systematically

        // nextBytes with negative size
        listOf(-1, -10, -100, Int.MIN_VALUE).forEach { size ->
            val result = secureRandom.nextBytes(size)
            assertTrue(result.isFailure, "Should fail with negative size: $size")
            assertTrue(result.exceptionOrNull() is InvalidParameterException)
        }

        // nextInt with invalid bounds
        listOf(-1, -10, 0, Int.MIN_VALUE).forEach { bound ->
            val result = secureRandom.nextInt(bound)
            assertTrue(result.isFailure, "Should fail with invalid bound: $bound")
        }

        // nextInt with invalid ranges
        val invalidRanges = listOf(
            10 to 5,    // min > max
            0 to 0,     // min == max
            -5 to -10,  // negative range
            Int.MAX_VALUE to Int.MIN_VALUE
        )

        invalidRanges.forEach { (min, max) ->
            val result = secureRandom.nextInt(min, max)
            assertTrue(result.isFailure, "Should fail with invalid range: [$min, $max)")
        }

        // nextLong with invalid bounds
        listOf(-1L, -10L, 0L, Long.MIN_VALUE).forEach { bound ->
            val result = secureRandom.nextLong(bound)
            assertTrue(result.isFailure, "Should fail with invalid long bound: $bound")
        }
    }

    @Test
    fun testExtremeBounds() {
        // Test with very large bounds to ensure no overflow issues

        // Large positive int bound
        val largeIntResult = secureRandom.nextInt(1_000_000)
        assertTrue(largeIntResult.isSuccess)
        val largeInt = largeIntResult.getOrNull()!!
        assertTrue(largeInt >= 0 && largeInt < 1_000_000)

        // Large positive long bound
        val largeLongResult = secureRandom.nextLong(1_000_000_000L)
        assertTrue(largeLongResult.isSuccess)
        val largeLong = largeLongResult.getOrNull()!!
        assertTrue(largeLong >= 0L && largeLong < 1_000_000_000L)

        // Test ranges with large values
        val largeRangeResult = secureRandom.nextInt(1_000_000, 2_000_000)
        assertTrue(largeRangeResult.isSuccess)
        val largeRangeValue = largeRangeResult.getOrNull()!!
        assertTrue(largeRangeValue >= 1_000_000 && largeRangeValue < 2_000_000)
    }

    @Test
    fun testSequentialCalls() {
        // Test that sequential calls maintain quality
        val callCount = 100
        val values = mutableListOf<Int>()

        for (i in 0 until callCount) {
            val result = secureRandom.nextInt(1000)
            assertTrue(result.isSuccess, "Sequential call $i failed")
            values.add(result.getOrNull()!!)
        }

        // Check for basic patterns that shouldn't occur
        var consecutive = 0
        var ascending = 0

        for (i in 1 until values.size) {
            if (values[i] == values[i-1]) consecutive++
            if (values[i] > values[i-1]) ascending++
        }

        // Should have few consecutive identical values
        assertTrue(consecutive < callCount / 10, "Too many consecutive identical values: $consecutive")

        // Should not have strong ascending bias
        val ascendingRatio = ascending.toDouble() / (callCount - 1)
        assertTrue(
            ascendingRatio > 0.3 && ascendingRatio < 0.7,
            "Strong bias in sequential values: $ascendingRatio"
        )
    }

    @Test
    fun testFactoryFunction() {
        // Test that the factory function works correctly
        val result = createSecureRandom()
        assertTrue(result.isSuccess, "Factory function should succeed")

        val random = result.getOrNull()!!
        assertNotNull(random)

        // Test that the created instance works
        val bytesResult = random.nextBytes(16)
        assertTrue(bytesResult.isSuccess, "Created instance should work")
    }

    // Note: testLongSequences() removed due to mathematical flaw
    // The test checked for repeating N-byte patterns in random data, but with birthday paradox,
    // this has ~7-8% false positive rate for 2-byte patterns in 1000 bytes even with perfect randomness.
    // See: https://github.com/scottnj/KMP-Secure-Random/commit/[commit-hash]
}