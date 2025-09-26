package com.scottnj.kmp_secure_random

import co.touchlab.kermit.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Advanced tests for SecureRandomResult focusing on edge cases and complex scenarios.
 */
class SecureRandomResultAdvancedTest {

    private val logger = Logger.withTag("SecureRandomResultAdvancedTest")

    @Test
    fun testNestedResultChaining() {
        logger.d { "Testing nested Result chaining patterns" }

        val result = SecureRandomResult.success(42)
            .flatMap { value ->
                SecureRandomResult.success(value * 2)
            }
            .flatMap { value ->
                if (value > 50) {
                    SecureRandomResult.success(value + 10)
                } else {
                    SecureRandomResult.failure(SecureRandomGenerationException("Value too small"))
                }
            }

        assertTrue(result.isSuccess)
        assertEquals(94, result.getOrNull())
    }

    @Test
    fun testFlatMapWithMultipleFailures() {
        logger.d { "Testing flatMap behavior with multiple failure points" }

        val originalException = SecureRandomInitializationException("Original failure")
        val result = SecureRandomResult.failure<Int>(originalException)
            .flatMap { value ->
                // This should not be called
                SecureRandomResult.failure<Int>(SecureRandomGenerationException("Should not reach here"))
            }
            .flatMap { value ->
                // This should also not be called
                SecureRandomResult.success(value * 2)
            }

        assertTrue(result.isFailure)
        assertEquals(originalException, result.exceptionOrNull())
    }

    @Test
    fun testMapTransformationTypes() {
        logger.d { "Testing map transformations with different types" }

        // Int to String
        val intToString = SecureRandomResult.success(42)
            .map { it.toString() }

        assertTrue(intToString.isSuccess)
        assertEquals("42", intToString.getOrNull())

        // String to ByteArray
        val stringToBytes = intToString
            .map { it.encodeToByteArray() }

        assertTrue(stringToBytes.isSuccess)
        val bytes = stringToBytes.getOrNull()
        assertNotNull(bytes)
        assertTrue(bytes.contentEquals("42".encodeToByteArray()))

        // ByteArray to List
        val bytesToList = stringToBytes
            .map { it.toList() }

        assertTrue(bytesToList.isSuccess)
        assertEquals(listOf(52.toByte(), 50.toByte()), bytesToList.getOrNull())
    }

    @Test
    fun testCallbackExecutionOrder() {
        logger.d { "Testing callback execution order" }

        val executionOrder = mutableListOf<String>()

        val result = SecureRandomResult.success(42)
            .onSuccess { executionOrder.add("first-success") }
            .map {
                executionOrder.add("map")
                it * 2
            }
            .onSuccess { executionOrder.add("second-success") }
            .flatMap {
                executionOrder.add("flatMap")
                SecureRandomResult.success(it + 10)
            }
            .onSuccess { executionOrder.add("third-success") }

        assertTrue(result.isSuccess)
        assertEquals(94, result.getOrNull())

        val expectedOrder = listOf("first-success", "map", "second-success", "flatMap", "third-success")
        assertEquals(expectedOrder, executionOrder)
    }

    @Test
    fun testFailureCallbackExecutionOrder() {
        logger.d { "Testing failure callback execution order" }

        val executionOrder = mutableListOf<String>()
        val exception = SecureRandomGenerationException("Test failure")

        val result = SecureRandomResult.failure<Int>(exception)
            .onFailure { executionOrder.add("first-failure") }
            .map {
                executionOrder.add("map-should-not-execute")
                it * 2
            }
            .onFailure { executionOrder.add("second-failure") }
            .flatMap {
                executionOrder.add("flatMap-should-not-execute")
                SecureRandomResult.success(it + 10)
            }
            .onFailure { executionOrder.add("third-failure") }

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())

