package com.scottnj.kmp_secure_random

import co.touchlab.kermit.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests and edge case testing for the SecureRandom library.
 * Tests complex scenarios, boundary conditions, and real-world usage patterns.
 */
class IntegrationAndEdgeCaseTest {

    private val logger = Logger.withTag("IntegrationTest")

    @Test
    fun testParameterValidationIntegration() {
        logger.d { "Testing parameter validation integration with Result types" }

        // Test that validation errors are properly wrapped in Results
        val validationResult = ParameterValidation.validateAndExecute {
            ParameterValidation.requirePositiveBound(42)
            ParameterValidation.requireNonNegativeSize(100)
            ParameterValidation.requireValidRange(0, 10)
            "All validations passed"
        }

        assertTrue(validationResult.isSuccess)
        assertEquals("All validations passed", validationResult.getOrNull())

        // Test validation failure
        val failureResult = ParameterValidation.validateAndExecute {
            ParameterValidation.requirePositiveBound(-1)
            "Should not reach here"
        }

        assertTrue(failureResult.isFailure)
        assertTrue(failureResult.exceptionOrNull() is InvalidParameterException)
    }

    @Test
    fun testExceptionHierarchyInPractice() {
        logger.d { "Testing exception hierarchy in realistic scenarios" }

        // Simulate platform initialization failure
        val initFailure = SecureRandomInitializationException(
            "Platform crypto API not available",
            UnsupportedOperationException("No crypto provider")
        )

        val initResult = SecureRandomResult.failure<SecureRandom>(initFailure)
        assertTrue(initResult.isFailure)
        assertTrue(initResult.exceptionOrNull() is SecureRandomInitializationException)
        assertTrue(initResult.exceptionOrNull()?.cause is UnsupportedOperationException)

        // Simulate generation failure with resource exhaustion
        val resourceFailure = InsufficientResourcesException(
            "System entropy pool depleted",
            "entropy"
        )

        val resourceResult = SecureRandomResult.failure<ByteArray>(resourceFailure)
        assertTrue(resourceResult.isFailure)
        assertTrue(resourceResult.exceptionOrNull() is InsufficientResourcesException)
        assertEquals("entropy", (resourceResult.exceptionOrNull() as InsufficientResourcesException).resourceType)

        // Simulate parameter validation in API usage
        val paramFailure = InvalidParameterException(
            "must be positive and less than 2^31",
            "bound",
            -1
        )

        val paramResult = SecureRandomResult.failure<Int>(paramFailure)
        assertTrue(paramResult.isFailure)
        val paramException = paramResult.exceptionOrNull() as InvalidParameterException
        assertEquals("bound", paramException.parameterName)
        assertEquals(-1, paramException.parameterValue)
    }

    @Test
    fun testComplexParameterValidationScenarios() {
        logger.d { "Testing complex parameter validation scenarios" }

        // Test boundary values for integers
        val maxIntBound = Int.MAX_VALUE
        ParameterValidation.requirePositiveBound(maxIntBound)

        val minValidSize = 0
        ParameterValidation.requireNonNegativeSize(minValidSize)

        // Test boundary values for longs
        val maxLongBound = Long.MAX_VALUE
        ParameterValidation.requirePositiveBound(maxLongBound)

        // Test complex range validations
        ParameterValidation.requireValidRange(Int.MIN_VALUE, Int.MAX_VALUE)
        ParameterValidation.requireValidRange(Long.MIN_VALUE, Long.MAX_VALUE)

        // Test array validations
        val singleElementArray = byteArrayOf(42)
        ParameterValidation.requireNonEmptyByteArray(singleElementArray)

        val largeArray = ByteArray(10000) { it.toByte() }
        ParameterValidation.requireNonEmptyByteArray(largeArray)

        // Test size limit validations
        ParameterValidation.requireSizeWithinLimit(0, 1000)
        ParameterValidation.requireSizeWithinLimit(1000, 1000)

        // Test range validations
        ParameterValidation.requireInRange(0, 0, 100)
        ParameterValidation.requireInRange(100, 0, 100)
        ParameterValidation.requireInRange(50, 0, 100)
    }

