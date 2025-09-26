package com.scottnj.kmp_secure_random

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for parameter validation utilities used across SecureRandom operations.
 */
class ParameterValidationTest {

    @Test
    fun testRequirePositiveBoundInt() {
        // Valid positive bounds should not throw
        ParameterValidation.requirePositiveBound(1)
        ParameterValidation.requirePositiveBound(100)
        ParameterValidation.requirePositiveBound(Int.MAX_VALUE)

        // Zero should throw
        assertFailsWith<InvalidParameterException> {
            ParameterValidation.requirePositiveBound(0)
        }

        // Negative should throw
        assertFailsWith<InvalidParameterException> {
            ParameterValidation.requirePositiveBound(-1)
        }
        assertFailsWith<InvalidParameterException> {
            ParameterValidation.requirePositiveBound(Int.MIN_VALUE)
        }
    }

    @Test
    fun testRequirePositiveBoundLong() {
        // Valid positive bounds should not throw
        ParameterValidation.requirePositiveBound(1L)
        ParameterValidation.requirePositiveBound(100L)
        ParameterValidation.requirePositiveBound(Long.MAX_VALUE)

        // Zero should throw
        assertFailsWith<InvalidParameterException> {
            ParameterValidation.requirePositiveBound(0L)
        }

        // Negative should throw
        assertFailsWith<InvalidParameterException> {
            ParameterValidation.requirePositiveBound(-1L)
        }
        assertFailsWith<InvalidParameterException> {
            ParameterValidation.requirePositiveBound(Long.MIN_VALUE)
        }
    }

    @Test
    fun testRequireNonNegativeSize() {
        // Valid non-negative sizes should not throw
        ParameterValidation.requireNonNegativeSize(0)
        ParameterValidation.requireNonNegativeSize(1)
        ParameterValidation.requireNonNegativeSize(100)
        ParameterValidation.requireNonNegativeSize(Int.MAX_VALUE)

        // Negative should throw
        assertFailsWith<InvalidParameterException> {
            ParameterValidation.requireNonNegativeSize(-1)
        }
        assertFailsWith<InvalidParameterException> {
            ParameterValidation.requireNonNegativeSize(Int.MIN_VALUE)
        }
    }

    @Test
    fun testRequireValidRangeInt() {
        // Valid ranges should not throw
        ParameterValidation.requireValidRange(0, 1)
        ParameterValidation.requireValidRange(-10, 10)
        ParameterValidation.requireValidRange(Int.MIN_VALUE, Int.MAX_VALUE)

        // Equal values should throw
        assertFailsWith<InvalidParameterException> {
            ParameterValidation.requireValidRange(5, 5)
        }

        // Reversed range should throw
        assertFailsWith<InvalidParameterException> {
            ParameterValidation.requireValidRange(10, 5)
        }
    }

    @Test
    fun testRequireValidRangeLong() {
        // Valid ranges should not throw
        ParameterValidation.requireValidRange(0L, 1L)
        ParameterValidation.requireValidRange(-10L, 10L)
        ParameterValidation.requireValidRange(Long.MIN_VALUE, Long.MAX_VALUE)

        // Equal values should throw
        assertFailsWith<InvalidParameterException> {
            ParameterValidation.requireValidRange(5L, 5L)
        }

        // Reversed range should throw
        assertFailsWith<InvalidParameterException> {
            ParameterValidation.requireValidRange(10L, 5L)
        }
    }

    @Test
    fun testRequireNonEmptyByteArray() {
        // Non-empty arrays should not throw
        ParameterValidation.requireNonEmptyByteArray(byteArrayOf(1))
        ParameterValidation.requireNonEmptyByteArray(byteArrayOf(1, 2, 3))

        // Empty array should throw
        assertFailsWith<InvalidParameterException> {
            ParameterValidation.requireNonEmptyByteArray(byteArrayOf())
        }
    }

    @Test
    fun testRequireSizeWithinLimit() {
        // Valid sizes should not throw
        ParameterValidation.requireSizeWithinLimit(0, 100)
        ParameterValidation.requireSizeWithinLimit(50, 100)
        ParameterValidation.requireSizeWithinLimit(100, 100)

        // Exceeding limit should throw
        assertFailsWith<InvalidParameterException> {
            ParameterValidation.requireSizeWithinLimit(101, 100)
        }
    }

    @Test
    fun testRequireInRangeInt() {
        // Valid values should not throw
        ParameterValidation.requireInRange(5, 0, 10)
        ParameterValidation.requireInRange(0, 0, 10)
        ParameterValidation.requireInRange(10, 0, 10)

        // Out of range should throw
        assertFailsWith<InvalidParameterException> {
            ParameterValidation.requireInRange(-1, 0, 10)
        }
        assertFailsWith<InvalidParameterException> {
            ParameterValidation.requireInRange(11, 0, 10)
        }
    }

    @Test
    fun testRequireInRangeLong() {
        // Valid values should not throw
        ParameterValidation.requireInRange(5L, 0L, 10L)
        ParameterValidation.requireInRange(0L, 0L, 10L)
        ParameterValidation.requireInRange(10L, 0L, 10L)

        // Out of range should throw
        assertFailsWith<InvalidParameterException> {
            ParameterValidation.requireInRange(-1L, 0L, 10L)
        }
        assertFailsWith<InvalidParameterException> {
            ParameterValidation.requireInRange(11L, 0L, 10L)
        }
    }

    @Test
    fun testValidateAndExecuteSuccess() {
        val result = ParameterValidation.validateAndExecute {
            ParameterValidation.requirePositiveBound(42)
            "success"
        }

        assertTrue(result.isSuccess)
        assertEquals("success", result.getOrNull())
    }

    @Test
    fun testValidateAndExecuteFailure() {
        val result = ParameterValidation.validateAndExecute {
            ParameterValidation.requirePositiveBound(-1)
            "should not reach here"
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidParameterException)
    }

    @Test
    fun testParameterExceptionMessages() {
        val boundException = assertFailsWith<InvalidParameterException> {
            ParameterValidation.requirePositiveBound(-5, "testBound")
        }
        assertTrue(boundException.message?.contains("testBound") == true)
        assertTrue(boundException.message?.contains("-5") == true)

        val sizeException = assertFailsWith<InvalidParameterException> {
            ParameterValidation.requireNonNegativeSize(-1, "testSize")
        }
        assertTrue(sizeException.message?.contains("testSize") == true)
        assertTrue(sizeException.message?.contains("-1") == true)

        val rangeException = assertFailsWith<InvalidParameterException> {
            ParameterValidation.requireValidRange(10, 5, "testMin", "testMax")
        }
        assertTrue(rangeException.message?.contains("testMax") == true)
        assertTrue(rangeException.message?.contains("testMin") == true)
    }

    @Test
    fun testValidateAndExecuteDoesNotCatchOtherExceptions() {
        // validateAndExecute should only catch InvalidParameterException
        assertFailsWith<IllegalStateException> {
            ParameterValidation.validateAndExecute {
                throw IllegalStateException("Not a parameter validation error")
            }
        }
    }

    @Test
    fun testCustomParameterNames() {
        val exception = assertFailsWith<InvalidParameterException> {
            ParameterValidation.requirePositiveBound(-1, "customParameterName")
        }
        assertEquals("customParameterName", exception.parameterName)
        assertEquals(-1, exception.parameterValue)
    }
}