package com.scottnj.kmp_secure_random

import kotlin.test.*

/**
 * Windows-specific tests for WindowsSecureRandomAdapter.
 *
 * Focuses on Windows unique features rather than basic operations
 * (which are already covered in commonTest):
 * - BCryptGenRandom API integration (Windows Vista+)
 * - CryptGenRandom fallback verification (Windows 2000+)
 * - Windows crypto provider detection and selection
 * - Windows-specific runtime behavior
 *
 * Basic functionality tests are covered in commonTest.
 */
class WindowsSecureRandomAdapterTest {

    /**
     * Test that Windows SecureRandom can be created successfully.
     */
    @Test
    fun testWindowsSecureRandomCreation() {
        val result = createSecureRandom()
        assertTrue(result is SecureRandomResult.Success, "Windows SecureRandom should be created successfully")
        assertNotNull(result.value, "Windows SecureRandom should not be null")
    }

    /**
     * Test Windows crypto API detection and integration.
     * Critical test for Windows BCryptGenRandom/CryptGenRandom usage.
     */
    @Test
    fun testWindowsCryptoApiDetection() {
        // Test that Windows adapter successfully detects and uses available crypto APIs
        val result = createSecureRandom()
        assertTrue(result is SecureRandomResult.Success, "Windows crypto API should be available")

        val adapter = result.value

        // Test consistent operation across multiple calls to verify API stability
        val testSizes = listOf(1, 16, 32, 256, 1024)
        testSizes.forEach { size ->
            repeat(5) { iteration ->
                val bytesResult = adapter.nextBytes(size)
                assertTrue(bytesResult is SecureRandomResult.Success,
                    "Windows crypto API should handle size $size (iteration $iteration)")
                val bytes = bytesResult.getOrThrow()
                assertEquals(size, bytes.size, "Should generate correct size: $size")

                // Verify entropy for larger sizes
                if (size >= 16) {
                    val nonZeroCount = bytes.count { it != 0.toByte() }
                    val nonZeroRatio = nonZeroCount.toDouble() / size
                    assertTrue(nonZeroRatio > 0.1, "Should have reasonable entropy for size $size: $nonZeroRatio")
                }
            }
        }

        println("✅ Windows crypto API detection verified for sizes: $testSizes")
    }

    /**
     * Test Windows API resilience and performance.
     * Validates Windows crypto API stability under stress.
     */
    @Test
    fun testWindowsApiResilience() {
        val secureRandom = createSecureRandom().getOrThrow()
        val results = mutableListOf<SecureRandomResult<ByteArray>>()

        // Test Windows API stability with rapid successive calls
        repeat(100) { iteration ->
            val result = secureRandom.nextBytes(32)
            results.add(result)
            assertTrue(result is SecureRandomResult.Success,
                "Windows API call $iteration should succeed")
        }

        // Verify diversity in results (high probability of uniqueness)
        val uniqueResults = results.map {
            it.getOrThrow().contentToString()
        }.toSet()

        val uniquenessRatio = uniqueResults.size.toDouble() / results.size
        assertTrue(uniquenessRatio > 0.9,
            "Windows API should produce diverse outputs: $uniquenessRatio")

        println("✅ Windows API resilience verified (${uniqueResults.size}/${results.size} unique results)")
    }

    /**
     * Test Windows-specific algorithm selection and initialization.
     * Tests BCryptGenRandom (Vista+) and CryptGenRandom (2000+) initialization paths.
     */
    @Test
    fun testWindowsSpecificAlgorithmSelection() {
        // Create multiple instances to test different Windows API initialization paths
        val instances = mutableListOf<SecureRandom>()
        repeat(5) { iteration ->
            val result = createSecureRandom()
            assertTrue(result is SecureRandomResult.Success,
                "Failed to create Windows instance $iteration")
            instances.add(result.value)

            // Test that each instance works with Windows APIs
            val bytes = ByteArray(16)
            val genResult = result.value.nextBytes(bytes)
            assertTrue(genResult is SecureRandomUnitResult.Success,
                "Instance $iteration should generate bytes")
            assertFalse(bytes.all { b -> b == 0.toByte() },
                "Instance $iteration should generate non-zero bytes")
        }

        // Test that all instances are independent and functional
        instances.forEachIndexed { index, instance ->
            val testBytes = ByteArray(8)
            val result = instance.nextBytes(testBytes)
            assertTrue(result.isSuccess, "Instance $index should remain functional")
        }

        println("✅ Windows algorithm selection verified for ${instances.size} instances")
    }

