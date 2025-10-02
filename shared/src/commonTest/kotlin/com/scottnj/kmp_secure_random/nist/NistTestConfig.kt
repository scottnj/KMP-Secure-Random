package com.scottnj.kmp_secure_random.nist

import kotlin.math.sqrt

/**
 * Configuration for NIST SP 800-22 statistical tests.
 *
 * NIST SP 800-22 requirements:
 * - Minimum 55 independent sequences (Section 4)
 * - Minimum 1,000,000 bits per sequence (Section 4)
 * - Multi-sequence analysis with proportion passing and P-value uniformity tests
 *
 * This configuration uses NIST minimum requirements for standards compliance.
 */
object NistTestConfig {

    /**
     * Number of independent sequences to test (NIST Section 4 minimum).
     */
    const val sequenceCount: Int = 55

    /**
     * Bit length for each sequence (NIST Section 4 minimum).
     */
    const val sequenceLength: Int = 1_000_000

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
