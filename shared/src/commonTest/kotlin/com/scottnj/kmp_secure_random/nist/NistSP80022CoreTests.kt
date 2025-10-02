package com.scottnj.kmp_secure_random.nist

import com.scottnj.kmp_secure_random.SecureRandom
import com.scottnj.kmp_secure_random.createSecureRandom
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.ln
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * NIST SP 800-22 Core Statistical Test Suite for Random Number Generators.
 *
 * This test suite implements the core tests from NIST Special Publication 800-22
 * "A Statistical Test Suite for Random and Pseudorandom Number Generators for
 * Cryptographic Applications".
 *
 * Tests included:
 * 1. Frequency Test within a Block
 * 2. Runs Test
 * 3. Test for the Longest Run of Ones in a Block
 * 4. Binary Matrix Rank Test
 * 5. Cumulative Sums (Cusum) Test
 *
 * NIST-Compliant Testing Methodology:
 * - Tests multiple independent sequences (55-1000 depending on mode)
 * - Uses 1M bit sequences (Standard/Comprehensive) or 100K bits (Quick)
 * - Evaluates both proportion passing and P-value uniformity
 * - Significance level: α = 0.01 (99% confidence)
 *
 * Test modes configured via NIST_TEST_MODE environment variable:
 * - quick: 55 sequences × 100K bits (~2-3 min)
 * - standard: 100 sequences × 1M bits (~10-15 min)  [default]
 * - comprehensive: 1000 sequences × 1M bits (~60+ min)
 */
class NistSP80022CoreTests {

    private val secureRandom = createSecureRandom().getOrThrow()

    /**
     * Performs a test with automatic retry on failure.
     *
     * Statistical tests can occasionally fail due to random variance even with perfect randomness.
     * This helper retries failed tests once to distinguish transient statistical variance
     * from actual implementation problems.
     *
     * With STANDARD mode (100 sequences), the uniformity test has ~0.74% false positive rate.
     * Retrying reduces this to ~0.005% (1 in 20,000 runs).
     *
     * @param testName Human-readable name of the test
     * @param testLogic Function that performs the test and returns a NistTestResult
     * @return The result (either first attempt if passed, or retry attempt)
     */
    private fun performTestWithRetry(
        testName: String,
        testLogic: () -> NistTestResult
    ): NistTestResult {
        val firstAttempt = testLogic()

        if (firstAttempt.passed) {
            return firstAttempt
        }

        // Log the retry for transparency
        println()
        println("⚠️  NIST $testName failed on first attempt")
        println("    Proportion: ${firstAttempt.proportionPassing}/${NistTestConfig.sequenceCount}")
        println("    Uniformity P-value: ${firstAttempt.uniformityPValue}")
        println("    Retrying once to distinguish transient statistical variance from actual failures...")
        println()

        val retryAttempt = testLogic()

        if (retryAttempt.passed) {
            println("✅ NIST $testName passed on retry")
            println("   Proportion: ${retryAttempt.proportionPassing}/${NistTestConfig.sequenceCount}")
            println("   Uniformity P-value: ${retryAttempt.uniformityPValue}")
            println()
        } else {
            println("❌ NIST $testName failed on both attempts - likely indicates a real issue")
            println("   First attempt - Uniformity P-value: ${firstAttempt.uniformityPValue}")
            println("   Retry attempt - Uniformity P-value: ${retryAttempt.uniformityPValue}")
            println()
        }

        return retryAttempt
    }

