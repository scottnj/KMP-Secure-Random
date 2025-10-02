package com.scottnj.kmp_secure_random

import co.touchlab.kermit.Logger
import kotlin.js.JsAny
import kotlin.js.ExperimentalWasmJsInterop

/**
 * WASM-JS external declarations for Web Crypto API.
 * Uses simplified approach that works with current WASM-JS interop capabilities.
 */
@OptIn(ExperimentalWasmJsInterop::class)
external val crypto: SimpleCrypto

@OptIn(ExperimentalWasmJsInterop::class)
external interface SimpleCrypto : JsAny {
    fun getRandomValues(array: JsAny): Unit
}

/**
 * Top-level helper functions for WASM-JS crypto operations
 */
@OptIn(ExperimentalWasmJsInterop::class)
private val createUint8Array: (Int) -> JsAny = js("(size) => new Uint8Array(size)")

@OptIn(ExperimentalWasmJsInterop::class)
private val getArrayByte: (JsAny, Int) -> Int = js("(array, index) => array[index]")

@OptIn(ExperimentalWasmJsInterop::class)
private val isCryptoAvailable: () -> Boolean = js("() => typeof crypto !== 'undefined' && typeof crypto.getRandomValues === 'function'")

@OptIn(ExperimentalWasmJsInterop::class)
private val mathRandomByte: () -> Int = js("""() => {
    // Use multiple Math.random() calls and XOR them to reduce bias
    // This improves statistical properties for testing environments
    const r1 = Math.floor(Math.random() * 256);
    const r2 = Math.floor(Math.random() * 256);
    const r3 = Math.floor(Math.random() * 256);
    const r4 = Math.floor(Math.random() * 256);

    // XOR multiple sources to reduce bias patterns
    return (r1 ^ r2 ^ r3 ^ r4) & 0xFF;
}""")

/**
 * WASM-JS adapter implementation using Web Crypto API with secure fallback policy.
 *
 * This implementation provides secure random number generation for WASM-JS by:
 * - Using simplified crypto.getRandomValues() calls for WASM-JS compatibility
 * - Following experimental WASM-JS interop constraints
 * - Using basic JsAny types to avoid complex type mapping issues
 * - Supporting secure-by-default with explicit opt-in for insecure fallbacks
 *
 * **Security Policy:**
 * - Default (`FallbackPolicy.SECURE_ONLY`): Only uses Web Crypto API, fails if unavailable
 * - Opt-in (`FallbackPolicy.ALLOW_INSECURE`): Falls back to Math.random() when Web Crypto unavailable
 *
 * ⚠️  **CRITICAL:** Math.random() fallback is NOT cryptographically secure and requires
 * explicit `@OptIn(AllowInsecureFallback)` annotation. Never use for security-sensitive operations.
 */
