package com.scottnj.kmp_secure_random

/**
 * Opt-in requirement for using insecure fallback methods when secure random generation is unavailable.
 *
 * This annotation is required when using createSecureRandom with FallbackPolicy.ALLOW_INSECURE.
 * Using insecure fallbacks compromises cryptographic security and should only be used when:
 * - Secure random generation is unavailable on the platform
 * - The use case can tolerate non-cryptographically secure randomness
 * - You understand the security implications
 *
 * @see FallbackPolicy.ALLOW_INSECURE
 */
@RequiresOptIn("Allows insecure fallback when secure random unavailable. Understand security implications.")
@Retention(AnnotationRetention.BINARY)
annotation class AllowInsecureFallback

/**
 * Policy for handling situations where secure random generation is unavailable.
 */
enum class FallbackPolicy {
    /**
     * Only allow cryptographically secure random generation.
     * Fail with an exception if secure random generation is unavailable.
     * This is the recommended and default behavior.
     */
    SECURE_ONLY,

    /**
     * Allow fallback to insecure random generation if secure methods are unavailable.
     * This requires the @AllowInsecureFallback opt-in annotation and should only be used
     * when you understand the security implications.
     *
     * @see AllowInsecureFallback
     */
    ALLOW_INSECURE
}

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
 * Creates a new platform-specific SecureRandom instance with the specified fallback policy.
 *
 * @param fallbackPolicy The policy for handling situations where secure random generation is unavailable.
 *                       Defaults to SECURE_ONLY for maximum security.
 * @return A SecureRandomResult containing a SecureRandom implementation optimized for the current platform, or an error
 *
 * @see FallbackPolicy
 * @see AllowInsecureFallback
 */
@AllowInsecureFallback
expect fun createSecureRandom(fallbackPolicy: FallbackPolicy): SecureRandomResult<SecureRandom>

/**
 * Creates a new platform-specific SecureRandom instance using secure-only policy.
 *
 * This is equivalent to calling createSecureRandom(FallbackPolicy.SECURE_ONLY) and is the recommended
 * approach for maximum security. It will fail if secure random generation is unavailable.
 *
 * @return A SecureRandomResult containing a SecureRandom implementation optimized for the current platform, or an error
 */
expect fun createSecureRandom(): SecureRandomResult<SecureRandom>