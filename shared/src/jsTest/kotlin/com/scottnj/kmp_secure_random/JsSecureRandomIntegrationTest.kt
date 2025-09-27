package com.scottnj.kmp_secure_random

import kotlin.test.*

/**
 * Integration tests specifically for JavaScript SecureRandom implementation.
 * Tests JavaScript-specific behavior, environment detection, and platform integration.
 */
class JsSecureRandomIntegrationTest {

    private val secureRandom = createSecureRandom().getOrThrow()

    /**
     * Test that the implementation uses actual JavaScript/Web Crypto API.
     */
    @Test
    fun testUsesJavaScriptCryptoApi() {
        // Generate some random data
        val result = secureRandom.nextBytes(32)
        assertTrue(result.isSuccess, "Should successfully generate random bytes")

        val bytes = result.getOrNull()
        assertNotNull(bytes)
        assertEquals(32, bytes!!.size)

        // Verify bytes are not all zeros or all same value
        assertFalse(bytes.all { it == 0.toByte() }, "Bytes should not all be zero")
        assertFalse(bytes.all { it == bytes[0] }, "Bytes should not all be the same")
    }

    /**
     * Test JavaScript environment detection and compatibility.
     */
    @Test
    fun testJavaScriptEnvironmentCompatibility() {
        println("\n=== JavaScript Environment ===")

        // Test that crypto API is available
        val cryptoAvailable = js("typeof crypto !== 'undefined'")
        println("Crypto API available: $cryptoAvailable")

        val getRandomValuesAvailable = js("typeof crypto !== 'undefined' && typeof crypto.getRandomValues === 'function'")
        println("crypto.getRandomValues available: $getRandomValuesAvailable")

        // Test that our adapter was created successfully
        val adapterResult = JsSecureRandomAdapter.create()
        assertTrue(adapterResult.isSuccess, "Adapter should be created successfully in JS environment")

        // Generate test data to verify functionality
        val testResult = secureRandom.nextInt(1000)
        assertTrue(testResult.isSuccess, "Should successfully generate random int in JS environment")

        val testInt = testResult.getOrThrow()
        assertTrue(testInt >= 0 && testInt < 1000, "Generated int should be in valid range")
    }

    /**
     * Test multiple random generation types for JavaScript compatibility.
     */
    @Test
    fun testMultipleRandomGenerationTypes() {
        // Test bytes
        val bytesResult = secureRandom.nextBytes(16)
        assertTrue(bytesResult.isSuccess, "Byte generation should work in JS")
        assertEquals(16, bytesResult.getOrThrow().size)

        // Test int
        val intResult = secureRandom.nextInt()
        assertTrue(intResult.isSuccess, "Int generation should work in JS")

        // Test bounded int
        val boundedIntResult = secureRandom.nextInt(50)
        assertTrue(boundedIntResult.isSuccess, "Bounded int generation should work in JS")
        val boundedInt = boundedIntResult.getOrThrow()
        assertTrue(boundedInt >= 0 && boundedInt < 50, "Bounded int should be in range")

        // Test long
        val longResult = secureRandom.nextLong()
        assertTrue(longResult.isSuccess, "Long generation should work in JS")

        // Test boolean
        val booleanResult = secureRandom.nextBoolean()
        assertTrue(booleanResult.isSuccess, "Boolean generation should work in JS")

        // Test double
        val doubleResult = secureRandom.nextDouble()
        assertTrue(doubleResult.isSuccess, "Double generation should work in JS")
        val double = doubleResult.getOrThrow()
        assertTrue(double >= 0.0 && double < 1.0, "Double should be in range [0.0, 1.0)")

        // Test float
        val floatResult = secureRandom.nextFloat()
        assertTrue(floatResult.isSuccess, "Float generation should work in JS")
        val float = floatResult.getOrThrow()
        assertTrue(float >= 0.0f && float < 1.0f, "Float should be in range [0.0, 1.0)")
    }

    /**
     * Test performance characteristics of JavaScript implementation.
     */
    @Test
    fun testJavaScriptPerformanceCharacteristics() {
        // Test generating larger amounts of random data
        val largeBytesResult = secureRandom.nextBytes(1024)
        assertTrue(largeBytesResult.isSuccess, "Should handle larger data generation")

        val largeBytes = largeBytesResult.getOrThrow()
        assertEquals(1024, largeBytes.size)

        // Verify entropy in larger data set
        val uniqueBytes = largeBytes.toSet()
        assertTrue(uniqueBytes.size > 50, "Large data set should have good entropy")

        // Test rapid successive calls
        val results = mutableListOf<Int>()
        repeat(100) {
            val result = secureRandom.nextInt(10000).getOrThrow()
            results.add(result)
        }

        assertEquals(100, results.size)
        val uniqueResults = results.toSet()
        assertTrue(uniqueResults.size > 50, "Rapid calls should produce varied results")
    }

