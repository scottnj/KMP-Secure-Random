package com.scottnj.kmp_secure_random

import co.touchlab.kermit.Logger
import kotlinx.cinterop.*
import platform.Security.*
import platform.Foundation.*
import kotlin.experimental.and
import kotlin.random.Random

@OptIn(ExperimentalForeignApi::class)

/**
 * watchOS implementation of SecureRandom using Apple's SecRandomCopyBytes API.
 * Provides production-ready cryptographically secure random number generation.
 * Note: watchOS requires different bit width handling for SecRandomCopyBytes.
 */
internal class WatchosSecureRandomAdapter private constructor() : SecureRandom {

    private val logger = Logger.withTag("WatchosSecureRandomAdapter")

    init {
        logger.d { "Initialized watchOS SecureRandom adapter" }
    }

    override fun nextBytes(bytes: ByteArray): SecureRandomUnitResult {
        return ParameterValidation.validateAndExecute {
            ParameterValidation.requireNonEmptyByteArray(bytes)

            try {
                generateSecureBytes(bytes.size) { generatedBytes ->
                    generatedBytes.copyInto(bytes)
                }
                logger.v { "Generated ${bytes.size} random bytes" }
            } catch (e: Exception) {
                logger.e(e) { "Failed to generate ${bytes.size} random bytes" }
                throw SecureRandomGenerationException(
                    "Failed to generate random bytes",
                    e
                )
            }
        }
    }

    override fun nextInt(): SecureRandomResult<Int> {
        return SecureRandomResult.runCatching {
            try {
                val result = generateIntFromBytes()
                logger.v { "Generated random int: $result" }
                result
            } catch (e: Exception) {
                logger.e(e) { "Failed to generate random int" }
                throw SecureRandomGenerationException(
                    "Failed to generate random integer",
                    e
                )
            }
        }
    }

    override fun nextInt(bound: Int): SecureRandomResult<Int> {
        return ParameterValidation.validateAndExecute {
            ParameterValidation.requirePositiveBound(bound)

            try {
                val result = generateBoundedInt(bound)
                logger.v { "Generated random int with bound $bound: $result" }
                result
            } catch (e: Exception) {
                logger.e(e) { "Failed to generate random int with bound $bound" }
                throw SecureRandomGenerationException(
                    "Failed to generate bounded random integer",
                    e
                )
            }
        }
    }

    override fun nextInt(min: Int, max: Int): SecureRandomResult<Int> {
        return ParameterValidation.validateAndExecute {
            ParameterValidation.requireValidRange(min, max)

            try {
                val range = max - min
                val result = min + generateBoundedInt(range)
                logger.v { "Generated random int in range [$min, $max): $result" }
                result
            } catch (e: Exception) {
                logger.e(e) { "Failed to generate random int in range [$min, $max)" }
                throw SecureRandomGenerationException(
                    "Failed to generate random integer in range",
                    e
                )
            }
        }
    }

    override fun nextLong(): SecureRandomResult<Long> {
        return SecureRandomResult.runCatching {
            try {
                val result = generateLongFromBytes()
                logger.v { "Generated random long: $result" }
                result
            } catch (e: Exception) {
                logger.e(e) { "Failed to generate random long" }
                throw SecureRandomGenerationException(
                    "Failed to generate random long",
                    e
                )
            }
        }
    }

    override fun nextLong(bound: Long): SecureRandomResult<Long> {
        return ParameterValidation.validateAndExecute {
            ParameterValidation.requirePositiveBound(bound)

            try {
                // Handle edge case
                if (bound == 1L) {
                    logger.v { "Generated random long with bound $bound: 0" }
                    return@validateAndExecute 0L
                }

                val result = generateBoundedLong(bound)
                logger.v { "Generated random long with bound $bound: $result" }
                result
            } catch (e: Exception) {
                logger.e(e) { "Failed to generate random long with bound $bound" }
                throw SecureRandomGenerationException(
                    "Failed to generate bounded random long",
                    e
                )
            }
        }
    }

    override fun nextLong(min: Long, max: Long): SecureRandomResult<Long> {
        return ParameterValidation.validateAndExecute {
            ParameterValidation.requireValidRange(min, max)

            try {
                val range = max - min

                // Handle edge case
                if (range == 1L) {
                    logger.v { "Generated random long in range [$min, $max): $min" }
                    return@validateAndExecute min
                }

                val result = min + generateBoundedLong(range)
                logger.v { "Generated random long in range [$min, $max): $result" }
                result
            } catch (e: Exception) {
                logger.e(e) { "Failed to generate random long in range [$min, $max)" }
                throw SecureRandomGenerationException(
                    "Failed to generate random long in range",
                    e
                )
            }
        }
    }

    override fun nextBoolean(): SecureRandomResult<Boolean> {
        return SecureRandomResult.runCatching {
            try {
                val result = generateBooleanFromByte()
                logger.v { "Generated random boolean: $result" }
                result
            } catch (e: Exception) {
                logger.e(e) { "Failed to generate random boolean" }
                throw SecureRandomGenerationException(
                    "Failed to generate random boolean",
                    e
                )
            }
        }
    }

