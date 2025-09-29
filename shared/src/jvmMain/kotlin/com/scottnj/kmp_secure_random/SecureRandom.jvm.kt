package com.scottnj.kmp_secure_random

import co.touchlab.kermit.Logger
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom as JavaSecureRandom
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * JVM adapter implementation wrapping java.security.SecureRandom with comprehensive
 * error handling, algorithm selection, and performance optimization.
 */
internal class JvmSecureRandomAdapter private constructor(
    private val javaSecureRandom: JavaSecureRandom,
    private val algorithmName: String
) : SecureRandom {

    private val lock = ReentrantReadWriteLock()
    private val logger = Logger.withTag("JvmSecureRandomAdapter")

    init {
        logger.d { "Initialized JVM SecureRandom adapter with algorithm: $algorithmName" }
    }

    override fun nextBytes(bytes: ByteArray): SecureRandomUnitResult {
        return ParameterValidation.validateAndExecute {
            ParameterValidation.requireNonEmptyByteArray(bytes)

            lock.read {
                try {
                    javaSecureRandom.nextBytes(bytes)
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
    }

    override fun nextInt(): SecureRandomResult<Int> {
        return SecureRandomResult.runCatching {
            lock.read {
                try {
                    val result = javaSecureRandom.nextInt()
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
    }

    override fun nextInt(bound: Int): SecureRandomResult<Int> {
        return ParameterValidation.validateAndExecute {
            ParameterValidation.requirePositiveBound(bound)

            lock.read {
                try {
                    val result = javaSecureRandom.nextInt(bound)
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
    }

    override fun nextInt(min: Int, max: Int): SecureRandomResult<Int> {
        return ParameterValidation.validateAndExecute {
            ParameterValidation.requireValidRange(min, max)

            lock.read {
                try {
                    val range = max - min
                    val result = min + javaSecureRandom.nextInt(range)
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
    }

    override fun nextLong(): SecureRandomResult<Long> {
        return SecureRandomResult.runCatching {
            lock.read {
                try {
                    val result = javaSecureRandom.nextLong()
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
    }

    override fun nextLong(bound: Long): SecureRandomResult<Long> {
        return ParameterValidation.validateAndExecute {
            ParameterValidation.requirePositiveBound(bound)

            lock.read {
                try {
                    // Handle edge case
                    if (bound == 1L) {
                        logger.v { "Generated random long with bound $bound: 0" }
                        return@validateAndExecute 0L
                    }

                    // Use rejection sampling for unbiased results
                    var result: Long
                    do {
                        result = javaSecureRandom.nextLong() and Long.MAX_VALUE // Make positive
                        result %= bound
                    } while (result < 0) // This shouldn't happen with the mask above, but be safe

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
    }

    override fun nextLong(min: Long, max: Long): SecureRandomResult<Long> {
        return ParameterValidation.validateAndExecute {
            ParameterValidation.requireValidRange(min, max)

            lock.read {
                try {
                    val range = max - min

                    // Handle edge case
                    if (range == 1L) {
                        logger.v { "Generated random long in range [$min, $max): $min" }
                        return@validateAndExecute min
                    }

                    // Generate random in range using simple modulo approach
                    var result: Long
                    do {
                        result = javaSecureRandom.nextLong() and Long.MAX_VALUE
                        result %= range
                    } while (result < 0)

                    result += min
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
    }

    override fun nextBoolean(): SecureRandomResult<Boolean> {
        return SecureRandomResult.runCatching {
            lock.read {
                try {
                    val result = javaSecureRandom.nextBoolean()
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
    }

    override fun nextDouble(): SecureRandomResult<Double> {
        return SecureRandomResult.runCatching {
            lock.read {
                try {
                    val result = javaSecureRandom.nextDouble()
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
    }

    override fun nextFloat(): SecureRandomResult<Float> {
        return SecureRandomResult.runCatching {
            lock.read {
                try {
                    val result = javaSecureRandom.nextFloat()
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
    }

    override fun nextBytes(size: Int): SecureRandomResult<ByteArray> {
        return ParameterValidation.validateAndExecute {
            ParameterValidation.requireNonNegativeSize(size)
            ParameterValidation.requireSizeWithinLimit(size, MAX_BYTE_ARRAY_SIZE)

            if (size == 0) {
                return@validateAndExecute ByteArray(0)
            }

            lock.read {
                try {
                    val bytes = ByteArray(size)
                    javaSecureRandom.nextBytes(bytes)
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
    }

    companion object {
        private const val MAX_BYTE_ARRAY_SIZE = Int.MAX_VALUE - 8 // Conservative limit
        private val logger = Logger.withTag("JvmSecureRandomAdapter")

        private val PREFERRED_ALGORITHMS = listOf(
            "NativePRNG",         // Unix/Linux /dev/urandom
            "Windows-PRNG",       // Windows CryptGenRandom
            "SHA1PRNG",           // Fallback SHA1-based PRNG
            "SecureRandom"        // Generic fallback
        )

        fun create(): SecureRandomResult<JvmSecureRandomAdapter> {
            return SecureRandomResult.runCatching {
                val (javaSecureRandom, algorithmName) = selectBestAlgorithm()

                // Seed the random number generator for better entropy
                try {
                    javaSecureRandom.generateSeed(1) // Force seeding
                } catch (e: Exception) {
                    logger.w(e) { "Warning: Could not generate seed for initialization" }
                }

                logger.i { "Successfully created JVM SecureRandom with algorithm: $algorithmName" }
                JvmSecureRandomAdapter(javaSecureRandom, algorithmName)
            }
        }

        private fun selectBestAlgorithm(): Pair<JavaSecureRandom, String> {
            for (algorithm in PREFERRED_ALGORITHMS) {
                try {
                    val secureRandom = JavaSecureRandom.getInstance(algorithm)
                    logger.d { "Successfully created SecureRandom with algorithm: $algorithm" }
                    return Pair(secureRandom, algorithm)
                } catch (e: NoSuchAlgorithmException) {
                    logger.d { "Algorithm $algorithm not available: ${e.message}" }
                }
            }

            // Fallback to default SecureRandom
            try {
                val secureRandom = JavaSecureRandom()
                val algorithmName = secureRandom.algorithm ?: "default"
                logger.i { "Using default SecureRandom algorithm: $algorithmName" }
                return Pair(secureRandom, algorithmName)
            } catch (e: Exception) {
                logger.e(e) { "Failed to create default SecureRandom" }
                throw SecureRandomInitializationException(
                    "Failed to initialize any SecureRandom algorithm",
                    e
                )
            }
        }
    }
}

@AllowInsecureFallback
actual fun createSecureRandom(fallbackPolicy: FallbackPolicy): SecureRandomResult<SecureRandom> {
    // JVM platform has no insecure fallbacks - all SecureRandom algorithms are cryptographically secure
    // fallbackPolicy parameter is ignored as there are no insecure fallbacks to allow
    return JvmSecureRandomAdapter.create().map { it as SecureRandom }
}

actual fun createSecureRandom(): SecureRandomResult<SecureRandom> {
    return JvmSecureRandomAdapter.create().map { it as SecureRandom }
}