    /**
     * NIST Test 1.2: Frequency Test within a Block
     *
     * Purpose: Determine whether the frequency of ones in an M-bit block is approximately M/2.
     *
     * The test divides the sequence into N non-overlapping blocks of length M.
     * For each block, the proportion of ones is determined. A chi-square statistic is computed
     * from these proportions and compared to the critical value.
     *
     * NIST Recommendation: M >= 20, M > 0.01*n, N < 100
     *
     * NIST-Compliant Testing:
     * - Tests multiple independent sequences (configured by NIST_TEST_MODE)
     * - Evaluates proportion passing and P-value uniformity
     * - Uses 1M bit sequences (Standard/Comprehensive) or 100K bits (Quick)
     */
    @Test
    fun testFrequencyWithinBlock() {
        val testName = "Frequency Test within a Block"

        // Perform test with automatic retry on failure
        val result = performTestWithRetry(testName) {
            val pValues = mutableListOf<Double>()

            // Test multiple independent sequences
            repeat(NistTestConfig.sequenceCount) { sequenceIndex ->
                val pValue = performSingleFrequencyWithinBlockTest(sequenceIndex + 1)
                pValues.add(pValue)
            }

            // Perform NIST multi-sequence analysis
            NistStatisticalAnalysis.analyzeMultipleSequences(testName, pValues)
        }

        // Print detailed report
        println(result.toReport())

        // Assert both proportion and uniformity tests pass
        assertTrue(
            result.passed,
            "NIST $testName failed after retry. " +
            "Proportion: ${result.proportionPassing}/${NistTestConfig.sequenceCount}, " +
            "Uniformity P-value: ${result.uniformityPValue}"
        )
    }

    /**
     * Performs a single Frequency Test within a Block.
     * @param sequenceIndex The sequence number for logging
     * @return P-value for this sequence
     */
    private fun performSingleFrequencyWithinBlockTest(sequenceIndex: Int): Double {
        val n = NistTestConfig.sequenceLength
        val M = 128 // Block size (NIST recommendation: M >= 20)
        val N = n / M // Number of blocks

        val rng = createSecureRandom().getOrThrow()
        val bytesResult = rng.nextBytes(n / 8)
        assertTrue(bytesResult.isSuccess, "Failed to generate random bytes in sequence $sequenceIndex")

        val bits = bytesToBits(bytesResult.getOrNull()!!)

        // Calculate proportion of ones in each block
        var chiSquare = 0.0
        for (i in 0 until N) {
            var blockOnes = 0
            for (j in 0 until M) {
                blockOnes += bits[i * M + j]
            }
            val pi = blockOnes.toDouble() / M
            chiSquare += (pi - 0.5).pow(2)
        }
        chiSquare *= 4.0 * M

        // Calculate P-value using incomplete gamma function approximation
        return igamc(N / 2.0, chiSquare / 2.0)
    }

    /**
     * NIST Test 1.3: Runs Test
     *
     * Purpose: Determine whether the number of runs of ones and zeros of various lengths
     * is as expected for a random sequence.
     *
     * A run is an uninterrupted sequence of identical bits. The test determines whether
     * the oscillation between zeros and ones is too fast or too slow.
     *
     * Prerequisite: The sequence must pass the Frequency (Monobit) Test.
     */
    @Test
    fun testRuns() {
        val testName = "Runs Test"

        // Perform test with automatic retry on failure
        val result = performTestWithRetry(testName) {
            val pValues = mutableListOf<Double>()

            // Test multiple independent sequences
            repeat(NistTestConfig.sequenceCount) { sequenceIndex ->
                val pValue = performSingleRunsTest(sequenceIndex + 1)
                pValues.add(pValue)
            }

            // Perform NIST multi-sequence analysis
            NistStatisticalAnalysis.analyzeMultipleSequences(testName, pValues)
        }

        // Print detailed report
        println(result.toReport())

        // Assert both proportion and uniformity tests pass
        assertTrue(
            result.passed,
            "NIST $testName failed after retry. " +
            "Proportion: ${result.proportionPassing}/${NistTestConfig.sequenceCount}, " +
            "Uniformity P-value: ${result.uniformityPValue}"
        )
    }