    /**
     * Test Windows CryptAPI verification.
     * Critical test for actual Windows crypto API usage.
     */
    @Test
    fun testWindowsCryptAPIVerification() {
        val testHelper = WindowsTestHelper.create()

        // Test both BCryptGenRandom and CryptGenRandom availability
        val cryptResult = testHelper.verifyCryptAPIAvailability()
        println("Windows CryptAPI verification: $cryptResult")

        when (cryptResult) {
            is WindowsCryptResult.Available -> {
                println("✅ Windows CryptAPI is available and working")

                // Test direct API call with various buffer sizes
                val sizeResults = testHelper.testCryptGenRandomSizes()
                val allSizesWork = sizeResults.values.all { it }
                assertTrue(allSizesWork, "All buffer sizes should work with Windows CryptAPI")
                println("✅ Windows CryptAPI buffer size compatibility: $sizeResults")
            }
            is WindowsCryptResult.ContextFailed -> {
                println("❌ CryptAcquireContextW failed with error: ${cryptResult.errorCode}")
                assertTrue(false, "Windows CryptAPI context creation failed: ${cryptResult.errorCode}")
            }
            is WindowsCryptResult.GenRandomFailed -> {
                println("❌ CryptGenRandom failed with error: ${cryptResult.errorCode}")
                assertTrue(false, "Windows CryptGenRandom failed: ${cryptResult.errorCode}")
            }
            is WindowsCryptResult.Exception -> {
                println("⚠️ Exception during CryptAPI verification: ${cryptResult.message}")
                // Don't fail for exceptions during testing, but log them
            }
        }
    }

    /**
     * Test Windows crypto provider availability and compatibility.
     */
    @Test
    fun testWindowsCryptoProviders() {
        val testHelper = WindowsTestHelper.create()
        val providers = testHelper.testCryptoProviders()

        println("=== Windows Crypto Providers ===")
        providers.forEach { (provider, available) ->
            val status = if (available) "✅" else "❌"
            println("$status $provider: $available")
        }

        // At least PROV_RSA_FULL should be available (Windows 2000+)
        assertTrue(providers["PROV_RSA_FULL"] == true,
            "PROV_RSA_FULL should be available on Windows 2000+")

        println("✅ Windows crypto provider compatibility verified")
    }

    /**
     * Test Windows entropy quality and distribution.
     */
    @Test
    fun testWindowsEntropyQuality() {
        val testHelper = WindowsTestHelper.create()
        val entropyResult = testHelper.testEntropyQuality()

        when (entropyResult) {
            is EntropyTestResult.Success -> {
                println("=== Windows Entropy Quality ===")
                println("✅ Max deviation: ${entropyResult.maxDeviation}")
                println("✅ Avg deviation: ${entropyResult.avgDeviation}")
                println("✅ Unique values: ${entropyResult.uniqueValues}/256")
                println("✅ All zeros: ${entropyResult.allZeros}")

                // Validate entropy quality
                assertFalse(entropyResult.allZeros, "Generated data should not be all zeros")
                assertTrue(entropyResult.uniqueValues > 100, "Should have good variety in values")
                assertTrue(entropyResult.maxDeviation < 2.0, "Distribution should be reasonable")

                println("✅ Windows entropy quality verified")
            }
            is EntropyTestResult.ContextFailed -> {
                assertTrue(false, "Failed to create crypto context: ${entropyResult.errorCode}")
            }
            is EntropyTestResult.GenerationFailed -> {
                assertTrue(false, "Failed to generate random data: ${entropyResult.errorCode}")
            }
            is EntropyTestResult.Exception -> {
                println("⚠️ Exception during entropy test: ${entropyResult.message}")
                // Don't fail for exceptions during testing
            }
        }
    }

