package com.kmp.securerandom

import com.scottnj.kmp_secure_random.createSecureRandom
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * JVM-specific statistical randomness tests for SecureRandom implementation.
 * These tests focus on advanced statistical analysis that requires JVM-specific features.
 *
 * Note: Basic statistical tests have been moved to StatisticalAdvancedTest in commonTest
 * for cross-platform validation. This file contains JVM-specific advanced tests.
 */
class StatisticalRandomnessTest {

    private val secureRandom = createSecureRandom().getOrThrow()
    private val sampleSize = 5000 // Reduced for faster execution
    private val significanceLevel = 0.01 // 99% confidence level

    // Chi-square test moved to StatisticalAdvancedTest in commonTest for cross-platform validation

    // Shannon entropy test moved to StatisticalAdvancedTest in commonTest for cross-platform validation

    /**
     * Autocorrelation test to check for independence between consecutive values.
     * Low autocorrelation indicates good randomness.
     */
    @Test
    fun testAutocorrelation() {
        val bytesResult = secureRandom.nextBytes(sampleSize)
        assertTrue(bytesResult.isSuccess, "Failed to generate random bytes")

        val bytes = bytesResult.getOrNull()!!

        // Test autocorrelation at various lags
        val lags = listOf(1, 2, 5, 10)

        for (lag in lags) {
            val autocorrelation = calculateAutocorrelation(bytes, lag)
            val threshold = 0.2 // Threshold for acceptable autocorrelation

            println("Autocorrelation at lag $lag: $autocorrelation")
            assertTrue(
                abs(autocorrelation) < threshold,
                "High autocorrelation at lag $lag: ${abs(autocorrelation)} >= $threshold"
            )
        }
    }

    /**
     * Monobit frequency test (simplified NIST SP 800-22).
     * Tests if the number of 0s and 1s in the bit sequence are approximately equal.
     *
     * Uses multiple iterations with majority decision to reduce statistical flakiness
     * while maintaining test effectiveness for detecting true bias.
     */
    @Test
    fun testMonobitFrequency() {
        val iterations = 5
        val results = mutableListOf<Pair<Double, String>>()
        var passes = 0

        // Critical value for normal distribution at 0.05 significance level
        // Using Bonferroni correction: α = 0.05/iterations ≈ 0.01, critical value = 2.58
        val criticalValue = 2.58

        repeat(iterations) { iteration ->
            val result = performSingleMonobitTest(criticalValue, iteration + 1)
            results.add(result)
            if (result.first < criticalValue) passes++
        }

        // Print all results for debugging
        results.forEachIndexed { index, (statistic, details) ->
            val status = if (statistic < criticalValue) "PASS" else "FAIL"
            println("Monobit test ${index + 1}: $details, statistic=$statistic [$status]")
        }

        // Require majority of tests to pass (at least 3/5)
        val requiredPasses = 3
        assertTrue(
            passes >= requiredPasses,
            "Monobit test failed too often: $passes/$iterations passed (need $requiredPasses). " +
            "Multiple failures may indicate systematic bias in bit generation."
        )
    }

    /**
     * Performs a single monobit frequency test.
     * @param criticalValue The critical value for the test
     * @param iteration The iteration number for logging
     * @return Pair of (test statistic, debug info)
     */
    private fun performSingleMonobitTest(criticalValue: Double, iteration: Int): Pair<Double, String> {
        val bytesResult = secureRandom.nextBytes(sampleSize)
        assertTrue(bytesResult.isSuccess, "Failed to generate random bytes in iteration $iteration")

        val bytes = bytesResult.getOrNull()!!

        // Count 1s in the bit sequence
        var oneCount = 0
        bytes.forEach { byte ->
            for (i in 0..7) {
                if ((byte.toInt() shr i) and 1 == 1) {
                    oneCount++
                }
            }
        }

        val totalBits = sampleSize * 8
        val zeroCount = totalBits - oneCount

        // Calculate test statistic
        val sum = oneCount - zeroCount
        val statistic = abs(sum) / sqrt(totalBits.toDouble())

        val details = "ones=$oneCount, zeros=$zeroCount"
        return Pair(statistic, details)
    }

    // Integer distribution test moved to StatisticalAdvancedTest in commonTest for cross-platform validation

    // Double distribution test moved to StatisticalAdvancedTest in commonTest for cross-platform validation

    /**
     * Helper function to calculate autocorrelation at a given lag.
     */
    private fun calculateAutocorrelation(data: ByteArray, lag: Int): Double {
        if (lag >= data.size) return 0.0

        val n = data.size - lag
        val values = data.map { it.toDouble() }
        val mean = values.average()

        var covariance = 0.0
        var variance = 0.0

        for (i in 0 until n) {
            val diff1 = values[i] - mean
            val diff2 = values[i + lag] - mean
            covariance += diff1 * diff2
            variance += diff1 * diff1
        }

        return if (variance > 0) covariance / variance else 0.0
    }
}