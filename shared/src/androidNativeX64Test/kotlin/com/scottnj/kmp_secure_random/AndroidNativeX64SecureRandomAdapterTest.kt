package com.scottnj.kmp_secure_random

import kotlin.test.*

/**
 * Android Native x86_64-specific tests for AndroidNativeX64SecureRandomAdapter.
 *
 * These tests validate x86_64-specific implementation details:
 * - getrandom() syscall #318 integration (x86_64)
 * - /dev/urandom fallback mechanism
 * - 64-bit ULong type handling
 * - Architectural isolation for x86_64
 *
 * Basic functionality tests are covered in commonTest.
 */
class AndroidNativeX64SecureRandomAdapterTest {

    @Test
    fun testAndroidNativeX64SecureRandomCreation() {
        val result = createSecureRandom()
        assertTrue(result is SecureRandomResult.Success)
        assertNotNull(result.value)
    }

    @Test
    fun testAndroidNativeX64SpecificSyscallHandling() {
        // Verify that we're actually using x86_64-specific syscall #318
        val adapter = createAndroidNativeX64AdapterForTesting()

        // Test syscall verification
        val syscallResult = adapter.verifySyscallAvailability()
        println("x86_64 getrandom() syscall #318 availability: $syscallResult")

        // Create multiple instances to test x86_64 syscall initialization paths
        repeat(3) {
            val result = createSecureRandom()
            assertTrue(result is SecureRandomResult.Success, "Failed to create x86_64 instance $it")

            // Test that each instance works with x86_64 architecture
            val bytes = ByteArray(8)
            val genResult = result.value.nextBytes(bytes)
            assertTrue(genResult is SecureRandomUnitResult.Success)
            assertFalse(bytes.all { b -> b == 0.toByte() })
        }
    }

    /**
     * Test that verifies the specific x86_64 syscall number (318) is being used.
     */
    @Test
    fun testX64SyscallNumber() {
        val adapter = createAndroidNativeX64AdapterForTesting()

        // Verify the syscall number constant matches x86_64 specification
        val expectedSyscallNumber = 318
        val actualSyscallNumber = adapter.getSyscallNumber()

        assertEquals(expectedSyscallNumber, actualSyscallNumber,
            "x86_64 should use getrandom() syscall #318, but found #$actualSyscallNumber")
        println("✅ Verified x86_64 using correct getrandom() syscall #$actualSyscallNumber")
    }

    /**
     * Test that verifies 64-bit ULong types are being used (x86_64-specific).
     */
    @Test
    fun testX64TypeHandling() {
        val adapter = createAndroidNativeX64AdapterForTesting()

        // x86_64 should use 64-bit types for size parameters
        val is64BitTypeUsed = adapter.uses64BitTypes()
        assertTrue(is64BitTypeUsed, "x86_64 should use 64-bit ULong types for syscall parameters")
        println("✅ Verified x86_64 using 64-bit ULong types")
    }

    /**
     * Test the /dev/urandom fallback mechanism specific to x86_64.
     */
    @Test
    fun testFallbackMechanismVerification() {
        val adapter = createAndroidNativeX64AdapterForTesting()

        // Verify /dev/urandom is available as a fallback
        val fallbackAvailable = adapter.isDevUrandomAvailable()
        assertTrue(fallbackAvailable, "/dev/urandom should be available as fallback")

        // Test that fallback actually works
        val fallbackWorks = adapter.testDevUrandomFallback()
        assertTrue(fallbackWorks, "/dev/urandom fallback should work")

        println("✅ Verified x86_64 /dev/urandom fallback mechanism")
    }

    /**
     * Test architectural isolation to ensure x86_64 doesn't conflict with other architectures.
     */
    @Test
    fun testArchitecturalIsolation() {
        val adapter = createAndroidNativeX64AdapterForTesting()

        // Test that x86_64-specific constants are correctly isolated
        val archInfo = adapter.getArchitectureInfo()
        assertTrue(archInfo.contains("x86_64") || archInfo.contains("X64"), "Should report x86_64 architecture")
        println("✅ Architecture info: $archInfo")

        // Verify syscall isolation
        val syscallNumber = adapter.getSyscallNumber()
        assertEquals(318, syscallNumber, "x86_64 should use syscall #318")

        // Test that type sizes are correct for x86_64
        val uses64Bit = adapter.uses64BitTypes()
        assertTrue(uses64Bit, "x86_64 should use 64-bit types")

        println("✅ Verified x86_64 architectural isolation")
    }

    /**
     * Test syscall-related constants are correct for x86_64.
     */
    @Test
    fun testSyscallConstantsVerification() {
        val adapter = createAndroidNativeX64AdapterForTesting()

        // Verify x86_64-specific syscall constants
        val constants = adapter.getSyscallConstants()

        // Check that we have the right syscall number
        assertTrue(constants.containsKey("SYS_GETRANDOM"), "Should have SYS_GETRANDOM constant")
        assertEquals(318, constants["SYS_GETRANDOM"], "SYS_GETRANDOM should be 318 for x86_64")

        // Check flags if available
        if (constants.containsKey("GRND_NONBLOCK")) {
            assertEquals(1, constants["GRND_NONBLOCK"], "GRND_NONBLOCK should be 1")
        }

        println("✅ Verified x86_64 syscall constants: $constants")
    }

    /**
     * Test comprehensive platform verification for x86_64.
     */
    @Test
    fun testComprehensivePlatformVerification() {
        val adapter = createAndroidNativeX64AdapterForTesting()

        println("=== Android Native x86_64 Platform Verification ===")

        // 1. Syscall number verification
        val syscallNum = adapter.getSyscallNumber()
        assertEquals(318, syscallNum, "x86_64 should use getrandom() syscall #318")
        println("✅ Syscall number: #$syscallNum")

        // 2. Type system verification
        val uses64Bit = adapter.uses64BitTypes()
        assertTrue(uses64Bit, "x86_64 should use 64-bit types")
        println("✅ 64-bit types: $uses64Bit")

        // 3. Architecture info
        val archInfo = adapter.getArchitectureInfo()
        assertTrue(archInfo.contains("x86_64") || archInfo.contains("X64"), "Should report x86_64")
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
        assertEquals(318, constants["SYS_GETRANDOM"], "Syscall constant must be correct")
        println("✅ Constants verified: ${constants.size} constants")

        println("=== Platform Verification Complete ===")
    }

    /**
     * Test x86_64 specific performance characteristics.
     */
    @Test
    fun testX64PerformanceCharacteristics() {
        val adapter = createAndroidNativeX64AdapterForTesting()

        // Test that x86_64 can handle larger buffer sizes efficiently
        val largeBuffer = ByteArray(4096)
        val result = createSecureRandom().getOrThrow().nextBytes(largeBuffer)
        assertTrue(result.isSuccess, "x86_64 should handle large buffers efficiently")

        // Test rapid successive calls (should not block)
        val startTime = System.currentTimeMillis()
        repeat(100) {
            val smallBuffer = ByteArray(8)
            val quickResult = createSecureRandom().getOrThrow().nextBytes(smallBuffer)
            assertTrue(quickResult.isSuccess, "Rapid calls should succeed")
        }
        val duration = System.currentTimeMillis() - startTime
        assertTrue(duration < 5000, "x86_64 operations should be reasonably fast: ${duration}ms")

        println("✅ x86_64 performance characteristics verified (${duration}ms for 100 operations)")
    }

    /**
     * Helper function to create adapter with testing access.
     */
    private fun createAndroidNativeX64AdapterForTesting(): AndroidNativeX64TestHelper {
        return AndroidNativeX64TestHelper.create()
    }
}