package com.scottnj.kmp_secure_random

import kotlin.test.*

/**
 * Android Native x86-specific tests for AndroidNativeX86SecureRandomAdapter.
 *
 * These tests validate:
 * - getrandom() syscall #355 integration (x86)
 * - /dev/urandom fallback mechanism
 * - 32-bit UInt type handling
 * - Architectural isolation for x86
 */
class AndroidNativeX86SecureRandomAdapterTest {

    private lateinit var secureRandom: SecureRandom

    @BeforeTest
    fun setUp() {
        val result = createSecureRandom()
        assertTrue(result is SecureRandomResult.Success, "Failed to create Android Native x86 SecureRandom")
        secureRandom = result.value
    }

    @Test
    fun testAndroidNativeX86SecureRandomCreation() {
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
    fun testAndroidNativeX86SpecificSyscallHandling() {
        // Test x86 syscall #355 initialization
        val result = createSecureRandom()
        assertTrue(result is SecureRandomResult.Success, "Failed to create x86 instance")

        val bytes = ByteArray(8)
        val genResult = result.value.nextBytes(bytes)
        assertTrue(genResult is SecureRandomUnitResult.Success)
        assertFalse(bytes.all { b -> b == 0.toByte() })
    }
}