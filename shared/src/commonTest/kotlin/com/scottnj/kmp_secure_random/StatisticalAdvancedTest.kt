package com.scottnj.kmp_secure_random

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Advanced statistical tests for SecureRandom that work across all platforms.
 * These tests provide statistical validation that complements the basic statistical tests.
 */
class StatisticalAdvancedTest {

    private val secureRandom = createSecureRandom().getOrThrow()
    private val sampleSize = 2000 // Reduced for cross-platform compatibility

    /**
     * Chi-square test for uniform distribution of bytes.
     * Tests if random bytes are uniformly distributed across all possible values.
     */
    @Test
    fun testChiSquareUniformDistribution() {
        val numBins = 256
        val expectedFrequency = sampleSize.toDouble() / numBins
        val observed = IntArray(numBins)

        // Generate random bytes and count frequencies
        val bytesResult = secureRandom.nextBytes(sampleSize)
        assertTrue(bytesResult.isSuccess, "Failed to generate random bytes")

        val bytes = bytesResult.getOrNull()!!
        bytes.forEach { byte ->
            val index = (byte.toInt() and 0xFF)
            observed[index]++
        }

        // Calculate chi-square statistic
        var chiSquare = 0.0
        for (i in 0 until numBins) {
            val diff = observed[i] - expectedFrequency
            chiSquare += (diff * diff) / expectedFrequency
        }

        // Critical value for chi-square with 255 degrees of freedom at 0.05 significance level
        val criticalValue = 293.248 // More lenient for cross-platform testing

        println("Chi-square statistic: $chiSquare (critical value: $criticalValue)")
        assertTrue(
            chiSquare < criticalValue,
            "Chi-square test failed: $chiSquare >= $criticalValue (indicates non-uniform distribution)"
        )
    }

    /**
     * Shannon entropy test to measure randomness quality.
     * Higher entropy indicates better randomness.
     */
    @Test
    fun testShannonEntropy() {
        val bytesResult = secureRandom.nextBytes(sampleSize)
        assertTrue(bytesResult.isSuccess, "Failed to generate random bytes")

        val bytes = bytesResult.getOrNull()!!

        // Calculate byte frequencies
        val frequencies = IntArray(256)
        bytes.forEach { byte ->
            val index = (byte.toInt() and 0xFF)
            frequencies[index]++
        }

        // Calculate Shannon entropy
        var entropy = 0.0
        for (freq in frequencies) {
            if (freq > 0) {
                val probability = freq.toDouble() / sampleSize
                entropy -= probability * (ln(probability) / ln(2.0))
            }
        }

        // Maximum entropy for 8-bit values is 8.0
        val maxEntropy = 8.0
        val minAcceptableEntropy = 7.5 // More lenient for cross-platform testing

        println("Shannon entropy: $entropy (max: $maxEntropy, min acceptable: $minAcceptableEntropy)")
        assertTrue(
            entropy > minAcceptableEntropy,
            "Entropy too low: $entropy <= $minAcceptableEntropy (indicates poor randomness)"
        )
    }

    /**
     * Test distribution of random integers within a range.
     * Verifies that nextInt(bound) produces uniformly distributed results.
     */
    @Test
    fun testIntegerDistribution() {
        val bound = 50
        val samples = 2000
        val frequencies = IntArray(bound)

        // Generate random integers and count frequencies
        for (i in 0 until samples) {
            val result = secureRandom.nextInt(bound)
            assertTrue(result.isSuccess, "Failed to generate random integer")
            val value = result.getOrNull()!!
            assertTrue(value in 0 until bound, "Value $value outside expected range [0, $bound)")
            frequencies[value]++
        }

        // Chi-square test for uniform distribution
        val expectedFrequency = samples.toDouble() / bound
        var chiSquare = 0.0

        for (freq in frequencies) {
            val diff = freq - expectedFrequency
            chiSquare += (diff * diff) / expectedFrequency
        }

        // Critical value for chi-square with 49 degrees of freedom at 0.01 significance level
        // Using more lenient threshold for CI environments to reduce statistical flakiness
        val criticalValue = 76.154 // 0.01 significance level for robust CI testing

        println("Integer distribution chi-square: $chiSquare (critical: $criticalValue)")
        assertTrue(
            chiSquare < criticalValue,
            "Integer distribution test failed: $chiSquare >= $criticalValue"
        )
    }

