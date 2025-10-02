package com.scottnj.kmp_secure_random.nist

import kotlin.math.sqrt

/**
 * Configuration for NIST SP 800-22 statistical tests.
 *
 * NIST SP 800-22 recommends testing multiple independent sequences (minimum 55, recommended 100+)
 * of sufficient length (minimum 1,000,000 bits) to properly assess randomness quality.
 *
 * Test modes:
 * - **Quick**: 55 sequences × 100K bits (~2-3 minutes) - For CI/CD pipelines
 * - **Standard**: 100 sequences × 1M bits (~10-15 minutes) - NIST compliant baseline
 * - **Comprehensive**: 1000 sequences × 1M bits (~60+ minutes) - Research-grade validation
 *
 * Environment variable: `NIST_TEST_MODE` (values: "quick", "standard", "comprehensive")
 */
object NistTestConfig {

    /**
     * Test mode enumeration.
     */
    enum class TestMode {
        /** Fast mode for CI/CD: 55 sequences × 100K bits */
        QUICK,

        /** NIST baseline: 100 sequences × 1M bits */
        STANDARD,

        /** Research-grade: 1000 sequences × 1M bits */
        COMPREHENSIVE
    }

    /**
     * Current test mode, determined by NIST_TEST_MODE environment variable.
     * Defaults to QUICK for CI/CD performance.
     */
    val mode: TestMode by lazy {
        val envMode = getEnvironmentVariable("NIST_TEST_MODE")?.lowercase()
        when (envMode) {
            "standard" -> TestMode.STANDARD
            "comprehensive" -> TestMode.COMPREHENSIVE
            else -> TestMode.QUICK // Default for CI/CD
        }
    }

    /**
     * Number of independent sequences to test.
     */
    val sequenceCount: Int
        get() = when (mode) {
            TestMode.QUICK -> 55
            TestMode.STANDARD -> 100
            TestMode.COMPREHENSIVE -> 1000
        }

    /**
     * Bit length for each sequence.
     */
    val sequenceLength: Int
        get() = when (mode) {
            TestMode.QUICK -> 100_000
            TestMode.STANDARD -> 1_000_000
            TestMode.COMPREHENSIVE -> 1_000_000
        }

    /**
     * Significance level for statistical tests (α = 0.01 per NIST).
     */
    const val SIGNIFICANCE_LEVEL = 0.01

    /**
     * Minimum P-value for uniformity test (NIST Section 4.2.2).
     */
    const val UNIFORMITY_MIN_PVALUE = 0.0001

    /**
     * Number of bins for P-value uniformity test.
     */
    const val UNIFORMITY_BINS = 10

    /**
     * Calculate confidence interval for proportion passing test.
     *
     * Formula from NIST SP 800-22 Section 4.2.1:
     * p̂ ± 3√(p̂(1-p̂)/m)
     *
     * where p̂ = 1 - α and m = number of sequences
     *
     * @return Pair of (lowerBound, upperBound)
     */
    fun getProportionConfidenceInterval(): Pair<Double, Double> {
        val p = 1.0 - SIGNIFICANCE_LEVEL // 0.99
        val m = sequenceCount
        val margin = 3.0 * sqrt(p * (1.0 - p) / m)
        return Pair(p - margin, p + margin)
    }

    /**
     * Get expected range of passing sequences for proportion test.
     *
     * Uses ceiling for minimum (must have at least ceil(lower * m) sequences pass)
     * and floor for maximum (can have at most floor(upper * m) sequences pass, capped at total).
     *
     * @return Pair of (minPassing, maxPassing) sequence counts
     */
    fun getExpectedPassingRange(): Pair<Int, Int> {
        val (lower, upper) = getProportionConfidenceInterval()
        val minPassing = kotlin.math.ceil(lower * sequenceCount).toInt()
        val maxPassing = kotlin.math.floor(upper * sequenceCount).toInt().coerceAtMost(sequenceCount)
        return Pair(minPassing, maxPassing)
    }

