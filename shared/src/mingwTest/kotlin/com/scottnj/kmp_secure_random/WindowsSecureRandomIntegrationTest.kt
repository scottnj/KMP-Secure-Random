package com.scottnj.kmp_secure_random

import kotlin.test.*
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Integration tests for Windows SecureRandom implementation.
 *
 * These tests validate the Windows crypto API integration and
 * statistical properties of the random number generator.
 */
class WindowsSecureRandomIntegrationTest {

    private lateinit var secureRandom: SecureRandom

    @BeforeTest
    fun setUp() {
        val result = createSecureRandom()
        assertTrue(result is SecureRandomResult.Success)
        secureRandom = result.value
    }

    @Test
    fun testWindowsAPICompatibility() {
        // Test that we can generate random data successfully
        // This validates BCryptGenRandom or CryptGenRandom is working

        val smallBuffer = ByteArray(16)
        val result1 = secureRandom.nextBytes(smallBuffer)
        assertTrue(result1 is SecureRandomUnitResult.Success)

        val mediumBuffer = ByteArray(1024)
        val result2 = secureRandom.nextBytes(mediumBuffer)
        assertTrue(result2 is SecureRandomUnitResult.Success)

        val largeBuffer = ByteArray(65536)
        val result3 = secureRandom.nextBytes(largeBuffer)
        assertTrue(result3 is SecureRandomUnitResult.Success)
    }

    @Test
    fun testStatisticalRandomnessBasic() {
        // Chi-square test for uniform distribution
        val buckets = IntArray(10)
        val samples = 10000

        repeat(samples) {
            val result = secureRandom.nextInt(10)
            assertTrue(result is SecureRandomResult.Success)
            buckets[result.value]++
        }

        val expected = samples.toDouble() / 10
        var chiSquare = 0.0

        for (count in buckets) {
            val diff = count - expected
            chiSquare += (diff * diff) / expected
        }

        // Critical value for 9 degrees of freedom at 0.05 significance level is 16.919
        assertTrue(chiSquare < 16.919, "Chi-square value $chiSquare indicates non-uniform distribution")
    }

    @Test
    fun testEntropyEstimation() {
        // Shannon entropy test
        val bytes = ByteArray(10000)
        val result = secureRandom.nextBytes(bytes)
        assertTrue(result is SecureRandomUnitResult.Success)

        val frequency = IntArray(256)
        bytes.forEach { frequency[it.toInt() and 0xFF]++ }

        var entropy = 0.0
        val total = bytes.size.toDouble()

        for (count in frequency) {
            if (count > 0) {
                val probability = count / total
                entropy -= probability * (kotlin.math.ln(probability) / kotlin.math.ln(2.0))
            }
        }

        // Good random data should have entropy close to 8 bits per byte
        assertTrue(entropy > 7.9, "Entropy $entropy is too low for secure random data")
    }

    @Test
    fun testNoObviousPatterns() {
        // Generate sequence and check for obvious patterns
        val sequence = mutableListOf<Int>()

        repeat(100) {
            val result = secureRandom.nextInt(100)
            assertTrue(result is SecureRandomResult.Success)
            sequence.add(result.value)
        }

        // Check for no immediate repetitions
        var repetitions = 0
        for (i in 1 until sequence.size) {
            if (sequence[i] == sequence[i - 1]) {
                repetitions++
            }
        }

        // In truly random sequence, consecutive repetitions should be rare (about 1%)
        assertTrue(repetitions < 5, "Too many consecutive repetitions: $repetitions")

        // Check for no arithmetic progressions
        var progressions = 0
        for (i in 2 until sequence.size) {
            if (sequence[i] - sequence[i - 1] == sequence[i - 1] - sequence[i - 2]) {
                progressions++
            }
        }

        assertTrue(progressions < 5, "Too many arithmetic progressions: $progressions")
    }

    @Test
    fun testWindowsCryptoAPIPerformance() {
        // Performance test - ensure Windows crypto APIs respond in reasonable time
        val startTime = kotlin.system.getTimeMillis()
        val iterations = 1000

        repeat(iterations) {
            val result = secureRandom.nextBytes(256)
            assertTrue(result is SecureRandomResult.Success)
        }

        val endTime = kotlin.system.getTimeMillis()
        val duration = endTime - startTime

        // Should complete 1000 iterations of 256-byte generation in under 5 seconds
        assertTrue(duration < 5000, "Performance too slow: ${duration}ms for $iterations iterations")
    }