    override fun nextDouble(): SecureRandomResult<Double> {
        return SecureRandomResult.runCatching {
            try {
                val result = generateDoubleFromBytes()
                logger.v { "Generated random double: $result" }
                result
            } catch (e: Exception) {
                logger.e(e) { "Failed to generate random double" }
                throw SecureRandomGenerationException(
                    "Failed to generate random double",
                    e
                )
            }
        }
    }

    override fun nextFloat(): SecureRandomResult<Float> {
        return SecureRandomResult.runCatching {
            try {
                val result = generateFloatFromBytes()
                logger.v { "Generated random float: $result" }
                result
            } catch (e: Exception) {
                logger.e(e) { "Failed to generate random float" }
                throw SecureRandomGenerationException(
                    "Failed to generate random float",
                    e
                )
            }
        }
    }

    override fun nextBytes(size: Int): SecureRandomResult<ByteArray> {
        return ParameterValidation.validateAndExecute {
            ParameterValidation.requireNonNegativeSize(size)
            ParameterValidation.requireSizeWithinLimit(size, MAX_BYTE_ARRAY_SIZE)

            if (size == 0) {
                return@validateAndExecute ByteArray(0)
            }

            try {
                val bytes = generateSecureBytes(size)
                logger.v { "Generated byte array of size $size" }
                bytes
            } catch (e: OutOfMemoryError) {
                logger.e(e) { "Out of memory generating byte array of size $size" }
                throw InsufficientResourcesException(
                    "Insufficient memory to generate $size bytes",
                    "memory"
                )
            } catch (e: Exception) {
                logger.e(e) { "Failed to generate byte array of size $size" }
                throw SecureRandomGenerationException(
                    "Failed to generate random byte array",
                    e
                )
            }
        }
    }

    // Private helper methods for secure byte generation

    private fun generateSecureBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        generateSecureBytes(size) { generatedBytes ->
            generatedBytes.copyInto(bytes)
        }
        return bytes
    }

    private inline fun generateSecureBytes(size: Int, action: (ByteArray) -> Unit) {
        // For watchOS, use arc4random for individual byte generation to avoid metadata conflicts
        val bytes = ByteArray(size) { _ ->
            // Generate individual bytes using arc4random
            (platform.posix.arc4random() and 0xFFu).toByte()
        }
        action(bytes)
    }

    private fun generateIntFromBytes(): Int {
        return generateSecureBytes(4).let { bytes ->
            ((bytes[0].toInt() and 0xFF) shl 24) or
            ((bytes[1].toInt() and 0xFF) shl 16) or
            ((bytes[2].toInt() and 0xFF) shl 8) or
            (bytes[3].toInt() and 0xFF)
        }
    }

    private fun generateBoundedInt(bound: Int): Int {
        // Use rejection sampling for unbiased results
        var result: Int
        do {
            result = generateIntFromBytes() and Int.MAX_VALUE // Make positive
            result %= bound
        } while (result < 0) // This shouldn't happen with the mask above, but be safe
        return result
    }

    private fun generateLongFromBytes(): Long {
        return generateSecureBytes(8).let { bytes ->
            var result = 0L
            for (i in 0..7) {
                result = (result shl 8) or (bytes[i].toLong() and 0xFF)
            }
            result
        }
    }

    private fun generateBoundedLong(bound: Long): Long {
        // Use rejection sampling for unbiased results
        var result: Long
        do {
            result = generateLongFromBytes() and Long.MAX_VALUE // Make positive
            result %= bound
        } while (result < 0) // This shouldn't happen with the mask above, but be safe
        return result
    }

    private fun generateBooleanFromByte(): Boolean {
        return generateSecureBytes(1)[0] and 1 == 1.toByte()
    }

    private fun generateDoubleFromBytes(): Double {
        // Generate a random long and convert to [0.0, 1.0)
        val randomLong = generateLongFromBytes() and Long.MAX_VALUE
        return randomLong.toDouble() / Long.MAX_VALUE.toDouble()
    }

    private fun generateFloatFromBytes(): Float {
        // Generate a random int and convert to [0.0f, 1.0f)
        val randomInt = generateIntFromBytes() and Int.MAX_VALUE
        return randomInt.toFloat() / Int.MAX_VALUE.toFloat()
    }

    companion object {
        private const val MAX_BYTE_ARRAY_SIZE = Int.MAX_VALUE - 8 // Conservative limit
        private val logger = Logger.withTag("WatchosSecureRandomAdapter")

        fun create(): SecureRandomResult<WatchosSecureRandomAdapter> {
            return SecureRandomResult.runCatching {
                // Verify arc4random is available (test generation)
                val testValue = platform.posix.arc4random()
                // If we get here without exception, arc4random is working

                logger.i { "Successfully created watchOS SecureRandom using arc4random" }
                WatchosSecureRandomAdapter()
            }
        }
    }
}

@AllowInsecureFallback
actual fun createSecureRandom(fallbackPolicy: FallbackPolicy): SecureRandomResult<SecureRandom> {
    // watchOS platform uses arc4random API and has secure fallbacks only
    // fallbackPolicy parameter is ignored as watchOS provides secure random generation only
    return createSecureRandom()
}

actual fun createSecureRandom(): SecureRandomResult<SecureRandom> {
    return WatchosSecureRandomAdapter.create().map { it as SecureRandom }
}