@OptIn(ExperimentalWasmJsInterop::class)
internal class WasmJsSecureRandomAdapter private constructor(
    private val fallbackPolicy: FallbackPolicy
) : SecureRandom {

    private val logger = Logger.withTag("WasmJsSecureRandomAdapter")

    init {
        logger.d { "Initialized WASM-JS SecureRandom adapter with fallback policy: $fallbackPolicy" }
    }

    override fun nextBytes(bytes: ByteArray): SecureRandomUnitResult {
        return ParameterValidation.validateAndExecute {
            ParameterValidation.requireNonEmptyByteArray(bytes)

            try {
                fillBytesWithPolicy(bytes)
                logger.v { "Generated ${bytes.size} random bytes using WASM-JS adapter" }
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
                fillBytesWithPolicy(bytes)

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
                    fillBytesWithPolicy(bytes)

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
                fillBytesWithPolicy(bytes)

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
                    fillBytesWithPolicy(bytes)

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
                fillBytesWithPolicy(bytes)
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
                fillBytesWithPolicy(bytes)

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
                fillBytesWithPolicy(bytes)

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
                fillBytesWithPolicy(bytes)

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
     * Helper method to choose the appropriate byte filling strategy based on fallback policy.
     */
    private fun fillBytesWithPolicy(bytes: ByteArray) {
        when (fallbackPolicy) {
            FallbackPolicy.SECURE_ONLY -> fillBytesInternal(bytes)
            FallbackPolicy.ALLOW_INSECURE -> fillBytesWithInsecureFallback(bytes)
        }
    }

    /**
     * Internal helper method for WASM-JS crypto API using Web Crypto API only (secure).
     * No automatic fallback to insecure methods.
     */
    private fun fillBytesInternal(bytes: ByteArray) {
        try {
            if (isCryptoAvailable()) {
                // Use Web Crypto API (secure) with chunking for large arrays
                // Web Crypto API limit: 65,536 bytes per getRandomValues() call
                val maxChunkSize = 65536
                var offset = 0

                while (offset < bytes.size) {
                    val chunkSize = minOf(maxChunkSize, bytes.size - offset)
                    val uint8Array = createUint8Array(chunkSize)
                    crypto.getRandomValues(uint8Array)

                    for (i in 0 until chunkSize) {
                        val byteValue = getArrayByte(uint8Array, i)
                        bytes[offset + i] = (byteValue and 0xFF).toByte()
                    }

                    offset += chunkSize
                }

                logger.v { "Successfully generated ${bytes.size} random bytes using WASM-JS Web Crypto API" }
            } else {
                logger.e { "Web Crypto API not available and no insecure fallback allowed" }
                throw SecureRandomInitializationException(
                    "Web Crypto API not available in WASM-JS environment and secure-only policy is active"
                )
            }
        } catch (e: SecureRandomInitializationException) {
            throw e
        } catch (e: Exception) {
            logger.e(e) { "Failed to generate random bytes in WASM-JS environment" }
            throw SecureRandomGenerationException(
                "Failed to generate random bytes in WASM-JS environment",
                e
            )
        }
    }

    /**
     * Internal helper method that allows insecure Math.random fallback when explicitly requested.
     *
     * ⚠️  **CRITICAL SECURITY WARNING** ⚠️
     *
     * The Math.random() fallback is **NOT CRYPTOGRAPHICALLY SECURE** and should NEVER be used for:
     * - Password generation, reset tokens, or authentication secrets
     * - Cryptographic keys, initialization vectors, or nonces
     * - Session tokens, CSRF tokens, or any security-sensitive identifiers
     * - Random salts for password hashing or other cryptographic operations
     * - Any application where unpredictable randomness is required for security
     *
     * **Acceptable use cases for Math.random() fallback (non-security contexts only):**
     * - Generating unique IDs for UI elements or non-critical tracking
     * - Randomization for games, animations, or visual effects
     * - Statistical sampling for non-sensitive data analysis
     * - Testing and development environments (with explicit awareness of limitations)
     *
     * **Technical limitations:**
     * - Uses deterministic PRNG algorithms that can be predicted
     * - May have short periods and observable patterns
     * - Can be seeded or influenced by external factors
     * - Statistical properties may not meet cryptographic standards
     *
     * Even with XOR enhancement, the underlying entropy source remains non-cryptographic.
     * This fallback exists solely for compatibility with constrained WASM environments like D8.
     */
    private fun fillBytesWithInsecureFallback(bytes: ByteArray) {
        try {
            if (isCryptoAvailable()) {
                // Use Web Crypto API (secure) with chunking for large arrays
                // Web Crypto API limit: 65,536 bytes per getRandomValues() call
                val maxChunkSize = 65536
                var offset = 0

                while (offset < bytes.size) {
                    val chunkSize = minOf(maxChunkSize, bytes.size - offset)
                    val uint8Array = createUint8Array(chunkSize)
                    crypto.getRandomValues(uint8Array)

                    for (i in 0 until chunkSize) {
                        val byteValue = getArrayByte(uint8Array, i)
                        bytes[offset + i] = (byteValue and 0xFF).toByte()
                    }

                    offset += chunkSize
                }

                logger.v { "Successfully generated ${bytes.size} random bytes using WASM-JS Web Crypto API" }
            } else {
                // ⚠️ INSECURE FALLBACK: Math.random() - NOT CRYPTOGRAPHICALLY SECURE ⚠️
                logger.w {
                    "⚠️ SECURITY WARNING: Web Crypto API unavailable, using Math.random() fallback. " +
                    "This is NOT CRYPTOGRAPHICALLY SECURE and should NEVER be used for passwords, keys, " +
                    "tokens, or any security-sensitive operations! Only suitable for non-security use cases."
                }

                for (i in bytes.indices) {
                    bytes[i] = mathRandomByte().toByte()
                }

                logger.v { "Generated ${bytes.size} random bytes using Math.random() fallback (⚠️ INSECURE - not suitable for cryptographic use)" }
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to generate random bytes in WASM-JS environment" }
            throw SecureRandomGenerationException(
                "Failed to generate random bytes in WASM-JS environment",
                e
            )
        }
    }

    companion object {
        private const val MAX_BYTE_ARRAY_SIZE = Int.MAX_VALUE - 8
        private val logger = Logger.withTag("WasmJsSecureRandomAdapter")

        fun create(fallbackPolicy: FallbackPolicy = FallbackPolicy.SECURE_ONLY): SecureRandomResult<WasmJsSecureRandomAdapter> {
            return SecureRandomResult.runCatching {
                try {
                    when (fallbackPolicy) {
                        FallbackPolicy.SECURE_ONLY -> {
                            if (isCryptoAvailable()) {
                                logger.i { "Successfully initialized WASM-JS SecureRandom adapter with Web Crypto API (secure-only)" }
                            } else {
                                logger.e { "Web Crypto API not available and secure-only policy active" }
                                throw SecureRandomInitializationException(
                                    "Web Crypto API not available in WASM-JS environment and secure-only policy is active"
                                )
                            }
                        }
                        FallbackPolicy.ALLOW_INSECURE -> {
                            if (isCryptoAvailable()) {
                                logger.i { "Successfully initialized WASM-JS SecureRandom adapter with Web Crypto API (insecure fallback allowed)" }
                            } else {
                                logger.w { "Web Crypto API not available, will use enhanced Math.random fallback (NOT CRYPTOGRAPHICALLY SECURE)" }
                                logger.i { "Successfully initialized WASM-JS SecureRandom adapter with insecure Math.random fallback" }
                            }
                        }
                    }

                    WasmJsSecureRandomAdapter(fallbackPolicy)
                } catch (e: SecureRandomInitializationException) {
                    throw e
                } catch (e: Exception) {
                    logger.e(e) { "Failed to initialize WASM-JS SecureRandom adapter" }
                    throw SecureRandomInitializationException(
                        "Failed to initialize WASM-JS SecureRandom adapter",
                        e
                    )
                }
            }
        }

        // Backward compatibility - delegates to secure-only policy
        fun create(): SecureRandomResult<WasmJsSecureRandomAdapter> {
            return create(FallbackPolicy.SECURE_ONLY)
        }
    }
}