    @Test
    fun testDistributionAcrossByteValues() {
        val bytes = ByteArray(100000)
        val result = secureRandom.nextBytes(bytes)
        assertTrue(result is SecureRandomUnitResult.Success)

        val counts = IntArray(256)
        bytes.forEach { counts[it.toInt() and 0xFF]++ }

        val expected = bytes.size / 256.0
        val stdDev = sqrt(expected * (255.0 / 256.0))

        // Each byte value should appear within 3 standard deviations of expected
        var outliers = 0
        for (count in counts) {
            if (abs(count - expected) > 3 * stdDev) {
                outliers++
            }
        }

        assertTrue(outliers < 5, "Too many outlier byte values: $outliers")
    }

    @Test
    fun testMultipleInstancesIndependence() {
        // Create multiple instances and verify they produce different results
        val result1 = createSecureRandom()
        val result2 = createSecureRandom()

        assertTrue(result1 is SecureRandomResult.Success)
        assertTrue(result2 is SecureRandomResult.Success)

        val bytes1 = ByteArray(32)
        val bytes2 = ByteArray(32)

        val genResult1 = result1.value.nextBytes(bytes1)
        val genResult2 = result2.value.nextBytes(bytes2)

        assertTrue(genResult1 is SecureRandomUnitResult.Success)
        assertTrue(genResult2 is SecureRandomUnitResult.Success)

        assertFalse(bytes1.contentEquals(bytes2), "Different instances should produce different bytes")
    }

    @Test
    fun testWindowsErrorRecovery() {
        // Test that the implementation can recover from transient errors
        val results = mutableListOf<ByteArray>()

        repeat(10) {
            // Simulate potential resource pressure
            val largeRequest = secureRandom.nextBytes(1024 * 1024) // 1MB

            if (largeRequest is SecureRandomResult.Success) {
                results.add(largeRequest.value)
            }

            // Normal request should still work
            val normalRequest = secureRandom.nextBytes(16)
            assertTrue(normalRequest is SecureRandomResult.Success, "Normal request failed after large request")
        }

        // Verify we got at least some successful large requests
        assertTrue(results.size > 5, "Too many large request failures: ${10 - results.size}")
    }

    @Test
    fun testFloatingPointDistribution() {
        // Test that nextDouble and nextFloat produce uniform distributions
        val doubles = mutableListOf<Double>()
        val floats = mutableListOf<Float>()

        repeat(1000) {
            val doubleResult = secureRandom.nextDouble()
            val floatResult = secureRandom.nextFloat()

            assertTrue(doubleResult is SecureRandomResult.Success)
            assertTrue(floatResult is SecureRandomResult.Success)

            doubles.add(doubleResult.value)
            floats.add(floatResult.value)
        }

        // Check uniform distribution using buckets
        val doubleBuckets = IntArray(10)
        val floatBuckets = IntArray(10)

        doubles.forEach { doubleBuckets[(it * 10).toInt().coerceIn(0, 9)]++ }
        floats.forEach { floatBuckets[(it * 10).toInt().coerceIn(0, 9)]++ }

        // Each bucket should have roughly 100 values (±30%)
        for (i in 0..9) {
            assertTrue(doubleBuckets[i] in 70..130, "Double bucket $i has ${doubleBuckets[i]} values")
            assertTrue(floatBuckets[i] in 70..130, "Float bucket $i has ${floatBuckets[i]} values")
        }
    }

    @Test
    fun testBooleanFairness() {
        // Test that boolean generation is approximately fair (50/50)
        var trueCount = 0
        val total = 10000

        repeat(total) {
            val result = secureRandom.nextBoolean()
            assertTrue(result is SecureRandomResult.Success)
            if (result.value) trueCount++
        }

        val ratio = trueCount.toDouble() / total
        // Should be close to 0.5 (within 2%)
        assertTrue(ratio in 0.48..0.52, "Boolean ratio $ratio is not fair")
    }
}