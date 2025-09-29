package com.scottnj.kmp_secure_random

import kotlin.test.*

/**
 * Android Native ARM32-specific tests for AndroidNativeArm32SecureRandomAdapter.
 *
 * These tests validate:
 * - getrandom() syscall #384 integration (ARM32)
 * - /dev/urandom fallback mechanism
 * - 32-bit UInt type handling
 * - Architectural isolation for ARM32
 */
class AndroidNativeArm32SecureRandomAdapterTest {

    private lateinit var secureRandom: SecureRandom

    @BeforeTest
    fun setUp() {
        val result = createSecureRandom()
        assertTrue(result is SecureRandomResult.Success, "Failed to create Android Native ARM32 SecureRandom")
        secureRandom = result.value
    }

    @Test
    fun testAndroidNativeArm32SecureRandomCreation() {
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
    fun testAndroidNativeArm32SpecificSyscallHandling() {
        // Verify that we're actually using ARM32-specific syscall #384
        val adapter = createAndroidNativeArm32AdapterForTesting()

        // Test syscall verification
        val syscallResult = adapter.verifySyscallAvailability()
        println("ARM32 getrandom() syscall #384 availability: $syscallResult")

        // Create multiple instances to test ARM32 syscall initialization paths
        repeat(3) {
            val result = createSecureRandom()
            assertTrue(result is SecureRandomResult.Success, "Failed to create ARM32 instance $it")

            // Test that each instance works with ARM32 architecture
            val bytes = ByteArray(8)
            val genResult = result.value.nextBytes(bytes)
            assertTrue(genResult is SecureRandomUnitResult.Success)
            assertFalse(bytes.all { b -> b == 0.toByte() })
        }
    }

    /**
     * Test that verifies the specific ARM32 syscall number (384) is being used.
     */
    @Test
    fun testArm32SyscallNumber() {
        val adapter = createAndroidNativeArm32AdapterForTesting()

        // Verify the syscall number constant matches ARM32 specification
        val expectedSyscallNumber = 384
        val actualSyscallNumber = adapter.getSyscallNumber()

        assertEquals(expectedSyscallNumber, actualSyscallNumber,
            "ARM32 should use getrandom() syscall #384, but found #$actualSyscallNumber")
        println("✅ Verified ARM32 using correct getrandom() syscall #$actualSyscallNumber")
    }

    /**
     * Test that verifies 32-bit UInt types are being used (ARM32-specific).
     */
    @Test
    fun testArm32TypeHandling() {
        val adapter = createAndroidNativeArm32AdapterForTesting()

        // ARM32 should use 32-bit types for size parameters
        val is32BitTypeUsed = adapter.uses32BitTypes()
        assertTrue(is32BitTypeUsed, "ARM32 should use 32-bit UInt types for syscall parameters")
        println("✅ Verified ARM32 using 32-bit UInt types")
    }

    /**
     * Test comprehensive platform verification.
     */
    @Test
    fun testComprehensivePlatformVerification() {
        val adapter = createAndroidNativeArm32AdapterForTesting()

        println("=== Android Native ARM32 Platform Verification ===")

        // 1. Syscall number verification
        val syscallNum = adapter.getSyscallNumber()
        assertEquals(384, syscallNum, "ARM32 should use getrandom() syscall #384")
        println("✅ Syscall number: #$syscallNum")

        // 2. Type system verification
        val uses32Bit = adapter.uses32BitTypes()
        assertTrue(uses32Bit, "ARM32 should use 32-bit types")
        println("✅ 32-bit types: $uses32Bit")

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
    private fun createAndroidNativeArm32AdapterForTesting(): AndroidNativeArm32TestHelper {
        return AndroidNativeArm32TestHelper.create()
    }
}