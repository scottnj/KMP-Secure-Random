package com.scottnj.kmp_secure_random

import kotlin.test.*

/**
 * Android Native x86-specific tests for AndroidNativeX86SecureRandomAdapter.
 *
 * These tests validate x86-specific implementation details:
 * - getrandom() syscall #355 integration (x86)
 * - /dev/urandom fallback mechanism
 * - 32-bit UInt type handling
 * - Architectural isolation for x86
 *
 * Basic functionality tests are covered in commonTest.
 */
class AndroidNativeX86SecureRandomAdapterTest {

    @Test
    fun testAndroidNativeX86SecureRandomCreation() {
        val result = createSecureRandom()
        assertTrue(result is SecureRandomResult.Success)
        assertNotNull(result.value)
    }

    @Test
    fun testAndroidNativeX86SpecificSyscallHandling() {
        // Verify that we're actually using x86-specific syscall #355
        val adapter = createAndroidNativeX86AdapterForTesting()

        // Test syscall verification
        val syscallResult = adapter.verifySyscallAvailability()
        println("x86 getrandom() syscall #355 availability: $syscallResult")

        // Create multiple instances to test x86 syscall initialization paths
        repeat(3) {
            val result = createSecureRandom()
            assertTrue(result is SecureRandomResult.Success, "Failed to create x86 instance $it")

            // Test that each instance works with x86 architecture
            val bytes = ByteArray(8)
            val genResult = result.value.nextBytes(bytes)
            assertTrue(genResult is SecureRandomUnitResult.Success)
            assertFalse(bytes.all { b -> b == 0.toByte() })
        }
    }

    /**
     * Test that verifies the specific x86 syscall number (355) is being used.
     */
    @Test
    fun testX86SyscallNumber() {
        val adapter = createAndroidNativeX86AdapterForTesting()

        // Verify the syscall number constant matches x86 specification
        val expectedSyscallNumber = 355
        val actualSyscallNumber = adapter.getSyscallNumber()

        assertEquals(expectedSyscallNumber, actualSyscallNumber,
            "x86 should use getrandom() syscall #355, but found #$actualSyscallNumber")
        println("✅ Verified x86 using correct getrandom() syscall #$actualSyscallNumber")
    }

    /**
     * Test that verifies 32-bit UInt types are being used (x86-specific).
     */
    @Test
    fun testX86TypeHandling() {
        val adapter = createAndroidNativeX86AdapterForTesting()

        // x86 should use 32-bit types for size parameters
        val is32BitTypeUsed = adapter.uses32BitTypes()
        assertTrue(is32BitTypeUsed, "x86 should use 32-bit UInt types for syscall parameters")
        println("✅ Verified x86 using 32-bit UInt types")
    }

    /**
     * Test the /dev/urandom fallback mechanism specific to x86.
     */
    @Test
    fun testFallbackMechanismVerification() {
        val adapter = createAndroidNativeX86AdapterForTesting()

        // Verify /dev/urandom is available as a fallback
        val fallbackAvailable = adapter.isDevUrandomAvailable()
        assertTrue(fallbackAvailable, "/dev/urandom should be available as fallback")

        // Test that fallback actually works
        val fallbackWorks = adapter.testDevUrandomFallback()
        assertTrue(fallbackWorks, "/dev/urandom fallback should work")

        println("✅ Verified x86 /dev/urandom fallback mechanism")
    }

    /**
     * Test architectural isolation to ensure x86 doesn't conflict with other architectures.
     */
    @Test
    fun testArchitecturalIsolation() {
        val adapter = createAndroidNativeX86AdapterForTesting()

        // Test that x86-specific constants are correctly isolated
        val archInfo = adapter.getArchitectureInfo()
        assertTrue(archInfo.contains("x86") || archInfo.contains("X86"), "Should report x86 architecture")
        println("✅ Architecture info: $archInfo")

        // Verify syscall isolation
        val syscallNumber = adapter.getSyscallNumber()
        assertEquals(355, syscallNumber, "x86 should use syscall #355")

        // Test that type sizes are correct for x86
        val uses32Bit = adapter.uses32BitTypes()
        assertTrue(uses32Bit, "x86 should use 32-bit types")

        println("✅ Verified x86 architectural isolation")
    }

