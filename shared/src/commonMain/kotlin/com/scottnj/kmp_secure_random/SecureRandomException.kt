package com.scottnj.kmp_secure_random

/**
 * Base class for all secure random related exceptions.
 *
 * This provides a common exception hierarchy for all secure random operations,
 * allowing for consistent error handling across platforms.
 *
 * @param message The detail message explaining the exception
 * @param cause The underlying cause of this exception, if any
 */
open class SecureRandomException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when secure random initialization fails.
 *
 * This can occur when:
 * - Platform-specific secure random APIs are unavailable
 * - Required system resources cannot be accessed
 * - Security providers are not properly configured
 *
 * @param message The detail message explaining the initialization failure
 * @param cause The underlying cause of the initialization failure
 */
class SecureRandomInitializationException(
    message: String,
    cause: Throwable? = null
) : SecureRandomException(message, cause)

/**
 * Exception thrown when secure random generation fails.
 *
 * This can occur when:
 * - The underlying random number generator encounters an error
 * - System entropy is temporarily unavailable
 * - Hardware random number generators fail
 *
 * @param message The detail message explaining the generation failure
 * @param cause The underlying cause of the generation failure
 */
class SecureRandomGenerationException(
    message: String,
    cause: Throwable? = null
) : SecureRandomException(message, cause)

/**
 * Exception thrown when invalid parameters are provided to secure random operations.
 *
 * This can occur when:
 * - Negative bounds are provided to bounded random methods
 * - Zero bounds are provided where positive values are required
 * - Null or invalid arrays are provided
 * - Parameters are outside acceptable ranges
 *
 * @param message The detail message explaining the parameter violation
 * @param parameterName The name of the invalid parameter
 * @param parameterValue The invalid parameter value
 */
class InvalidParameterException(
    message: String,
    val parameterName: String,
    val parameterValue: Any?
) : SecureRandomException("Invalid parameter '$parameterName' with value '$parameterValue': $message")

/**
 * Exception thrown when secure random operations are not supported on the current platform.
 *
 * This can occur when:
 * - Platform-specific implementations are not available
 * - Required APIs are not supported in the current environment
 * - Feature is not yet implemented for the target platform
 *
 * @param message The detail message explaining the unsupported operation
 * @param platformName The name of the platform where the operation is unsupported
 */
class UnsupportedPlatformException(
    message: String,
    val platformName: String
) : SecureRandomException("Unsupported operation on platform '$platformName': $message")

/**
 * Exception thrown when secure random operations fail due to insufficient system resources.
 *
 * This can occur when:
 * - System is low on entropy
 * - Memory allocation fails
 * - System resources are temporarily unavailable
 *
 * @param message The detail message explaining the resource constraint
 * @param resourceType The type of resource that is constrained
 */
class InsufficientResourcesException(
    message: String,
    val resourceType: String
) : SecureRandomException("Insufficient $resourceType: $message")