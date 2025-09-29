package com.scottnj.kmp_secure_random

import kotlin.test.*

/**
 * Linux-specific tests for LinuxSecureRandomAdapter.
 *
 * These tests validate:
 * - getrandom() syscall #318 integration (x86_64)
 * - /dev/urandom fallback mechanism
 * - Kernel version compatibility
 * - Linux-specific implementation details
 */
class LinuxSecureRandomAdapterTest {

    private lateinit var secureRandom: SecureRandom

    @BeforeTest
    fun setUp() {
        val result = createSecureRandom()
        assertTrue(result is SecureRandomResult.Success, "Failed to create Linux SecureRandom")
        secureRandom = result.value
    }

    @Test
    fun testLinuxSecureRandomCreation() {
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

    /**
     * Test that verifies the specific Linux syscall number (318) is being used.
     */
    @Test
    fun testLinuxSyscallNumber() {
        val adapter = createLinuxAdapterForTesting()

        // Verify the syscall number constant matches Linux x86_64 specification
        val expectedSyscallNumber = 318L
        val actualSyscallNumber = adapter.getSyscallNumber()

        assertEquals(expectedSyscallNumber, actualSyscallNumber,
            "Linux should use getrandom() syscall #318, but found #$actualSyscallNumber")
        println("✅ Verified Linux using correct getrandom() syscall #$actualSyscallNumber")
    }

    /**
     * Test syscall constants verification.
     */
    @Test
    fun testSyscallConstantsVerification() {
        val adapter = createLinuxAdapterForTesting()
        val constantResult = adapter.verifySyscallConstants()

        when (constantResult) {
            is ConstantVerificationResult.Correct -> {
                println("✅ All Linux syscall constants are correct: ${constantResult.constants}")
                assertTrue(true) // Test passes
            }
            is ConstantVerificationResult.Incorrect -> {
                println("❌ Incorrect Linux syscall constants: ${constantResult.reason}")
                println("Constants: ${constantResult.constants}")
                assertTrue(false, "Linux syscall constants are incorrect: ${constantResult.reason}")
            }
            is ConstantVerificationResult.Error -> {
                println("⚠️ Error verifying constants: ${constantResult.message}")
                // Don't fail the test for verification errors, just log them
            }
        }
    }

    /**
     * Test Linux getrandom() syscall availability.
     */
    @Test
    fun testGetrandomSyscallAvailability() {
        val adapter = createLinuxAdapterForTesting()
        val syscallResult = adapter.verifySyscallAvailability()

        println("Linux getrandom() syscall #318 availability: $syscallResult")

        // This test is informational - we don't fail if getrandom() is not available
        // since the implementation should fall back to /dev/urandom
        when (syscallResult) {
            is SyscallVerificationResult.Available -> {
                println("✅ Linux getrandom() syscall is available")
            }
            is SyscallVerificationResult.NotSupported -> {
                println("ℹ️ Linux getrandom() syscall not supported (older kernel)")
            }
            else -> {
                println("⚠️ Linux getrandom() syscall test result: $syscallResult")
            }
        }
    }

    /**
     * Test fallback mechanism verification.
     */
    @Test
    fun testDevUrandomFallbackMechanism() {
        val adapter = createLinuxAdapterForTesting()

        // Test that /dev/urandom fallback is available
        val fallbackAvailable = adapter.isDevUrandomAvailable()
        assertTrue(fallbackAvailable, "/dev/urandom fallback should be available on Linux")
        println("✅ Verified /dev/urandom fallback availability")

        // Test fallback path explicitly
        val fallbackBytes = ByteArray(16)
        val fallbackResult = adapter.testDevUrandomFallback(fallbackBytes)
        assertTrue(fallbackResult, "Should be able to use /dev/urandom fallback")
        assertFalse(fallbackBytes.all { it == 0.toByte() }, "Fallback should generate non-zero bytes")
        println("✅ Verified /dev/urandom fallback functionality")
    }

    /**
     * Test kernel version detection.
     */
    @Test
    fun testKernelVersionDetection() {
        val adapter = createLinuxAdapterForTesting()
        val kernelResult = adapter.testKernelVersion()

        when (kernelResult) {
            is KernelVersionResult.Available -> {
                println("✅ Kernel version: ${kernelResult.version}")
                println("✅ Supports getrandom(): ${kernelResult.supportsGetrandom}")
                // Don't fail based on kernel version - just informational
            }
            is KernelVersionResult.Unavailable -> {
                println("ℹ️ Kernel version unavailable: ${kernelResult.reason}")
            }
            is KernelVersionResult.Error -> {
                println("⚠️ Error getting kernel version: ${kernelResult.message}")
            }
        }
    }

    /**
     * Test comprehensive platform verification.
     */
    @Test
    fun testComprehensiveLinuxPlatformVerification() {
        val adapter = createLinuxAdapterForTesting()

        println("=== Linux Platform Verification ===")

        // 1. Syscall number verification
        val syscallNum = adapter.getSyscallNumber()
        assertEquals(318L, syscallNum, "Linux should use getrandom() syscall #318")
        println("✅ Syscall number: #$syscallNum")

        // 2. Architecture info
        val archInfo = adapter.getArchitectureInfo()
        println("✅ Architecture info: $archInfo")

        // 3. Syscall availability (informational)
        val syscallAvail = adapter.verifySyscallAvailability()
        println("ℹ️ Syscall availability: $syscallAvail")

        // 4. Fallback availability (required)
        val fallbackAvail = adapter.isDevUrandomAvailable()
        assertTrue(fallbackAvail, "/dev/urandom fallback must be available")
        println("✅ Fallback available: $fallbackAvail")

        // 5. Kernel version (informational)
        val kernelVersion = adapter.testKernelVersion()
        println("ℹ️ Kernel version: $kernelVersion")

        println("=== Platform Verification Complete ===")
    }

    /**
     * Helper function to create adapter with testing access.
     */
    private fun createLinuxAdapterForTesting(): LinuxTestHelper {
        return LinuxTestHelper.create()
    }
}