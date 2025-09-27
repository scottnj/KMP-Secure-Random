package com.scottnj.kmp_secure_random

import co.touchlab.kermit.Logger
import kotlinx.cinterop.*
import platform.windows.*
import kotlin.experimental.ExperimentalNativeApi

/**
 * Windows implementation of SecureRandom using CryptGenRandom.
 *
 * CryptGenRandom is available on Windows 2000 and later and provides
 * cryptographically secure random number generation.
 *
 * This implementation is thread-safe and provides cryptographically secure randomness.
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
internal class WindowsSecureRandom : SecureRandom {

    private val logger = Logger.withTag("WindowsSecureRandom")

    // CryptAPI context for Windows
    private var cryptContext: HCRYPTPROV? = null

    init {
        // Initialize CryptAPI (Windows 2000 and later)
        val cryptResult = tryInitCryptAPI()
        if (!cryptResult) {
            logger.w { "Failed to initialize Windows SecureRandom with CryptAPI, will use per-call initialization" }
        } else {
            logger.d { "Windows SecureRandom initialized with CryptGenRandom" }
        }
    }

    private fun tryInitCryptAPI(): Boolean = memScoped {
        try {
            val hProv = alloc<ULongVarOf<ULong>>()
            val result = CryptAcquireContextW(
                hProv.ptr,
                null,
                null,
                1u, // PROV_RSA_FULL
                0xF0000000u // CRYPT_VERIFYCONTEXT
            )

            if (result != 0) {
                cryptContext = hProv.value
                return true
            }

            val error = GetLastError()
            logger.w { "CryptAcquireContextW failed with error: $error" }
            return false
        } catch (e: Exception) {
            logger.w { "CryptAPI initialization failed: ${e.message}" }
            return false
        }
    }

    /**
     * Fills the given byte array with cryptographically secure random bytes.
     */
    private fun fillBytesSecurely(bytes: ByteArray): Unit = memScoped {
        if (bytes.isEmpty()) return

        bytes.usePinned { pinned ->
            val buffer = pinned.addressOf(0).reinterpret<UByteVar>()
            val size = bytes.size.toUInt()

            // Try CryptAPI
            if (cryptContext != null) {
                val result = CryptGenRandom(
                    cryptContext!!,
                    size,
                    buffer
                )

                if (result != 0) {
                    return
                }

                val error = GetLastError()
                logger.w { "CryptGenRandom failed with error: $error" }
            }

            // Last resort: try to initialize and use CryptAPI on demand
            val hProv = alloc<ULongVarOf<ULong>>()
            val contextResult = CryptAcquireContextW(
                hProv.ptr,
                null,
                null,
                1u, // PROV_RSA_FULL
                0xF0000000u // CRYPT_VERIFYCONTEXT
            )

            if (contextResult != 0) {
                val genResult = CryptGenRandom(
                    hProv.value,
                    size,
                    buffer
                )

                CryptReleaseContext(hProv.value, 0u)

                if (genResult != 0) {
                    return
                }
            }

            throw SecureRandomGenerationException("Failed to generate random bytes on Windows")
        }
    }

    override fun nextBytes(bytes: ByteArray): SecureRandomUnitResult {
        return SecureRandomUnitResult.runCatching {
            fillBytesSecurely(bytes)
            logger.v { "Generated ${bytes.size} random bytes" }
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
     * Clean up Windows crypto resources.
     * Note: This is called by the garbage collector, but can be called manually if needed.
     */
    @Suppress("unused")
    fun cleanup() {
        cryptContext?.let {
            CryptReleaseContext(it, 0u)
            cryptContext = null
        }
    }
}