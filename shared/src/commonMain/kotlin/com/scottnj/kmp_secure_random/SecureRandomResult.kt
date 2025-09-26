package com.scottnj.kmp_secure_random

/**
 * A sealed class representing the result of secure random operations that can either succeed or fail.
 *
 * This follows the Result<T> pattern for explicit error handling without exceptions,
 * making it safer and more predictable across different platforms.
 *
 * @param T The type of the successful result
 */
sealed class SecureRandomResult<T> {

    /**
     * Represents a successful operation with a result value.
     *
     * @param value The successful result
     */
    data class Success<T>(val value: T) : SecureRandomResult<T>()

    /**
     * Represents a failed operation with error information.
     *
     * @param exception The exception that caused the failure
     */
    data class Failure(val exception: SecureRandomException) : SecureRandomResult<Nothing>()

    /**
     * Returns true if this result represents a success.
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * Returns true if this result represents a failure.
     */
    val isFailure: Boolean get() = this is Failure

    /**
     * Returns the successful value or null if this is a failure.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    /**
     * Returns the successful value or throws the contained exception if this is a failure.
     */
    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw exception
    }

    /**
     * Returns the successful value or the provided default value if this is a failure.
     */
    fun getOrDefault(defaultValue: T): T = when (this) {
        is Success -> value
        is Failure -> defaultValue
    }

    /**
     * Returns the exception if this is a failure, or null if this is a success.
     */
    fun exceptionOrNull(): SecureRandomException? = when (this) {
        is Success -> null
        is Failure -> exception
    }

    /**
     * Transforms the successful value using the provided function.
     * If this is a failure, returns the same failure.
     */
    inline fun <R> map(transform: (T) -> R): SecureRandomResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this as SecureRandomResult<R>
    }

    /**
     * Transforms the successful value using the provided function that returns a SecureRandomResult.
     * If this is a failure, returns the same failure.
     */
    inline fun <R> flatMap(transform: (T) -> SecureRandomResult<R>): SecureRandomResult<R> = when (this) {
        is Success -> transform(value)
        is Failure -> this as SecureRandomResult<R>
    }

    /**
     * Executes the provided action if this is a success.
     */
    inline fun onSuccess(action: (T) -> Unit): SecureRandomResult<T> {
        if (this is Success) action(value)
        return this
    }

    /**
     * Executes the provided action if this is a failure.
     */
    inline fun onFailure(action: (SecureRandomException) -> Unit): SecureRandomResult<T> {
        if (this is Failure) action(exception)
        return this
    }

    companion object {
        /**
         * Creates a successful result with the given value.
         */
        fun <T> success(value: T): SecureRandomResult<T> = Success(value)

        /**
         * Creates a failure result with the given exception.
         */
        fun <T> failure(exception: SecureRandomException): SecureRandomResult<T> = Failure(exception) as SecureRandomResult<T>

        /**
         * Creates a result by executing the given block, catching any SecureRandomException
         * and wrapping it in a failure result.
         */
        inline fun <T> runCatching(block: () -> T): SecureRandomResult<T> = try {
            success(block())
        } catch (e: SecureRandomException) {
            failure(e)
        }
    }
}

/**
 * Type alias for SecureRandomResult<Unit> for operations that don't return a value.
 */
typealias SecureRandomUnitResult = SecureRandomResult<Unit>