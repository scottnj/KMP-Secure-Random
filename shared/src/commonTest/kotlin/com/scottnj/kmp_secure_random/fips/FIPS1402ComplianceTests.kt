package com.scottnj.kmp_secure_random.fips

import com.scottnj.kmp_secure_random.createSecureRandom
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * FIPS 140-2 Statistical Test Suite for Random Number Generators.
 *
 * This test suite implements the four randomness tests specified in
 * FIPS PUB 140-2 "Security Requirements for Cryptographic Modules".
 *
 * Tests included:
 * 1. Monobit Test
 * 2. Poker Test
 * 3. Runs Test
 * 4. Long Run Test
 *
 * These tests use a 20,000-bit sequence and have strict pass/fail criteria.
 * All four tests must pass for FIPS 140-2 compliance.
 */
class FIPS1402ComplianceTests {

    private val secureRandom = createSecureRandom().getOrThrow()
    private val testSequenceLength = 20000 // FIPS 140-2 standard test length

    /**
     * FIPS 140-2 Test 1: Monobit Test
     *
     * Purpose: Determine if the number of 1s and 0s in the sequence are approximately equal.
     *
     * Pass criteria: 9,725 < (count of 1s) < 10,275
     *
     * This is a stricter version of the NIST Monobit Frequency Test.
     */
    @Test
    fun testFipsMonobit() {
        val results = mutableListOf<Pair<Boolean, String>>()
        val iterations = 5
        var passes = 0

        repeat(iterations) { iteration ->
            val result = performSingleMonobitTest(iteration + 1)
            results.add(result)
            if (result.first) passes++
        }

        // Print all results for debugging
        results.forEachIndexed { index, (passed, details) ->
            val status = if (passed) "PASS" else "FAIL"
            println("FIPS 140-2 Monobit Test ${index + 1}: $details [$status]")
        }

        // Require majority of tests to pass (at least 3/5)
        assertTrue(
            passes >= 3,
            "FIPS 140-2 Monobit Test failed too often: $passes/$iterations passed (need 3). " +
            "Multiple failures indicate non-compliance with FIPS 140-2 requirements."
        )
    }

    /**
     * Performs a single FIPS 140-2 Monobit Test.
     */
    private fun performSingleMonobitTest(iteration: Int): Pair<Boolean, String> {
        val bytesResult = secureRandom.nextBytes(testSequenceLength / 8)
        assertTrue(bytesResult.isSuccess, "Failed to generate random bytes in iteration $iteration")

        val bits = bytesToBits(bytesResult.getOrNull()!!)

        // Count ones
        val ones = bits.sum()

        // FIPS 140-2 criteria: 9,725 < ones < 10,275
        val passed = ones in 9726..10274

        val details = "ones=$ones, required=9726-10274"
        return Pair(passed, details)
    }

    /**
     * FIPS 140-2 Test 2: Poker Test
     *
     * Purpose: Divide the sequence into 5,000 4-bit segments and determine if the
     * distribution of 16 possible values is approximately uniform.
     *
     * Pass criteria: 2.16 < X < 46.17
     * where X = (16/5000) * Σ(f_i²) - 5000
     * f_i is the number of occurrences of the i-th 4-bit value
     */
    @Test
    fun testFipsPoker() {
        val results = mutableListOf<Pair<Boolean, String>>()
        val iterations = 5
        var passes = 0

        repeat(iterations) { iteration ->
            val result = performSinglePokerTest(iteration + 1)
            results.add(result)
            if (result.first) passes++
        }

        // Print all results for debugging
        results.forEachIndexed { index, (passed, details) ->
            val status = if (passed) "PASS" else "FAIL"
            println("FIPS 140-2 Poker Test ${index + 1}: $details [$status]")
        }

        // Require majority of tests to pass (at least 3/5)
        assertTrue(
            passes >= 3,
            "FIPS 140-2 Poker Test failed too often: $passes/$iterations passed (need 3). " +
            "Multiple failures indicate non-uniform 4-bit pattern distribution."
        )
    }

    /**
     * Performs a single FIPS 140-2 Poker Test.
     */
    private fun performSinglePokerTest(iteration: Int): Pair<Boolean, String> {
        val bytesResult = secureRandom.nextBytes(testSequenceLength / 8)
        assertTrue(bytesResult.isSuccess, "Failed to generate random bytes in iteration $iteration")

        val bits = bytesToBits(bytesResult.getOrNull()!!)

        // Divide into 5,000 4-bit segments
        val segments = 5000
        val frequencies = IntArray(16) // 16 possible 4-bit values (0000 to 1111)

        for (i in 0 until segments) {
            val startBit = i * 4
            val segment = bits[startBit] * 8 + bits[startBit + 1] * 4 + bits[startBit + 2] * 2 + bits[startBit + 3]
            frequencies[segment]++
        }

        // Calculate X statistic
        var sumOfSquares = 0
        for (freq in frequencies) {
            sumOfSquares += freq * freq
        }
        val X = (16.0 / segments) * sumOfSquares - segments

        // FIPS 140-2 criteria: 2.16 < X < 46.17
        val passed = X > 2.16 && X < 46.17

        val details = "X=$X, required=2.16-46.17, freq=${frequencies.toList()}"
        return Pair(passed, details)
    }

