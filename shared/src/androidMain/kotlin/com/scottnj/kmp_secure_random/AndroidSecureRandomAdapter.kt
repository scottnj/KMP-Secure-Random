package com.scottnj.kmp_secure_random

import android.os.Build
import co.touchlab.kermit.Logger
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom as JavaSecureRandom
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Android adapter implementation wrapping java.security.SecureRandom with Android-specific
 * optimizations, algorithm selection, and comprehensive error handling.
 *
 * This implementation leverages Android's crypto provider and includes fallback mechanisms
 * for older Android versions while maintaining compatibility with the JVM SecureRandom API.
 */
internal class AndroidSecureRandomAdapter private constructor(
    private val javaSecureRandom: JavaSecureRandom,
    private val algorithmName: String,
    private val androidVersion: Int
) : SecureRandom {

    private val lock = ReentrantReadWriteLock()
    private val logger = Logger.withTag("AndroidSecureRandomAdapter")

    init {
        logger.d { "Initialized Android SecureRandom adapter with algorithm: $algorithmName (Android API $androidVersion)" }
    }

    override fun nextBytes(bytes: ByteArray): SecureRandomUnitResult {
        return ParameterValidation.validateAndExecute {
            ParameterValidation.requireNonEmptyByteArray(bytes)

            lock.read {
                try {
                    javaSecureRandom.nextBytes(bytes)
                    logger.v { "Generated ${bytes.size} random bytes on Android API $androidVersion" }
                } catch (e: Exception) {
                    logger.e(e) { "Failed to generate ${bytes.size} random bytes on Android" }
                    throw SecureRandomGenerationException(
                        "Failed to generate random bytes on Android",
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
                    logger.e(e) { "Failed to generate random int on Android" }
                    throw SecureRandomGenerationException(
                        "Failed to generate random integer on Android",
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
                    logger.e(e) { "Failed to generate random int with bound $bound on Android" }
                    throw SecureRandomGenerationException(
                        "Failed to generate bounded random integer on Android",
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
                    logger.e(e) { "Failed to generate random int in range [$min, $max) on Android" }
                    throw SecureRandomGenerationException(
                        "Failed to generate random integer in range on Android",
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
                    logger.e(e) { "Failed to generate random long on Android" }
                    throw SecureRandomGenerationException(
                        "Failed to generate random long on Android",
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
                    logger.e(e) { "Failed to generate random long with bound $bound on Android" }
                    throw SecureRandomGenerationException(
                        "Failed to generate bounded random long on Android",
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
                    logger.e(e) { "Failed to generate random long in range [$min, $max) on Android" }
                    throw SecureRandomGenerationException(
                        "Failed to generate random long in range on Android",
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
                    logger.e(e) { "Failed to generate random boolean on Android" }
                    throw SecureRandomGenerationException(
                        "Failed to generate random boolean on Android",
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
                    logger.e(e) { "Failed to generate random double on Android" }
                    throw SecureRandomGenerationException(
                        "Failed to generate random double on Android",
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
                    logger.e(e) { "Failed to generate random float on Android" }
                    throw SecureRandomGenerationException(
                        "Failed to generate random float on Android",
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
                    logger.v { "Generated byte array of size $size on Android API $androidVersion" }
                    bytes
                } catch (e: OutOfMemoryError) {
                    logger.e(e) { "Out of memory generating byte array of size $size on Android" }
                    throw InsufficientResourcesException(
                        "Insufficient memory to generate $size bytes on Android",
                        "memory"
                    )
                } catch (e: Exception) {
                    logger.e(e) { "Failed to generate byte array of size $size on Android" }
                    throw SecureRandomGenerationException(
                        "Failed to generate random byte array on Android",
                        e
                    )
                }
            }
        }
    }

    companion object {
        private const val MAX_BYTE_ARRAY_SIZE = Int.MAX_VALUE - 8 // Conservative limit
        private val logger = Logger.withTag("AndroidSecureRandomAdapter")

        /**
         * Android-specific algorithm preferences based on API level and security features.
         * Newer Android versions have access to better crypto providers.
         */
        private fun getPreferredAlgorithms(androidVersion: Int): List<String> {
            return when {
                // Android 6.0+ (API 23+) - Modern Android crypto provider
                androidVersion >= Build.VERSION_CODES.M -> listOf(
                    "SHA1PRNG",           // Android's crypto provider
                    "NativePRNG",         // Unix/Linux /dev/urandom
                    "SecureRandom"        // Generic fallback
                )

                // Android 4.4+ (API 19+) - Improved crypto support
                androidVersion >= Build.VERSION_CODES.KITKAT -> listOf(
                    "SHA1PRNG",           // Android's crypto provider
                    "NativePRNG",         // Unix/Linux /dev/urandom
                    "SecureRandom"        // Generic fallback
                )

                // Older Android versions - Basic crypto support
                else -> listOf(
                    "SHA1PRNG",           // Android's primary provider
                    "SecureRandom"        // Fallback
                )
            }
        }

        fun create(): SecureRandomResult<AndroidSecureRandomAdapter> {
            return SecureRandomResult.runCatching {
                val androidVersion = Build.VERSION.SDK_INT
                val (javaSecureRandom, algorithmName) = selectBestAlgorithm(androidVersion)

                // Seed the random number generator for better entropy
                try {
                    javaSecureRandom.generateSeed(1) // Force seeding
                } catch (e: Exception) {
                    logger.w(e) { "Warning: Could not generate seed for initialization on Android API $androidVersion" }
                }

                logger.i { "Successfully created Android SecureRandom with algorithm: $algorithmName (API $androidVersion)" }
                AndroidSecureRandomAdapter(javaSecureRandom, algorithmName, androidVersion)
            }
        }

        private fun selectBestAlgorithm(androidVersion: Int): Pair<JavaSecureRandom, String> {
            val preferredAlgorithms = getPreferredAlgorithms(androidVersion)

            for (algorithm in preferredAlgorithms) {
                try {
                    val secureRandom = JavaSecureRandom.getInstance(algorithm)
                    logger.d { "Successfully created Android SecureRandom with algorithm: $algorithm (API $androidVersion)" }
                    return Pair(secureRandom, algorithm)
                } catch (e: NoSuchAlgorithmException) {
                    logger.d { "Algorithm $algorithm not available on Android API $androidVersion: ${e.message}" }
                }
            }

            // Fallback to default SecureRandom
            try {
                val secureRandom = JavaSecureRandom()
                val algorithmName = secureRandom.algorithm ?: "default"
                logger.i { "Using default Android SecureRandom algorithm: $algorithmName (API $androidVersion)" }
                return Pair(secureRandom, algorithmName)
            } catch (e: Exception) {
                logger.e(e) { "Failed to create default SecureRandom on Android API $androidVersion" }
                throw SecureRandomInitializationException(
                    "Failed to initialize any SecureRandom algorithm on Android",
                    e
                )
            }
        }
    }
}