    /**
     * Test syscall-related constants are correct for x86.
     */
    @Test
    fun testSyscallConstantsVerification() {
        val adapter = createAndroidNativeX86AdapterForTesting()

        // Verify x86-specific syscall constants
        val constants = adapter.getSyscallConstants()

        // Check that we have the right syscall number
        assertTrue(constants.containsKey("SYS_GETRANDOM"), "Should have SYS_GETRANDOM constant")
        assertEquals(355, constants["SYS_GETRANDOM"], "SYS_GETRANDOM should be 355 for x86")

        // Check flags if available
        if (constants.containsKey("GRND_NONBLOCK")) {
            assertEquals(1, constants["GRND_NONBLOCK"], "GRND_NONBLOCK should be 1")
        }

        println("✅ Verified x86 syscall constants: $constants")
    }

    /**
     * Test comprehensive platform verification for x86.
     */
    @Test
    fun testComprehensivePlatformVerification() {
        val adapter = createAndroidNativeX86AdapterForTesting()

        println("=== Android Native x86 Platform Verification ===")

        // 1. Syscall number verification
        val syscallNum = adapter.getSyscallNumber()
        assertEquals(355, syscallNum, "x86 should use getrandom() syscall #355")
        println("✅ Syscall number: #$syscallNum")

        // 2. Type system verification
        val uses32Bit = adapter.uses32BitTypes()
        assertTrue(uses32Bit, "x86 should use 32-bit types")
        println("✅ 32-bit types: $uses32Bit")

        // 3. Architecture info
        val archInfo = adapter.getArchitectureInfo()
        assertTrue(archInfo.contains("x86") || archInfo.contains("X86"), "Should report x86")
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
        assertEquals(355, constants["SYS_GETRANDOM"], "Syscall constant must be correct")
        println("✅ Constants verified: ${constants.size} constants")

        println("=== Platform Verification Complete ===")
    }

    /**
     * Test x86 specific performance characteristics.
     */
    @Test
    fun testX86PerformanceCharacteristics() {
        val adapter = createAndroidNativeX86AdapterForTesting()

        // Test that x86 can handle reasonable buffer sizes efficiently
        val mediumBuffer = ByteArray(2048) // Smaller than x64 due to 32-bit constraints
        val result = createSecureRandom().getOrThrow().nextBytes(mediumBuffer)
        assertTrue(result.isSuccess, "x86 should handle medium buffers efficiently")

        // Test rapid successive calls (should not block)
        val startTime = System.currentTimeMillis()
        repeat(100) {
            val smallBuffer = ByteArray(8)
            val quickResult = createSecureRandom().getOrThrow().nextBytes(smallBuffer)
            assertTrue(quickResult.isSuccess, "Rapid calls should succeed")
        }
        val duration = System.currentTimeMillis() - startTime
        assertTrue(duration < 5000, "x86 operations should be reasonably fast: ${duration}ms")

        println("✅ x86 performance characteristics verified (${duration}ms for 100 operations)")
    }

    /**
     * Test x86 32-bit constraints and limits.
     */
    @Test
    fun testX86ConstraintsAndLimits() {
        val adapter = createAndroidNativeX86AdapterForTesting()

        // Verify 32-bit type handling
        assertTrue(adapter.uses32BitTypes(), "x86 must use 32-bit types")

        // Test that we handle 32-bit size limits appropriately
        val maxReasonableSize = 1024 * 1024 // 1MB should be fine for 32-bit
        val largeBuffer = ByteArray(maxReasonableSize)
        val result = createSecureRandom().getOrThrow().nextBytes(largeBuffer)
        assertTrue(result.isSuccess, "x86 should handle reasonable large buffers")

        println("✅ x86 constraints and limits verified")
    }

    /**
     * Helper function to create adapter with testing access.
     */
    private fun createAndroidNativeX86AdapterForTesting(): AndroidNativeX86TestHelper {
        return AndroidNativeX86TestHelper.create()
    }
}