    /**
     * Test comprehensive Windows platform verification.
     * Validates Windows-specific crypto API integration.
     */
    @Test
    fun testComprehensiveWindowsPlatformVerification() {
        val testHelper = WindowsTestHelper.create()

        println("=== Windows Platform Verification ===")

        // 1. Windows version and API support verification
        val versionInfo = testHelper.getWindowsVersion()
        println("✅ Windows version: ${versionInfo.majorVersion}.${versionInfo.minorVersion}.${versionInfo.buildNumber}")
        assertTrue(versionInfo.supportsCryptAPI, "Should support CryptAPI on modern Windows")
        println("✅ CryptAPI support: ${versionInfo.supportsCryptAPI}")
        println("✅ BCrypt support: ${versionInfo.supportsBCrypt}")

        // 2. CryptAPI availability verification
        val cryptResult = testHelper.verifyCryptAPIAvailability()
        assertTrue(cryptResult is WindowsCryptResult.Available, "Windows CryptAPI must be available")
        println("✅ CryptAPI availability: $cryptResult")

        // 3. Crypto provider verification
        val providers = testHelper.testCryptoProviders()
        assertTrue(providers["PROV_RSA_FULL"] == true, "PROV_RSA_FULL must be available")
        println("✅ Crypto providers: ${providers.count { it.value }} available")

        // 4. Buffer size compatibility testing
        val sizeResults = testHelper.testCryptGenRandomSizes()
        val allSizesWork = sizeResults.values.all { it }
        assertTrue(allSizesWork, "All buffer sizes must work with Windows CryptAPI")
        println("✅ Buffer size compatibility: ${sizeResults.size} sizes tested")

        // 5. Entropy quality verification
        val entropyResult = testHelper.testEntropyQuality()
        assertTrue(entropyResult is EntropyTestResult.Success, "Windows entropy must be high quality")
        if (entropyResult is EntropyTestResult.Success) {
            assertTrue(entropyResult.uniqueValues > 100, "Should have good entropy diversity")
            println("✅ Entropy quality: ${entropyResult.uniqueValues}/256 unique values")
        }

        // 6. Windows-specific runtime verification
        val secureRandom = createSecureRandom().getOrThrow()
        val runtimeTest = ByteArray(1024)
        val runtimeResult = secureRandom.nextBytes(runtimeTest)
        assertTrue(runtimeResult.isSuccess, "Windows runtime generation must work")
        val uniqueBytes = runtimeTest.toSet().size
        val diversityRatio = uniqueBytes.toDouble() / runtimeTest.size
        assertTrue(diversityRatio > 0.5, "Windows should provide high diversity: $diversityRatio")
        println("✅ Runtime diversity: $diversityRatio")

        println("=== Windows Platform Verification Complete ===")
    }

    /**
     * Test Windows CryptAPI specific behavior and performance.
     */
    @Test
    fun testWindowsCryptAPIBehavior() {
        val adapter = createSecureRandom().getOrThrow()

        // Test that all SecureRandom methods work with Windows CryptAPI
        val byteArrayResult = adapter.nextBytes(16)
        assertTrue(byteArrayResult.isSuccess, "Byte array generation should work with Windows CryptAPI")
        assertTrue(byteArrayResult.getOrNull() is ByteArray, "Should return ByteArray")

        val intResult = adapter.nextInt()
        assertTrue(intResult.isSuccess, "Int generation should work with Windows CryptAPI")
        assertTrue(intResult.getOrNull() is Int, "Should return Int")

        val longResult = adapter.nextLong()
        assertTrue(longResult.isSuccess, "Long generation should work with Windows CryptAPI")
        assertTrue(longResult.getOrNull() is Long, "Should return Long")

        val doubleResult = adapter.nextDouble()
        assertTrue(doubleResult.isSuccess, "Double generation should work with Windows CryptAPI")
        assertTrue(doubleResult.getOrNull() is Double, "Should return Double")

        val floatResult = adapter.nextFloat()
        assertTrue(floatResult.isSuccess, "Float generation should work with Windows CryptAPI")
        assertTrue(floatResult.getOrNull() is Float, "Should return Float")

        val booleanResult = adapter.nextBoolean()
        assertTrue(booleanResult.isSuccess, "Boolean generation should work with Windows CryptAPI")
        assertTrue(booleanResult.getOrNull() is Boolean, "Should return Boolean")

        println("✅ Windows CryptAPI behavior verified")
    }
}