    /**
     * FIPS 140-2 Test 3: Runs Test
     *
     * Purpose: Determine if the number of runs of various lengths is as expected.
     *
     * A run is a sequence of identical bits. The test checks runs of length 1, 2, 3, 4, 5, and 6+.
     *
     * Pass criteria (for both 0-runs and 1-runs):
     * - Length 1: 2,315 ≤ count ≤ 2,685
     * - Length 2: 1,114 ≤ count ≤ 1,386
     * - Length 3: 527 ≤ count ≤ 723
     * - Length 4: 240 ≤ count ≤ 384
     * - Length 5: 103 ≤ count ≤ 209
     * - Length 6+: 103 ≤ count ≤ 209
     */
    @Test
    fun testFipsRuns() {
        val results = mutableListOf<Pair<Boolean, String>>()
        val iterations = 5
        var passes = 0

        repeat(iterations) { iteration ->
            val result = performSingleRunsTest(iteration + 1)
            results.add(result)
            if (result.first) passes++
        }

        // Print all results for debugging
        results.forEachIndexed { index, (passed, details) ->
            val status = if (passed) "PASS" else "FAIL"
            println("FIPS 140-2 Runs Test ${index + 1}: $details [$status]")
        }

        // Require majority of tests to pass (at least 3/5)
        assertTrue(
            passes >= 3,
            "FIPS 140-2 Runs Test failed too often: $passes/$iterations passed (need 3). " +
            "Multiple failures indicate non-random run distribution."
        )
    }

    /**
     * Performs a single FIPS 140-2 Runs Test.
     */
    private fun performSingleRunsTest(iteration: Int): Pair<Boolean, String> {
        val bytesResult = secureRandom.nextBytes(testSequenceLength / 8)
        assertTrue(bytesResult.isSuccess, "Failed to generate random bytes in iteration $iteration")

        val bits = bytesToBits(bytesResult.getOrNull()!!)

        // Count runs of different lengths for both 0s and 1s
        val runsOf0 = IntArray(7) // Indices 1-6 for run lengths, 6 includes all runs >= 6
        val runsOf1 = IntArray(7)

        var i = 0
        while (i < bits.size) {
            val bit = bits[i]
            var runLength = 1

            // Count consecutive identical bits
            while (i + runLength < bits.size && bits[i + runLength] == bit) {
                runLength++
            }

            // Categorize run length (cap at 6+)
            val category = minOf(runLength, 6)

            if (bit == 0) {
                runsOf0[category]++
            } else {
                runsOf1[category]++
            }

            i += runLength
        }

        // FIPS 140-2 criteria for each run length
        val criteria = arrayOf(
            Pair(0, 0),     // Index 0 not used
            Pair(2315, 2685), // Length 1
            Pair(1114, 1386), // Length 2
            Pair(527, 723),   // Length 3
            Pair(240, 384),   // Length 4
            Pair(103, 209),   // Length 5
            Pair(103, 209)    // Length 6+
        )

        // Check all run lengths for both 0s and 1s
        var passed = true
        val failedChecks = mutableListOf<String>()

        for (length in 1..6) {
            val (min, max) = criteria[length]
            if (runsOf0[length] !in min..max) {
                passed = false
                failedChecks.add("0-runs[len=$length]=${runsOf0[length]} not in $min-$max")
            }
            if (runsOf1[length] !in min..max) {
                passed = false
                failedChecks.add("1-runs[len=$length]=${runsOf1[length]} not in $min-$max")
            }
        }

        val details = if (passed) {
            "0-runs=${runsOf0.sliceArray(1..6).toList()}, 1-runs=${runsOf1.sliceArray(1..6).toList()}"
        } else {
            "FAILED: ${failedChecks.joinToString(", ")}"
        }

        return Pair(passed, details)
    }

    /**
     * FIPS 140-2 Test 4: Long Run Test
     *
     * Purpose: Ensure there are no runs of 26 or more consecutive identical bits.
     *
     * Pass criteria: No runs of length >= 26
     *
     * This is a critical test - even a single long run indicates a serious problem.
     */
    @Test
    fun testFipsLongRun() {
        val results = mutableListOf<Pair<Boolean, String>>()
        val iterations = 5
        var passes = 0

        repeat(iterations) { iteration ->
            val result = performSingleLongRunTest(iteration + 1)
            results.add(result)
            if (result.first) passes++
        }

        // Print all results for debugging
        results.forEachIndexed { index, (passed, details) ->
            val status = if (passed) "PASS" else "FAIL"
            println("FIPS 140-2 Long Run Test ${index + 1}: $details [$status]")
        }

        // Require ALL tests to pass for this critical test
        assertTrue(
            passes == iterations,
            "FIPS 140-2 Long Run Test failed: $passes/$iterations passed. " +
            "Long runs indicate a CRITICAL failure in random generation."
        )
    }

