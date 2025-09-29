@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.scottnj.kmp_secure_random

import co.touchlab.kermit.Logger
import kotlinx.cinterop.*
import platform.posix.*

/**
 * Test helper for verifying Linux-specific implementation details.
 * This class provides access to internal implementation details for testing purposes.
 */
internal class LinuxTestHelper private constructor() {

    private val logger = Logger.withTag("LinuxTestHelper")

    companion object {
        // Linux x86_64 getrandom() syscall number
        private const val SYS_GETRANDOM = 318L
        private const val GRND_NONBLOCK = 0x0001L

        fun create(): LinuxTestHelper {
            return LinuxTestHelper()
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

                // Check long size
                info.add("long=${sizeOf<LongVar>()} bytes")

                // Linux x86_64 characteristics
                if (sizeOf<COpaquePointerVar>() == 8L && sizeOf<size_t>() == 8L) {
                    info.add("arch=Linux/x86_64")
                } else {
                    info.add("arch=Linux/unknown")
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

            // Linux x86_64 getrandom syscall should be 318
            if (SYS_GETRANDOM == 318L) {
                logger.d { "✅ Correct Linux syscall number: $SYS_GETRANDOM" }
                ConstantVerificationResult.Correct(constants)
            } else {
                logger.w { "❌ Incorrect Linux syscall number: $SYS_GETRANDOM (expected 318)" }
                ConstantVerificationResult.Incorrect(constants, "Expected SYS_GETRANDOM=318, got $SYS_GETRANDOM")
            }
        } catch (e: Exception) {
            logger.w(e) { "Exception during constant verification" }
            ConstantVerificationResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Tests kernel version compatibility.
     */
    fun testKernelVersion(): KernelVersionResult {
        return try {
            // Try to read kernel version from /proc/version
            val file = fopen("/proc/version", "r")
            if (file == null) {
                return KernelVersionResult.Unavailable("Cannot read /proc/version")
            }

            try {
                memScoped {
                    val buffer = allocArray<ByteVar>(256)
                    val readResult = fgets(buffer, 256, file)

                    if (readResult != null) {
                        val versionStr = buffer.toKString()
                        logger.d { "Kernel version: $versionStr" }

                        // Check if it mentions getrandom support (Linux 3.17+)
                        val hasGetrandomSupport = versionStr.contains("3.1[789]") ||
                                                versionStr.contains("3.[2-9][0-9]") ||
                                                versionStr.contains("[4-9].")

                        KernelVersionResult.Available(versionStr, hasGetrandomSupport)
                    } else {
                        KernelVersionResult.Unavailable("Failed to read kernel version")
                    }
                }
            } finally {
                fclose(file)
            }
        } catch (e: Exception) {
            logger.w(e) { "Exception reading kernel version" }
            KernelVersionResult.Error(e.message ?: "Unknown error")
        }
    }
}

/**
 * Result of kernel version testing.
 */
sealed class KernelVersionResult {
    data class Available(val version: String, val supportsGetrandom: Boolean) : KernelVersionResult()
    data class Unavailable(val reason: String) : KernelVersionResult()
    data class Error(val message: String) : KernelVersionResult()
}