    /**
     * Performs a single Runs Test.
     * @param sequenceIndex The sequence number for logging
     * @return P-value for this sequence
     */
    private fun performSingleRunsTest(sequenceIndex: Int): Double {
        val n = NistTestConfig.sequenceLength

        val rng = createSecureRandom().getOrThrow()
        val bytesResult = rng.nextBytes(n / 8)
        assertTrue(bytesResult.isSuccess, "Failed to generate random bytes in sequence $sequenceIndex")

        val bits = bytesToBits(bytesResult.getOrNull()!!)

        // Calculate proportion of ones
        val ones = bits.sum()
        val pi = ones.toDouble() / n

        // Prerequisite check: |π - 0.5| < 2/√n
        val threshold = 2.0 / sqrt(n.toDouble())
        if (abs(pi - 0.5) >= threshold) {
            // Sequence doesn't pass prerequisite, return failure P-value
            return 0.0
        }

        // Count runs
        var runs = 1
        for (i in 1 until n) {
            if (bits[i] != bits[i - 1]) runs++
        }

        // Calculate test statistic
        val expectedRuns = 2.0 * n * pi * (1.0 - pi)
        val numerator = abs(runs - expectedRuns)
        val denominator = 2.0 * sqrt(2.0 * n) * pi * (1.0 - pi)
        val testStatistic = numerator / denominator

        // Calculate P-value using complementary error function approximation
        return erfc(testStatistic / sqrt(2.0))
    }

    /**
     * NIST Test 1.4: Test for the Longest Run of Ones in a Block
     *
     * Purpose: Determine whether the length of the longest run of ones within the tested
     * sequence is consistent with the length of the longest run of ones that would be
     * expected in a random sequence.
     *
     * The test divides the sequence into blocks and examines the longest run of ones
     * within each block. The test statistic follows a chi-square distribution.
     *
     * Note: This test uses fixed parameters (n=75,000, M=10,000) from NIST table,
     * independent of configured sequence length for proper statistical properties.
     */
    @Test
    fun testLongestRunOfOnes() {
        val testName = "Longest Run of Ones in a Block Test"

        // Perform test with automatic retry on failure
        val result = performTestWithRetry(testName) {
            val pValues = mutableListOf<Double>()

            // Test multiple independent sequences
            repeat(NistTestConfig.sequenceCount) { sequenceIndex ->
                val pValue = performSingleLongestRunTest(sequenceIndex + 1)
                pValues.add(pValue)
            }

            // Perform NIST multi-sequence analysis
            NistStatisticalAnalysis.analyzeMultipleSequences(testName, pValues)
        }

        // Print detailed report
        println(result.toReport())

        // Assert both proportion and uniformity tests pass
        assertTrue(
            result.passed,
            "NIST $testName failed after retry. " +
            "Proportion: ${result.proportionPassing}/${NistTestConfig.sequenceCount}, " +
            "Uniformity P-value: ${result.uniformityPValue}"
        )
    }

    /**
     * Performs a single Longest Run of Ones Test.
     * Uses parameters for n = 75,000 (K=6, M=10,000, N=7.5) from NIST table 2-4
     * @param sequenceIndex The sequence number for logging
     * @return P-value for this sequence
     */
    private fun performSingleLongestRunTest(sequenceIndex: Int): Double {
        val n = 6272 // Total bits (must be divisible by M)
        val M = 128 // Block size
        val N = n / M // Number of blocks = 49
        val K = 5 // Number of categories (for M=128, from NIST table)

        // Probability distribution for M=128 (from NIST SP 800-22 Table 2-3)
        // Categories: ≤4, 5, 6, 7, 8, ≥9
        val probabilities = doubleArrayOf(0.1174, 0.2430, 0.2493, 0.1752, 0.1027, 0.1124)

        val rng = createSecureRandom().getOrThrow()
        val bytesResult = rng.nextBytes(n / 8)
        assertTrue(bytesResult.isSuccess, "Failed to generate random bytes in sequence $sequenceIndex")

        val bits = bytesToBits(bytesResult.getOrNull()!!)

        // Count frequency of longest runs in each block
        val frequencies = IntArray(K + 1) // Categories: ≤4, 5, 6, 7, 8, ≥9

        for (i in 0 until N) {
            var longestRun = 0
            var currentRun = 0

            for (j in 0 until M) {
                val bit = bits[i * M + j]
                if (bit == 1) {
                    currentRun++
                    if (currentRun > longestRun) longestRun = currentRun
                } else {
                    currentRun = 0
                }
            }

            // Categorize the longest run (from NIST table for M=128)
            val category = when {
                longestRun <= 4 -> 0
                longestRun == 5 -> 1
                longestRun == 6 -> 2
                longestRun == 7 -> 3
                longestRun == 8 -> 4
                else -> 5 // ≥9
            }
            frequencies[category]++
        }

        // Calculate chi-square statistic
        var chiSquare = 0.0
        for (i in 0..K) {
            val expected = N * probabilities[i]
            chiSquare += (frequencies[i] - expected).pow(2) / expected
        }

        // Calculate P-value (K degrees of freedom)
        return igamc(K / 2.0, chiSquare / 2.0)
    }

