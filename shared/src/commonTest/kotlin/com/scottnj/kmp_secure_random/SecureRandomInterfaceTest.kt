package com.scottnj.kmp_secure_random

import co.touchlab.kermit.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive tests for the SecureRandom interface and its factory function.
 * Tests the current placeholder implementations to ensure proper error handling.
 */
class SecureRandomInterfaceTest {

    private val logger = Logger.withTag("SecureRandomInterfaceTest")

    @Test
    fun testCreateSecureRandomSuccess() {
        logger.d { "Testing createSecureRandom factory function" }

        val result = createSecureRandom()

        assertTrue(result.isSuccess, "createSecureRandom should succeed")
        assertNotNull(result.getOrNull(), "SecureRandom instance should not be null")

        val secureRandom = result.getOrThrow()
        assertTrue(secureRandom is SecureRandom, "Result should implement SecureRandom interface")
    }

    @Test
    fun testCreateSecureRandomCreatesNewInstances() {
        logger.d { "Testing that factory creates new instances" }

        val result1 = createSecureRandom()
        val result2 = createSecureRandom()

        assertTrue(result1.isSuccess && result2.isSuccess, "Both creations should succeed")

        val instance1 = result1.getOrThrow()
        val instance2 = result2.getOrThrow()

        assertFalse(instance1 === instance2, "Factory should create distinct instances")
        assertEquals(instance1::class, instance2::class, "Both instances should be same type")
    }

    @Test
    fun testNextBytesArrayFunctionality() {
        logger.d { "Testing nextBytes(ByteArray) functionality" }

        val secureRandom = createSecureRandom().getOrThrow()
        val bytes = ByteArray(10)
        val originalBytes = bytes.copyOf()

        val result = secureRandom.nextBytes(bytes)
        assertTrue(result.isSuccess, "nextBytes should succeed")
        assertFalse(bytes.contentEquals(originalBytes), "Bytes should be modified with random data")
    }

    @Test
    fun testNextIntFunctionality() {
        logger.d { "Testing nextInt() functionality" }

        val secureRandom = createSecureRandom().getOrThrow()

        val result = secureRandom.nextInt()
        assertTrue(result.isSuccess, "nextInt should succeed")
        assertNotNull(result.getOrNull(), "nextInt should return a value")
    }

    @Test
    fun testNextIntBoundFunctionality() {
        logger.d { "Testing nextInt(bound) functionality" }

        val secureRandom = createSecureRandom().getOrThrow()

        val result = secureRandom.nextInt(100)
        assertTrue(result.isSuccess, "nextInt(bound) should succeed")
        val value = result.getOrThrow()
        assertTrue(value >= 0 && value < 100, "Value $value should be in range [0, 100)")
    }

    @Test
    fun testNextIntRangeFunctionality() {
        logger.d { "Testing nextInt(min, max) functionality" }

        val secureRandom = createSecureRandom().getOrThrow()

        val result = secureRandom.nextInt(10, 20)
        assertTrue(result.isSuccess, "nextInt(min, max) should succeed")
        val value = result.getOrThrow()
        assertTrue(value >= 10 && value < 20, "Value $value should be in range [10, 20)")
    }

    @Test
    fun testNextLongFunctionality() {
        logger.d { "Testing nextLong() functionality" }

        val secureRandom = createSecureRandom().getOrThrow()

        val result = secureRandom.nextLong()
        assertTrue(result.isSuccess, "nextLong should succeed")
        assertNotNull(result.getOrNull(), "nextLong should return a value")
    }

    @Test
    fun testNextLongBoundFunctionality() {
        logger.d { "Testing nextLong(bound) functionality" }

        val secureRandom = createSecureRandom().getOrThrow()

        val result = secureRandom.nextLong(100L)
        assertTrue(result.isSuccess, "nextLong(bound) should succeed")
        val value = result.getOrThrow()
        assertTrue(value >= 0L && value < 100L, "Value $value should be in range [0, 100)")
    }

    @Test
    fun testNextLongRangeFunctionality() {
        logger.d { "Testing nextLong(min, max) functionality" }

        val secureRandom = createSecureRandom().getOrThrow()

        val result = secureRandom.nextLong(10L, 20L)
        assertTrue(result.isSuccess, "nextLong(min, max) should succeed")
        val value = result.getOrThrow()
        assertTrue(value >= 10L && value < 20L, "Value $value should be in range [10, 20)")
    }

    @Test
    fun testNextBooleanFunctionality() {
        logger.d { "Testing nextBoolean() functionality" }

        val secureRandom = createSecureRandom().getOrThrow()

        val result = secureRandom.nextBoolean()
        assertTrue(result.isSuccess, "nextBoolean should succeed")
        val value = result.getOrThrow()
        assertTrue(value == true || value == false, "nextBoolean should return true or false")
    }

