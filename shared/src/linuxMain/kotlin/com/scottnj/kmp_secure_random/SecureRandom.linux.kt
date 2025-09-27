@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.scottnj.kmp_secure_random

import co.touchlab.kermit.Logger
import kotlinx.cinterop.*
import platform.posix.*

/**
 * Linux adapter implementation using native Linux secure random APIs with comprehensive
 * error handling, syscall support, and fallback mechanisms.
 *
 * Preferentially uses getrandom() syscall (Linux 3.17+) with fallback to /dev/urandom
 * for maximum security and compatibility across Linux distributions.
 */
internal class LinuxSecureRandomAdapter private constructor() : SecureRandom {

    private val logger = Logger.withTag("LinuxSecureRandomAdapter")

    companion object {
        // getrandom() syscall number for x86_64 Linux
        private const val SYS_GETRANDOM = 318L
        private const val GRND_NONBLOCK = 0x0001

        /**
         * Creates a new LinuxSecureRandomAdapter instance with proper initialization.
         */
        fun create(): SecureRandomResult<LinuxSecureRandomAdapter> {
            return SecureRandomResult.runCatching {
                val adapter = LinuxSecureRandomAdapter()
                adapter.logger.d { "Initialized Linux SecureRandom adapter" }
                adapter
            }
        }
    }

    override fun nextBytes(bytes: ByteArray): SecureRandomUnitResult {
        return ParameterValidation.validateAndExecute {
            ParameterValidation.requireNonEmptyByteArray(bytes)

            try {
                fillBytesSecurely(bytes)
                logger.v { "Generated ${bytes.size} random bytes" }
            } catch (e: Exception) {
                logger.e(e) { "Failed to generate ${bytes.size} random bytes" }
                throw SecureRandomGenerationException(
                    "Failed to generate random bytes on Linux",
                    e
                )
            }
        }
    }

    override fun nextInt(): SecureRandomResult<Int> {
        return SecureRandomResult.runCatching {
            val bytes = ByteArray(4)
            fillBytesSecurely(bytes)
            val result = ((bytes[0].toInt() and 0xFF) shl 24) or
                        ((bytes[1].toInt() and 0xFF) shl 16) or
                        ((bytes[2].toInt() and 0xFF) shl 8) or
                        (bytes[3].toInt() and 0xFF)
            logger.v { "Generated random int" }
            result
        }
    }

    override fun nextInt(bound: Int): SecureRandomResult<Int> {
        return SecureRandomResult.runCatching {
            ParameterValidation.requirePositiveBound(bound)

            // Use rejection sampling to avoid modulo bias
            val maxValidValue = Int.MAX_VALUE - (Int.MAX_VALUE % bound)
            var candidate: Int
            do {
                candidate = nextInt().getOrThrow() and Int.MAX_VALUE
            } while (candidate >= maxValidValue)

            val result = candidate % bound
            logger.v { "Generated random int with bound $bound" }
            result
        }
    }

    override fun nextInt(min: Int, max: Int): SecureRandomResult<Int> {
        return SecureRandomResult.runCatching {
            ParameterValidation.requireValidRange(min, max)
            val range = max - min
            val result = min + nextInt(range).getOrThrow()
            logger.v { "Generated random int in range [$min, $max)" }
            result
        }
    }

    override fun nextLong(): SecureRandomResult<Long> {
        return SecureRandomResult.runCatching {
            val bytes = ByteArray(8)
            fillBytesSecurely(bytes)
            val result = ((bytes[0].toLong() and 0xFF) shl 56) or
                        ((bytes[1].toLong() and 0xFF) shl 48) or
                        ((bytes[2].toLong() and 0xFF) shl 40) or
                        ((bytes[3].toLong() and 0xFF) shl 32) or
                        ((bytes[4].toLong() and 0xFF) shl 24) or
                        ((bytes[5].toLong() and 0xFF) shl 16) or
                        ((bytes[6].toLong() and 0xFF) shl 8) or
                        (bytes[7].toLong() and 0xFF)
            logger.v { "Generated random long" }
            result
        }
    }

    override fun nextLong(bound: Long): SecureRandomResult<Long> {
        return SecureRandomResult.runCatching {
            ParameterValidation.requirePositiveBound(bound)

            // Use rejection sampling to avoid modulo bias
            val maxValidValue = Long.MAX_VALUE - (Long.MAX_VALUE % bound)
            var candidate: Long
            do {
                candidate = nextLong().getOrThrow() and Long.MAX_VALUE
            } while (candidate >= maxValidValue)

            val result = candidate % bound
            logger.v { "Generated random long with bound $bound" }
            result
        }
    }

    override fun nextLong(min: Long, max: Long): SecureRandomResult<Long> {
        return SecureRandomResult.runCatching {
            ParameterValidation.requireValidRange(min, max)
            val range = max - min
            val result = min + nextLong(range).getOrThrow()
            logger.v { "Generated random long in range [$min, $max)" }
            result
        }
    }

    override fun nextBoolean(): SecureRandomResult<Boolean> {
        return SecureRandomResult.runCatching {
            val bytes = ByteArray(1)
            fillBytesSecurely(bytes)
            val result = (bytes[0].toInt() and 1) == 1
            logger.v { "Generated random boolean" }
            result
        }
    }