        val expectedOrder = listOf("first-failure", "second-failure", "third-failure")
        assertEquals(expectedOrder, executionOrder)
        assertFalse(executionOrder.contains("map-should-not-execute"))
        assertFalse(executionOrder.contains("flatMap-should-not-execute"))
    }

    @Test
    fun testExceptionTypePreservation() {
        logger.d { "Testing that specific exception types are preserved through chains" }

        val initException = SecureRandomInitializationException("Init failed")
        val genException = SecureRandomGenerationException("Gen failed")
        val paramException = InvalidParameterException("Invalid", "param", 42)

        // Test each exception type preservation
        val initResult = SecureRandomResult.failure<Int>(initException)
            .map { it * 2 }
            .flatMap { SecureRandomResult.success(it) }

        assertTrue(initResult.isFailure)
        assertTrue(initResult.exceptionOrNull() is SecureRandomInitializationException)
        assertEquals(initException, initResult.exceptionOrNull())

        val genResult = SecureRandomResult.failure<Int>(genException)
            .map { it * 2 }

        assertTrue(genResult.isFailure)
        assertTrue(genResult.exceptionOrNull() is SecureRandomGenerationException)

        val paramResult = SecureRandomResult.failure<Int>(paramException)
            .flatMap { SecureRandomResult.success(it) }

        assertTrue(paramResult.isFailure)
        assertTrue(paramResult.exceptionOrNull() is InvalidParameterException)
        assertEquals("param", (paramResult.exceptionOrNull() as InvalidParameterException).parameterName)
    }

    @Test
    fun testGetOrDefaultWithComplexTypes() {
        logger.d { "Testing getOrDefault with complex types" }

        val successResult = SecureRandomResult.success(listOf(1, 2, 3))
        assertEquals(listOf(1, 2, 3), successResult.getOrDefault(emptyList()))

        val failureResult = SecureRandomResult.failure<List<Int>>(SecureRandomGenerationException("Failed"))
        assertEquals(emptyList(), failureResult.getOrDefault(emptyList()))
        assertEquals(listOf(42), failureResult.getOrDefault(listOf(42)))

        // Test with nullable types
        val nullableSuccess = SecureRandomResult.success<String?>("value")
        assertEquals("value", nullableSuccess.getOrDefault(null))

        val nullableFailure = SecureRandomResult.failure<String?>(SecureRandomGenerationException("Failed"))
        assertEquals(null, nullableFailure.getOrDefault(null))
        assertEquals("default", nullableFailure.getOrDefault("default"))
    }

    @Test
    fun testRunCatchingWithDifferentExceptionTypes() {
        logger.d { "Testing runCatching with various SecureRandomException subtypes" }

        // Test with SecureRandomInitializationException
        val initResult = SecureRandomResult.runCatching {
            throw SecureRandomInitializationException("Init failed")
        }
        assertTrue(initResult.isFailure)
        assertTrue(initResult.exceptionOrNull() is SecureRandomInitializationException)

        // Test with SecureRandomGenerationException
        val genResult = SecureRandomResult.runCatching {
            throw SecureRandomGenerationException("Gen failed")
        }
        assertTrue(genResult.isFailure)
        assertTrue(genResult.exceptionOrNull() is SecureRandomGenerationException)

        // Test with InvalidParameterException
        val paramResult = SecureRandomResult.runCatching {
            throw InvalidParameterException("Invalid", "test", 0)
        }
        assertTrue(paramResult.isFailure)
        assertTrue(paramResult.exceptionOrNull() is InvalidParameterException)

        // Test with UnsupportedPlatformException
        val platformResult = SecureRandomResult.runCatching {
            throw UnsupportedPlatformException("Not supported", "TestPlatform")
        }
        assertTrue(platformResult.isFailure)
        assertTrue(platformResult.exceptionOrNull() is UnsupportedPlatformException)

        // Test with InsufficientResourcesException
        val resourceResult = SecureRandomResult.runCatching {
            throw InsufficientResourcesException("No resources", "memory")
        }
        assertTrue(resourceResult.isFailure)
        assertTrue(resourceResult.exceptionOrNull() is InsufficientResourcesException)
    }

    @Test
    fun testRunCatchingWithNestedExceptions() {
        logger.d { "Testing runCatching with nested exceptions" }

        val cause = IllegalStateException("Root cause")
        val result = SecureRandomResult.runCatching {
            throw SecureRandomGenerationException("Wrapper", cause)
        }

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as SecureRandomGenerationException
        assertEquals("Wrapper", exception.message)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun testResultEquality() {
        logger.d { "Testing Result equality behavior" }

        val success1 = SecureRandomResult.success(42)
        val success2 = SecureRandomResult.success(42)
        val success3 = SecureRandomResult.success(43)

        val exception1 = SecureRandomGenerationException("Test")
        val exception2 = SecureRandomGenerationException("Test")
        val failure1 = SecureRandomResult.failure<Int>(exception1)
        val failure2 = SecureRandomResult.failure<Int>(exception2)

        // Note: Results might not implement equals, so we test behavior not equality
        assertEquals(success1.getOrNull(), success2.getOrNull())
        assertNotNull(success3.getOrNull())
        assertTrue(success3.getOrNull() != success1.getOrNull())

        assertEquals(failure1.exceptionOrNull()?.message, failure2.exceptionOrNull()?.message)
    }

    @Test
    fun testLargeValueHandling() {
        logger.d { "Testing Result with large values" }

        val largeInt = Int.MAX_VALUE
        val largeLong = Long.MAX_VALUE
        val largeByteArray = ByteArray(10000) { it.toByte() }

        val intResult = SecureRandomResult.success(largeInt)
        assertTrue(intResult.isSuccess)
        assertEquals(largeInt, intResult.getOrNull())

        val longResult = SecureRandomResult.success(largeLong)
        assertTrue(longResult.isSuccess)
        assertEquals(largeLong, longResult.getOrNull())

        val arrayResult = SecureRandomResult.success(largeByteArray)
        assertTrue(arrayResult.isSuccess)
        val resultArray = arrayResult.getOrNull()
        assertNotNull(resultArray)
        assertEquals(10000, resultArray.size)
        assertTrue(resultArray.contentEquals(largeByteArray))
    }

    @Test
    fun testUnitTypeHandling() {
        logger.d { "Testing Result with Unit type" }

        val unitSuccess: SecureRandomUnitResult = SecureRandomResult.success(Unit)
        assertTrue(unitSuccess.isSuccess)
        assertEquals(Unit, unitSuccess.getOrNull())
        assertEquals(Unit, unitSuccess.getOrDefault(Unit))

        val unitFailure: SecureRandomUnitResult = SecureRandomResult.failure(
            SecureRandomGenerationException("Unit operation failed")
        )
        assertTrue(unitFailure.isFailure)
        assertNull(unitFailure.getOrNull())
        assertEquals(Unit, unitFailure.getOrDefault(Unit))

        // Test chaining with Unit
        var called = false
        unitSuccess.onSuccess {
            called = true
            assertEquals(Unit, it)
        }
        assertTrue(called)
    }
}