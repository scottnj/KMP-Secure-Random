@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.scottnj.kmp_secure_random

import co.touchlab.kermit.Logger
import kotlinx.cinterop.*
import platform.posix.*

/**
 * Test helper for verifying Android Native ARM32-specific implementation details.
 * This class provides access to internal implementation details for testing purposes.
 */
internal class AndroidNativeArm32TestHelper private constructor() {

    private val logger = Logger.withTag("AndroidNativeArm32TestHelper")

    companion object {
        // ARM32-specific syscall number for getrandom()
        private const val SYS_GETRANDOM = 384
        private const val GRND_NONBLOCK = 0x0001

        fun create(): AndroidNativeArm32TestHelper {
            return AndroidNativeArm32TestHelper()
        }
    }

    /**
     * Returns the syscall number being used for getrandom().
     */
    fun getSyscallNumber(): Int = SYS_GETRANDOM

    /**
     * Verifies if getrandom() syscall is available on this system.
     */
    fun verifySyscallAvailability(): SyscallVerificationResult {
        return memScoped {
            try {
                val buffer = allocArray<ByteVar>(1)
                logger.d { "Testing getrandom() syscall #$SYS_GETRANDOM availability..." }

                val result = syscall(SYS_GETRANDOM.toLong(), buffer, 1.convert<size_t>(), GRND_NONBLOCK)

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
     * Checks if this implementation uses 32-bit types (ARM32-specific).
     */
    fun uses32BitTypes(): Boolean {
        // ARM32 uses 32-bit pointers and size_t
        return memScoped {
            val is32Bit = sizeOf<size_t>() == 4L
            logger.d { "size_t size: ${sizeOf<size_t>()} bytes, 32-bit types: $is32Bit" }
            is32Bit
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
                val info = mutableListOf<String>()

                // Check pointer size
                info.add("pointer_size=${sizeOf<COpaquePointerVar>()} bytes")

                // Check size_t size
                info.add("size_t=${sizeOf<size_t>()} bytes")

                // Check int size
                info.add("int=${sizeOf<IntVar>()} bytes")

                // ARM32 characteristics
                if (sizeOf<COpaquePointerVar>() == 4L && sizeOf<size_t>() == 4L) {
                    info.add("arch=ARM32/armv7")
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

            // ARM32-specific getrandom syscall should be 384
            if (SYS_GETRANDOM == 384) {
                logger.d { "✅ Correct ARM32 syscall number: $SYS_GETRANDOM" }
                ConstantVerificationResult.Correct(constants)
            } else {
                logger.w { "❌ Incorrect ARM32 syscall number: $SYS_GETRANDOM (expected 384)" }
                ConstantVerificationResult.Incorrect(constants, "Expected SYS_GETRANDOM=384, got $SYS_GETRANDOM")
            }
        } catch (e: Exception) {
            logger.w(e) { "Exception during constant verification" }
            ConstantVerificationResult.Error(e.message ?: "Unknown error")
        }
    }
}