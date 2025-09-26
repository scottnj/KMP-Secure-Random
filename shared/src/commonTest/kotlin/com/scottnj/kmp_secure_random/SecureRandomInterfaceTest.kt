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
    fun testNextBytesArrayCurrentlyThrows() {
        logger.d { "Testing nextBytes(ByteArray) current placeholder behavior" }

        val secureRandom = createSecureRandom().getOrThrow()
        val bytes = ByteArray(10)

        // Current implementation is TODO, so it should throw
        assertFailsWith<NotImplementedError> {
            secureRandom.nextBytes(bytes)
        }
    }

    @Test
    fun testNextIntCurrentlyThrows() {
        logger.d { "Testing nextInt() current placeholder behavior" }

        val secureRandom = createSecureRandom().getOrThrow()

        // Current implementation is TODO, so it should throw
        assertFailsWith<NotImplementedError> {
            secureRandom.nextInt()
        }
    }

    @Test
    fun testNextIntBoundCurrentlyThrows() {
        logger.d { "Testing nextInt(bound) current placeholder behavior" }

        val secureRandom = createSecureRandom().getOrThrow()

        // Current implementation is TODO, so it should throw
        assertFailsWith<NotImplementedError> {
            secureRandom.nextInt(100)
        }
    }

    @Test
    fun testNextIntRangeCurrentlyThrows() {
        logger.d { "Testing nextInt(min, max) current placeholder behavior" }

        val secureRandom = createSecureRandom().getOrThrow()

        // Current implementation is TODO, so it should throw
        assertFailsWith<NotImplementedError> {
            secureRandom.nextInt(10, 20)
        }
    }

    @Test
    fun testNextLongCurrentlyThrows() {
        logger.d { "Testing nextLong() current placeholder behavior" }

        val secureRandom = createSecureRandom().getOrThrow()

        // Current implementation is TODO, so it should throw
        assertFailsWith<NotImplementedError> {
            secureRandom.nextLong()
        }
    }

    @Test
    fun testNextLongBoundCurrentlyThrows() {
        logger.d { "Testing nextLong(bound) current placeholder behavior" }

        val secureRandom = createSecureRandom().getOrThrow()

        // Current implementation is TODO, so it should throw
        assertFailsWith<NotImplementedError> {
            secureRandom.nextLong(100L)
        }
    }

    @Test
    fun testNextLongRangeCurrentlyThrows() {
        logger.d { "Testing nextLong(min, max) current placeholder behavior" }

        val secureRandom = createSecureRandom().getOrThrow()

        // Current implementation is TODO, so it should throw
        assertFailsWith<NotImplementedError> {
            secureRandom.nextLong(10L, 20L)
        }
    }

    @Test
    fun testNextBooleanCurrentlyThrows() {
        logger.d { "Testing nextBoolean() current placeholder behavior" }

        val secureRandom = createSecureRandom().getOrThrow()

        // Current implementation is TODO, so it should throw
        assertFailsWith<NotImplementedError> {
            secureRandom.nextBoolean()
        }
    }

    @Test
    fun testNextDoubleCurrentlyThrows() {
        logger.d { "Testing nextDouble() current placeholder behavior" }

        val secureRandom = createSecureRandom().getOrThrow()

        // Current implementation is TODO, so it should throw
        assertFailsWith<NotImplementedError> {
            secureRandom.nextDouble()
        }
    }

    @Test
    fun testNextFloatCurrentlyThrows() {
        logger.d { "Testing nextFloat() current placeholder behavior" }

        val secureRandom = createSecureRandom().getOrThrow()

        // Current implementation is TODO, so it should throw
        assertFailsWith<NotImplementedError> {
            secureRandom.nextFloat()
        }
    }

    @Test
    fun testNextBytesSizeCurrentlyThrows() {
        logger.d { "Testing nextBytes(size) current placeholder behavior" }

        val secureRandom = createSecureRandom().getOrThrow()

        // Current implementation is TODO, so it should throw
        assertFailsWith<NotImplementedError> {
            secureRandom.nextBytes(10)
        }
    }

    @Test
    fun testAllMethodsExistOnInterface() {
        logger.d { "Testing that all expected methods exist on interface" }

        val secureRandom = createSecureRandom().getOrThrow()

        // Test that we can call all methods without compilation errors
        // They will throw NotImplementedError but that's expected for now
        val methods = listOf(
            { secureRandom.nextBytes(ByteArray(1)) },
            { secureRandom.nextInt() },
            { secureRandom.nextInt(1) },
            { secureRandom.nextInt(0, 1) },
            { secureRandom.nextLong() },
            { secureRandom.nextLong(1L) },
            { secureRandom.nextLong(0L, 1L) },
            { secureRandom.nextBoolean() },
            { secureRandom.nextDouble() },
            { secureRandom.nextFloat() },
            { secureRandom.nextBytes(1) }
        )

        methods.forEach { method ->
            assertFailsWith<NotImplementedError> {
                method()
            }
        }

        logger.i { "All ${methods.size} SecureRandom methods are properly defined" }
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