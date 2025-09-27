package com.scottnj.kmp_secure_random

import co.touchlab.kermit.Logger
import org.khronos.webgl.Uint8Array

/**
 * JavaScript external declarations for Web Crypto API and Node.js crypto module.
 */
external val crypto: dynamic

/**
 * Node.js crypto module external declaration
 */
external fun require(module: String): dynamic

/**
 * Represents the JavaScript runtime environment
 */
enum class JsEnvironment {
    BROWSER, // Web browser with Web Crypto API
    NODEJS   // Node.js with crypto module
}

/**
 * JavaScript adapter implementation using Web Crypto API for browsers and Node.js crypto for server-side.
 *
 * This implementation provides secure random number generation by:
 * - Using crypto.getRandomValues() in browser environments (Web Crypto API)
 * - Using crypto.randomBytes() in Node.js environments
 * - Automatic environment detection with appropriate fallbacks
 */
internal class JsSecureRandomAdapter private constructor(
    private val environment: JsEnvironment,
    private val cryptoApi: dynamic
) : SecureRandom {

    private val logger = Logger.withTag("JsSecureRandomAdapter")

    init {
        logger.d { "Initialized JavaScript SecureRandom adapter for environment: ${environment.name}" }
    }

    override fun nextBytes(bytes: ByteArray): SecureRandomUnitResult {
        return ParameterValidation.validateAndExecute {
            ParameterValidation.requireNonEmptyByteArray(bytes)

            try {
                when (environment) {
                    JsEnvironment.BROWSER -> {
                        val uint8Array = Uint8Array(bytes.size)
                        cryptoApi.getRandomValues(uint8Array)

                        // Copy from Uint8Array to ByteArray
                        for (i in bytes.indices) {
                            bytes[i] = uint8Array.asDynamic()[i].unsafeCast<Int>().toByte()
                        }

                        logger.v { "Generated ${bytes.size} random bytes using Web Crypto API" }
                    }
                    JsEnvironment.NODEJS -> {
                        val buffer = cryptoApi.randomBytes(bytes.size)

                        // Copy from Node.js Buffer to ByteArray
                        for (i in bytes.indices) {
                            bytes[i] = buffer.asDynamic()[i].unsafeCast<Int>().toByte()
                        }

                        logger.v { "Generated ${bytes.size} random bytes using Node.js crypto" }
                    }
                }
            } catch (e: Exception) {
                logger.e(e) { "Failed to generate ${bytes.size} random bytes in JavaScript" }
                throw SecureRandomGenerationException(
                    "Failed to generate random bytes in JavaScript environment",
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
                logger.e(e) { "Failed to generate random int in JavaScript" }
                throw SecureRandomGenerationException(
                    "Failed to generate random integer in JavaScript environment",
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
                logger.e(e) { "Failed to generate random int with bound $bound in JavaScript" }
                throw SecureRandomGenerationException(
                    "Failed to generate bounded random integer in JavaScript environment",
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
                logger.e(e) { "Failed to generate random int in range [$min, $max) in JavaScript" }
                throw SecureRandomGenerationException(
                    "Failed to generate random integer in range in JavaScript environment",
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
                logger.e(e) { "Failed to generate random long in JavaScript" }
                throw SecureRandomGenerationException(
                    "Failed to generate random long in JavaScript environment",
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
                logger.e(e) { "Failed to generate random long with bound $bound in JavaScript" }
                throw SecureRandomGenerationException(
                    "Failed to generate bounded random long in JavaScript environment",
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
                logger.e(e) { "Failed to generate random long in range [$min, $max) in JavaScript" }
                throw SecureRandomGenerationException(
                    "Failed to generate random long in range in JavaScript environment",
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
                logger.e(e) { "Failed to generate random boolean in JavaScript" }
                throw SecureRandomGenerationException(
                    "Failed to generate random boolean in JavaScript environment",
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
                logger.e(e) { "Failed to generate random double in JavaScript" }
                throw SecureRandomGenerationException(
                    "Failed to generate random double in JavaScript environment",
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
                logger.e(e) { "Failed to generate random float in JavaScript" }
                throw SecureRandomGenerationException(
                    "Failed to generate random float in JavaScript environment",
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

                logger.v { "Generated byte array of size $size in JavaScript" }
                bytes
            } catch (e: Exception) {
                // Check if it's a memory-related error
                if (e.message?.contains("memory", ignoreCase = true) == true) {
                    logger.e(e) { "Out of memory generating byte array of size $size in JavaScript" }
                    throw InsufficientResourcesException(
                        "Insufficient memory to generate $size bytes in JavaScript",
                        "memory"
                    )
                } else {
                    logger.e(e) { "Failed to generate byte array of size $size in JavaScript" }
                    throw SecureRandomGenerationException(
                        "Failed to generate random byte array in JavaScript environment",
                        e
                    )
                }
            }
        }
    }

    /**
     * Internal helper method to fill bytes using the appropriate crypto API
     */
    private fun fillBytesInternal(bytes: ByteArray) {
        when (environment) {
            JsEnvironment.BROWSER -> {
                val uint8Array = Uint8Array(bytes.size)
                cryptoApi.getRandomValues(uint8Array)

                for (i in bytes.indices) {
                    bytes[i] = uint8Array.asDynamic()[i].unsafeCast<Int>().toByte()
                }
            }
            JsEnvironment.NODEJS -> {
                val buffer = cryptoApi.randomBytes(bytes.size)

                for (i in bytes.indices) {
                    bytes[i] = buffer.asDynamic()[i].unsafeCast<Int>().toByte()
                }
            }
        }
    }

    companion object {
        private const val MAX_BYTE_ARRAY_SIZE = Int.MAX_VALUE - 8
        private val logger = Logger.withTag("JsSecureRandomAdapter")

        fun create(): SecureRandomResult<JsSecureRandomAdapter> {
            return SecureRandomResult.runCatching {
                val (environment, cryptoApi) = detectEnvironmentAndGetCrypto()

                logger.i { "Successfully created JavaScript SecureRandom adapter for environment: ${environment.name}" }
                JsSecureRandomAdapter(environment, cryptoApi)
            }
        }

        private fun detectEnvironmentAndGetCrypto(): Pair<JsEnvironment, dynamic> {
            return try {
                // Try to detect browser environment first
                if (js("typeof window !== 'undefined' && typeof window.crypto !== 'undefined'")) {
                    val webCrypto = js("window.crypto")
                    if (js("typeof webCrypto.getRandomValues === 'function'")) {
                        logger.d { "Detected browser environment with Web Crypto API" }
                        return Pair(JsEnvironment.BROWSER, webCrypto)
                    }
                }

                // Try Node.js environment
                if (js("typeof require !== 'undefined'")) {
                    try {
                        val nodeCrypto = require("crypto")
                        if (js("typeof nodeCrypto.randomBytes === 'function'")) {
                            logger.d { "Detected Node.js environment with crypto module" }
                            return Pair(JsEnvironment.NODEJS, nodeCrypto)
                        }
                    } catch (e: Exception) {
                        logger.w(e) { "Node.js crypto module not available" }
                    }
                }

                // Fallback: try global crypto
                if (js("typeof crypto !== 'undefined' && typeof crypto.getRandomValues === 'function'")) {
                    logger.d { "Using global crypto API" }
                    return Pair(JsEnvironment.BROWSER, crypto)
                }

                logger.e { "No secure random API available in JavaScript environment" }
                throw SecureRandomInitializationException(
                    "No secure random number generation API available in this JavaScript environment"
                )
            } catch (e: Exception) {
                logger.e(e) { "Failed to initialize JavaScript crypto API" }
                throw SecureRandomInitializationException(
                    "Failed to initialize secure random generation in JavaScript",
                    e
                )
            }
        }
    }
}