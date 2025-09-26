package com.scottnj.kmp_secure_random

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive tests for SecureRandomResult functionality and error handling.
 */
class SecureRandomResultTest {

    @Test
    fun testSuccessResult() {
        val result = SecureRandomResult.success(42)

        assertTrue(result.isSuccess)
        assertFalse(result.isFailure)
        assertEquals(42, result.getOrNull())
        assertEquals(42, result.getOrThrow())
        assertEquals(42, result.getOrDefault(100))
        assertNull(result.exceptionOrNull())
    }

    @Test
    fun testFailureResult() {
        val exception = SecureRandomGenerationException("Test failure")
        val result = SecureRandomResult.failure<Int>(exception)

        assertFalse(result.isSuccess)
        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
        assertEquals(100, result.getOrDefault(100))
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun testFailureResultThrows() {
        val exception = SecureRandomGenerationException("Test failure")
        val result = SecureRandomResult.failure<Int>(exception)

        assertFailsWith<SecureRandomGenerationException> {
            result.getOrThrow()
        }
    }

    @Test
    fun testMapOnSuccess() {
        val result = SecureRandomResult.success(42)
        val mapped = result.map { it * 2 }

        assertTrue(mapped.isSuccess)
        assertEquals(84, mapped.getOrNull())
    }

    @Test
    fun testMapOnFailure() {
        val exception = SecureRandomGenerationException("Test failure")
        val result = SecureRandomResult.failure<Int>(exception)
        val mapped = result.map { it * 2 }

        assertTrue(mapped.isFailure)
        assertEquals(exception, mapped.exceptionOrNull())
    }

    @Test
    fun testFlatMapOnSuccess() {
        val result = SecureRandomResult.success(42)
        val flatMapped = result.flatMap { SecureRandomResult.success(it * 2) }

        assertTrue(flatMapped.isSuccess)
        assertEquals(84, flatMapped.getOrNull())
    }

    @Test
    fun testFlatMapOnFailure() {
        val exception = SecureRandomGenerationException("Test failure")
        val result = SecureRandomResult.failure<Int>(exception)
        val flatMapped = result.flatMap { SecureRandomResult.success(it * 2) }

        assertTrue(flatMapped.isFailure)
        assertEquals(exception, flatMapped.exceptionOrNull())
    }

    @Test
    fun testFlatMapReturnsFailure() {
        val result = SecureRandomResult.success(42)
        val newException = SecureRandomGenerationException("New failure")
        val flatMapped = result.flatMap { SecureRandomResult.failure<Int>(newException) }

        assertTrue(flatMapped.isFailure)
        assertEquals(newException, flatMapped.exceptionOrNull())
    }

    @Test
    fun testOnSuccessCallback() {
        var called = false
        var value = 0

        val result = SecureRandomResult.success(42)
        result.onSuccess {
            called = true
            value = it
        }

        assertTrue(called)
        assertEquals(42, value)
    }

    @Test
    fun testOnSuccessNotCalledOnFailure() {
        var called = false

        val exception = SecureRandomGenerationException("Test failure")
        val result = SecureRandomResult.failure<Int>(exception)
        result.onSuccess { called = true }

        assertFalse(called)
    }

    @Test
    fun testOnFailureCallback() {
        var called = false
        var receivedException: SecureRandomException? = null

        val exception = SecureRandomGenerationException("Test failure")
        val result = SecureRandomResult.failure<Int>(exception)
        result.onFailure {
            called = true
            receivedException = it
        }

        assertTrue(called)
        assertEquals(exception, receivedException)
    }

    @Test
    fun testOnFailureNotCalledOnSuccess() {
        var called = false

        val result = SecureRandomResult.success(42)
        result.onFailure { called = true }

        assertFalse(called)
    }

    @Test
    fun testRunCatchingSuccess() {
        val result = SecureRandomResult.runCatching { 42 }

        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun testRunCatchingFailure() {
        val exception = SecureRandomGenerationException("Test failure")
        val result = SecureRandomResult.runCatching { throw exception }

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun testRunCatchingIgnoresNonSecureRandomExceptions() {
        // runCatching should only catch SecureRandomException and its subclasses
        assertFailsWith<IllegalArgumentException> {
            SecureRandomResult.runCatching { throw IllegalArgumentException("Not a SecureRandomException") }
        }
    }

    @Test
    fun testChaining() {
        val result = SecureRandomResult.success(42)
            .map { it * 2 }
            .flatMap { SecureRandomResult.success(it + 10) }
            .onSuccess { /* callback */ }

        assertTrue(result.isSuccess)
        assertEquals(94, result.getOrNull())
    }

    @Test
    fun testChainingWithFailure() {
        val exception = SecureRandomGenerationException("Test failure")
        val result = SecureRandomResult.failure<Int>(exception)
            .map { it * 2 }
            .flatMap { SecureRandomResult.success(it + 10) }
            .onFailure { /* callback */ }

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun testUnitResultTypealias() {
        val unitResult: SecureRandomUnitResult = SecureRandomResult.success(Unit)
        assertTrue(unitResult.isSuccess)
        assertEquals(Unit, unitResult.getOrNull())
    }
}