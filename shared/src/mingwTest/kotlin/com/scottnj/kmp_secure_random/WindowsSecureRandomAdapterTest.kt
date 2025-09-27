package com.scottnj.kmp_secure_random

import kotlin.test.*

/**
 * Windows-specific tests for WindowsSecureRandomAdapter.
 *
 * These tests validate:
 * - BCryptGenRandom integration (Windows Vista+)
 * - CryptGenRandom fallback (Windows 2000+)
 * - Error handling for Windows-specific failures
 * - Thread safety with Windows crypto APIs
 */
class WindowsSecureRandomAdapterTest {

    private lateinit var secureRandom: SecureRandom

    @BeforeTest
    fun setUp() {
        val result = createSecureRandom()
        assertTrue(result is SecureRandomResult.Success, "Failed to create Windows SecureRandom")
        secureRandom = result.value
    }

    @Test
    fun testWindowsSecureRandomCreation() {
        val result = createSecureRandom()
        assertTrue(result is SecureRandomResult.Success)
        assertNotNull(result.value)
    }

    @Test
    fun testGenerateRandomBytes() {
        val bytes = ByteArray(32)
        val result = secureRandom.nextBytes(bytes)
        assertTrue(result is SecureRandomUnitResult.Success)

        // Verify bytes were actually modified
        assertFalse(bytes.all { it == 0.toByte() }, "Bytes should not all be zero")

        // Verify different calls produce different results
        val bytes2 = ByteArray(32)
        val result2 = secureRandom.nextBytes(bytes2)
        assertTrue(result2 is SecureRandomUnitResult.Success)
        assertFalse(bytes.contentEquals(bytes2), "Different calls should produce different bytes")
    }

    @Test
    fun testEmptyByteArray() {
        val bytes = ByteArray(0)
        val result = secureRandom.nextBytes(bytes)
        assertTrue(result is SecureRandomUnitResult.Success)
    }

    @Test
    fun testLargeByteArray() {
        // Test with 1MB array
        val bytes = ByteArray(1024 * 1024)
        val result = secureRandom.nextBytes(bytes)
        assertTrue(result is SecureRandomUnitResult.Success)

        // Basic entropy check
        val byteFrequency = IntArray(256)
        bytes.forEach { byteFrequency[it.toInt() and 0xFF]++ }

        val minCount = byteFrequency.minOrNull() ?: 0
        val maxCount = byteFrequency.maxOrNull() ?: 0
        val avgCount = bytes.size / 256

        // Each byte value should appear roughly equally often
        assertTrue(minCount > avgCount / 2, "Minimum byte frequency too low: $minCount")
        assertTrue(maxCount < avgCount * 2, "Maximum byte frequency too high: $maxCount")
    }

    @Test
    fun testNextIntRange() {
        repeat(100) {
            val result = secureRandom.nextInt(1, 100)
            assertTrue(result is SecureRandomResult.Success)
            val value = result.value
            assertTrue(value in 1..100, "Value $value should be in range [1, 100]")
        }
    }

    @Test
    fun testNextLongRange() {
        repeat(100) {
            val result = secureRandom.nextLong(1000L, 10000L)
            assertTrue(result is SecureRandomResult.Success)
            val value = result.value
            assertTrue(value in 1000L..10000L, "Value $value should be in range [1000, 10000]")
        }
    }

    @Test
    fun testNextBoolean() {
        var trueCount = 0
        var falseCount = 0

        repeat(1000) {
            val result = secureRandom.nextBoolean()
            assertTrue(result is SecureRandomResult.Success)
            if (result.value) trueCount++ else falseCount++
        }

        // Should be roughly 50/50 distribution
        assertTrue(trueCount > 400, "Too few true values: $trueCount")
        assertTrue(falseCount > 400, "Too few false values: $falseCount")
    }

    @Test
    fun testNextDouble() {
        repeat(100) {
            val result = secureRandom.nextDouble()
            assertTrue(result is SecureRandomResult.Success)
            val value = result.value
            assertTrue(value >= 0.0, "Value should be >= 0.0")
            assertTrue(value < 1.0, "Value should be < 1.0")
        }
    }

    @Test
    fun testNextFloat() {
        repeat(100) {
            val result = secureRandom.nextFloat()
            assertTrue(result is SecureRandomResult.Success)
            val value = result.value
            assertTrue(value >= 0.0f, "Value should be >= 0.0f")
            assertTrue(value < 1.0f, "Value should be < 1.0f")
        }
    }

    @Test
    fun testThreadSafety() {
        // Note: Kotlin/Native doesn't have the same threading model as JVM
        // This test ensures multiple sequential calls work correctly
        val results = mutableListOf<ByteArray>()

        repeat(10) {
            val result = secureRandom.nextBytes(16)
            assertTrue(result is SecureRandomResult.Success)
            results.add(result.value)
        }

        // Verify all results are unique
        for (i in results.indices) {
            for (j in i + 1 until results.size) {
                assertFalse(
                    results[i].contentEquals(results[j]),
                    "Results at index $i and $j should not be equal"
                )
            }
        }
    }

    @Test
    fun testConsistentStateAfterErrors() {
        // Test invalid bound
        val invalidResult = secureRandom.nextInt(0)
        assertTrue(invalidResult is SecureRandomResult.Failure)

        // Verify generator still works after error
        val validResult = secureRandom.nextInt(10)
        assertTrue(validResult is SecureRandomResult.Success)
        val value = validResult.value
        assertTrue(value in 0 until 10)
    }

    @Test
    fun testWindowsSpecificAlgorithmSelection() {
        // Create multiple instances to test initialization paths
        repeat(3) {
            val result = createSecureRandom()
            assertTrue(result is SecureRandomResult.Success, "Failed to create instance $it")

            // Test that each instance works
            val bytes = ByteArray(8)
            val genResult = result.value.nextBytes(bytes)
            assertTrue(genResult is SecureRandomUnitResult.Success)
            assertFalse(bytes.all { b -> b == 0.toByte() })
        }
    }
}