@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.scottnj.kmp_secure_random

import co.touchlab.kermit.Logger
import kotlinx.cinterop.*
import platform.Security.*
import platform.Foundation.*

/**
 * Test helper for verifying Apple platform-specific implementation details.
 * This class provides access to internal SecRandomCopyBytes API usage for testing purposes.
 */
internal class AppleTestHelper private constructor(
    private val platformName: String
) {

    private val logger = Logger.withTag("AppleTestHelper")

    companion object {
        fun create(platformName: String = "iOS"): AppleTestHelper {
            return AppleTestHelper(platformName)
        }
    }

    /**
     * Verifies if SecRandomCopyBytes API is available on this system.
     */
    fun verifySecRandomCopyBytesAvailability(): AppleSecRandomResult {
        return memScoped {
            try {
                logger.d { "Testing SecRandomCopyBytes availability..." }

                val buffer = allocArray<UByteVar>(1)
                val status = SecRandomCopyBytes(kSecRandomDefault, 1.convert(), buffer)

                when (status) {
                    errSecSuccess -> {
                        logger.d { "✅ SecRandomCopyBytes is available and working" }
                        AppleSecRandomResult.Available
                    }
                    else -> {
                        logger.w { "SecRandomCopyBytes failed with status: $status" }
                        AppleSecRandomResult.Failed(status)
                    }
                }
            } catch (e: Exception) {
                logger.w(e) { "Exception during SecRandomCopyBytes verification" }
                AppleSecRandomResult.Exception(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Tests SecRandomCopyBytes with different buffer sizes.
     */
    fun testSecRandomCopyBytesSizes(): Map<Int, Boolean> {
        val results = mutableMapOf<Int, Boolean>()
        val testSizes = listOf(1, 4, 16, 64, 256, 1024)

        for (size in testSizes) {
            try {
                memScoped {
                    val buffer = allocArray<UByteVar>(size)
                    val status = SecRandomCopyBytes(kSecRandomDefault, size.convert(), buffer)
                    results[size] = status == errSecSuccess

                    if (status != errSecSuccess) {
                        logger.w { "SecRandomCopyBytes failed for size $size with status: $status" }
                    }
                }
            } catch (e: Exception) {
                logger.w(e) { "Exception testing SecRandomCopyBytes for size $size" }
                results[size] = false
            }
        }

        return results
    }

    /**
     * Tests the SecRandomCopyBytes API directly and analyzes the output.
     */
    fun testSecRandomCopyBytesOutput(bytes: ByteArray): Boolean {
        return try {
            memScoped {
                val buffer = allocArray<UByteVar>(bytes.size)
                val status = SecRandomCopyBytes(kSecRandomDefault, bytes.size.convert(), buffer)

                if (status != errSecSuccess) {
                    logger.w { "SecRandomCopyBytes failed with status: $status" }
                    return false
                }

                // Copy bytes from native buffer
                for (i in bytes.indices) {
                    bytes[i] = buffer[i].toByte()
                }

                logger.d { "✅ Successfully generated ${bytes.size} bytes using SecRandomCopyBytes" }
                true
            }
        } catch (e: Exception) {
            logger.w(e) { "Exception during SecRandomCopyBytes output test" }
            false
        }
    }

    /**
     * Returns Apple platform information for verification.
     */
    fun getApplePlatformInfo(): String {
        return try {
            val info = mutableListOf<String>()

            // Platform name
            info.add("platform=$platformName")

            // Check pointer size
            info.add("pointer_size=${sizeOf<COpaquePointerVar>()} bytes")

            // Check if we're on a 64-bit Apple platform
            val is64Bit = sizeOf<COpaquePointerVar>() == 8L
            info.add("64bit=$is64Bit")

            // Security framework availability
            info.add("security_framework=available")

            info.joinToString(", ")
        } catch (e: Exception) {
            logger.w(e) { "Exception getting Apple platform info" }
            "platform=error: ${e.message}"
        }
    }

    /**
     * Verifies Security framework constants and APIs.
     */
    fun verifySecurityFrameworkConstants(): SecurityFrameworkResult {
        return try {
            val constants = mutableMapOf<String, Any>()

            // Check key constants
            constants["kSecRandomDefault"] = kSecRandomDefault.toString()
            constants["errSecSuccess"] = errSecSuccess

            // Verify SecRandomCopyBytes function is available
            val testAvailable = verifySecRandomCopyBytesAvailability()

            when (testAvailable) {
                is AppleSecRandomResult.Available -> {
                    logger.d { "✅ Security framework constants verified" }
                    SecurityFrameworkResult.Available(constants)
                }
                is AppleSecRandomResult.Failed -> {
                    logger.w { "❌ SecRandomCopyBytes not working, status: ${testAvailable.status}" }
                    SecurityFrameworkResult.ApiNotWorking(constants, testAvailable.status)
                }
                is AppleSecRandomResult.Exception -> {
                    SecurityFrameworkResult.Error(testAvailable.message)
                }
            }
        } catch (e: Exception) {
            logger.w(e) { "Exception during Security framework verification" }
            SecurityFrameworkResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Tests entropy quality of SecRandomCopyBytes output.
     */
    fun testEntropyQuality(): EntropyTestResult {
        return try {
            // Generate test data
            val sampleSize = 1000
            val bytes = ByteArray(sampleSize)
            val success = testSecRandomCopyBytesOutput(bytes)

            if (!success) {
                return EntropyTestResult.GenerationFailed(0)
            }

            // Basic entropy analysis
            val frequencies = IntArray(256)
            bytes.forEach { byte ->
                val index = (byte.toInt() and 0xFF)
                frequencies[index]++
            }

            // Calculate basic statistics
            val expectedFreq = sampleSize.toDouble() / 256
            val deviations = frequencies.map { freq ->
                kotlin.math.abs(freq - expectedFreq) / expectedFreq
            }

            val maxDeviation = deviations.maxOrNull() ?: 0.0
            val avgDeviation = deviations.average()

            // Check for all zeros (very bad)
            val allZeros = bytes.all { it == 0.toByte() }
            val uniqueValues = bytes.toSet().size

            EntropyTestResult.Success(
                maxDeviation = maxDeviation,
                avgDeviation = avgDeviation,
                uniqueValues = uniqueValues,
                allZeros = allZeros
            )
        } catch (e: Exception) {
            logger.w(e) { "Exception during entropy quality test" }
            EntropyTestResult.Exception(e.message ?: "Unknown error")
        }
    }
}

/**
 * Result of Apple SecRandomCopyBytes verification.
 */
sealed class AppleSecRandomResult {
    object Available : AppleSecRandomResult()
    data class Failed(val status: Int) : AppleSecRandomResult()
    data class Exception(val message: String) : AppleSecRandomResult()

    override fun toString(): String = when (this) {
        is Available -> "Available"
        is Failed -> "Failed (status=$status)"
        is Exception -> "Exception: $message"
    }
}

/**
 * Result of Security framework verification.
 */
sealed class SecurityFrameworkResult {
    data class Available(val constants: Map<String, Any>) : SecurityFrameworkResult()
    data class ApiNotWorking(val constants: Map<String, Any>, val status: Int) : SecurityFrameworkResult()
    data class Error(val message: String) : SecurityFrameworkResult()
}