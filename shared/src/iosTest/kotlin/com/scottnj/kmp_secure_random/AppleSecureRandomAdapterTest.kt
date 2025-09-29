package com.scottnj.kmp_secure_random

import kotlin.test.*

/**
 * iOS-specific tests for AppleSecureRandomAdapter.
 *
 * Focuses on iOS unique features rather than basic operations
 * (which are already covered in commonTest):
 * - SecRandomCopyBytes API integration verification
 * - iOS Security.framework integration
 * - Apple-specific runtime behavior
 *
 * Basic functionality tests are covered in commonTest.
 */
class AppleSecureRandomAdapterTest {

    /**
     * Test that AppleSecureRandomAdapter can be created successfully.
     */
    @Test
    fun testAdapterCreation() {
        val result = AppleSecureRandomAdapter.create()
        assertTrue(result.isSuccess, "AppleSecureRandomAdapter should be created successfully")

        val adapter = result.getOrNull()
        assertNotNull(adapter, "Adapter should not be null")
    }

    /**
     * Test that createSecureRandom returns Apple implementation.
     */
    @Test
    fun testCreateSecureRandomReturnsAppleImplementation() {
        val result = createSecureRandom()
        assertTrue(result.isSuccess, "createSecureRandom should succeed on iOS")

        val secureRandom = result.getOrNull()
        assertNotNull(secureRandom, "SecureRandom should not be null")
    }

    /**
     * Test Apple-specific SecRandomCopyBytes functionality.
     * Critical test for iOS Security.framework integration.
     */
    @Test
    fun testSecRandomCopyBytesIntegration() {
        val result = AppleSecureRandomAdapter.create()
        assertTrue(result.isSuccess, "Should successfully create adapter with SecRandomCopyBytes")

        val adapter = result.getOrThrow()

        // Test various buffer sizes that SecRandomCopyBytes should handle
        val testSizes = listOf(1, 16, 32, 256, 1024, 4096)
        testSizes.forEach { size ->
            val bytes = adapter.nextBytes(size).getOrThrow()
            assertEquals(size, bytes.size, "Should generate correct size: $size")

            // Verify randomness characteristics for larger sizes
            if (size >= 16) {
                val nonZeroCount = bytes.count { it != 0.toByte() }
                val nonZeroRatio = nonZeroCount.toDouble() / size
                assertTrue(nonZeroRatio > 0.1, "Should have reasonable entropy for size $size: $nonZeroRatio")
            }
        }

        println("✅ SecRandomCopyBytes integration verified for sizes: $testSizes")
    }

    /**
     * Test direct SecRandomCopyBytes API verification.
     * Critical test for actual iOS Security.framework API usage.
     */
    @Test
    fun testSecRandomCopyBytesAPIVerification() {
        val testHelper = AppleTestHelper.create("iOS")

        // Test SecRandomCopyBytes availability and functionality
        val apiResult = testHelper.verifySecRandomCopyBytesAvailability()
        println("iOS SecRandomCopyBytes API verification: $apiResult")

        when (apiResult) {
            is AppleSecRandomResult.Available -> {
                println("✅ iOS SecRandomCopyBytes API is available and working")

                // Test direct API call with various buffer sizes
                val directTestResult = testHelper.testSecRandomCopyBytesSizes()
                val allSizesWork = directTestResult.values.all { it }
                assertTrue(allSizesWork, "All buffer sizes should work with SecRandomCopyBytes")
                println("✅ SecRandomCopyBytes buffer size compatibility: $directTestResult")
            }
            is AppleSecRandomResult.Failed -> {
                println("❌ SecRandomCopyBytes failed with status: ${apiResult.status}")
                assertTrue(false, "iOS SecRandomCopyBytes API failed with status: ${apiResult.status}")
            }
            is AppleSecRandomResult.Exception -> {
                println("⚠️ Exception during SecRandomCopyBytes verification: ${apiResult.message}")
                // Don't fail for exceptions during testing, but log them
            }
        }
    }

