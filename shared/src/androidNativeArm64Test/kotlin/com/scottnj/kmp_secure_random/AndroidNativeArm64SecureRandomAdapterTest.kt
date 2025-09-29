package com.scottnj.kmp_secure_random

import kotlin.test.*

/**
 * Android Native ARM64-specific tests for AndroidNativeArm64SecureRandomAdapter.
 *
 * These tests validate ARM64-specific implementation details:
 * - getrandom() syscall #278 integration (ARM64)
 * - /dev/urandom fallback mechanism
 * - 64-bit ULong type handling
 * - Error handling for Android Native ARM64-specific failures
 * - Architectural isolation and metadata conflict prevention
 *
 * Basic functionality tests are covered in commonTest.
 */
class AndroidNativeArm64SecureRandomAdapterTest {

    @Test
    fun testAndroidNativeArm64SecureRandomCreation() {
        val result = createSecureRandom()
        assertTrue(result is SecureRandomResult.Success)
        assertNotNull(result.value)
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
        val expectedSyscallNumber = 278
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
     * Test the /dev/urandom fallback mechanism specific to ARM64.
     */
    @Test
    fun testFallbackMechanismVerification() {
        val adapter = createAndroidNativeArm64AdapterForTesting()

        // Verify /dev/urandom is available as a fallback
        val fallbackAvailable = adapter.isDevUrandomAvailable()
        assertTrue(fallbackAvailable, "/dev/urandom should be available as fallback")

        // Test that fallback actually works
        val fallbackWorks = adapter.testDevUrandomFallback()
        assertTrue(fallbackWorks, "/dev/urandom fallback should work")

        println("✅ Verified ARM64 /dev/urandom fallback mechanism")
    }

    /**
     * Test architectural isolation to ensure ARM64 doesn't conflict with other architectures.
     */
    @Test
    fun testArchitecturalIsolation() {
        val adapter = createAndroidNativeArm64AdapterForTesting()

        // Test that ARM64-specific constants are correctly isolated
        val archInfo = adapter.getArchitectureInfo()
        assertTrue(archInfo.contains("ARM64"), "Should report ARM64 architecture")
        println("✅ Architecture info: $archInfo")

        // Verify syscall isolation
        val syscallNumber = adapter.getSyscallNumber()
        assertEquals(278, syscallNumber, "ARM64 should use syscall #278")

        // Test that type sizes are correct for ARM64
        val uses64Bit = adapter.uses64BitTypes()
        assertTrue(uses64Bit, "ARM64 should use 64-bit types")

        println("✅ Verified ARM64 architectural isolation")
    }

    /**
     * Test syscall-related constants are correct for ARM64.
     */
    @Test
    fun testSyscallConstantsVerification() {
        val adapter = createAndroidNativeArm64AdapterForTesting()

        // Verify ARM64-specific syscall constants
        val constants = adapter.getSyscallConstants()

        // Check that we have the right syscall number
        assertTrue(constants.containsKey("SYS_GETRANDOM"), "Should have SYS_GETRANDOM constant")
        assertEquals(278, constants["SYS_GETRANDOM"], "SYS_GETRANDOM should be 278 for ARM64")

        // Check flags if available
        if (constants.containsKey("GRND_NONBLOCK")) {
            assertEquals(1, constants["GRND_NONBLOCK"], "GRND_NONBLOCK should be 1")
        }

        println("✅ Verified ARM64 syscall constants: $constants")
    }

    /**
     * Test comprehensive platform verification for ARM64.
     */
    @Test
    fun testComprehensivePlatformVerification() {
        val adapter = createAndroidNativeArm64AdapterForTesting()

        println("=== Android Native ARM64 Platform Verification ===")

        // 1. Syscall number verification
        val syscallNum = adapter.getSyscallNumber()
        assertEquals(278, syscallNum, "ARM64 should use getrandom() syscall #278")
        println("✅ Syscall number: #$syscallNum")

        // 2. Type system verification
        val uses64Bit = adapter.uses64BitTypes()
        assertTrue(uses64Bit, "ARM64 should use 64-bit types")
        println("✅ 64-bit types: $uses64Bit")

        // 3. Architecture info
        val archInfo = adapter.getArchitectureInfo()
        assertTrue(archInfo.contains("ARM64"), "Should report ARM64")
        println("✅ Architecture info: $archInfo")

        // 4. Syscall availability
        val syscallAvail = adapter.verifySyscallAvailability()
        println("ℹ️ Syscall availability: $syscallAvail")

        // 5. Fallback availability
        val fallbackAvail = adapter.isDevUrandomAvailable()
        assertTrue(fallbackAvail, "/dev/urandom fallback must be available")
        println("✅ Fallback available: $fallbackAvail")

        // 6. Constants verification
        val constants = adapter.getSyscallConstants()
        assertEquals(278, constants["SYS_GETRANDOM"], "Syscall constant must be correct")
        println("✅ Constants verified: ${constants.size} constants")

        println("=== Platform Verification Complete ===")
    }

    /**
     * Helper function to create adapter with testing access.
     */
    private fun createAndroidNativeArm64AdapterForTesting(): AndroidNativeArm64TestHelper {
        return AndroidNativeArm64TestHelper.create()
    }
}