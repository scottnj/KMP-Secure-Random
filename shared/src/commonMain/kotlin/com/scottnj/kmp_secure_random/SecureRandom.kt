package com.scottnj.kmp_secure_random

/**
 * A cryptographically secure random number generator interface for Kotlin Multiplatform.
 *
 * This interface provides access to platform-specific secure random number generation
 * using each platform's recommended cryptographic random source.
 */
interface SecureRandom {

    /**
     * Fills the given byte array with cryptographically secure random bytes.
     *
     * @param bytes The byte array to fill with random data
     */
    fun nextBytes(bytes: ByteArray)

    /**
     * Generates a cryptographically secure random integer.
     *
     * @return A random 32-bit signed integer
     */
    fun nextInt(): Int

    /**
     * Generates a cryptographically secure random integer within the specified range.
     *
     * @param bound The upper bound (exclusive). Must be positive.
     * @return A random integer between 0 (inclusive) and bound (exclusive)
     * @throws IllegalArgumentException if bound is not positive
     */
    fun nextInt(bound: Int): Int

    /**
     * Generates a cryptographically secure random long value.
     *
     * @return A random 64-bit signed long
     */
    fun nextLong(): Long

    /**
     * Generates a cryptographically secure random boolean value.
     *
     * @return A random boolean
     */
    fun nextBoolean(): Boolean

    /**
     * Generates a cryptographically secure random double value between 0.0 (inclusive) and 1.0 (exclusive).
     *
     * @return A random double value
     */
    fun nextDouble(): Double
}

/**
 * Creates a new platform-specific SecureRandom instance.
 *
 * @return A SecureRandom implementation optimized for the current platform
 */
expect fun createSecureRandom(): SecureRandom