    @Test
    fun testParameterValidationErrorMessages() {
        logger.d { "Testing parameter validation error message quality" }

        // Test custom parameter names in error messages
        val customBoundException = assertFailsWith<InvalidParameterException> {
            ParameterValidation.requirePositiveBound(-5, "customBound")
        }
        assertTrue(customBoundException.message?.contains("customBound") == true)
        assertTrue(customBoundException.message?.contains("-5") == true)

        val customSizeException = assertFailsWith<InvalidParameterException> {
            ParameterValidation.requireNonNegativeSize(-10, "arraySize")
        }
        assertTrue(customSizeException.message?.contains("arraySize") == true)
        assertTrue(customSizeException.message?.contains("-10") == true)

        val customRangeException = assertFailsWith<InvalidParameterException> {
            ParameterValidation.requireValidRange(100, 50, "minimum", "maximum")
        }
        assertTrue(customRangeException.message?.contains("minimum") == true)
        assertTrue(customRangeException.message?.contains("maximum") == true)

        val limitException = assertFailsWith<InvalidParameterException> {
            ParameterValidation.requireSizeWithinLimit(1000, 500, "requestedSize")
        }
        assertTrue(limitException.message?.contains("requestedSize") == true)

        val rangeException = assertFailsWith<InvalidParameterException> {
            ParameterValidation.requireInRange(-1, 0, 100, "inputValue")
        }
        assertTrue(rangeException.message?.contains("inputValue") == true)
    }

    @Test
    fun testSecureRandomFactoryReliability() {
        logger.d { "Testing SecureRandom factory reliability under stress" }

        // Test multiple rapid creations
        val results = (1..100).map { createSecureRandom() }

        // All should succeed
        assertTrue(results.all { it.isSuccess }, "All factory calls should succeed")

        // All should return non-null instances
        val instances = results.map { it.getOrNull() }
        assertTrue(instances.all { it != null }, "All instances should be non-null")

        // All instances should be distinct
        val distinctInstances = instances.distinct()
        assertEquals(instances.size, distinctInstances.size, "All instances should be distinct")

        // All instances should implement SecureRandom
        assertTrue(instances.all { it is SecureRandom }, "All instances should implement SecureRandom")

        // All instances should be same concrete type
        val types = instances.map { it?.let { instance -> instance::class } }
        val uniqueTypes = types.distinct()
        assertEquals(1, uniqueTypes.size, "All instances should be same concrete type")
    }

    @Test
    fun testLoggingIntegration() {
        logger.d { "Testing logging integration throughout the library" }

        // Test that logging works at different levels
        val testLogger = Logger.withTag("TestTag")

        testLogger.v { "Verbose logging test" }
        testLogger.d { "Debug logging test" }
        testLogger.i { "Info logging test" }
        testLogger.w { "Warning logging test" }
        testLogger.e { "Error logging test" }

        // Test structured logging with context
        val secureRandom = createSecureRandom().getOrThrow()
        testLogger.i { "Created SecureRandom instance: ${secureRandom::class.simpleName}" }

        // Test logging with exceptions
        val exception = SecureRandomGenerationException("Test exception for logging")
        testLogger.e(exception) { "Exception occurred during testing" }

        // Verify logging doesn't interfere with functionality
        val result = ParameterValidation.validateAndExecute {
            testLogger.d { "Executing validation within Result context" }
            ParameterValidation.requirePositiveBound(42)
            "Success with logging"
        }

        assertTrue(result.isSuccess)
        assertEquals("Success with logging", result.getOrNull())
    }

    @Test
    fun testMemoryAndResourceHandling() {
        logger.d { "Testing memory and resource handling patterns" }

        // Test with large byte arrays
        val largeSizes = listOf(1024, 8192, 65536, 1048576)

        largeSizes.forEach { size ->
            val array = ByteArray(size)

            // Should not throw for reasonable sizes
            ParameterValidation.requireNonEmptyByteArray(array)
            ParameterValidation.requireSizeWithinLimit(size, 10 * 1024 * 1024) // 10MB limit

            logger.d { "Successfully validated array of size $size" }
        }

        // Test with null-safe operations
        val nullByteArray: ByteArray? = null
        val nonNullArray = ByteArray(10)

        // These should work properly with non-null arrays
        ParameterValidation.requireNonEmptyByteArray(nonNullArray)

        // Test exception creation doesn't leak memory
        repeat(1000) {
            val exception = SecureRandomGenerationException("Test exception $it")
            assertTrue(exception.message?.contains("$it") == true)
        }
    }

