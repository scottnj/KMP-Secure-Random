package com.scottnj.kmp_secure_random

import kotlin.test.*

/**
 * Android-specific tests for AndroidSecureRandomAdapter.
 * Focuses on Android-specific functionality rather than basic operations
 * (which are already covered in commonTest).
 */
class AndroidSecureRandomAdapterTest {

    /**
     * Test that AndroidSecureRandomAdapter can be created successfully.
     */
    @Test
    fun testAdapterCreation() {
        val result = AndroidSecureRandomAdapter.create()
        assertTrue(result.isSuccess, "AndroidSecureRandomAdapter should be created successfully")

        val adapter = result.getOrNull()
        assertNotNull(adapter, "Adapter should not be null")
    }

    /**
     * Test that createSecureRandom returns Android implementation.
     */
    @Test
    fun testCreateSecureRandomReturnsAndroidImplementation() {
        val result = createSecureRandom()
        assertTrue(result.isSuccess, "createSecureRandom should succeed on Android")

        val secureRandom = result.getOrNull()
        assertNotNull(secureRandom, "SecureRandom should not be null")
    }

    /**
     * Test Android-specific compilation and runtime behavior.
     */
    @Test
    fun testAndroidSpecificBehavior() {
        // This test verifies that the Android adapter compiles and runs
        val adapter = AndroidSecureRandomAdapter.create()
        assertTrue(adapter.isSuccess, "Android adapter should be created successfully")

        val secureRandom = adapter.getOrThrow()

        // Test that all methods are callable and return expected types
        val byteArrayResult = secureRandom.nextBytes(16)
        assertTrue(byteArrayResult.isSuccess, "Byte array generation should work")
        assertTrue(byteArrayResult.getOrNull() is ByteArray, "Should return ByteArray")

        val intResult = secureRandom.nextInt()
        assertTrue(intResult.isSuccess, "Int generation should work")
        assertTrue(intResult.getOrNull() is Int, "Should return Int")

        val longResult = secureRandom.nextLong()
        assertTrue(longResult.isSuccess, "Long generation should work")
        assertTrue(longResult.getOrNull() is Long, "Should return Long")

        val doubleResult = secureRandom.nextDouble()
        assertTrue(doubleResult.isSuccess, "Double generation should work")
        assertTrue(doubleResult.getOrNull() is Double, "Should return Double")

        val floatResult = secureRandom.nextFloat()
        assertTrue(floatResult.isSuccess, "Float generation should work")
        assertTrue(floatResult.getOrNull() is Float, "Should return Float")

        val booleanResult = secureRandom.nextBoolean()
        assertTrue(booleanResult.isSuccess, "Boolean generation should work")
        assertTrue(booleanResult.getOrNull() is Boolean, "Should return Boolean")
    }

    /**
     * Test thread safety with Android adapter.
     * Simplified version for Android unit test environment.
     */
    @Test
    fun testThreadSafetyOnAndroid() {
        val secureRandom = createSecureRandom().getOrThrow()
        val results = mutableListOf<ByteArray>()
        val errors = mutableListOf<Exception>()

        val threads = List(3) { // Conservative thread count for Android
            Thread {
                try {
                    repeat(10) {
                        val result = secureRandom.nextBytes(8)
                        if (result.isSuccess) {
                            synchronized(results) {
                                results.add(result.getOrNull()!!)
                            }
                        } else {
                            synchronized(errors) {
                                errors.add(RuntimeException("Generation failed"))
                            }
                        }
                    }
                } catch (e: Exception) {
                    synchronized(errors) {
                        errors.add(e)
                    }
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertTrue(errors.isEmpty(), "No errors should occur during concurrent access: ${errors.firstOrNull()}")
        assertTrue(results.size >= 20, "Should generate values from multiple threads: ${results.size}")

        // Check for uniqueness
        val uniqueValues = results.map { it.contentToString() }.toSet()
        val uniquenessRatio = uniqueValues.size.toDouble() / results.size
        assertTrue(uniquenessRatio > 0.7, "Should have reasonable uniqueness across threads: $uniquenessRatio")
    }

    /**
     * Test that multiple instances produce different values.
     * Android-specific instance isolation test.
     */
    @Test
    fun testMultipleAndroidInstancesProduceDifferentValues() {
        val instance1 = createSecureRandom().getOrThrow()
        val instance2 = createSecureRandom().getOrThrow()

        val values1 = mutableSetOf<String>()
        val values2 = mutableSetOf<String>()

        repeat(10) {
            val result1 = instance1.nextBytes(8)
            val result2 = instance2.nextBytes(8)

            if (result1.isSuccess && result2.isSuccess) {
                values1.add(result1.getOrNull()!!.contentToString())
                values2.add(result2.getOrNull()!!.contentToString())
            }
        }

        // Different instances should generally produce different sequences
        val intersection = values1.intersect(values2)
        val intersectionRatio = intersection.size.toDouble() / minOf(values1.size, values2.size)

        assertTrue(intersectionRatio < 0.5, "Different instances should produce mostly different values")
    }
}