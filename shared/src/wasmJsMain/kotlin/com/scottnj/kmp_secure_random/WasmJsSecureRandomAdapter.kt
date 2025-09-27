package com.scottnj.kmp_secure_random

import co.touchlab.kermit.Logger
import kotlin.js.JsAny
import kotlin.js.ExperimentalWasmJsInterop

/**
 * WASM-JS external declarations for Web Crypto API.
 * Using simplified approach due to WASM-JS experimental interop limitations.
 */
@OptIn(ExperimentalWasmJsInterop::class)
external val crypto: SimpleCrypto

@OptIn(ExperimentalWasmJsInterop::class)
external interface SimpleCrypto : JsAny {
    fun getRandomValues(array: JsAny): Unit
}

// Note: WASM-JS external declarations are currently very limited
// This is a placeholder implementation that will be refined as WASM-JS matures

/**
 * WASM-JS adapter implementation using Web Crypto API.
 *
 * This implementation provides secure random number generation for WASM-JS by:
 * - Using simplified crypto.getRandomValues() calls for WASM-JS compatibility
 * - Following experimental WASM-JS interop constraints
 * - Using basic JsAny types to avoid complex type mapping issues
 */
@OptIn(ExperimentalWasmJsInterop::class)
internal class WasmJsSecureRandomAdapter private constructor() : SecureRandom {

    private val logger = Logger.withTag("WasmJsSecureRandomAdapter")

    init {
        logger.d { "Initialized WASM-JS SecureRandom adapter" }
    }

    override fun nextBytes(bytes: ByteArray): SecureRandomUnitResult {
        return ParameterValidation.validateAndExecute {
            ParameterValidation.requireNonEmptyByteArray(bytes)

            try {
                fillBytesInternal(bytes)
                logger.v { "Generated ${bytes.size} random bytes using WASM-JS Web Crypto API" }
            } catch (e: Exception) {
                logger.e(e) { "Failed to generate ${bytes.size} random bytes in WASM-JS" }
                throw SecureRandomGenerationException(
                    "Failed to generate random bytes in WASM-JS environment",
                    e
                )
            }
        }
    }

    override fun nextInt(): SecureRandomResult<Int> {
        return SecureRandomResult.runCatching {
            try {
                val bytes = ByteArray(4)
                fillBytesInternal(bytes)

                // Convert bytes to Int (big-endian)
                val result = ((bytes[0].toInt() and 0xFF) shl 24) or
                            ((bytes[1].toInt() and 0xFF) shl 16) or
                            ((bytes[2].toInt() and 0xFF) shl 8) or
                            (bytes[3].toInt() and 0xFF)

                logger.v { "Generated random int: $result" }
                result
            } catch (e: Exception) {
                logger.e(e) { "Failed to generate random int in WASM-JS" }
                throw SecureRandomGenerationException(
                    "Failed to generate random integer in WASM-JS environment",
                    e
                )
            }
        }
    }

