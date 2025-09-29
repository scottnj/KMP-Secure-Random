@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.scottnj.kmp_secure_random

import co.touchlab.kermit.Logger
import kotlinx.cinterop.*
import platform.posix.*

/**
 * Android Native ARM64 adapter implementation using native Android secure random APIs
 * with comprehensive error handling, syscall support, and fallback mechanisms.
 *
 * Uses ARM64-specific getrandom() syscall (syscall #278) with 64-bit ULong types
 * and fallback to /dev/urandom for maximum security and compatibility.
 */
internal class AndroidNativeArm64SecureRandomAdapter private constructor() : SecureRandom {

    private val logger = Logger.withTag("AndroidNativeArm64SecureRandomAdapter")

    companion object {
        // getrandom() syscall number for ARM64 Android Native
        private const val SYS_GETRANDOM = 278L
        private const val GRND_NONBLOCK = 0x0001L

        /**
         * Creates a new AndroidNativeArm64SecureRandomAdapter instance with proper initialization.
         */
        fun create(): SecureRandomResult<AndroidNativeArm64SecureRandomAdapter> {
            return SecureRandomResult.runCatching {
                val adapter = AndroidNativeArm64SecureRandomAdapter()
                adapter.logger.i { "Creating Android Native ARM64 SecureRandom adapter..." }

                // Test initialization with diagnostic checks
                adapter.performInitializationChecks()

                adapter.logger.i { "Android Native ARM64 SecureRandom adapter initialized successfully" }
                adapter
            }
        }
    }

    /**
     * Performs diagnostic checks during initialization to help debug issues.
     */
    private fun performInitializationChecks() {
        logger.i { "Performing Android Native ARM64 environment diagnostic checks..." }

        // Check /dev/urandom accessibility
        try {
            val testFile = fopen("/dev/urandom", "rb")
            if (testFile != null) {
                logger.i { "✅ /dev/urandom is accessible" }
                fclose(testFile)
            } else {
                logger.e { "❌ Cannot open /dev/urandom: errno $errno" }
                throw SecureRandomInitializationException("Cannot access /dev/urandom: errno $errno")
            }
        } catch (e: Exception) {
            logger.e(e) { "❌ /dev/urandom accessibility check failed" }
            throw e
        }

        // Test getrandom() availability for ARM64
        try {
            logger.i { "Testing getrandom() syscall availability (ARM64 #278)..." }
            val testBytes = ByteArray(1)
            val available = tryGetrandomDiagnostic(testBytes)
            if (available) {
                logger.i { "✅ getrandom() syscall is available and working on ARM64" }
            } else {
                logger.i { "ℹ️ getrandom() syscall not available, will use /dev/urandom fallback" }
            }
        } catch (e: Exception) {
            logger.w(e) { "⚠️ getrandom() test failed, will use /dev/urandom fallback" }
        }

        // Test small random generation
        try {
            logger.i { "Testing small random byte generation..." }
            val testBytes = ByteArray(4)
            fillBytesSecurely(testBytes)
            logger.i { "✅ Successfully generated 4 test bytes" }
        } catch (e: Exception) {
            logger.e(e) { "❌ Failed to generate test bytes" }
            throw e
        }

        logger.i { "🎯 All Android Native ARM64 environment checks passed!" }
    }

