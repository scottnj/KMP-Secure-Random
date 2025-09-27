package com.scottnj.kmp_secure_random

import kotlin.test.*

/**
 * Integration tests specifically for WASM-JS SecureRandom implementation.
 * Tests WASM-JS-specific behavior, environment compatibility, and platform integration.
 */
class WasmJsSecureRandomIntegrationTest {

    private val secureRandom = createSecureRandom().getOrThrow()

    /**
     * Test that the implementation uses actual WASM-JS/Web Crypto API.
     */
    @Test
    fun testUsesWasmJsCryptoApi() {
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
     * Test WASM-JS environment detection and compatibility.
     */
    @Test
    fun testWasmJsEnvironmentCompatibility() {
        println("\n=== WASM-JS Environment ===")

        // Note: WASM-JS doesn't support runtime JS calls like js()
        // We test functionality through actual adapter behavior
        println("Testing WASM-JS crypto functionality through adapter")

        // Test that our adapter was created successfully
        val adapterResult = WasmJsSecureRandomAdapter.create()
        assertTrue(adapterResult.isSuccess, "Adapter should be created successfully in WASM-JS environment")

        // Generate test data to verify functionality
        val testResult = secureRandom.nextInt(1000)
        assertTrue(testResult.isSuccess, "Should successfully generate random int in WASM-JS environment")

        val testInt = testResult.getOrThrow()
        assertTrue(testInt >= 0 && testInt < 1000, "Generated int should be in valid range")
    }

    /**
     * Test multiple random generation types for WASM-JS compatibility.
     */
    @Test
    fun testMultipleRandomGenerationTypes() {
        // Test bytes
        val bytesResult = secureRandom.nextBytes(16)
        assertTrue(bytesResult.isSuccess, "Byte generation should work in WASM-JS")
        assertEquals(16, bytesResult.getOrThrow().size)

        // Test int
        val intResult = secureRandom.nextInt()
        assertTrue(intResult.isSuccess, "Int generation should work in WASM-JS")

        // Test bounded int
        val boundedIntResult = secureRandom.nextInt(50)
        assertTrue(boundedIntResult.isSuccess, "Bounded int generation should work in WASM-JS")
        val boundedInt = boundedIntResult.getOrThrow()
        assertTrue(boundedInt >= 0 && boundedInt < 50, "Bounded int should be in range")

        // Test long
        val longResult = secureRandom.nextLong()
        assertTrue(longResult.isSuccess, "Long generation should work in WASM-JS")

        // Test boolean
        val booleanResult = secureRandom.nextBoolean()
        assertTrue(booleanResult.isSuccess, "Boolean generation should work in WASM-JS")

        // Test double
        val doubleResult = secureRandom.nextDouble()
        assertTrue(doubleResult.isSuccess, "Double generation should work in WASM-JS")
        val double = doubleResult.getOrThrow()
        assertTrue(double >= 0.0 && double < 1.0, "Double should be in range [0.0, 1.0)")

        // Test float
        val floatResult = secureRandom.nextFloat()
        assertTrue(floatResult.isSuccess, "Float generation should work in WASM-JS")
        val float = floatResult.getOrThrow()
        assertTrue(float >= 0.0f && float < 1.0f, "Float should be in range [0.0, 1.0)")
    }

    /**
     * Test WASM-JS memory management and performance characteristics.
     */
    @Test
    fun testWasmJsMemoryManagement() {
        // Test generating larger amounts of random data in WASM
        val largeBytesResult = secureRandom.nextBytes(2048)
        assertTrue(largeBytesResult.isSuccess, "Should handle larger data generation in WASM")

        val largeBytes = largeBytesResult.getOrThrow()
        assertEquals(2048, largeBytes.size)

        // Verify entropy in larger data set
        val uniqueBytes = largeBytes.toSet()
        assertTrue(uniqueBytes.size > 100, "Large data set should have good entropy")

        // Test rapid successive calls to stress WASM memory handling
        val results = mutableListOf<Int>()
        repeat(200) {
            val result = secureRandom.nextInt(10000).getOrThrow()
            results.add(result)
        }

        assertEquals(200, results.size)
        val uniqueResults = results.toSet()
        assertTrue(uniqueResults.size > 100, "Rapid calls should produce varied results")
    }

    /**
     * Test WASM-JS specific performance and stability.
     */
    @Test
    fun testWasmJsPerformanceAndStability() {
        // Test sustained operation in WASM environment
        val iterations = 50
        val batchSize = 64

        repeat(iterations) { iteration ->
            val bytesResult = secureRandom.nextBytes(batchSize)
            assertTrue(bytesResult.isSuccess, "Iteration $iteration: bytes generation should work")

            val intResult = secureRandom.nextInt(1000)
            assertTrue(intResult.isSuccess, "Iteration $iteration: int generation should work")

            val longResult = secureRandom.nextLong(1000L)
            assertTrue(longResult.isSuccess, "Iteration $iteration: long generation should work")

            // Verify results are in expected ranges
            assertTrue(intResult.getOrThrow() >= 0 && intResult.getOrThrow() < 1000)
            assertTrue(longResult.getOrThrow() >= 0L && longResult.getOrThrow() < 1000L)
            assertEquals(batchSize, bytesResult.getOrThrow().size)
        }
    }

    /**
     * Test statistical properties specific to WASM-JS implementation.
     */
    @Test
    fun testWasmJsStatisticalProperties() {
        // Generate a set of random integers and test basic statistical properties
        val sampleSize = 200
        val integers = (1..sampleSize).map { secureRandom.nextInt(256).getOrThrow() }

        // Test range compliance
        assertTrue(integers.all { it >= 0 && it < 256 }, "All integers should be in range [0, 256)")

        // Test that we get reasonable distribution
        val uniqueValues = integers.toSet()
        assertTrue(uniqueValues.size >= sampleSize / 4, "Should have reasonable variety in $sampleSize samples")

        // Test bytes for bit distribution
        val randomBytes = secureRandom.nextBytes(512).getOrThrow()
        val byteCounts = IntArray(256) { 0 }
        randomBytes.forEach { byte ->
            byteCounts[byte.toInt() and 0xFF]++
        }

        // Should have reasonable distribution across byte values
        val nonZeroCounts = byteCounts.count { it > 0 }
        assertTrue(nonZeroCounts > 100, "Should have reasonable byte value distribution")
    }

    /**
     * Test error recovery and robustness in WASM-JS environment.
     */
    @Test
    fun testWasmJsErrorRecoveryAndRobustness() {
        // Test that adapter continues to work after various operations

        // Generate different types in sequence
        assertTrue(secureRandom.nextInt().isSuccess)
        assertTrue(secureRandom.nextLong().isSuccess)
        assertTrue(secureRandom.nextBoolean().isSuccess)
        assertTrue(secureRandom.nextDouble().isSuccess)
        assertTrue(secureRandom.nextFloat().isSuccess)
        assertTrue(secureRandom.nextBytes(10).isSuccess)

        // Test edge cases specific to WASM
        assertTrue(secureRandom.nextInt(1).getOrThrow() == 0, "nextInt(1) should always return 0")
        assertTrue(secureRandom.nextLong(1L).getOrThrow() == 0L, "nextLong(1) should always return 0")

        // Test boundary values for WASM number handling
        val maxIntResult = secureRandom.nextInt(Int.MAX_VALUE)
        assertTrue(maxIntResult.isSuccess, "Should handle large int bounds in WASM")

        val maxLongResult = secureRandom.nextLong(Long.MAX_VALUE)
        assertTrue(maxLongResult.isSuccess, "Should handle large long bounds in WASM")

        // Continue working after edge cases
        assertTrue(secureRandom.nextInt(100).isSuccess, "Should continue working after edge cases")
    }

    /**
     * Test that WASM-JS implementation properly handles various buffer sizes.
     */
    @Test
    fun testWasmJsVariousBufferSizes() {
        val sizes = listOf(1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048)

        for (size in sizes) {
            val result = secureRandom.nextBytes(size)
            assertTrue(result.isSuccess, "Should handle buffer size $size in WASM")

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
        assertTrue(zeroResult.isSuccess, "Should handle zero size in WASM")
        assertEquals(0, zeroResult.getOrThrow().size)
    }

    /**
     * Test WASM-JS cross-platform consistency.
     */
    @Test
    fun testWasmJsCrossPlatformConsistency() {
        // Test that the same operations work consistently in WASM
        repeat(20) { iteration ->
            // Each iteration should produce valid results
            val intResult = secureRandom.nextInt(1000)
            assertTrue(intResult.isSuccess, "WASM Iteration $iteration: int generation should work")

            val longResult = secureRandom.nextLong(1000L)
            assertTrue(longResult.isSuccess, "WASM Iteration $iteration: long generation should work")

            val bytesResult = secureRandom.nextBytes(32)
            assertTrue(bytesResult.isSuccess, "WASM Iteration $iteration: bytes generation should work")

            val doubleResult = secureRandom.nextDouble()
            assertTrue(doubleResult.isSuccess, "WASM Iteration $iteration: double generation should work")

            // Verify ranges
            assertTrue(intResult.getOrThrow() >= 0 && intResult.getOrThrow() < 1000)
            assertTrue(longResult.getOrThrow() >= 0L && longResult.getOrThrow() < 1000L)
            assertTrue(bytesResult.getOrThrow().size == 32)
            assertTrue(doubleResult.getOrThrow() >= 0.0 && doubleResult.getOrThrow() < 1.0)
        }
    }

    /**
     * Test WASM-JS specific constraints and limitations.
     */
    @Test
    fun testWasmJsConstraintsAndLimitations() {
        // Test very large allocations that might stress WASM memory
        val veryLargeBytesResult = secureRandom.nextBytes(4096)
        assertTrue(veryLargeBytesResult.isSuccess, "Should handle very large allocations in WASM")

        val veryLargeBytes = veryLargeBytesResult.getOrThrow()
        assertEquals(4096, veryLargeBytes.size)

        // Test many small allocations
        val smallAllocations = mutableListOf<ByteArray>()
        repeat(100) {
            val smallBytes = secureRandom.nextBytes(16).getOrThrow()
            smallAllocations.add(smallBytes)
        }

        assertEquals(100, smallAllocations.size)
        smallAllocations.forEach { bytes ->
            assertEquals(16, bytes.size)
            assertFalse(bytes.all { it == 0.toByte() }, "Each allocation should have content")
        }

        // Test that the system is still responsive after stress
        val finalResult = secureRandom.nextInt(42)
        assertTrue(finalResult.isSuccess, "Should still work after stress test")
        assertTrue(finalResult.getOrThrow() >= 0 && finalResult.getOrThrow() < 42)
    }
}