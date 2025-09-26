package com.scottnj.kmp_secure_random

/**
 * A cryptographically secure random number generator interface for Kotlin Multiplatform.
 *
 * This interface provides access to platform-specific secure random number generation
 * using each platform's recommended cryptographic random source.
 *
 * All methods return SecureRandomResult<T> to provide explicit error handling without exceptions,
 * making random generation more predictable and safer across different platforms.
 */
interface SecureRandom {

    /**
     * Fills the given byte array with cryptographically secure random bytes.
     *
     * @param bytes The byte array to fill with random data
     * @return SecureRandomUnitResult indicating success or failure
     */
    fun nextBytes(bytes: ByteArray): SecureRandomUnitResult

    /**
     * Generates a cryptographically secure random integer.
     *
     * @return A SecureRandomResult containing a random 32-bit signed integer or an error
     */
    fun nextInt(): SecureRandomResult<Int>

    /**
     * Generates a cryptographically secure random integer within the specified range.
     *
     * @param bound The upper bound (exclusive). Must be positive.
     * @return A SecureRandomResult containing a random integer between 0 (inclusive) and bound (exclusive), or an error
     */
    fun nextInt(bound: Int): SecureRandomResult<Int>

    /**
     * Generates a cryptographically secure random integer within the specified range.
     *
     * @param min The lower bound (inclusive)
     * @param max The upper bound (exclusive). Must be greater than min.
     * @return A SecureRandomResult containing a random integer between min (inclusive) and max (exclusive), or an error
     */
    fun nextInt(min: Int, max: Int): SecureRandomResult<Int>

    /**
     * Generates a cryptographically secure random long value.
     *
     * @return A SecureRandomResult containing a random 64-bit signed long or an error
     */
    fun nextLong(): SecureRandomResult<Long>

    /**
     * Generates a cryptographically secure random long within the specified range.
     *
     * @param bound The upper bound (exclusive). Must be positive.
     * @return A SecureRandomResult containing a random long between 0 (inclusive) and bound (exclusive), or an error
     */
    fun nextLong(bound: Long): SecureRandomResult<Long>

    /**
     * Generates a cryptographically secure random long within the specified range.
     *
     * @param min The lower bound (inclusive)
     * @param max The upper bound (exclusive). Must be greater than min.
     * @return A SecureRandomResult containing a random long between min (inclusive) and max (exclusive), or an error
     */
    fun nextLong(min: Long, max: Long): SecureRandomResult<Long>

    /**
     * Generates a cryptographically secure random boolean value.
     *
     * @return A SecureRandomResult containing a random boolean or an error
     */
    fun nextBoolean(): SecureRandomResult<Boolean>

    /**
     * Generates a cryptographically secure random double value between 0.0 (inclusive) and 1.0 (exclusive).
     *
     * @return A SecureRandomResult containing a random double value or an error
     */
    fun nextDouble(): SecureRandomResult<Double>

    /**
     * Generates a cryptographically secure random float value between 0.0 (inclusive) and 1.0 (exclusive).
     *
     * @return A SecureRandomResult containing a random float value or an error
     */
    fun nextFloat(): SecureRandomResult<Float>

    /**
     * Generates a cryptographically secure random byte array of the specified size.
     *
     * @param size The number of bytes to generate. Must be non-negative.
     * @return A SecureRandomResult containing a new byte array filled with random data, or an error
     */
    fun nextBytes(size: Int): SecureRandomResult<ByteArray>
}

/**
 * Creates a new platform-specific SecureRandom instance.
 *
 * @return A SecureRandomResult containing a SecureRandom implementation optimized for the current platform, or an error
 */
expect fun createSecureRandom(): SecureRandomResult<SecureRandom>