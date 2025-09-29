package com.scottnj.kmp_secure_random

import co.touchlab.kermit.Logger
import kotlinx.cinterop.*
import platform.windows.*

/**
 * Test helper for verifying Windows-specific implementation details.
 * This class provides access to internal Windows API usage for testing purposes.
 */
internal class WindowsTestHelper private constructor() {

    private val logger = Logger.withTag("WindowsTestHelper")

    companion object {
        fun create(): WindowsTestHelper {
            return WindowsTestHelper()
        }
    }

    /**
     * Verifies Windows CryptAPI availability and functionality.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun verifyCryptAPIAvailability(): WindowsCryptResult {
        return memScoped {
            try {
                logger.d { "Testing Windows CryptAPI availability..." }

                val hProv = alloc<ULongVarOf<ULong>>()
                val result = CryptAcquireContextW(
                    hProv.ptr,
                    null,
                    null,
                    1u, // PROV_RSA_FULL
                    0xF0000000u // CRYPT_VERIFYCONTEXT
                )

                if (result != 0) {
                    logger.d { "✅ CryptAcquireContextW successful" }

                    // Test CryptGenRandom
                    val testBuffer = allocArray<UByteVar>(4)
                    val genResult = CryptGenRandom(hProv.value, 4u, testBuffer)

                    // Clean up
                    CryptReleaseContext(hProv.value, 0u)

                    if (genResult != 0) {
                        logger.d { "✅ CryptGenRandom successful" }
                        WindowsCryptResult.Available
                    } else {
                        val error = GetLastError()
                        logger.w { "CryptGenRandom failed with error: $error" }
                        WindowsCryptResult.GenRandomFailed(error.toInt())
                    }
                } else {
                    val error = GetLastError()
                    logger.w { "CryptAcquireContextW failed with error: $error" }
                    WindowsCryptResult.ContextFailed(error.toInt())
                }
            } catch (e: Exception) {
                logger.w(e) { "Exception during Windows CryptAPI verification" }
                WindowsCryptResult.Exception(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Tests specific Windows crypto provider types.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun testCryptoProviders(): Map<String, Boolean> {
        val providers = mutableMapOf<String, Boolean>()

        memScoped {
            val hProv = alloc<ULongVarOf<ULong>>()

            // Test PROV_RSA_FULL (Type 1)
            try {
                val result1 = CryptAcquireContextW(
                    hProv.ptr, null, null, 1u, 0xF0000000u
                )
                providers["PROV_RSA_FULL"] = result1 != 0
                if (result1 != 0) {
                    CryptReleaseContext(hProv.value, 0u)
                }
            } catch (e: Exception) {
                providers["PROV_RSA_FULL"] = false
            }

            // Test PROV_RSA_AES (Type 24) - newer provider
            try {
                val result24 = CryptAcquireContextW(
                    hProv.ptr, null, null, 24u, 0xF0000000u
                )
                providers["PROV_RSA_AES"] = result24 != 0
                if (result24 != 0) {
                    CryptReleaseContext(hProv.value, 0u)
                }
            } catch (e: Exception) {
                providers["PROV_RSA_AES"] = false
            }
        }

        return providers
    }

    /**
     * Verifies Windows version compatibility using actual Windows API.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun getWindowsVersion(): WindowsVersionInfo {
        return memScoped {
            try {
                logger.d { "Getting Windows version information..." }

                // Use GetVersionExW to get actual Windows version
                val versionInfo = alloc<OSVERSIONINFOW>()
                versionInfo.dwOSVersionInfoSize = sizeOf<OSVERSIONINFOW>().toUInt()

                val result = GetVersionExW(versionInfo.ptr)

                if (result != 0) {
                    val majorVersion = versionInfo.dwMajorVersion.toInt()
                    val minorVersion = versionInfo.dwMinorVersion.toInt()
                    val buildNumber = versionInfo.dwBuildNumber.toInt()

                    logger.d { "✅ Windows version detected: $majorVersion.$minorVersion.$buildNumber" }

                    WindowsVersionInfo(
                        majorVersion = majorVersion,
                        minorVersion = minorVersion,
                        buildNumber = buildNumber,
                        supportsCryptAPI = true, // CryptAPI available since Windows 2000 (5.0+)
                        supportsBCrypt = majorVersion >= 6 // BCrypt available since Vista (6.0+)
                    )
                } else {
                    val error = GetLastError()
                    logger.w { "GetVersionExW failed with error: $error" }

                    // Fallback: assume modern Windows with CryptAPI support
                    WindowsVersionInfo(
                        majorVersion = 10, // Default to Windows 10+
                        minorVersion = 0,
                        buildNumber = 0,
                        supportsCryptAPI = true,
                        supportsBCrypt = true
                    )
                }
            } catch (e: Exception) {
                logger.w(e) { "Exception during Windows version detection" }

                // Fallback: assume modern Windows with full crypto support
                WindowsVersionInfo(
                    majorVersion = 10,
                    minorVersion = 0,
                    buildNumber = 0,
                    supportsCryptAPI = true,
                    supportsBCrypt = true
                )
            }
        }
    }

    /**
     * Tests CryptGenRandom with different buffer sizes.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun testCryptGenRandomSizes(): Map<Int, Boolean> {
        val results = mutableMapOf<Int, Boolean>()
        val testSizes = listOf(1, 4, 16, 64, 256, 1024)

        memScoped {
            val hProv = alloc<ULongVarOf<ULong>>()
            val contextResult = CryptAcquireContextW(
                hProv.ptr, null, null, 1u, 0xF0000000u
            )

            if (contextResult != 0) {
                try {
                    for (size in testSizes) {
                        val buffer = allocArray<UByteVar>(size)
                        val result = CryptGenRandom(hProv.value, size.toUInt(), buffer)
                        results[size] = result != 0

                        if (result == 0) {
                            val error = GetLastError()
                            logger.w { "CryptGenRandom failed for size $size with error: $error" }
                        }
                    }
                } finally {
                    CryptReleaseContext(hProv.value, 0u)
                }
            } else {
                val error = GetLastError()
                logger.w { "Failed to acquire crypto context: $error" }
                testSizes.forEach { results[it] = false }
            }
        }

        return results
    }

    /**
     * Verifies entropy quality of generated random data.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun testEntropyQuality(): EntropyTestResult {
        return memScoped {
            try {
                val hProv = alloc<ULongVarOf<ULong>>()
                val contextResult = CryptAcquireContextW(
                    hProv.ptr, null, null, 1u, 0xF0000000u
                )

                if (contextResult == 0) {
                    return EntropyTestResult.ContextFailed(GetLastError().toInt())
                }

                try {
                    // Generate test data
                    val sampleSize = 1000
                    val buffer = allocArray<UByteVar>(sampleSize)
                    val result = CryptGenRandom(hProv.value, sampleSize.toUInt(), buffer)

                    if (result == 0) {
                        return EntropyTestResult.GenerationFailed(GetLastError().toInt())
                    }

                    // Convert to Kotlin ByteArray for analysis
                    val bytes = ByteArray(sampleSize)
                    for (i in 0 until sampleSize) {
                        bytes[i] = buffer[i]
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
                } finally {
                    CryptReleaseContext(hProv.value, 0u)
                }
            } catch (e: Exception) {
                logger.w(e) { "Exception during entropy quality test" }
                EntropyTestResult.Exception(e.message ?: "Unknown error")
            }
        }
    }
}

/**
 * Result of Windows CryptAPI verification.
 */
sealed class WindowsCryptResult {
    object Available : WindowsCryptResult()
    data class ContextFailed(val errorCode: Int) : WindowsCryptResult()
    data class GenRandomFailed(val errorCode: Int) : WindowsCryptResult()
    data class Exception(val message: String) : WindowsCryptResult()

    override fun toString(): String = when (this) {
        is Available -> "Available"
        is ContextFailed -> "Context Failed (error=$errorCode)"
        is GenRandomFailed -> "GenRandom Failed (error=$errorCode)"
        is Exception -> "Exception: $message"
    }
}

/**
 * Windows version information.
 */
data class WindowsVersionInfo(
    val majorVersion: Int,
    val minorVersion: Int,
    val buildNumber: Int,
    val supportsCryptAPI: Boolean,
    val supportsBCrypt: Boolean
)

/**
 * Result of entropy quality testing.
 */
sealed class EntropyTestResult {
    data class Success(
        val maxDeviation: Double,
        val avgDeviation: Double,
        val uniqueValues: Int,
        val allZeros: Boolean
    ) : EntropyTestResult()

    data class ContextFailed(val errorCode: Int) : EntropyTestResult()
    data class GenerationFailed(val errorCode: Int) : EntropyTestResult()
    data class Exception(val message: String) : EntropyTestResult()
}