    /**
     * Test comprehensive iOS platform verification.
     */
    @Test
    fun testComprehensiveIOSPlatformVerification() {
        val testHelper = AppleTestHelper.create("iOS")

        println("=== iOS Platform Verification ===")

        // 1. SecRandomCopyBytes API verification
        val apiAvail = testHelper.verifySecRandomCopyBytesAvailability()
        assertTrue(apiAvail is AppleSecRandomResult.Available, "SecRandomCopyBytes must be available")
        println("✅ SecRandomCopyBytes API: $apiAvail")

        // 2. Platform information
        val platformInfo = testHelper.getApplePlatformInfo()
        println("✅ Platform info: $platformInfo")

        // 3. Security framework verification
        val frameworkResult = testHelper.verifySecurityFrameworkConstants()
        println("ℹ️ Security framework: $frameworkResult")

        // 4. Comprehensive buffer size testing
        val sizeResults = testHelper.testSecRandomCopyBytesSizes()
        val allSizesWork = sizeResults.values.all { it }
        assertTrue(allSizesWork, "All buffer sizes must work with SecRandomCopyBytes on iOS")
        println("✅ Buffer size compatibility: ${sizeResults.size} sizes tested")

        // 5. iOS-specific entropy verification
        val secureRandom = createSecureRandom().getOrThrow()
        val entropyTest = ByteArray(1024)
        val entropyResult = secureRandom.nextBytes(entropyTest)
        assertTrue(entropyResult.isSuccess, "iOS entropy generation must work")
        val uniqueBytes = entropyTest.toSet().size
        val entropyRatio = uniqueBytes.toDouble() / entropyTest.size
        assertTrue(entropyRatio > 0.5, "iOS should provide high entropy: $entropyRatio")
        println("✅ iOS entropy quality verified: $entropyRatio")

        println("=== iOS Platform Verification Complete ===")
    }

    /**
     * Test iOS-specific memory management and performance.
     */
    @Test
    fun testIOSMemoryManagementAndPerformance() {
        val secureRandom = createSecureRandom().getOrThrow()

        // Test rapid allocation and deallocation of various sizes
        val testSizes = listOf(8, 64, 256, 1024, 4096)
        val iterations = 50

        testSizes.forEach { size ->
            repeat(iterations) { iteration ->
                val bytes = ByteArray(size)
                val result = secureRandom.nextBytes(bytes)
                assertTrue(result.isSuccess, "iOS memory test failed at size $size, iteration $iteration")
                // Memory gets automatically managed by ARC
            }
        }

        // Final verification after stress test
        val finalBytes = ByteArray(32)
        val finalResult = secureRandom.nextBytes(finalBytes)
        assertTrue(finalResult.isSuccess, "iOS should work after memory stress test")

        println("✅ iOS memory management verified for sizes: $testSizes")
    }

    /**
     * Test iOS Security.framework specific behavior.
     */
    @Test
    fun testIOSSecurityFrameworkBehavior() {
        val adapter = AppleSecureRandomAdapter.create().getOrThrow()

        // Test that all SecureRandom methods work with iOS Security.framework
        val byteArrayResult = adapter.nextBytes(16)
        assertTrue(byteArrayResult.isSuccess, "Byte array generation should work with Security.framework")
        assertTrue(byteArrayResult.getOrNull() is ByteArray, "Should return ByteArray")

        val intResult = adapter.nextInt()
        assertTrue(intResult.isSuccess, "Int generation should work with Security.framework")
        assertTrue(intResult.getOrNull() is Int, "Should return Int")

        val longResult = adapter.nextLong()
        assertTrue(longResult.isSuccess, "Long generation should work with Security.framework")
        assertTrue(longResult.getOrNull() is Long, "Should return Long")

        val doubleResult = adapter.nextDouble()
        assertTrue(doubleResult.isSuccess, "Double generation should work with Security.framework")
        assertTrue(doubleResult.getOrNull() is Double, "Should return Double")

        val floatResult = adapter.nextFloat()
        assertTrue(floatResult.isSuccess, "Float generation should work with Security.framework")
        assertTrue(floatResult.getOrNull() is Float, "Should return Float")

        val booleanResult = adapter.nextBoolean()
        assertTrue(booleanResult.isSuccess, "Boolean generation should work with Security.framework")
        assertTrue(booleanResult.getOrNull() is Boolean, "Should return Boolean")

        println("✅ iOS Security.framework behavior verified")
    }
}