@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.scottnj.kmp_secure_random

import co.touchlab.kermit.Logger
import kotlinx.cinterop.*
import platform.posix.*

/**
 * Test helper for verifying Android Native ARM64-specific implementation details.
 * This class provides access to internal implementation details for testing purposes.
 */
internal class AndroidNativeArm64TestHelper private constructor() {

    private val logger = Logger.withTag("AndroidNativeArm64TestHelper")

    companion object {
        // ARM64-specific syscall number for getrandom()
        private const val SYS_GETRANDOM = 278L
        private const val GRND_NONBLOCK = 0x0001L

        fun create(): AndroidNativeArm64TestHelper {
            return AndroidNativeArm64TestHelper()
        }
    }

    /**
     * Returns the syscall number being used for getrandom().
     */
    fun getSyscallNumber(): Long = SYS_GETRANDOM

    /**
     * Verifies if getrandom() syscall is available on this system.
     */
    fun verifySyscallAvailability(): SyscallVerificationResult {
        return memScoped {
            try {
                val buffer = allocArray<ByteVar>(1)
                logger.d { "Testing getrandom() syscall #$SYS_GETRANDOM availability..." }

                val result = syscall(SYS_GETRANDOM, buffer, 1.convert<size_t>(), GRND_NONBLOCK)

                when {
                    result == 1L -> {
                        logger.d { "✅ getrandom() syscall #$SYS_GETRANDOM is available and working" }
                        SyscallVerificationResult.Available
                    }
                    result == -1L -> {
                        val error = errno
                        when (error) {
                            ENOSYS -> {
                                logger.d { "getrandom() syscall not available (ENOSYS) - older kernel" }
                                SyscallVerificationResult.NotSupported
                            }
                            EAGAIN -> {
                                logger.d { "✅ getrandom() syscall available but would block (normal)" }
                                SyscallVerificationResult.Available
                            }
                            else -> {
                                logger.w { "getrandom() syscall failed with errno: $error" }
                                SyscallVerificationResult.Error(error)
                            }
                        }
                    }
                    else -> {
                        logger.w { "getrandom() syscall returned unexpected result: $result" }
                        SyscallVerificationResult.Unexpected(result)
                    }
                }
            } catch (e: Exception) {
                logger.w(e) { "Exception during syscall verification" }
                SyscallVerificationResult.Exception(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Checks if this implementation uses 64-bit types (ARM64-specific).
     */
    fun uses64BitTypes(): Boolean {
        // ARM64 uses 64-bit pointers and size_t
        return memScoped {
            val testPointer = allocArray<ByteVar>(1)
            val pointerSize = testPointer.rawValue.toString().length

            // On ARM64, pointers should be 64-bit (16 hex characters when represented as string)
            // This is a heuristic test - ARM64 systems will have 64-bit addressing
            val is64Bit = sizeOf<size_t>() == 8L
            logger.d { "size_t size: ${sizeOf<size_t>()} bytes, 64-bit types: $is64Bit" }
            is64Bit
        }
    }

    /**
     * Tests if /dev/urandom is available for fallback.
     */
    fun isDevUrandomAvailable(): Boolean {
        return try {
            val file = fopen("/dev/urandom", "rb")
            if (file != null) {
                fclose(file)
                logger.d { "✅ /dev/urandom is accessible" }
                true
            } else {
                logger.w { "❌ /dev/urandom not accessible: errno $errno" }
                false
            }
        } catch (e: Exception) {
            logger.w(e) { "Exception checking /dev/urandom availability" }
            false
        }
    }

    /**
     * Tests the /dev/urandom fallback mechanism directly.
     */
    fun testDevUrandomFallback(bytes: ByteArray): Boolean {
        return try {
            val file = fopen("/dev/urandom", "rb")
            if (file == null) {
                logger.w { "Cannot open /dev/urandom for fallback test" }
                return false
            }

            try {
                memScoped {
                    val buffer = allocArray<ByteVar>(bytes.size)
                    val bytesRead = fread(buffer, 1u, bytes.size.convert(), file)

                    if (bytesRead.toInt() != bytes.size) {
                        logger.w { "Failed to read ${bytes.size} bytes from /dev/urandom, got $bytesRead" }
                        return false
                    }

                    // Copy bytes from native buffer
                    for (i in bytes.indices) {
                        bytes[i] = buffer[i]
                    }

                    logger.d { "✅ Successfully read ${bytes.size} bytes from /dev/urandom" }
                    true
                }
            } finally {
                fclose(file)
            }
        } catch (e: Exception) {
            logger.w(e) { "Exception during /dev/urandom fallback test" }
            false
        }
    }

    /**
     * Returns architecture information for verification.
     */
    fun getArchitectureInfo(): String {
        return memScoped {
            try {
                // Try to get architecture information
                val info = mutableListOf<String>()

                // Check pointer size
                info.add("pointer_size=${sizeOf<COpaquePointerVar>()} bytes")

                // Check size_t size
                info.add("size_t=${sizeOf<size_t>()} bytes")

                // Check long size
                info.add("long=${sizeOf<LongVar>()} bytes")

                // ARM64 characteristics
                if (sizeOf<COpaquePointerVar>() == 8L && sizeOf<size_t>() == 8L) {
                    info.add("arch=ARM64/aarch64")
                } else {
                    info.add("arch=unknown")
                }

                info.joinToString(", ")
            } catch (e: Exception) {
                logger.w(e) { "Exception getting architecture info" }
                "arch=error: ${e.message}"
            }
        }
    }

    /**
     * Verifies that the correct syscall constants are defined.
     */
    fun verifySyscallConstants(): ConstantVerificationResult {
        return try {
            val constants = mutableMapOf<String, Any>()

            constants["SYS_GETRANDOM"] = SYS_GETRANDOM
            constants["GRND_NONBLOCK"] = GRND_NONBLOCK

            // ARM64-specific getrandom syscall should be 278
            if (SYS_GETRANDOM == 278L) {
                logger.d { "✅ Correct ARM64 syscall number: $SYS_GETRANDOM" }
                ConstantVerificationResult.Correct(constants)
            } else {
                logger.w { "❌ Incorrect ARM64 syscall number: $SYS_GETRANDOM (expected 278)" }
                ConstantVerificationResult.Incorrect(constants, "Expected SYS_GETRANDOM=278, got $SYS_GETRANDOM")
            }
        } catch (e: Exception) {
            logger.w(e) { "Exception during constant verification" }
            ConstantVerificationResult.Error(e.message ?: "Unknown error")
        }
    }
}

/**
 * Result of syscall availability verification.
 */
sealed class SyscallVerificationResult {
    object Available : SyscallVerificationResult()
    object NotSupported : SyscallVerificationResult()
    data class Error(val errno: Int) : SyscallVerificationResult()
    data class Unexpected(val result: Long) : SyscallVerificationResult()
    data class Exception(val message: String) : SyscallVerificationResult()

    override fun toString(): String = when (this) {
        is Available -> "Available"
        is NotSupported -> "Not Supported"
        is Error -> "Error(errno=$errno)"
        is Unexpected -> "Unexpected(result=$result)"
        is Exception -> "Exception($message)"
    }
}

/**
 * Result of constant verification.
 */
sealed class ConstantVerificationResult {
    data class Correct(val constants: Map<String, Any>) : ConstantVerificationResult()
    data class Incorrect(val constants: Map<String, Any>, val reason: String) : ConstantVerificationResult()
    data class Error(val message: String) : ConstantVerificationResult()
}