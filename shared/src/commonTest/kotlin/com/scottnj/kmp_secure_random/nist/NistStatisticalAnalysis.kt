package com.scottnj.kmp_secure_random.nist

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Statistical analysis utilities for NIST SP 800-22 multi-sequence testing.
 *
 * Implements:
 * - P-value uniformity test (NIST Section 4.2.2)
 * - Proportion passing test (NIST Section 4.2.1)
 */
object NistStatisticalAnalysis {

    /**
     * Perform complete NIST multi-sequence analysis.
     *
     * Tests both:
     * 1. Proportion of sequences passing (within confidence interval)
     * 2. Uniformity of P-value distribution (chi-square test)
     *
     * @param testName Name of the NIST test being analyzed
     * @param pValues List of P-values from multiple independent sequences
     * @return NistTestResult containing all analysis results
     */
    fun analyzeMultipleSequences(testName: String, pValues: List<Double>): NistTestResult {
        require(pValues.isNotEmpty()) { "P-values list cannot be empty" }
        require(pValues.all { it in 0.0..1.0 }) { "All P-values must be in range [0, 1]" }

        val proportionPassing = pValues.count { it >= NistTestConfig.SIGNIFICANCE_LEVEL }
        val uniformityPValue = calculateUniformityPValue(pValues)
        val proportionTestPassed = checkProportionPassing(proportionPassing, pValues.size)
        val uniformityTestPassed = uniformityPValue >= NistTestConfig.UNIFORMITY_MIN_PVALUE

        return NistTestResult(
            testName = testName,
            pValues = pValues,
            proportionPassing = proportionPassing,
            proportionPValue = if (proportionTestPassed) 1.0 else 0.0, // Simplified for binary result
            uniformityPValue = uniformityPValue,
            passed = proportionTestPassed && uniformityTestPassed
        )
    }

    /**
     * Calculate P-value for uniformity test (NIST Section 4.2.2).
     *
     * Uses chi-square goodness-of-fit test to determine if P-values are uniformly
     * distributed across 10 equal bins [0.0-0.1, 0.1-0.2, ..., 0.9-1.0].
     *
     * Formula:
     * χ² = Σ[(observed_i - expected)² / expected]
     *
     * where expected = n / 10 for n sequences
     *
     * @param pValues List of P-values from multiple sequences
     * @return P-value for chi-square test (9 degrees of freedom)
     */
    fun calculateUniformityPValue(pValues: List<Double>): Double {
        val n = pValues.size
        val bins = IntArray(NistTestConfig.UNIFORMITY_BINS)

        // Count P-values in each bin
        for (p in pValues) {
            val binIndex = (p * NistTestConfig.UNIFORMITY_BINS).toInt()
                .coerceIn(0, NistTestConfig.UNIFORMITY_BINS - 1)
            bins[binIndex]++
        }

        // Expected count per bin (uniform distribution)
        val expected = n.toDouble() / NistTestConfig.UNIFORMITY_BINS

        // Calculate chi-square statistic
        var chiSquare = 0.0
        for (count in bins) {
            val diff = count - expected
            chiSquare += (diff * diff) / expected
        }

        // Calculate P-value using incomplete gamma function
        // Degrees of freedom = bins - 1 = 9
        val df = NistTestConfig.UNIFORMITY_BINS - 1
        return igamc(df / 2.0, chiSquare / 2.0)
    }

    /**
     * Check if proportion of passing sequences falls within confidence interval.
     *
     * Formula from NIST Section 4.2.1:
     * Confidence interval = p̂ ± 3√(p̂(1-p̂)/m)
     * where p̂ = 1 - α = 0.99 and m = number of sequences
     *
     * @param passing Number of sequences that passed (P-value ≥ α)
     * @param total Total number of sequences tested
     * @return True if proportion falls within confidence interval
     */
    fun checkProportionPassing(passing: Int, total: Int): Boolean {
        val proportion = passing.toDouble() / total
        val (lower, upper) = NistTestConfig.getProportionConfidenceInterval()
        return proportion >= lower && proportion <= upper
    }

    /**
     * Incomplete gamma function (upper) approximation.
     * Used for chi-square P-value calculation.
     *
     * This is the same implementation used in NistSP80022CoreTests and NistSP80022AdvancedTests.
     */
    fun igamc(a: Double, x: Double): Double {
        if (x <= 0.0 || a <= 0.0) return 1.0
        if (x < a + 1.0) return 1.0 - igam(a, x)

        // Continued fraction approximation
        var ax = a * ln(x) - x - lnGamma(a)
        if (ax < -709.0) return 0.0
        ax = kotlin.math.exp(ax)

        var y = 1.0 - a
        var z = x + y + 1.0
        var c = 0.0
        var pkm2 = 1.0
        var qkm2 = x
        var pkm1 = x + 1.0
        var qkm1 = z * x
        var ans = pkm1 / qkm1

        for (n in 1..100) {
            c += 1.0
            y += 1.0
            z += 2.0
            val yc = y * c
            val pk = pkm1 * z - pkm2 * yc
            val qk = qkm1 * z - qkm2 * yc

            if (qk != 0.0) {
                val r = pk / qk
                val t = kotlin.math.abs((ans - r) / r)
                ans = r
                if (t <= 1e-9) break
            }

            pkm2 = pkm1
            pkm1 = pk
            qkm2 = qkm1
            qkm1 = qk
        }

        return ans * ax
    }

    /**
     * Incomplete gamma function (lower) approximation.
     */
    private fun igam(a: Double, x: Double): Double {
        if (x <= 0.0 || a <= 0.0) return 0.0
        if (x > a + 1.0) return 1.0 - igamc(a, x)

        var ax = a * ln(x) - x - lnGamma(a)
        if (ax < -709.0) return 0.0
        ax = kotlin.math.exp(ax)

        var r = a
        var c = 1.0
        var ans = 1.0

        for (n in 1..100) {
            r += 1.0
            c *= x / r
            ans += c
            if (c / ans <= 1e-9) break
        }

        return ans * ax / a
    }

    /**
     * Natural logarithm of gamma function.
     * Stirling's approximation.
     */
    private fun lnGamma(x: Double): Double {
        val tmp = x + 5.5 - (x + 0.5) * ln(x + 5.5)
        val ser = 1.0 + 76.18009173 / (x + 1.0) - 86.50532033 / (x + 2.0) +
                24.01409822 / (x + 3.0) - 1.231739516 / (x + 4.0) +
                0.00120858003 / (x + 5.0) - 0.00000536382 / (x + 6.0)
        return -tmp + ln(2.50662827465 * ser / x)
    }
}
