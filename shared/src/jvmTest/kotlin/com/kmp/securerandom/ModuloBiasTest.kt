package com.kmp.securerandom

import com.scottnj.kmp_secure_random.createSecureRandom
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for modulo bias elimination in bounded random generation.
 * Verifies that the rejection sampling implementation provides uniform distribution.
 */
class ModuloBiasTest {

    private val secureRandom = createSecureRandom().getOrThrow()

    /**
     * Test that nextLong(bound) produces uniform distribution without modulo bias.
     * Uses a bound that would exhibit bias with naive modulo approach.
     */
    @Test
    fun testLongBoundUniformDistribution() {
        // Use a bound that's not a power of 2 to detect modulo bias
        val bound = 3L
        val samples = 30000
        val bins = LongArray(bound.toInt()) { 0 }

        // Generate samples
        repeat(samples) {
            val value = secureRandom.nextLong(bound).getOrThrow()
            assertTrue(value >= 0 && value < bound, "Value $value out of bounds [0, $bound)")
            bins[value.toInt()]++
        }

        // Check for uniform distribution
        val expectedCount = samples / bound.toDouble()
        val maxDeviation = expectedCount * 0.05 // Allow 5% deviation

        for (i in bins.indices) {
            val deviation = abs(bins[i] - expectedCount)
            assertTrue(
                deviation <= maxDeviation,
                "Bin $i has ${bins[i]} samples, expected ~$expectedCount (deviation: $deviation)"
            )
        }

        println("Long bound uniformity test passed. Bin counts: ${bins.contentToString()}")
    }

    /**
     * Test that nextLong(min, max) produces uniform distribution without modulo bias.
     */
    @Test
    fun testLongRangeUniformDistribution() {
        val min = 100L
        val max = 107L // Range of 7, not a power of 2
        val range = max - min
        val samples = 21000
        val bins = LongArray(range.toInt()) { 0 }

        // Generate samples
        repeat(samples) {
            val value = secureRandom.nextLong(min, max).getOrThrow()
            assertTrue(value >= min && value < max, "Value $value out of range [$min, $max)")
            bins[(value - min).toInt()]++
        }

        // Check for uniform distribution
        val expectedCount = samples / range.toDouble()
        val maxDeviation = expectedCount * 0.05 // Allow 5% deviation

        for (i in bins.indices) {
            val deviation = abs(bins[i] - expectedCount)
            assertTrue(
                deviation <= maxDeviation,
                "Bin $i has ${bins[i]} samples, expected ~$expectedCount (deviation: $deviation)"
            )
        }

        println("Long range uniformity test passed. Bin counts: ${bins.contentToString()}")
    }

    /**
     * Test that rejection sampling handles edge cases properly.
     */
    @Test
    fun testRejectionSamplingEdgeCases() {
        // Test with bound = 1 (should always return 0)
        repeat(100) {
            val value = secureRandom.nextLong(1L).getOrThrow()
            assertTrue(value == 0L, "nextLong(1) should always return 0, got $value")
        }

        // Test with large bound to ensure no overflow issues
        val largeBound = Long.MAX_VALUE / 2
        repeat(100) {
            val value = secureRandom.nextLong(largeBound).getOrThrow()
            assertTrue(value >= 0 && value < largeBound, "Large bound value $value out of range")
        }

        println("Rejection sampling edge cases test passed")
    }

    /**
     * Test that the rejection sampling doesn't create infinite loops
     * by timing the generation process.
     */
    @Test
    fun testRejectionSamplingPerformance() {
        // Use a bound that might require some rejection iterations
        val bound = (Long.MAX_VALUE / 3) * 2 + 1 // ~67% of Long.MAX_VALUE
        val startTime = System.currentTimeMillis()

        repeat(1000) {
            val value = secureRandom.nextLong(bound).getOrThrow()
            assertTrue(value >= 0 && value < bound, "Performance test value out of bounds")
        }

        val duration = System.currentTimeMillis() - startTime
        assertTrue(duration < 5000, "Rejection sampling took too long: ${duration}ms")

        println("Rejection sampling performance test passed in ${duration}ms")
    }
}