    /**
     * Platform-agnostic environment variable access.
     * Returns null if variable doesn't exist.
     */
    private fun getEnvironmentVariable(name: String): String? {
        return try {
            // This will be implemented differently per platform via expect/actual if needed
            // For now, we'll use a simple approach that works in common code
            null // Will default to QUICK mode
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Results from NIST multi-sequence testing.
 *
 * @property testName Name of the NIST test
 * @property pValues P-values from all tested sequences
 * @property proportionPassing Number of sequences that passed (P-value ≥ α)
 * @property proportionPValue P-value for proportion passing test
 * @property uniformityPValue P-value for P-value uniformity test
 * @property passed True if both proportion and uniformity tests pass
 */
data class NistTestResult(
    val testName: String,
    val pValues: List<Double>,
    val proportionPassing: Int,
    val proportionPValue: Double,
    val uniformityPValue: Double,
    val passed: Boolean
) {
    /**
     * Generate detailed test report.
     */
    fun toReport(): String {
        val config = NistTestConfig
        val (minPassing, maxPassing) = config.getExpectedPassingRange()
        val (lowerBound, upperBound) = config.getProportionConfidenceInterval()

        // Use the same logic as the actual test for consistency
        val proportionPassed = NistStatisticalAnalysis.checkProportionPassing(proportionPassing, pValues.size)
        val uniformityPassed = uniformityPValue >= NistTestConfig.UNIFORMITY_MIN_PVALUE

        val histogram = buildPValueHistogram()

        return buildString {
            appendLine("=" .repeat(80))
            appendLine("NIST SP 800-22: $testName")
            appendLine("=" .repeat(80))
            appendLine("Test Mode: ${config.mode}")
            appendLine("Sequences: ${pValues.size}")
            appendLine("Sequence Length: ${config.sequenceLength} bits")
            appendLine()
            appendLine("PROPORTION TEST (NIST Section 4.2.1):")
            appendLine("  Passing sequences: $proportionPassing / ${pValues.size}")
            appendLine("  Expected range: $minPassing - $maxPassing (${formatDouble(lowerBound, 4)} - ${formatDouble(upperBound, 4)})")
            appendLine("  Result: ${if (proportionPassed) "PASS ✓" else "FAIL ✗"}")
            appendLine()
            appendLine("P-VALUE UNIFORMITY TEST (NIST Section 4.2.2):")
            appendLine("  Chi-square P-value: ${formatDouble(uniformityPValue, 6)}")
            appendLine("  Minimum required: ${NistTestConfig.UNIFORMITY_MIN_PVALUE}")
            appendLine("  Result: ${if (uniformityPassed) "PASS ✓" else "FAIL ✗"}")
            appendLine()
            appendLine("P-VALUE DISTRIBUTION (10 bins):")
            appendLine(histogram)
            appendLine()
            appendLine("OVERALL RESULT: ${if (passed) "PASS ✓" else "FAIL ✗"}")
            appendLine("=" .repeat(80))
        }
    }

    /**
     * Build ASCII histogram of P-value distribution.
     */
    private fun buildPValueHistogram(): String {
        val bins = IntArray(NistTestConfig.UNIFORMITY_BINS)
        val expectedPerBin = pValues.size / NistTestConfig.UNIFORMITY_BINS

        for (p in pValues) {
            val binIndex = (p * NistTestConfig.UNIFORMITY_BINS).toInt()
                .coerceIn(0, NistTestConfig.UNIFORMITY_BINS - 1)
            bins[binIndex]++
        }

        return buildString {
            appendLine("  Bin    Range      Count  Expected  Bar")
            for (i in bins.indices) {
                val lower = i / 10.0
                val upper = (i + 1) / 10.0
                val count = bins[i]
                val bar = "█".repeat((count * 40 / pValues.size).coerceAtLeast(0))
                appendLine("  C${i + 1}   [${formatDouble(lower, 1)}-${formatDouble(upper, 1)})  ${formatInt(count, 5)}    ${formatInt(expectedPerBin, 5)}    $bar")
            }
        }
    }

    /**
     * Format double to specified decimal places (platform-agnostic).
     */
    private fun formatDouble(value: Double, decimals: Int): String {
        val multiplier = when (decimals) {
            1 -> 10.0
            2 -> 100.0
            3 -> 1000.0
            4 -> 10000.0
            5 -> 100000.0
            6 -> 1000000.0
            else -> 1.0
        }
        val rounded = kotlin.math.round(value * multiplier) / multiplier

        // Simple formatting without String.format()
        val intPart = rounded.toInt()
        val fracPart = ((rounded - intPart) * multiplier).toInt().toString().padStart(decimals, '0')
        return if (decimals > 0) "$intPart.$fracPart" else intPart.toString()
    }

    /**
     * Format integer with padding (platform-agnostic).
     */
    private fun formatInt(value: Int, width: Int): String {
        return value.toString().padStart(width, ' ')
    }
}