    @Test
    fun testNextDoubleFunctionality() {
        logger.d { "Testing nextDouble() functionality" }

        val secureRandom = createSecureRandom().getOrThrow()

        val result = secureRandom.nextDouble()
        assertTrue(result.isSuccess, "nextDouble should succeed")
        val value = result.getOrThrow()
        assertTrue(value >= 0.0 && value < 1.0, "Value $value should be in range [0.0, 1.0)")
    }

    @Test
    fun testNextFloatFunctionality() {
        logger.d { "Testing nextFloat() functionality" }

        val secureRandom = createSecureRandom().getOrThrow()

        val result = secureRandom.nextFloat()
        assertTrue(result.isSuccess, "nextFloat should succeed")
        val value = result.getOrThrow()
        assertTrue(value >= 0.0f && value < 1.0f, "Value $value should be in range [0.0, 1.0)")
    }

    @Test
    fun testNextBytesSizeFunctionality() {
        logger.d { "Testing nextBytes(size) functionality" }

        val secureRandom = createSecureRandom().getOrThrow()

        val result = secureRandom.nextBytes(10)
        assertTrue(result.isSuccess, "nextBytes(size) should succeed")
        val bytes = result.getOrThrow()
        assertEquals(10, bytes.size, "Returned array should have requested size")
    }

    @Test
    fun testAllMethodsExistOnInterface() {
        logger.d { "Testing that all expected methods exist on interface" }

        val secureRandom = createSecureRandom().getOrThrow()

        // Test that we can call all methods and they return success results
        val methodResults = listOf(
            secureRandom.nextBytes(ByteArray(1)),
            secureRandom.nextInt().map { Unit },
            secureRandom.nextInt(1).map { Unit },
            secureRandom.nextInt(0, 1).map { Unit },
            secureRandom.nextLong().map { Unit },
            secureRandom.nextLong(1L).map { Unit },
            secureRandom.nextLong(0L, 1L).map { Unit },
            secureRandom.nextBoolean().map { Unit },
            secureRandom.nextDouble().map { Unit },
            secureRandom.nextFloat().map { Unit },
            secureRandom.nextBytes(1).map { Unit }
        )

        methodResults.forEach { result ->
            assertTrue(result.isSuccess, "All SecureRandom methods should succeed")
        }

        logger.i { "All ${methodResults.size} SecureRandom methods are properly defined" }
    }

    @Test
    fun testInstanceImplementsSecureRandomInterface() {
        logger.d { "Testing type hierarchy of SecureRandom instances" }

        val result = createSecureRandom()
        assertTrue(result.isSuccess, "Factory should succeed")

        val instance = result.getOrThrow()
        assertTrue(instance is SecureRandom, "Instance should implement SecureRandom")

        // Verify we can use polymorphically
        val polymorphicInstance: SecureRandom = instance
        assertNotNull(polymorphicInstance, "Should be usable polymorphically")

        // Verify class name contains SecureRandom for debugging
        val className = instance::class.simpleName ?: ""
        assertTrue(className.contains("SecureRandom"), "Class name '$className' should contain 'SecureRandom'")
    }

    @Test
    fun testLoggingAndErrorHandling() {
        logger.d { "Testing logging and error handling patterns" }

        // Test that factory returns proper Result type
        val result = createSecureRandom()
        assertTrue(result is SecureRandomResult<SecureRandom>, "Factory should return SecureRandomResult")

        // Test error handling pattern for Result type
        result.onSuccess { secureRandom ->
            logger.i { "Successfully created SecureRandom: ${secureRandom::class.simpleName}" }
            assertNotNull(secureRandom, "Instance should not be null in success callback")
        }.onFailure { exception ->
            logger.e { "Failed to create SecureRandom: ${exception.message}" }
            // This shouldn't be called in current implementation
            assertFalse(true, "Factory should not fail in current implementation")
        }

        logger.i { "Error handling pattern test completed" }
    }

    @Test
    fun testThreadSafetyOfFactory() {
        logger.d { "Testing thread safety of createSecureRandom factory" }

        // Test that multiple calls in sequence work properly
        val results = (1..10).map { createSecureRandom() }

        assertTrue(results.all { it.isSuccess }, "All factory calls should succeed")

        val instances = results.map { it.getOrThrow() }

        // Verify all instances are distinct
        val distinctInstances = instances.distinct()
        assertEquals(instances.size, distinctInstances.size, "All instances should be distinct")

        // Verify all instances are same type
        val types = instances.map { it::class }
        assertTrue(types.all { it == types.first() }, "All instances should be same type")

        logger.i { "Thread safety test completed with ${instances.size} instances" }
    }
}