package com.scottnj.kmp_secure_random

import kotlin.test.*

/**
 * Android Native ARM32-specific tests for AndroidNativeArm32SecureRandomAdapter.
 *
 * These tests validate ARM32-specific implementation details:
 * - getrandom() syscall #384 integration (ARM32)
 * - /dev/urandom fallback mechanism
 * - 32-bit UInt type handling
 * - Architectural isolation for ARM32
 *
 * Basic functionality tests are covered in commonTest.
 */
class AndroidNativeArm32SecureRandomAdapterTest {

    @Test
    fun testAndroidNativeArm32SecureRandomCreation() {
        val result = createSecureRandom()
        assertTrue(result is SecureRandomResult.Success)
        assertNotNull(result.value)
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