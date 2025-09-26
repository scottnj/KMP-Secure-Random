package com.scottnj.kmp_secure_random

/**
 * Utilities for validating parameters passed to SecureRandom operations.
 *
 * These functions provide consistent parameter validation across all platforms
 * and return appropriate InvalidParameterException instances when validation fails.
 */
object ParameterValidation {

    /**
     * Validates that a bound parameter is positive.
     *
     * @param bound The bound value to validate
     * @param parameterName The name of the parameter for error reporting
     * @throws InvalidParameterException if bound is not positive
     */
    fun requirePositiveBound(bound: Int, parameterName: String = "bound") {
        if (bound <= 0) {
            throw InvalidParameterException(
                "must be positive",
                parameterName,
                bound
            )
        }
    }

    /**
     * Validates that a bound parameter is positive.
     *
     * @param bound The bound value to validate
     * @param parameterName The name of the parameter for error reporting
     * @throws InvalidParameterException if bound is not positive
     */
    fun requirePositiveBound(bound: Long, parameterName: String = "bound") {
        if (bound <= 0L) {
            throw InvalidParameterException(
                "must be positive",
                parameterName,
                bound
            )
        }
    }

    /**
     * Validates that a size parameter is non-negative.
     *
     * @param size The size value to validate
     * @param parameterName The name of the parameter for error reporting
     * @throws InvalidParameterException if size is negative
     */
    fun requireNonNegativeSize(size: Int, parameterName: String = "size") {
        if (size < 0) {
            throw InvalidParameterException(
                "must be non-negative",
                parameterName,
                size
            )
        }
    }

    /**
     * Validates that a range is valid (min < max).
     *
     * @param min The minimum value (inclusive)
     * @param max The maximum value (exclusive)
     * @param minParameterName The name of the min parameter for error reporting
     * @param maxParameterName The name of the max parameter for error reporting
     * @throws InvalidParameterException if min >= max
     */
    fun requireValidRange(min: Int, max: Int, minParameterName: String = "min", maxParameterName: String = "max") {
        if (min >= max) {
            throw InvalidParameterException(
                "$maxParameterName ($max) must be greater than $minParameterName ($min)",
                "range",
                "$min..$max"
            )
        }
    }

    /**
     * Validates that a range is valid (min < max).
     *
     * @param min The minimum value (inclusive)
     * @param max The maximum value (exclusive)
     * @param minParameterName The name of the min parameter for error reporting
     * @param maxParameterName The name of the max parameter for error reporting
     * @throws InvalidParameterException if min >= max
     */
    fun requireValidRange(min: Long, max: Long, minParameterName: String = "min", maxParameterName: String = "max") {
        if (min >= max) {
            throw InvalidParameterException(
                "$maxParameterName ($max) must be greater than $minParameterName ($min)",
                "range",
                "$min..$max"
            )
        }
    }

    /**
     * Validates that a byte array is not empty.
     *
     * @param bytes The byte array to validate
     * @param parameterName The name of the parameter for error reporting
     * @throws InvalidParameterException if the array is empty
     */
    fun requireNonEmptyByteArray(bytes: ByteArray, parameterName: String = "bytes") {
        if (bytes.isEmpty()) {
            throw InvalidParameterException(
                "must not be empty",
                parameterName,
                "empty array"
            )
        }
    }

    /**
     * Validates that a byte array size does not exceed the maximum allowed size.
     *
     * @param size The requested size to validate
     * @param maxSize The maximum allowed size
     * @param parameterName The name of the parameter for error reporting
     * @throws InvalidParameterException if size exceeds maxSize
     */
    fun requireSizeWithinLimit(size: Int, maxSize: Int, parameterName: String = "size") {
        if (size > maxSize) {
            throw InvalidParameterException(
                "exceeds maximum allowed size of $maxSize",
                parameterName,
                size
            )
        }
    }

    /**
     * Validates that an integer is within a specific range.
     *
     * @param value The value to validate
     * @param min The minimum allowed value (inclusive)
     * @param max The maximum allowed value (inclusive)
     * @param parameterName The name of the parameter for error reporting
     * @throws InvalidParameterException if value is outside the range
     */
    fun requireInRange(value: Int, min: Int, max: Int, parameterName: String = "value") {
        if (value < min || value > max) {
            throw InvalidParameterException(
                "must be in range [$min, $max]",
                parameterName,
                value
            )
        }
    }

    /**
     * Validates that a long is within a specific range.
     *
     * @param value The value to validate
     * @param min The minimum allowed value (inclusive)
     * @param max The maximum allowed value (inclusive)
     * @param parameterName The name of the parameter for error reporting
     * @throws InvalidParameterException if value is outside the range
     */
    fun requireInRange(value: Long, min: Long, max: Long, parameterName: String = "value") {
        if (value < min || value > max) {
            throw InvalidParameterException(
                "must be in range [$min, $max]",
                parameterName,
                value
            )
        }
    }

    /**
     * Safely executes a block of code and wraps any InvalidParameterException
     * in a SecureRandomResult.Failure.
     *
     * @param block The code block to execute
     * @return SecureRandomResult.Success if the block executes without parameter validation errors,
     *         SecureRandomResult.Failure if parameter validation fails
     */
    inline fun <T> validateAndExecute(block: () -> T): SecureRandomResult<T> {
        return try {
            SecureRandomResult.success(block())
        } catch (e: InvalidParameterException) {
            SecureRandomResult.failure(e)
        }
    }
}