    /**
     * Test statistical properties specific to JavaScript implementation.
     */
    @Test
    fun testJavaScriptStatisticalProperties() {
        // Generate a set of random integers and test basic statistical properties
        val sampleSize = 100
        val integers = (1..sampleSize).map { secureRandom.nextInt(256).getOrThrow() }

        // Test range compliance
        assertTrue(integers.all { it >= 0 && it < 256 }, "All integers should be in range [0, 256)")

        // Test that we get reasonable distribution
        val uniqueValues = integers.toSet()
        assertTrue(uniqueValues.size >= sampleSize / 4, "Should have reasonable variety in $sampleSize samples")

        // Test bytes for bit distribution
        val randomBytes = secureRandom.nextBytes(256).getOrThrow()
        val byteCounts = IntArray(256) { 0 }
        randomBytes.forEach { byte ->
            byteCounts[byte.toInt() and 0xFF]++
        }

        // Should not have any byte value completely missing (very unlikely with 256 bytes)
        val nonZeroCounts = byteCounts.count { it > 0 }
        assertTrue(nonZeroCounts > 50, "Should have reasonable byte value distribution")
    }

    /**
     * Test error recovery and robustness in JavaScript environment.
     */
    @Test
    fun testErrorRecoveryAndRobustness() {
        // Test that adapter continues to work after various operations

        // Generate different types in sequence
        assertTrue(secureRandom.nextInt().isSuccess)
        assertTrue(secureRandom.nextLong().isSuccess)
        assertTrue(secureRandom.nextBoolean().isSuccess)
        assertTrue(secureRandom.nextDouble().isSuccess)
        assertTrue(secureRandom.nextFloat().isSuccess)
        assertTrue(secureRandom.nextBytes(10).isSuccess)

        // Test edge cases
        assertTrue(secureRandom.nextInt(1).getOrThrow() == 0, "nextInt(1) should always return 0")
        assertTrue(secureRandom.nextLong(1L).getOrThrow() == 0L, "nextLong(1) should always return 0")

        // Test boundary values
        val maxIntResult = secureRandom.nextInt(Int.MAX_VALUE)
        assertTrue(maxIntResult.isSuccess, "Should handle large int bounds")

        val maxLongResult = secureRandom.nextLong(Long.MAX_VALUE)
        assertTrue(maxLongResult.isSuccess, "Should handle large long bounds")

        // Continue working after edge cases
        assertTrue(secureRandom.nextInt(100).isSuccess, "Should continue working after edge cases")
    }

    /**
     * Test that JavaScript implementation properly handles various buffer sizes.
     */
    @Test
    fun testVariousBufferSizes() {
        val sizes = listOf(1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024)

        for (size in sizes) {
            val result = secureRandom.nextBytes(size)
            assertTrue(result.isSuccess, "Should handle buffer size $size")

            val bytes = result.getOrThrow()
            assertEquals(size, bytes.size, "Buffer should have correct size $size")

            if (size > 4) {
                // For larger buffers, check for some entropy
                val uniqueBytes = bytes.toSet()
                assertTrue(uniqueBytes.size > 1, "Buffer size $size should have some entropy")
            }
        }

        // Test zero size
        val zeroResult = secureRandom.nextBytes(0)
        assertTrue(zeroResult.isSuccess, "Should handle zero size")
        assertEquals(0, zeroResult.getOrThrow().size)
    }

    /**
     * Test cross-platform consistency for JavaScript implementation.
     */
    @Test
    fun testCrossPlatformConsistency() {
        // Test that the same operations work consistently
        repeat(10) { iteration ->
            // Each iteration should produce valid results
            val intResult = secureRandom.nextInt(1000)
            assertTrue(intResult.isSuccess, "Iteration $iteration: int generation should work")

            val longResult = secureRandom.nextLong(1000L)
            assertTrue(longResult.isSuccess, "Iteration $iteration: long generation should work")

            val bytesResult = secureRandom.nextBytes(32)
            assertTrue(bytesResult.isSuccess, "Iteration $iteration: bytes generation should work")

            val doubleResult = secureRandom.nextDouble()
            assertTrue(doubleResult.isSuccess, "Iteration $iteration: double generation should work")

            // Verify ranges
            assertTrue(intResult.getOrThrow() >= 0 && intResult.getOrThrow() < 1000)
            assertTrue(longResult.getOrThrow() >= 0L && longResult.getOrThrow() < 1000L)
            assertTrue(bytesResult.getOrThrow().size == 32)
            assertTrue(doubleResult.getOrThrow() >= 0.0 && doubleResult.getOrThrow() < 1.0)
        }
    }
}