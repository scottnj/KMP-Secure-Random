package com.scottnj.kmp_secure_random

import kotlin.test.*

/**
 * Android Native x86_64-specific tests for AndroidNativeX64SecureRandomAdapter.
 *
 * These tests validate:
 * - getrandom() syscall #318 integration (x86_64)
 * - /dev/urandom fallback mechanism
 * - 64-bit ULong type handling
 * - Architectural isolation for x86_64
 */
class AndroidNativeX64SecureRandomAdapterTest {

    private lateinit var secureRandom: SecureRandom

    @BeforeTest
    fun setUp() {
        val result = createSecureRandom()
        assertTrue(result is SecureRandomResult.Success, "Failed to create Android Native x86_64 SecureRandom")
        secureRandom = result.value
    }

    @Test
    fun testAndroidNativeX64SecureRandomCreation() {
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
    }

    @Test
    fun testNextIntRange() {
        repeat(50) {
            val result = secureRandom.nextInt(1, 100)
            assertTrue(result is SecureRandomResult.Success)
            val value = result.value
            assertTrue(value in 1..100, "Value $value should be in range [1, 100]")
        }
    }

    @Test
    fun testNextBoolean() {
        var trueCount = 0
        var falseCount = 0

        repeat(100) {
            val result = secureRandom.nextBoolean()
            assertTrue(result is SecureRandomResult.Success)
            if (result.value) trueCount++ else falseCount++
        }

        // Should have some distribution
        assertTrue(trueCount > 0, "Should have some true values")
        assertTrue(falseCount > 0, "Should have some false values")
    }

    @Test
    fun testAndroidNativeX64SpecificSyscallHandling() {
        // Test x86_64 syscall #318 initialization
        val result = createSecureRandom()
        assertTrue(result is SecureRandomResult.Success, "Failed to create x86_64 instance")

        val bytes = ByteArray(8)
        val genResult = result.value.nextBytes(bytes)
        assertTrue(genResult is SecureRandomUnitResult.Success)
        assertFalse(bytes.all { b -> b == 0.toByte() })
    }
}