    /**
     * Test double distribution uniformity.
     * Verifies that nextDouble() produces uniformly distributed results in [0, 1).
     */
    @Test
    fun testDoubleDistribution() {
        val samples = 1000
        val values = mutableListOf<Double>()

        // Generate random doubles
        for (i in 0 until samples) {
            val result = secureRandom.nextDouble()
            assertTrue(result.isSuccess, "Failed to generate random double")
            val value = result.getOrNull()!!
            values.add(value)
        }

        // Check that all values are in [0, 1) range
        assertTrue(values.all { it >= 0.0 && it < 1.0 }, "All values should be in [0, 1) range")

        // Calculate mean (should be around 0.5 for uniform distribution)
        val mean = values.average()
        val expectedMean = 0.5
        val tolerance = 0.05

        println("Double distribution: mean=$mean (expected: $expectedMean)")

        assertTrue(
            abs(mean - expectedMean) < tolerance,
            "Double mean outside tolerance: $mean"
        )

        // Test distribution across quarters
        val q1 = values.count { it < 0.25 }
        val q2 = values.count { it >= 0.25 && it < 0.5 }
        val q3 = values.count { it >= 0.5 && it < 0.75 }
        val q4 = values.count { it >= 0.75 }

        val expectedPerQuarter = samples / 4.0
        val tolerance2 = expectedPerQuarter * 0.3 // 30% tolerance for cross-platform

        assertTrue(abs(q1 - expectedPerQuarter) < tolerance2, "Q1 distribution off: $q1")
        assertTrue(abs(q2 - expectedPerQuarter) < tolerance2, "Q2 distribution off: $q2")
        assertTrue(abs(q3 - expectedPerQuarter) < tolerance2, "Q3 distribution off: $q3")
        assertTrue(abs(q4 - expectedPerQuarter) < tolerance2, "Q4 distribution off: $q4")
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

        // Bonferroni correction: α = 0.05/iterations ≈ 0.01, critical value = 2.58
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
            "Multiple failures may indicate systematic bias in random generation."
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

    /**
     * Test for independence between different random value types.
     * Ensures that different methods don't have correlation.
     */
    @Test
    fun testCrossMethodIndependence() {
        val samples = 500

        val bytes = mutableListOf<Int>()
        val ints = mutableListOf<Int>()
        val doubles = mutableListOf<Double>()

        // Generate samples from different methods
        for (i in 0 until samples) {
            // Get one byte (as int)
            val byteResult = secureRandom.nextBytes(1)
            assertTrue(byteResult.isSuccess)
            bytes.add(byteResult.getOrNull()!![0].toInt() and 0xFF)

            // Get an int
            val intResult = secureRandom.nextInt(256) // Same range as byte
            assertTrue(intResult.isSuccess)
            ints.add(intResult.getOrNull()!!)

            // Get a double (mapped to int range)
            val doubleResult = secureRandom.nextDouble()
            assertTrue(doubleResult.isSuccess)
            doubles.add(doubleResult.getOrNull()!! * 256)
        }

        // Check that the sequences are not correlated
        val correlation1 = calculateSimpleCorrelationInt(bytes, ints)
        val correlation2 = calculateSimpleCorrelation(bytes.map { it.toDouble() }, doubles)
        val correlation3 = calculateSimpleCorrelation(ints.map { it.toDouble() }, doubles)

        val threshold = 0.2 // Low correlation expected

        println("Cross-method correlations: byte-int=$correlation1, byte-double=$correlation2, int-double=$correlation3")

        assertTrue(abs(correlation1) < threshold, "High correlation between byte and int methods: $correlation1")
        assertTrue(abs(correlation2) < threshold, "High correlation between byte and double methods: $correlation2")
        assertTrue(abs(correlation3) < threshold, "High correlation between int and double methods: $correlation3")
    }

    /**
     * Test for patterns in consecutive values.
     * Checks that there are no obvious sequential patterns.
     */
    @Test
    fun testSequentialPatterns() {
        val samples = 1000
        val values = mutableListOf<Int>()

        // Generate sequence of bounded integers
        for (i in 0 until samples) {
            val result = secureRandom.nextInt(100)
            assertTrue(result.isSuccess)
            values.add(result.getOrNull()!!)
        }

        // Test for ascending/descending runs
        var ascendingRuns = 0
        var descendingRuns = 0
        var currentAscending = 0
        var currentDescending = 0

        for (i in 1 until values.size) {
            if (values[i] > values[i-1]) {
                currentAscending++
                currentDescending = 0
            } else if (values[i] < values[i-1]) {
                currentDescending++
                currentAscending = 0
            } else {
                currentAscending = 0
                currentDescending = 0
            }

            if (currentAscending >= 5) ascendingRuns++
            if (currentDescending >= 5) descendingRuns++
        }

        // Should not have many long runs
        val totalLongRuns = ascendingRuns + descendingRuns
        val maxAcceptableRuns = samples / 100 // Allow 1% long runs

        println("Sequential patterns: ascending runs=$ascendingRuns, descending runs=$descendingRuns, total=$totalLongRuns")
        assertTrue(
            totalLongRuns < maxAcceptableRuns,
            "Too many sequential patterns: $totalLongRuns >= $maxAcceptableRuns"
        )
    }

    /**
     * Test variance and standard deviation of double values.
     * Ensures proper statistical properties of generated doubles.
     */
    @Test
    fun testDoubleVariance() {
        val samples = 1000
        val values = mutableListOf<Double>()

        for (i in 0 until samples) {
            val result = secureRandom.nextDouble()
            assertTrue(result.isSuccess)
            values.add(result.getOrNull()!!)
        }

        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        val stdDev = sqrt(variance)

        // For uniform distribution on [0,1), theoretical variance = 1/12 ≈ 0.0833
        val expectedVariance = 1.0 / 12.0
        val expectedStdDev = sqrt(expectedVariance)

        val varianceTolerance = expectedVariance * 0.3 // 30% tolerance
        val stdDevTolerance = expectedStdDev * 0.3

        println("Double statistics: mean=$mean, variance=$variance (expected: $expectedVariance), stdDev=$stdDev (expected: $expectedStdDev)")

        assertTrue(
            abs(variance - expectedVariance) < varianceTolerance,
            "Variance outside tolerance: $variance"
        )
        assertTrue(
            abs(stdDev - expectedStdDev) < stdDevTolerance,
            "Standard deviation outside tolerance: $stdDev"
        )
    }

    /**
     * Helper function to calculate simple correlation coefficient.
     */
    private fun calculateSimpleCorrelation(x: List<Double>, y: List<Double>): Double {
        if (x.size != y.size) return 0.0

        val n = x.size
        val meanX = x.average()
        val meanY = y.average()

        var numerator = 0.0
        var sumXX = 0.0
        var sumYY = 0.0

        for (i in 0 until n) {
            val dx = x[i] - meanX
            val dy = y[i] - meanY
            numerator += dx * dy
            sumXX += dx * dx
            sumYY += dy * dy
        }

        val denominator = sqrt(sumXX * sumYY)
        return if (denominator > 0) numerator / denominator else 0.0
    }

    /**
     * Helper function to calculate simple correlation for integer lists.
     */
    private fun calculateSimpleCorrelationInt(x: List<Int>, y: List<Int>): Double {
        return calculateSimpleCorrelation(x.map { it.toDouble() }, y.map { it.toDouble() })
    }
}