    /**
     * NIST Test 1.5: Binary Matrix Rank Test
     *
     * Purpose: Check for linear dependence among fixed-length substrings of the original sequence.
     *
     * The test divides the sequence into M×Q matrices and determines the rank of each matrix.
     * The distribution of ranks is compared to the theoretical distribution.
     */
    @Test
    fun testBinaryMatrixRank() {
        val testName = "Binary Matrix Rank Test"

        // Perform test with automatic retry on failure
        val result = performTestWithRetry(testName) {
            val pValues = mutableListOf<Double>()

            // Test multiple independent sequences
            repeat(NistTestConfig.sequenceCount) { sequenceIndex ->
                val pValue = performSingleMatrixRankTest(sequenceIndex + 1)
                pValues.add(pValue)
            }

            // Perform NIST multi-sequence analysis
            NistStatisticalAnalysis.analyzeMultipleSequences(testName, pValues)
        }

        // Print detailed report
        println(result.toReport())

        // Assert both proportion and uniformity tests pass
        assertTrue(
            result.passed,
            "NIST $testName failed after retry. " +
            "Proportion: ${result.proportionPassing}/${NistTestConfig.sequenceCount}, " +
            "Uniformity P-value: ${result.uniformityPValue}"
        )
    }

    /**
     * Performs a single Binary Matrix Rank Test.
     * @param sequenceIndex The sequence number for logging
     * @return P-value for this sequence
     */
    private fun performSingleMatrixRankTest(sequenceIndex: Int): Double {
        val M = 32 // Number of rows
        val Q = 32 // Number of columns
        val n = 38400 // Total bits (must be divisible by M*Q)
        val N = n / (M * Q) // Number of matrices

        val rng = createSecureRandom().getOrThrow()
        val bytesResult = rng.nextBytes(n / 8)
        assertTrue(bytesResult.isSuccess, "Failed to generate random bytes in sequence $sequenceIndex")

        val bits = bytesToBits(bytesResult.getOrNull()!!)

        // Count matrices by rank
        var fullRank = 0 // Rank = M
        var rankMinusOne = 0 // Rank = M-1
        var other = 0 // Rank < M-1

        for (i in 0 until N) {
            val matrix = Array(M) { IntArray(Q) }

            // Fill matrix from bits
            for (row in 0 until M) {
                for (col in 0 until Q) {
                    val bitIndex = i * M * Q + row * Q + col
                    matrix[row][col] = bits[bitIndex]
                }
            }

            val rank = computeBinaryRank(matrix, M, Q)

            when (rank) {
                M -> fullRank++
                M - 1 -> rankMinusOne++
                else -> other++
            }
        }

        // Theoretical probabilities for M=Q=32
        val probFullRank = 0.2888
        val probRankMinusOne = 0.5776
        val probOther = 0.1336

        // Calculate chi-square statistic
        val chiSquare =
            (fullRank - N * probFullRank).pow(2) / (N * probFullRank) +
            (rankMinusOne - N * probRankMinusOne).pow(2) / (N * probRankMinusOne) +
            (other - N * probOther).pow(2) / (N * probOther)

        // Calculate P-value (2 degrees of freedom)
        return kotlin.math.exp(-chiSquare / 2.0)
    }

