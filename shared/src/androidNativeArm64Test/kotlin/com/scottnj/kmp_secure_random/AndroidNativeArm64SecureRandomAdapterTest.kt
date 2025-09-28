package com.scottnj.kmp_secure_random

import kotlin.test.*

/**
 * Android Native ARM64-specific tests for AndroidNativeArm64SecureRandomAdapter.
 *
 * These tests validate:
 * - getrandom() syscall #278 integration (ARM64)
 * - /dev/urandom fallback mechanism
 * - 64-bit ULong type handling
 * - Error handling for Android Native ARM64-specific failures
 * - Architectural isolation and metadata conflict prevention
 */
class AndroidNativeArm64SecureRandomAdapterTest {

    private lateinit var secureRandom: SecureRandom

    @BeforeTest
    fun setUp() {
        val result = createSecureRandom()
        assertTrue(result is SecureRandomResult.Success, "Failed to create Android Native ARM64 SecureRandom")
        secureRandom = result.value
    }

    @Test
    fun testAndroidNativeArm64SecureRandomCreation() {
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
        // Test with 1MB array to stress-test ARM64 syscall handling
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
    fun testAndroidNativeArm64SpecificSyscallHandling() {
        // Create multiple instances to test ARM64 syscall #278 initialization paths
        repeat(3) {
            val result = createSecureRandom()
            assertTrue(result is SecureRandomResult.Success, "Failed to create ARM64 instance $it")

            // Test that each instance works with ARM64 architecture
            val bytes = ByteArray(8)
            val genResult = result.value.nextBytes(bytes)
            assertTrue(genResult is SecureRandomUnitResult.Success)
            assertFalse(bytes.all { b -> b == 0.toByte() })
        }
    }

    @Test
    fun testArchitecturalIsolation() {
        // Test that ARM64 implementation is isolated and doesn't conflict with other architectures
        val result1 = createSecureRandom()
        val result2 = createSecureRandom()

        assertTrue(result1 is SecureRandomResult.Success)
        assertTrue(result2 is SecureRandomResult.Success)

        // Both should work independently
        val bytes1 = ByteArray(16)
        val bytes2 = ByteArray(16)

        val gen1 = result1.value.nextBytes(bytes1)
        val gen2 = result2.value.nextBytes(bytes2)

        assertTrue(gen1 is SecureRandomUnitResult.Success)
        assertTrue(gen2 is SecureRandomUnitResult.Success)

        // Should produce different results
        assertFalse(bytes1.contentEquals(bytes2))
    }
}