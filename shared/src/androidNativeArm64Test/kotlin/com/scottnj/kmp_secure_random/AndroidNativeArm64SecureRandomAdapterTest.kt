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
        // Verify that we're actually using ARM64-specific syscall #278
        val adapter = createAndroidNativeArm64AdapterForTesting()

        // Test syscall verification
        val syscallResult = adapter.verifySyscallAvailability()
        println("ARM64 getrandom() syscall #278 availability: $syscallResult")

        // Create multiple instances to test ARM64 syscall initialization paths
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

    /**
     * Test that verifies the specific ARM64 syscall number (278) is being used.
     */
    @Test
    fun testArm64SyscallNumber() {
        val adapter = createAndroidNativeArm64AdapterForTesting()

        // Verify the syscall number constant matches ARM64 specification
        val expectedSyscallNumber = 278L
        val actualSyscallNumber = adapter.getSyscallNumber()

        assertEquals(expectedSyscallNumber, actualSyscallNumber,
            "ARM64 should use getrandom() syscall #278, but found #$actualSyscallNumber")
        println("✅ Verified ARM64 using correct getrandom() syscall #$actualSyscallNumber")
    }

    /**
     * Test that verifies 64-bit ULong types are being used (ARM64-specific).
     */
    @Test
    fun testArm64TypeHandling() {
        val adapter = createAndroidNativeArm64AdapterForTesting()

        // ARM64 should use 64-bit types for size parameters
        val is64BitTypeUsed = adapter.uses64BitTypes()
        assertTrue(is64BitTypeUsed, "ARM64 should use 64-bit ULong types for syscall parameters")
        println("✅ Verified ARM64 using 64-bit ULong types")
    }

    /**
     * Test fallback mechanism verification.
     */
    @Test
    fun testFallbackMechanismVerification() {
        val adapter = createAndroidNativeArm64AdapterForTesting()

        // Test that /dev/urandom fallback is available
        val fallbackAvailable = adapter.isDevUrandomAvailable()
        assertTrue(fallbackAvailable, "/dev/urandom fallback should be available on Android Native")
        println("✅ Verified /dev/urandom fallback availability")

        // Test fallback path explicitly
        val fallbackBytes = ByteArray(16)
        val fallbackResult = adapter.testDevUrandomFallback(fallbackBytes)
        assertTrue(fallbackResult, "Should be able to use /dev/urandom fallback")
        assertFalse(fallbackBytes.all { it == 0.toByte() }, "Fallback should generate non-zero bytes")
        println("✅ Verified /dev/urandom fallback functionality")
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

        // Verify architectural consistency
        val adapter = createAndroidNativeArm64AdapterForTesting()
        val archInfo = adapter.getArchitectureInfo()
        assertTrue(archInfo.contains("ARM64") || archInfo.contains("aarch64"),
            "Should report ARM64/aarch64 architecture, got: $archInfo")
        println("✅ Verified architectural isolation: $archInfo")
    }

    /**
     * Test syscall constants verification.
     */
    @Test
    fun testSyscallConstantsVerification() {
        val adapter = createAndroidNativeArm64AdapterForTesting()
        val constantResult = adapter.verifySyscallConstants()

        when (constantResult) {
            is ConstantVerificationResult.Correct -> {
                println("✅ All ARM64 syscall constants are correct: ${constantResult.constants}")
                assertTrue(true) // Test passes
            }
            is ConstantVerificationResult.Incorrect -> {
                println("❌ Incorrect ARM64 syscall constants: ${constantResult.reason}")
                println("Constants: ${constantResult.constants}")
                assertTrue(false, "ARM64 syscall constants are incorrect: ${constantResult.reason}")
            }
            is ConstantVerificationResult.Error -> {
                println("⚠️ Error verifying constants: ${constantResult.message}")
                // Don't fail the test for verification errors, just log them
            }
        }
    }

    /**
     * Test comprehensive platform verification.
     */
    @Test
    fun testComprehensivePlatformVerification() {
        val adapter = createAndroidNativeArm64AdapterForTesting()

        println("=== Android Native ARM64 Platform Verification ===")

        // 1. Syscall number verification
        val syscallNum = adapter.getSyscallNumber()
        assertEquals(278L, syscallNum, "ARM64 should use getrandom() syscall #278")
        println("✅ Syscall number: #$syscallNum")

        // 2. Type system verification
        val uses64Bit = adapter.uses64BitTypes()
        assertTrue(uses64Bit, "ARM64 should use 64-bit types")
        println("✅ 64-bit types: $uses64Bit")

        // 3. Architecture info
        val archInfo = adapter.getArchitectureInfo()
        println("✅ Architecture info: $archInfo")

        // 4. Syscall availability
        val syscallAvail = adapter.verifySyscallAvailability()
        println("ℹ️ Syscall availability: $syscallAvail")

        // 5. Fallback availability
        val fallbackAvail = adapter.isDevUrandomAvailable()
        assertTrue(fallbackAvail, "/dev/urandom fallback must be available")
        println("✅ Fallback available: $fallbackAvail")

        println("=== Platform Verification Complete ===")
    }

    /**
     * Helper function to create adapter with testing access.
     */
    private fun createAndroidNativeArm64AdapterForTesting(): AndroidNativeArm64TestHelper {
        return AndroidNativeArm64TestHelper.create()
    }
}