    /**
     * Performs a single FIPS 140-2 Long Run Test.
     */
    private fun performSingleLongRunTest(iteration: Int): Pair<Boolean, String> {
        val bytesResult = secureRandom.nextBytes(testSequenceLength / 8)
        assertTrue(bytesResult.isSuccess, "Failed to generate random bytes in iteration $iteration")

        val bits = bytesToBits(bytesResult.getOrNull()!!)

        // Find longest run
        var maxRunLength = 1
        var currentRunLength = 1

        for (i in 1 until bits.size) {
            if (bits[i] == bits[i - 1]) {
                currentRunLength++
                if (currentRunLength > maxRunLength) {
                    maxRunLength = currentRunLength
                }
            } else {
                currentRunLength = 1
            }
        }

        // FIPS 140-2 criteria: No run >= 26
        val passed = maxRunLength < 26

        val details = "maxRunLength=$maxRunLength, required=<26"
        return Pair(passed, details)
    }

    /**
     * Comprehensive FIPS 140-2 Compliance Test
     *
     * Runs all four FIPS 140-2 tests and reports overall compliance.
     * A generator must pass all four tests to be FIPS 140-2 compliant.
     */
    @Test
    fun testFips1402FullCompliance() {
        println("=" .repeat(70))
        println("FIPS 140-2 FULL COMPLIANCE TEST")
        println("=" .repeat(70))

        val bytesResult = secureRandom.nextBytes(testSequenceLength / 8)
        assertTrue(bytesResult.isSuccess, "Failed to generate test sequence")

        val bits = bytesToBits(bytesResult.getOrNull()!!)

        // Test 1: Monobit
        val ones = bits.sum()
        val monobitPass = ones in 9726..10274
        println("1. Monobit Test: ${if (monobitPass) "✓ PASS" else "✗ FAIL"} (ones=$ones, required=9726-10274)")

        // Test 2: Poker
        val frequencies = IntArray(16)
        for (i in 0 until 5000) {
            val startBit = i * 4
            val segment = bits[startBit] * 8 + bits[startBit + 1] * 4 + bits[startBit + 2] * 2 + bits[startBit + 3]
            frequencies[segment]++
        }
        var sumOfSquares = 0
        for (freq in frequencies) {
            sumOfSquares += freq * freq
        }
        val X = (16.0 / 5000) * sumOfSquares - 5000
        val pokerPass = X > 2.16 && X < 46.17
        println("2. Poker Test: ${if (pokerPass) "✓ PASS" else "✗ FAIL"} (X=$X, required=2.16-46.17)")

        // Test 3: Runs
        val runsOf0 = IntArray(7)
        val runsOf1 = IntArray(7)
        var i = 0
        while (i < bits.size) {
            val bit = bits[i]
            var runLength = 1
            while (i + runLength < bits.size && bits[i + runLength] == bit) {
                runLength++
            }
            val category = minOf(runLength, 6)
            if (bit == 0) runsOf0[category]++ else runsOf1[category]++
            i += runLength
        }

        val criteria = arrayOf(
            Pair(0, 0), Pair(2315, 2685), Pair(1114, 1386),
            Pair(527, 723), Pair(240, 384), Pair(103, 209), Pair(103, 209)
        )

        var runsPass = true
        for (length in 1..6) {
            val (min, max) = criteria[length]
            if (runsOf0[length] !in min..max || runsOf1[length] !in min..max) {
                runsPass = false
                break
            }
        }
        println("3. Runs Test: ${if (runsPass) "✓ PASS" else "✗ FAIL"}")
        println("   0-runs: ${runsOf0.sliceArray(1..6).toList()}")
        println("   1-runs: ${runsOf1.sliceArray(1..6).toList()}")

        // Test 4: Long Run
        var maxRunLength = 1
        var currentRunLength = 1
        for (j in 1 until bits.size) {
            if (bits[j] == bits[j - 1]) {
                currentRunLength++
                if (currentRunLength > maxRunLength) maxRunLength = currentRunLength
            } else {
                currentRunLength = 1
            }
        }
        val longRunPass = maxRunLength < 26
        println("4. Long Run Test: ${if (longRunPass) "✓ PASS" else "✗ FAIL"} (maxRun=$maxRunLength, required=<26)")

        // Overall compliance
        val fullCompliance = monobitPass && pokerPass && runsPass && longRunPass
        println("=" .repeat(70))
        println("OVERALL FIPS 140-2 COMPLIANCE: ${if (fullCompliance) "✓ COMPLIANT" else "✗ NON-COMPLIANT"}")
        println("=" .repeat(70))

        assertTrue(fullCompliance, "Random number generator is not FIPS 140-2 compliant")
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
}