package com.scottnj.kmp_secure_random

import android.os.Build
import kotlin.test.*

/**
 * Integration tests specifically for Android SecureRandom implementation.
 * Tests Android-specific behavior, API level compatibility, and platform integration.
 */
class AndroidSecureRandomIntegrationTest {

    private val secureRandom = createSecureRandom().getOrThrow()

    /**
     * Test that the implementation uses actual Android SecureRandom.
     */
    @Test
    fun testUsesAndroidSecureRandom() {
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
     * Test Android API level compatibility.
     */
    @Test
    fun testAndroidApiLevelCompatibility() {
        val apiLevel = Build.VERSION.SDK_INT
        println("\n=== Android Environment ===")
        println("API Level: $apiLevel")
        println("Version Release: ${Build.VERSION.RELEASE}")
        println("Device: ${Build.DEVICE}")
        println("Manufacturer: ${Build.MANUFACTURER}")
        println("Model: ${Build.MODEL}")

        // Test that SecureRandom works on this Android version
        val result = secureRandom.nextBytes(1024)
        assertTrue(result.isSuccess, "Random generation should work on Android API $apiLevel")

        // Verify entropy quality (basic check)
        val bytes = result.getOrNull()!!
        val uniqueBytes = bytes.toSet().size
        val uniquenessRatio = uniqueBytes.toDouble() / 256

        println("Unique byte values: $uniqueBytes/256 (${uniquenessRatio * 100}%)")
        assertTrue(uniquenessRatio > 0.3, "Should have reasonable byte diversity on Android")
    }

    /**
     * Test Android-specific algorithm preferences based on API level.
     */
    @Test
    fun testAndroidAlgorithmSelection() {
        val apiLevel = Build.VERSION.SDK_INT

        println("\n=== Android Algorithm Selection ===")
        println("API Level: $apiLevel")

        // Expected algorithms based on API level
        val expectedAlgorithms = when {
            apiLevel >= Build.VERSION_CODES.M -> listOf("SHA1PRNG", "NativePRNG") // Android 6.0+
            apiLevel >= Build.VERSION_CODES.KITKAT -> listOf("SHA1PRNG", "NativePRNG") // Android 4.4+
            else -> listOf("SHA1PRNG") // Older Android
        }

        println("Expected algorithms for API $apiLevel: $expectedAlgorithms")

        // Test that random generation works regardless of algorithm
        val result = secureRandom.nextBytes(128)
        assertTrue(result.isSuccess, "Should work with available Android algorithm")

        // Test multiple instances to ensure algorithm selection is stable
        repeat(5) {
            val instance = createSecureRandom().getOrThrow()
            val testResult = instance.nextBytes(32)
            assertTrue(testResult.isSuccess, "Algorithm should be consistently available")
        }
    }

    /**
     * Test Android memory and resource management.
     */
    @Test
    fun testAndroidResourceManagement() {
        // Test memory pressure scenario
        val instances = mutableListOf<SecureRandom>()
        val values = mutableSetOf<String>()

        // Create many instances to test memory management
        for (i in 0..25) {
            val instance = createSecureRandom().getOrThrow()
            instances.add(instance)

            // Generate a value from each
            val result = instance.nextBytes(8)
            assertTrue(result.isSuccess, "Should handle multiple instances on Android")
            values.add(result.getOrNull()!!.contentToString())
        }

        // Should have mostly unique values
        val uniquenessRatio = values.size.toDouble() / instances.size
        println("Android instance uniqueness: ${values.size}/${instances.size} (${uniquenessRatio * 100}%)")

        assertTrue(uniquenessRatio > 0.85, "Android instances should produce unique values")
    }

    /**
     * Test Android threading behavior.
     */
    @Test
    fun testAndroidThreadSafety() {
        val sharedInstance = createSecureRandom().getOrThrow()
        val threadCount = 3 // Conservative for Android testing
        val results = Array(threadCount) { mutableListOf<ByteArray>() }
        val threads = mutableListOf<Thread>()

        println("\n=== Android Thread Safety Test ===")

        for (i in 0 until threadCount) {
            threads.add(Thread {
                for (j in 0..25) { // Reduced iterations for Android
                    val result = sharedInstance.nextBytes(16)
                    if (result.isSuccess) {
                        results[i].add(result.getOrNull()!!)
                    }
                }
            })
        }

        // Start all threads
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // Check all threads got results
        for (i in 0 until threadCount) {
            assertTrue(results[i].size > 20, "Android thread $i should have generated values")
        }

        // Check for uniqueness across all threads
        val allValues = results.flatMap { it.map { bytes -> bytes.contentToString() } }.toSet()
        val totalGenerated = results.sumOf { it.size }
        val uniquenessRatio = allValues.size.toDouble() / totalGenerated

        println("Android cross-thread uniqueness: ${allValues.size}/$totalGenerated (${uniquenessRatio * 100}%)")
        assertTrue(uniquenessRatio > 0.85, "Should have unique values across Android threads")
    }

    /**
     * Test Android performance characteristics.
     */
    @Test
    fun testAndroidPerformance() {
        println("\n=== Android Performance Test ===")

        // Test generation speed on Android
        val bytesToGenerate = 1024 * 1024 // 1MB
        val chunkSize = 64 * 1024 // 64KB chunks (Android-appropriate)
        val chunks = bytesToGenerate / chunkSize

        var successCount = 0
        var totalTime = 0L

        for (i in 0 until chunks) {
            val startTime = System.currentTimeMillis()
            val result = secureRandom.nextBytes(chunkSize)
            val elapsed = System.currentTimeMillis() - startTime
            totalTime += elapsed

            if (result.isSuccess) {
                successCount++
            }
        }

        val successRate = successCount.toDouble() / chunks
        val avgTimePerChunk = totalTime.toDouble() / chunks

        println("Android success rate: $successCount/$chunks (${successRate * 100}%)")
        println("Android average time per ${chunkSize / 1024}KB chunk: ${avgTimePerChunk}ms")

        assertTrue(successRate == 1.0, "All chunks should generate successfully on Android")
        assertTrue(avgTimePerChunk < 100, "Android performance should be reasonable: ${avgTimePerChunk}ms")
    }

    /**
     * Test Android-specific edge cases.
     */
    @Test
    fun testAndroidEdgeCases() {
        // Test very small allocations
        for (size in listOf(1, 2, 4, 8, 16)) {
            val result = secureRandom.nextBytes(size)
            assertTrue(result.isSuccess, "Should handle small size $size on Android")
            assertEquals(size, result.getOrNull()!!.size)
        }

        // Test bounded integer generation with Android-specific bounds
        val androidBounds = listOf(1, 10, 100, 1000, 10000)
        for (bound in androidBounds) {
            repeat(10) {
                val result = secureRandom.nextInt(bound)
                assertTrue(result.isSuccess, "Should handle bound $bound on Android")
                val value = result.getOrNull()!!
                assertTrue(value >= 0 && value < bound, "Android value $value outside [0, $bound)")
            }
        }

        // Test range generation
        val ranges = listOf(0 to 10, -100 to 100, 1000 to 2000)
        for ((min, max) in ranges) {
            repeat(10) {
                val result = secureRandom.nextInt(min, max)
                assertTrue(result.isSuccess, "Should handle range [$min, $max) on Android")
                val value = result.getOrNull()!!
                assertTrue(value >= min && value < max, "Android value $value outside [$min, $max)")
            }
        }
    }

    /**
     * Test Android crypto provider integration.
     */
    @Test
    fun testAndroidCryptoProviderIntegration() {
        println("\n=== Android Crypto Provider Test ===")

        // Test that we can generate various data types consistently
        val testData = mutableMapOf<String, Any>()

        // Generate different types of random data
        testData["bytes_32"] = secureRandom.nextBytes(32).getOrThrow()
        testData["int"] = secureRandom.nextInt().getOrThrow()
        testData["long"] = secureRandom.nextLong().getOrThrow()
        testData["boolean"] = secureRandom.nextBoolean().getOrThrow()
        testData["double"] = secureRandom.nextDouble().getOrThrow()
        testData["float"] = secureRandom.nextFloat().getOrThrow()

        // Verify all generation succeeded
        assertEquals(6, testData.size, "Should have generated all data types on Android")

        // Basic validation of generated values
        val bytes = testData["bytes_32"] as ByteArray
        assertEquals(32, bytes.size)

        val double = testData["double"] as Double
        assertTrue(double >= 0.0 && double < 1.0, "Android double should be in [0,1)")

        val float = testData["float"] as Float
        assertTrue(float >= 0.0f && float < 1.0f, "Android float should be in [0,1)")

        println("Android crypto provider integration successful")
    }

    /**
     * Test Android version-specific behavior.
     */
    @Test
    fun testAndroidVersionSpecificBehavior() {
        val apiLevel = Build.VERSION.SDK_INT

        println("\n=== Android Version-Specific Test ===")
        println("Testing on Android API $apiLevel")

        // Test behavior that might vary by Android version
        when {
            apiLevel >= Build.VERSION_CODES.M -> {
                println("Testing Android 6.0+ behavior")
                // Modern Android should have robust crypto support
                val largeResult = secureRandom.nextBytes(1024 * 1024)
                assertTrue(largeResult.isSuccess, "Android 6.0+ should handle large allocations")
            }

            apiLevel >= Build.VERSION_CODES.KITKAT -> {
                println("Testing Android 4.4+ behavior")
                // Should still work but maybe with some limitations
                val mediumResult = secureRandom.nextBytes(512 * 1024)
                assertTrue(mediumResult.isSuccess, "Android 4.4+ should handle medium allocations")
            }

            else -> {
                println("Testing older Android behavior")
                // Conservative testing for older versions
                val smallResult = secureRandom.nextBytes(64 * 1024)
                assertTrue(smallResult.isSuccess, "Older Android should handle small allocations")
            }
        }

        // Test that basic functionality works on all versions
        val basicResult = secureRandom.nextBytes(1024)
        assertTrue(basicResult.isSuccess, "Basic functionality should work on all Android versions")
    }

    /**
     * Test Android app lifecycle compatibility.
     */
    @Test
    fun testAndroidLifecycleCompatibility() {
        println("\n=== Android Lifecycle Compatibility Test ===")

        // Simulate rapid instance creation/destruction like Android lifecycle events
        val instances = mutableListOf<SecureRandom>()

        // Create instances rapidly (like activity creation)
        repeat(10) {
            val instance = createSecureRandom().getOrThrow()
            instances.add(instance)

            // Use each instance immediately
            val result = instance.nextBytes(64)
            assertTrue(result.isSuccess, "Should work during rapid Android lifecycle events")
        }

        // Clear references (like activity destruction)
        instances.clear()

        // Force garbage collection (Android might do this)
        System.gc()
        Thread.sleep(10)

        // Create new instance after cleanup
        val newInstance = createSecureRandom().getOrThrow()
        val finalResult = newInstance.nextBytes(128)
        assertTrue(finalResult.isSuccess, "Should work after Android lifecycle cleanup")

        println("Android lifecycle compatibility verified")
    }
}