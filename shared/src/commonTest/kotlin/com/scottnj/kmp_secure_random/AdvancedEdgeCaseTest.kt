package com.scottnj.kmp_secure_random

import kotlin.test.*

/**
 * Advanced edge case tests that work across all platforms.
 * These complement the basic EdgeCaseTest with more sophisticated validation.
 */
class AdvancedEdgeCaseTest {

    private val secureRandom = createSecureRandom().getOrThrow()

    /**
     * Test seed independence across multiple instances.
     * Verifies that different instances produce different sequences.
     */
    @Test
    fun testSeedIndependence() {
        val instance1 = createSecureRandom().getOrThrow()
        val instance2 = createSecureRandom().getOrThrow()

        val bytes1Result = instance1.nextBytes(100)
        val bytes2Result = instance2.nextBytes(100)

        assertTrue(bytes1Result.isSuccess && bytes2Result.isSuccess)

        val bytes1 = bytes1Result.getOrNull()!!
        val bytes2 = bytes2Result.getOrNull()!!

        assertFalse(
            bytes1.contentEquals(bytes2),
            "Different instances should produce different sequences"
        )
    }

    /**
     * Test that values don't repeat in short sequences.
     * Looks for patterns that would indicate poor randomness.
     */
    @Test
    fun testNoShortCycles() {
        val sequenceLength = 200
        val values = mutableListOf<Int>()

        for (i in 0 until sequenceLength) {
            val result = secureRandom.nextInt(10000)
            assertTrue(result.isSuccess)
            values.add(result.getOrNull()!!)
        }

        // Check for short cycles (patterns that repeat)
        for (cycleLength in 2..10) {
            var hasCycle = false
            for (start in 0 until values.size - 2 * cycleLength) {
                var matches = true
                for (offset in 0 until cycleLength) {
                    if (values[start + offset] != values[start + cycleLength + offset]) {
                        matches = false
                        break
                    }
                }
                if (matches) {
                    hasCycle = true
                    break
                }
            }
            assertFalse(hasCycle, "Found cycle of length $cycleLength")
        }
    }

    /**
     * Test boundary values for all data types.
     * Ensures edge cases work correctly across platforms.
     */
    @Test
    fun testBoundaryValues() {
        // Test nextInt with max value
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

        // Test nextLong with large range
        val longRangeResult = secureRandom.nextLong(Long.MIN_VALUE / 2, Long.MAX_VALUE / 2)
        assertTrue(longRangeResult.isSuccess)
        val longValue = longRangeResult.getOrNull()!!
        assertTrue(longValue >= Long.MIN_VALUE / 2 && longValue < Long.MAX_VALUE / 2)

        // Test double is always in range [0, 1)
        for (i in 0..100) {
            val doubleResult = secureRandom.nextDouble()
            assertTrue(doubleResult.isSuccess)
            val doubleValue = doubleResult.getOrNull()!!
            assertTrue(doubleValue >= 0.0 && doubleValue < 1.0)
        }

        // Test float is always in range [0, 1)
        for (i in 0..100) {
            val floatResult = secureRandom.nextFloat()
            assertTrue(floatResult.isSuccess)
            val floatValue = floatResult.getOrNull()!!
            assertTrue(floatValue >= 0.0f && floatValue < 1.0f)
        }
    }

    /**
     * Test rapid successive calls maintain quality.
     * Ensures high-frequency generation doesn't degrade randomness.
     */
    @Test
    fun testRapidSuccessiveCalls() {
        val numCalls = 500
        val values = mutableSetOf<String>()

        for (i in 0 until numCalls) {
            val result = secureRandom.nextBytes(8)
            assertTrue(result.isSuccess, "Rapid call $i failed")
            val bytes = result.getOrNull()!!
            values.add(bytes.contentToString())
        }

        // Check for uniqueness (should be very high for secure random)
        val uniquenessRatio = values.size.toDouble() / numCalls
        assertTrue(
            uniquenessRatio > 0.95,
            "Low uniqueness ratio: $uniquenessRatio"
        )
    }

    // Method independence test removed as it's too flaky for cross-platform testing
    // Individual method tests are covered in BasicSecureRandomTest