    /**
     * NIST Test 1.6: Cumulative Sums (Cusum) Test
     *
     * Purpose: Determine whether the cumulative sum of the partial sequences occurring
     * in the tested sequence is too large or too small relative to the expected behavior
     * of that cumulative sum for random sequences.
     *
     * The test converts bits to -1 and +1, then computes cumulative sums.
     * Two modes: forward and backward.
     */
    @Test
    fun testCumulativeSums() {
        val testName = "Cumulative Sums (Cusum) Test"

        // Perform test with automatic retry on failure
        val result = performTestWithRetry(testName) {
            val pValues = mutableListOf<Double>()

            // Create single RNG instance shared across all sequences
            val rng = createSecureRandom().getOrThrow()

            // Test multiple sequences
            repeat(NistTestConfig.sequenceCount) { sequenceIndex ->
                val pValue = performSingleCusumTest(rng, sequenceIndex + 1)
                pValues.add(pValue)
            }

            // Perform NIST multi-sequence analysis
            NistStatisticalAnalysis.analyzeMultipleSequences(testName, pValues)
        }

        // Print detailed report
        println(result.toReport())

        // Assert both proportion and uniformity tests pass
        assertTrue(
            result.passed,
            "NIST $testName failed after retry. " +
            "Proportion: ${result.proportionPassing}/${NistTestConfig.sequenceCount}, " +
            "Uniformity P-value: ${result.uniformityPValue}"
        )
    }

    /**
     * Performs a single Cumulative Sums Test.
     * Tests both forward and backward modes.
     * @param rng The SecureRandom instance to use
     * @param sequenceIndex The sequence number for logging
     * @return P-value for this sequence (minimum of forward and backward)
     */
    private fun performSingleCusumTest(rng: SecureRandom, sequenceIndex: Int): Double {
        val n = NistTestConfig.sequenceLength

        val bytesResult = rng.nextBytes(n / 8)
        assertTrue(bytesResult.isSuccess, "Failed to generate random bytes in sequence $sequenceIndex")

        val bits = bytesToBits(bytesResult.getOrNull()!!)

        // Convert bits to -1 and +1
        val values = bits.map { if (it == 0) -1 else 1 }

        // Forward mode
        var maxForward = 0
        var cumSum = 0
        for (value in values) {
            cumSum += value
            if (abs(cumSum) > maxForward) maxForward = abs(cumSum)
        }

        // Backward mode
        var maxBackward = 0
        cumSum = 0
        for (i in values.size - 1 downTo 0) {
            cumSum += values[i]
            if (abs(cumSum) > maxBackward) maxBackward = abs(cumSum)
        }

        // Calculate P-values for both modes
        val pValueForward = calculateCusumPValue(maxForward, n)
        val pValueBackward = calculateCusumPValue(maxBackward, n)

        // Use the minimum P-value (most conservative)
        return minOf(pValueForward, pValueBackward)
    }

    // ==================== Helper Functions ====================

    /**
     * Convert byte array to bit array (0s and 1s).
     */
    private fun bytesToBits(bytes: ByteArray): IntArray {
        val bits = IntArray(bytes.size * 8)
        for (i in bytes.indices) {
            val byte = bytes[i].toInt() and 0xFF
            for (j in 0..7) {
                bits[i * 8 + j] = (byte shr (7 - j)) and 1
            }
        }
        return bits
    }