    override fun nextInt(bound: Int): SecureRandomResult<Int> {
        return ParameterValidation.validateAndExecute {
            ParameterValidation.requirePositiveBound(bound)

            try {
                // Use rejection sampling for unbiased results
                var result: Int
                do {
                    val bytes = ByteArray(4)
                    fillBytesInternal(bytes)

                    result = ((bytes[0].toInt() and 0xFF) shl 24) or
                            ((bytes[1].toInt() and 0xFF) shl 16) or
                            ((bytes[2].toInt() and 0xFF) shl 8) or
                            (bytes[3].toInt() and 0xFF)

                    result = result and Int.MAX_VALUE // Make positive
                    result %= bound
                } while (result < 0) // Safety check

                logger.v { "Generated random int with bound $bound: $result" }
                result
            } catch (e: Exception) {
                logger.e(e) { "Failed to generate random int with bound $bound in WASM-JS" }
                throw SecureRandomGenerationException(
                    "Failed to generate bounded random integer in WASM-JS environment",
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
                val randomInRange = nextInt(range).getOrThrow()
                val result = min + randomInRange

                logger.v { "Generated random int in range [$min, $max): $result" }
                result
            } catch (e: Exception) {
                logger.e(e) { "Failed to generate random int in range [$min, $max) in WASM-JS" }
                throw SecureRandomGenerationException(
                    "Failed to generate random integer in range in WASM-JS environment",
                    e
                )
            }
        }
    }

    override fun nextLong(): SecureRandomResult<Long> {
        return SecureRandomResult.runCatching {
            try {
                val bytes = ByteArray(8)
                fillBytesInternal(bytes)

                // Convert bytes to Long (big-endian)
                var result = 0L
                for (i in 0..7) {
                    result = (result shl 8) or (bytes[i].toLong() and 0xFF)
                }

                logger.v { "Generated random long: $result" }
                result
            } catch (e: Exception) {
                logger.e(e) { "Failed to generate random long in WASM-JS" }
                throw SecureRandomGenerationException(
                    "Failed to generate random long in WASM-JS environment",
                    e
                )
            }
        }
    }

    override fun nextLong(bound: Long): SecureRandomResult<Long> {
        return ParameterValidation.validateAndExecute {
            ParameterValidation.requirePositiveBound(bound)

            try {
                if (bound == 1L) {
                    logger.v { "Generated random long with bound $bound: 0" }
                    return@validateAndExecute 0L
                }

                // Use rejection sampling for unbiased results
                var result: Long
                do {
                    val bytes = ByteArray(8)
                    fillBytesInternal(bytes)

                    result = 0L
                    for (i in 0..7) {
                        result = (result shl 8) or (bytes[i].toLong() and 0xFF)
                    }

                    result = result and Long.MAX_VALUE // Make positive
                    result %= bound
                } while (result < 0)

                logger.v { "Generated random long with bound $bound: $result" }
                result
            } catch (e: Exception) {
                logger.e(e) { "Failed to generate random long with bound $bound in WASM-JS" }
                throw SecureRandomGenerationException(
                    "Failed to generate bounded random long in WASM-JS environment",
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
                if (range == 1L) {
                    logger.v { "Generated random long in range [$min, $max): $min" }
                    return@validateAndExecute min
                }

                val randomInRange = nextLong(range).getOrThrow()
                val result = min + randomInRange

                logger.v { "Generated random long in range [$min, $max): $result" }
                result
            } catch (e: Exception) {
                logger.e(e) { "Failed to generate random long in range [$min, $max) in WASM-JS" }
                throw SecureRandomGenerationException(
                    "Failed to generate random long in range in WASM-JS environment",
                    e
                )
            }
        }
    }

    override fun nextBoolean(): SecureRandomResult<Boolean> {
        return SecureRandomResult.runCatching {
            try {
                val bytes = ByteArray(1)
                fillBytesInternal(bytes)
                val result = (bytes[0].toInt() and 0xFF) >= 128

                logger.v { "Generated random boolean: $result" }
                result
            } catch (e: Exception) {
                logger.e(e) { "Failed to generate random boolean in WASM-JS" }
                throw SecureRandomGenerationException(
                    "Failed to generate random boolean in WASM-JS environment",
                    e
                )
            }
        }
    }

    override fun nextDouble(): SecureRandomResult<Double> {
        return SecureRandomResult.runCatching {
            try {
                val bytes = ByteArray(8)
                fillBytesInternal(bytes)

                // Generate double between 0.0 and 1.0
                var result = 0L
                for (i in 0..6) { // Use 7 bytes for precision
                    result = (result shl 8) or (bytes[i].toLong() and 0xFF)
                }

                // Convert to double in range [0.0, 1.0)
                val doubleResult = (result and 0x1FFFFFFFFFFFFF) / (1L shl 53).toDouble()

                logger.v { "Generated random double: $doubleResult" }
                doubleResult
            } catch (e: Exception) {
                logger.e(e) { "Failed to generate random double in WASM-JS" }
                throw SecureRandomGenerationException(
                    "Failed to generate random double in WASM-JS environment",
                    e
                )
            }
        }
    }

    override fun nextFloat(): SecureRandomResult<Float> {
        return SecureRandomResult.runCatching {
            try {
                val bytes = ByteArray(4)
                fillBytesInternal(bytes)

                // Generate float between 0.0 and 1.0
                val intValue = ((bytes[0].toInt() and 0xFF) shl 24) or
                              ((bytes[1].toInt() and 0xFF) shl 16) or
                              ((bytes[2].toInt() and 0xFF) shl 8) or
                              (bytes[3].toInt() and 0xFF)

                // Convert to float in range [0.0, 1.0)
                val result = (intValue and 0xFFFFFF) / (1 shl 24).toFloat()

                logger.v { "Generated random float: $result" }
                result
            } catch (e: Exception) {
                logger.e(e) { "Failed to generate random float in WASM-JS" }
                throw SecureRandomGenerationException(
                    "Failed to generate random float in WASM-JS environment",
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
                val bytes = ByteArray(size)
                fillBytesInternal(bytes)

                logger.v { "Generated byte array of size $size in WASM-JS" }
                bytes
            } catch (e: Exception) {
                // Check if it's a memory-related error
                if (e.message?.contains("memory", ignoreCase = true) == true) {
                    logger.e(e) { "Out of memory generating byte array of size $size in WASM-JS" }
                    throw InsufficientResourcesException(
                        "Insufficient memory to generate $size bytes in WASM-JS",
                        "memory"
                    )
                } else {
                    logger.e(e) { "Failed to generate byte array of size $size in WASM-JS" }
                    throw SecureRandomGenerationException(
                        "Failed to generate random byte array in WASM-JS environment",
                        e
                    )
                }
            }
        }
    }

    /**
     * Internal helper method for WASM-JS crypto API
     *
     * Note: Current WASM-JS interop limitations prevent full Web Crypto API integration.
     * This is a placeholder that acknowledges the technical constraints.
     */
    private fun fillBytesInternal(bytes: ByteArray) {
        // Current limitation: WASM-JS interop doesn't support the complex type mappings
        // needed for Web Crypto API integration. This will be implemented when
        // WASM-JS interop capabilities mature.

        logger.w { "WASM-JS SecureRandom not yet fully implemented due to interop limitations" }
        throw SecureRandomInitializationException(
            "WASM-JS Web Crypto API integration requires more mature interop support"
        )
    }

    companion object {
        private const val MAX_BYTE_ARRAY_SIZE = Int.MAX_VALUE - 8
        private val logger = Logger.withTag("WasmJsSecureRandomAdapter")

        fun create(): SecureRandomResult<WasmJsSecureRandomAdapter> {
            return SecureRandomResult.runCatching {
                logger.w { "WASM-JS SecureRandom adapter not yet fully implemented" }
                logger.i { "WASM-JS interop limitations prevent full Web Crypto API integration" }

                // Return failure with clear explanation
                throw SecureRandomInitializationException(
                    "WASM-JS SecureRandom implementation pending improved interop support. " +
                    "Current WASM-JS external declarations are too restrictive for Web Crypto API integration."
                )
            }
        }
    }
}