    override fun nextDouble(): SecureRandomResult<Double> {
        return SecureRandomResult.runCatching {
            val bytes = ByteArray(8)
            fillBytesSecurely(bytes)
            val longBits = ((bytes[0].toLong() and 0xFF) shl 56) or
                          ((bytes[1].toLong() and 0xFF) shl 48) or
                          ((bytes[2].toLong() and 0xFF) shl 40) or
                          ((bytes[3].toLong() and 0xFF) shl 32) or
                          ((bytes[4].toLong() and 0xFF) shl 24) or
                          ((bytes[5].toLong() and 0xFF) shl 16) or
                          ((bytes[6].toLong() and 0xFF) shl 8) or
                          (bytes[7].toLong() and 0xFF)

            // Convert to [0.0, 1.0) by using 53 bits of precision
            val result = (longBits ushr 11).toDouble() / (1L shl 53).toDouble()
            logger.v { "Generated random double" }
            result
        }
    }

    override fun nextFloat(): SecureRandomResult<Float> {
        return SecureRandomResult.runCatching {
            val bytes = ByteArray(4)
            fillBytesSecurely(bytes)
            val intBits = ((bytes[0].toInt() and 0xFF) shl 24) or
                         ((bytes[1].toInt() and 0xFF) shl 16) or
                         ((bytes[2].toInt() and 0xFF) shl 8) or
                         (bytes[3].toInt() and 0xFF)

            // Convert to [0.0, 1.0) by using 24 bits of precision
            val result = (intBits ushr 8).toFloat() / (1 shl 24).toFloat()
            logger.v { "Generated random float" }
            result
        }
    }

    override fun nextBytes(size: Int): SecureRandomResult<ByteArray> {
        return SecureRandomResult.runCatching {
            ParameterValidation.requireNonNegativeSize(size)
            val bytes = ByteArray(size)
            if (size > 0) {
                fillBytesSecurely(bytes)
            }
            logger.v { "Generated $size random bytes" }
            bytes
        }
    }

    /**
     * Fills the given byte array with cryptographically secure random bytes using
     * Linux-specific APIs with proper error handling and fallback mechanisms.
     */
    private fun fillBytesSecurely(bytes: ByteArray) {
        try {
            // Try getrandom() syscall first (Linux 3.17+)
            if (tryGetrandom(bytes)) {
                return
            }
        } catch (e: Exception) {
            logger.w(e) { "getrandom() failed, falling back to /dev/urandom" }
        }

        // Fallback to /dev/urandom for older kernels or when getrandom() fails
        readFromDevUrandom(bytes)
    }

    /**
     * Attempts to use the getrandom() syscall to fill the byte array.
     * Returns true if successful, false if the syscall is not available.
     */
    private fun tryGetrandom(bytes: ByteArray): Boolean {
        return memScoped {
            val buffer = allocArray<ByteVar>(bytes.size)

            try {
                val result = syscall(SYS_GETRANDOM, buffer, bytes.size.convert<size_t>(), 0)

                when {
                    result == bytes.size.toLong() -> {
                        // Success - copy bytes
                        for (i in bytes.indices) {
                            bytes[i] = buffer[i]
                        }
                        logger.v { "Successfully used getrandom() for ${bytes.size} bytes" }
                        true
                    }
                    result == -1L -> {
                        val error = errno
                        when (error) {
                            ENOSYS -> {
                                logger.d { "getrandom() not available (ENOSYS)" }
                                false
                            }
                            EAGAIN -> {
                                throw SecureRandomGenerationException(
                                    "getrandom() would block (insufficient entropy)"
                                )
                            }
                            EINTR -> {
                                throw SecureRandomGenerationException(
                                    "getrandom() interrupted by signal"
                                )
                            }
                            EFAULT -> {
                                throw SecureRandomGenerationException(
                                    "getrandom() buffer fault"
                                )
                            }
                            EINVAL -> {
                                throw SecureRandomGenerationException(
                                    "getrandom() invalid parameters"
                                )
                            }
                            else -> {
                                throw SecureRandomGenerationException(
                                    "getrandom() failed with errno: $error"
                                )
                            }
                        }
                    }
                    else -> {
                        throw SecureRandomGenerationException(
                            "getrandom() returned unexpected value: $result"
                        )
                    }
                }
            } catch (e: Exception) {
                logger.w(e) { "getrandom() syscall failed" }
                false
            }
        }
    }

    /**
     * Reads random bytes from /dev/urandom with proper error handling.
     */
    private fun readFromDevUrandom(bytes: ByteArray) {
        val file = fopen("/dev/urandom", "rb")
            ?: throw SecureRandomInitializationException(
                "Failed to open /dev/urandom: errno $errno"
            )

        try {
            memScoped {
                val buffer = allocArray<ByteVar>(bytes.size)
                val bytesRead = fread(buffer, 1u, bytes.size.convert(), file)

                if (bytesRead.toInt() != bytes.size) {
                    throw SecureRandomGenerationException(
                        "Failed to read ${bytes.size} bytes from /dev/urandom, got $bytesRead"
                    )
                }

                // Copy bytes from native buffer to Kotlin ByteArray
                for (i in bytes.indices) {
                    bytes[i] = buffer[i]
                }

                logger.v { "Successfully read ${bytes.size} bytes from /dev/urandom" }
            }
        } finally {
            fclose(file)
        }
    }
}

actual fun createSecureRandom(): SecureRandomResult<SecureRandom> {
    return LinuxSecureRandomAdapter.create().map { it as SecureRandom }
}