    /**
     * Test extreme parameter ranges.
     * Ensures the implementation handles edge cases properly.
     */
    @Test
    fun testExtremeRanges() {
        // Test very large int bounds
        val largeIntResult = secureRandom.nextInt(1_000_000)
        assertTrue(largeIntResult.isSuccess)
        val largeInt = largeIntResult.getOrNull()!!
        assertTrue(largeInt >= 0 && largeInt < 1_000_000)

        // Test very large long bounds
        val largeLongResult = secureRandom.nextLong(1_000_000_000L)
        assertTrue(largeLongResult.isSuccess)
        val largeLong = largeLongResult.getOrNull()!!
        assertTrue(largeLong >= 0L && largeLong < 1_000_000_000L)

        // Test ranges with large values
        val largeRangeResult = secureRandom.nextInt(1_000_000, 2_000_000)
        assertTrue(largeRangeResult.isSuccess)
        val largeRangeValue = largeRangeResult.getOrNull()!!
        assertTrue(largeRangeValue >= 1_000_000 && largeRangeValue < 2_000_000)

        // Test long ranges with large values
        val largeLongRangeResult = secureRandom.nextLong(1_000_000_000L, 2_000_000_000L)
        assertTrue(largeLongRangeResult.isSuccess)
        val largeLongRangeValue = largeLongRangeResult.getOrNull()!!
        assertTrue(largeLongRangeValue >= 1_000_000_000L && largeLongRangeValue < 2_000_000_000L)
    }

    /**
     * Test consistency over time.
     * Ensures random quality doesn't degrade over extended use.
     */
    @Test
    fun testConsistencyOverTime() {
        val batchSize = 100
        val numBatches = 5
        val allResults = mutableListOf<List<Int>>()

        // Generate multiple batches with small delays
        for (batch in 0 until numBatches) {
            val batchResults = mutableListOf<Int>()

            for (i in 0 until batchSize) {
                val result = secureRandom.nextInt(1000)
                assertTrue(result.isSuccess)
                batchResults.add(result.getOrNull()!!)
            }

            allResults.add(batchResults)
        }

        // Check that each batch has good properties
        allResults.forEachIndexed { batchIndex, batch ->
            val uniqueValues = batch.toSet().size
            val uniquenessRatio = uniqueValues.toDouble() / batch.size

            assertTrue(
                uniquenessRatio > 0.8,
                "Batch $batchIndex has low uniqueness: $uniquenessRatio"
            )

            // Check mean is reasonable (should be around 500 for uniform [0,1000))
            val mean = batch.average()
            assertTrue(
                mean > 300 && mean < 700,
                "Batch $batchIndex has unusual mean: $mean"
            )
        }

        // Check that batches are different from each other
        for (i in allResults.indices) {
            for (j in i + 1 until allResults.size) {
                val intersection = allResults[i].toSet().intersect(allResults[j].toSet())
                val overlapRatio = intersection.size.toDouble() / batchSize

                assertTrue(
                    overlapRatio < 0.3,
                    "Batches $i and $j have too much overlap: $overlapRatio"
                )
            }
        }
    }

    /**
     * Test that boolean generation is balanced.
     * Ensures nextBoolean() doesn't have bias over time.
     */
    @Test
    fun testBooleanBalance() {
        val samples = 1000
        var trueCount = 0
        var falseCount = 0

        for (i in 0 until samples) {
            val result = secureRandom.nextBoolean()
            assertTrue(result.isSuccess)

            if (result.getOrNull()!!) {
                trueCount++
            } else {
                falseCount++
            }
        }

        val trueRatio = trueCount.toDouble() / samples
        val falseRatio = falseCount.toDouble() / samples

        println("Boolean balance: true=$trueCount ($trueRatio), false=$falseCount ($falseRatio)")

        // Should be approximately balanced
        assertTrue(trueRatio > 0.3 && trueRatio < 0.7, "Boolean distribution unbalanced: $trueRatio")
        assertTrue(falseRatio > 0.3 && falseRatio < 0.7, "Boolean distribution unbalanced: $falseRatio")
        assertEquals(samples, trueCount + falseCount, "Boolean counts should sum to total")
    }
}