    /**
     * Compute binary rank of a matrix using Gaussian elimination.
     */
    private fun computeBinaryRank(matrix: Array<IntArray>, rows: Int, cols: Int): Int {
        val m = matrix.map { it.copyOf() }.toTypedArray()
        var rank = 0

        for (col in 0 until minOf(rows, cols)) {
            // Find pivot
            var pivotRow = -1
            for (row in rank until rows) {
                if (m[row][col] == 1) {
                    pivotRow = row
                    break
                }
            }

            if (pivotRow == -1) continue

            // Swap rows
            if (pivotRow != rank) {
                val temp = m[rank]
                m[rank] = m[pivotRow]
                m[pivotRow] = temp
            }

            // Eliminate
            for (row in 0 until rows) {
                if (row != rank && m[row][col] == 1) {
                    for (c in 0 until cols) {
                        m[row][c] = m[row][c] xor m[rank][c]
                    }
                }
            }

            rank++
        }

        return rank
    }

    /**
     * Calculate P-value for Cumulative Sums Test.
     * Implements the exact formula from NIST SP 800-22 Rev 1a Section 2.13.7.
     *
     * This uses the correct double summation formula from the NIST reference implementation.
     * Reference: https://github.com/kravietz/nist-sts/blob/master/cusum.c
     */
    private fun calculateCusumPValue(z: Int, n: Int): Double {
        if (z == 0) return 1.0
        if (z >= n) return 0.0

        val sqrtN = sqrt(n.toDouble())

        // Sum1: sum from k = (-n/z+1)/4 to (n/z-1)/4
        // Formula: Φ((4k+1)z/√n) - Φ((4k-1)z/√n)
        var sum1 = 0.0
        val k1Start = ((-n.toDouble() / z + 1.0) / 4.0).toInt()
        val k1End = ((n.toDouble() / z - 1.0) / 4.0).toInt()
        for (k in k1Start..k1End) {
            sum1 += normalCDF(((4 * k + 1) * z).toDouble() / sqrtN)
            sum1 -= normalCDF(((4 * k - 1) * z).toDouble() / sqrtN)
        }

        // Sum2: sum from k = (-n/z-3)/4 to (n/z-1)/4
        // Formula: Φ((4k+3)z/√n) - Φ((4k+1)z/√n)
        var sum2 = 0.0
        val k2Start = ((-n.toDouble() / z - 3.0) / 4.0).toInt()
        val k2End = ((n.toDouble() / z - 1.0) / 4.0).toInt()
        for (k in k2Start..k2End) {
            sum2 += normalCDF(((4 * k + 3) * z).toDouble() / sqrtN)
            sum2 -= normalCDF(((4 * k + 1) * z).toDouble() / sqrtN)
        }

        // NIST formula: P-value = 1 - sum1 + sum2
        val pValue = 1.0 - sum1 + sum2

        // Ensure P-value is in valid range [0, 1]
        return maxOf(0.0, minOf(1.0, pValue))
    }

    /**
     * Normal cumulative distribution function.
     */
    private fun normalCDF(x: Double): Double {
        return 0.5 * (1.0 + erf(x / sqrt(2.0)))
    }

    /**
     * Error function approximation.
     */
    private fun erf(x: Double): Double {
        // Abramowitz and Stegun approximation
        val sign = if (x >= 0) 1.0 else -1.0
        val absX = abs(x)

        val a1 = 0.254829592
        val a2 = -0.284496736
        val a3 = 1.421413741
        val a4 = -1.453152027
        val a5 = 1.061405429
        val p = 0.3275911

        val t = 1.0 / (1.0 + p * absX)
        val y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * kotlin.math.exp(-absX * absX)

        return sign * y
    }

    /**
     * Complementary error function.
     */
    private fun erfc(x: Double): Double {
        return 1.0 - erf(x)
    }

    /**
     * Incomplete gamma function (upper) approximation.
     * Used for chi-square P-value calculation.
     */
    private fun igamc(a: Double, x: Double): Double {
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
                val t = abs((ans - r) / r)
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