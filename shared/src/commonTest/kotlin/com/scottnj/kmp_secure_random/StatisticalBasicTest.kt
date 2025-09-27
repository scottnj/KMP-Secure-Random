package com.scottnj.kmp_secure_random

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Basic statistical tests for SecureRandom that can run on all platforms.
 * These are simplified versions of more comprehensive statistical tests.
 */
class StatisticalBasicTest {

    private val secureRandom = createSecureRandom().getOrThrow()

    @Test
    fun testByteUniformity() {
        val samples = 5000
        val bytesResult = secureRandom.nextBytes(samples)
        assertTrue(bytesResult.isSuccess)

        val bytes = bytesResult.getOrNull()!!
        val frequencies = IntArray(256)

        // Count byte frequencies
        bytes.forEach { byte ->
            val index = (byte.toInt() and 0xFF)
            frequencies[index]++
        }

        // Check that we have reasonable distribution
        val expectedFreq = samples.toDouble() / 256
        var extremeDeviations = 0

        for (freq in frequencies) {
            val deviation = abs(freq - expectedFreq) / expectedFreq
            if (deviation > 0.5) extremeDeviations++ // Allow 50% deviation
        }

        // Should not have too many extreme deviations
        assertTrue(
            extremeDeviations < 26, // Less than 10% of buckets with extreme deviation
            "Too many extreme deviations: $extremeDeviations"
        )
    }

    @Test
    fun testIntegerUniformity() {
        val samples = 1000
        val bound = 10
        val frequencies = IntArray(bound)

        // Generate random integers and count frequencies
        for (i in 0 until samples) {
            val result = secureRandom.nextInt(bound)
            assertTrue(result.isSuccess)
            val value = result.getOrNull()!!
            frequencies[value]++
        }

        // Check basic uniformity
        val expectedFreq = samples.toDouble() / bound
        for (freq in frequencies) {
            val deviation = abs(freq - expectedFreq) / expectedFreq
            assertTrue(deviation < 0.5, "Frequency deviation too high: $deviation")
        }
    }

    @Test
    fun testDoubleDistribution() {
        val samples = 1000
        val values = mutableListOf<Double>()

        for (i in 0 until samples) {
            val result = secureRandom.nextDouble()
            assertTrue(result.isSuccess)
            values.add(result.getOrNull()!!)
        }

        // All values should be in [0, 1)
        assertTrue(values.all { it >= 0.0 && it < 1.0 })

        // Mean should be approximately 0.5
        val mean = values.average()
        assertTrue(abs(mean - 0.5) < 0.05, "Mean too far from 0.5: $mean")

        // Should have values in different ranges
        val lowCount = values.count { it < 0.5 }
        val highCount = values.count { it >= 0.5 }
        val balance = abs(lowCount - highCount).toDouble() / samples

        assertTrue(balance < 0.2, "Distribution too unbalanced: $balance")
    }

    @Test
    fun testBooleanDistribution() {
        val samples = 1000
        var trueCount = 0

        for (i in 0 until samples) {
            val result = secureRandom.nextBoolean()
            assertTrue(result.isSuccess)
            if (result.getOrNull() == true) trueCount++
        }

        val trueRatio = trueCount.toDouble() / samples
        assertTrue(abs(trueRatio - 0.5) < 0.1, "Boolean distribution unbalanced: $trueRatio")
    }

    @Test
    fun testRandomnessBasic() {
        val samples = 1000
        val values = mutableSetOf<String>()

        // Generate byte arrays and check for uniqueness
        for (i in 0 until samples) {
            val result = secureRandom.nextBytes(8)
            assertTrue(result.isSuccess)
            values.add(result.getOrNull()!!.contentToString())
        }

        // Should have high uniqueness (very unlikely to have duplicates with 8 bytes)
        val uniquenessRatio = values.size.toDouble() / samples
        assertTrue(uniquenessRatio > 0.95, "Low uniqueness: $uniquenessRatio")
    }

    @Test
    fun testConsecutiveValues() {
        // Test that consecutive calls don't produce identical results
        val iterations = 100
        var identicalCount = 0

        for (i in 0 until iterations) {
            val result1 = secureRandom.nextBytes(16)
            val result2 = secureRandom.nextBytes(16)

            assertTrue(result1.isSuccess && result2.isSuccess)

            val bytes1 = result1.getOrNull()!!
            val bytes2 = result2.getOrNull()!!

            if (bytes1.contentEquals(bytes2)) {
                identicalCount++
            }
        }

        // Should have very few (ideally zero) identical consecutive results
        assertTrue(identicalCount < 3, "Too many identical consecutive results: $identicalCount")
    }

    @Test
    fun testZeroBytes() {
        val samples = 1000
        val totalBytes = samples * 16
        var zeroCount = 0

        // Count zero bytes across multiple generations
        for (i in 0 until samples) {
            val result = secureRandom.nextBytes(16)
            assertTrue(result.isSuccess)
            val bytes = result.getOrNull()!!

            zeroCount += bytes.count { it == 0.toByte() }
        }

        // Zero bytes should be roughly 1/256 of total
        val zeroRatio = zeroCount.toDouble() / totalBytes
        val expectedRatio = 1.0 / 256.0

        // Allow significant variance but check it's reasonable
        assertTrue(
            abs(zeroRatio - expectedRatio) < 0.01,
            "Zero byte ratio unusual: $zeroRatio (expected ~$expectedRatio)"
        )
    }
}