    @Test
    fun testPlatformAbstractionLayer() {
        logger.d { "Testing platform abstraction layer behavior" }

        // Test that factory function works consistently
        val results = (1..10).map {
            createSecureRandom()
        }

        // All results should have same success/failure pattern
        val successCount = results.count { it.isSuccess }
        val failureCount = results.count { it.isFailure }

        logger.i { "Factory results: $successCount successes, $failureCount failures" }

        // In current implementation, all should succeed
        assertEquals(10, successCount)
        assertEquals(0, failureCount)

        // Test that all instances follow same interface contract
        val instances = results.mapNotNull { it.getOrNull() }
        instances.forEach { instance ->
            assertTrue(instance is SecureRandom, "All instances must implement SecureRandom")

            // Test that all methods exist and throw NotImplementedError consistently
            assertFailsWith<NotImplementedError> { instance.nextInt() }
            assertFailsWith<NotImplementedError> { instance.nextLong() }
            assertFailsWith<NotImplementedError> { instance.nextBoolean() }
            assertFailsWith<NotImplementedError> { instance.nextDouble() }
            assertFailsWith<NotImplementedError> { instance.nextFloat() }
            assertFailsWith<NotImplementedError> { instance.nextBytes(10) }
            assertFailsWith<NotImplementedError> { instance.nextBytes(ByteArray(10)) }
        }
    }

    @Test
    fun testErrorHandlingConsistency() {
        logger.d { "Testing error handling consistency across the library" }

        // Test that all error creation follows same patterns
        val exceptions = listOf(
            SecureRandomException("Base exception"),
            SecureRandomInitializationException("Init failed"),
            SecureRandomGenerationException("Gen failed"),
            InvalidParameterException("Invalid", "param", 42),
            UnsupportedPlatformException("Not supported", "TestPlatform"),
            InsufficientResourcesException("No resources", "memory")
        )

        exceptions.forEach { exception ->
            // All should have non-null messages
            assertNotNull(exception.message, "Exception should have message: ${exception::class.simpleName}")

            // All should be throwable
            assertFailsWith<SecureRandomException> {
                throw exception
            }

            // All should work in Result context
            val result = SecureRandomResult.failure<String>(exception)
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

        // Test exception hierarchy consistency
        exceptions.forEach { exception ->
            assertTrue(exception is SecureRandomException, "${exception::class.simpleName} should extend SecureRandomException")
            assertTrue(exception is Exception, "${exception::class.simpleName} should extend Exception")
            assertTrue(exception is Throwable, "${exception::class.simpleName} should extend Throwable")
        }
    }

    @Test
    fun testLibraryIntegrationSmokeTest() {
        logger.d { "Running comprehensive library integration smoke test" }

        // Test complete workflow: factory -> validation -> error handling -> logging
        val workflow = ParameterValidation.validateAndExecute {
            // Step 1: Validate parameters
            ParameterValidation.requirePositiveBound(100)
            ParameterValidation.requireNonNegativeSize(50)

            // Step 2: Create SecureRandom instance
            val secureRandomResult = createSecureRandom()
            assertTrue(secureRandomResult.isSuccess, "Factory should succeed")

            val secureRandom = secureRandomResult.getOrThrow()
            logger.i { "Created ${secureRandom::class.simpleName} instance" }

            // Step 3: Test error handling (current implementation throws)
            try {
                secureRandom.nextInt(100)
                "Should not reach here"
            } catch (e: NotImplementedError) {
                "Implementation pending - this is expected"
            }
        }

        assertTrue(workflow.isSuccess)
        assertEquals("Implementation pending - this is expected", workflow.getOrNull())

        logger.i { "Library integration smoke test completed successfully" }
    }
}