    /**
     * Diagnostic version of tryGetrandom that provides more detailed logging.
     */
    private fun tryGetrandomDiagnostic(bytes: ByteArray): Boolean {
        return memScoped {
            val buffer = allocArray<ByteVar>(bytes.size)

            try {
                logger.d { "Attempting getrandom() syscall (ARM64 #278) with ${bytes.size} bytes..." }
                val result = syscall(SYS_GETRANDOM, buffer, bytes.size.convert<size_t>(), 0)
                logger.d { "getrandom() returned: $result (expected: ${bytes.size})" }

                when {
                    result == bytes.size.toLong() -> {
                        logger.d { "✅ getrandom() successful on ARM64" }
                        return true
                    }
                    result == -1L -> { // -1 indicates error
                        val error = errno
                        logger.d { "getrandom() failed with errno: $error" }
                        when (error) {
                            ENOSYS -> {
                                logger.d { "getrandom() not available (ENOSYS) - normal for older kernels" }
                                return false
                            }
                            else -> {
                                logger.w { "getrandom() failed with errno $error" }
                                return false
                            }
                        }
                    }
                    else -> {
                        logger.w { "getrandom() returned unexpected value: $result" }
                        return false
                    }
                }
            } catch (e: Exception) {
                logger.w(e) { "getrandom() syscall threw exception" }
                return false
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
                    "Failed to generate random bytes on Android Native ARM64",
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
     * Android Native ARM64-specific APIs with proper error handling and fallback mechanisms.
     */
    private fun fillBytesSecurely(bytes: ByteArray) {
        logger.v { "Filling ${bytes.size} bytes securely on ARM64..." }

        try {
            // Try getrandom() syscall first (ARM64 syscall #278)
            logger.v { "Attempting getrandom() syscall (ARM64 #278)..." }
            if (tryGetrandom(bytes)) {
                logger.v { "Successfully used getrandom() for ${bytes.size} bytes" }
                return
            }
            logger.v { "getrandom() not available, falling back to /dev/urandom" }
        } catch (e: Exception) {
            logger.w(e) { "getrandom() failed, falling back to /dev/urandom" }
        }

        // Fallback to /dev/urandom for older kernels or when getrandom() fails
        logger.v { "Using /dev/urandom fallback..." }
        readFromDevUrandom(bytes)
        logger.v { "Successfully used /dev/urandom for ${bytes.size} bytes" }
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
                    result == -1L -> { // -1 indicates error
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
        logger.v { "Opening /dev/urandom for ${bytes.size} bytes..." }

        val file = fopen("/dev/urandom", "rb")
        if (file == null) {
            val errorCode = errno
            logger.e { "Failed to open /dev/urandom: errno $errorCode" }
            throw SecureRandomInitializationException(
                "Failed to open /dev/urandom: errno $errorCode"
            )
        }

        logger.v { "Successfully opened /dev/urandom" }

        try {
            memScoped {
                val buffer = allocArray<ByteVar>(bytes.size)
                logger.v { "Reading ${bytes.size} bytes from /dev/urandom..." }
                val bytesRead = fread(buffer, 1u, bytes.size.convert(), file)

                logger.v { "Read $bytesRead bytes from /dev/urandom (expected: ${bytes.size})" }

                if (bytesRead.toInt() != bytes.size) {
                    val errorCode = errno
                    logger.e { "Failed to read ${bytes.size} bytes from /dev/urandom, got $bytesRead, errno: $errorCode" }
                    throw SecureRandomGenerationException(
                        "Failed to read ${bytes.size} bytes from /dev/urandom, got $bytesRead, errno: $errorCode"
                    )
                }

                // Copy bytes from native buffer to Kotlin ByteArray
                for (i in bytes.indices) {
                    bytes[i] = buffer[i]
                }

                logger.v { "Successfully read and copied ${bytes.size} bytes from /dev/urandom" }
            }
        } finally {
            logger.v { "Closing /dev/urandom file handle" }
            fclose(file)
        }
    }
}

@AllowInsecureFallback
actual fun createSecureRandom(fallbackPolicy: FallbackPolicy): SecureRandomResult<SecureRandom> {
    // Android Native ARM64 uses getrandom() → /dev/urandom and has secure fallbacks only
    // fallbackPolicy parameter is ignored as Android Native provides secure random generation only
    return createSecureRandom()
}

actual fun createSecureRandom(): SecureRandomResult<SecureRandom> {
    val logger = Logger.withTag("SecureRandom")
    logger.i { "Creating Android Native ARM64 SecureRandom..." }

    return try {
        val result = AndroidNativeArm64SecureRandomAdapter.create()
        when (result) {
            is SecureRandomResult.Success -> {
                logger.i { "✅ Android Native ARM64 SecureRandom created successfully" }
                SecureRandomResult.success(result.value as SecureRandom)
            }
            is SecureRandomResult.Failure -> {
                logger.e { "❌ Failed to create Android Native ARM64 SecureRandom: ${result.exception.message}" }
                SecureRandomResult.failure(result.exception)
            }
        }
    } catch (e: Exception) {
        logger.e(e) { "❌ Exception during Android Native ARM64 SecureRandom creation" }
        SecureRandomResult.failure(
            SecureRandomInitializationException("Android Native ARM64 SecureRandom creation failed", e)
        )
    }
}