package com.scottnj.kmp_secure_random

import co.touchlab.kermit.Logger

@AllowInsecureFallback
actual fun createSecureRandom(fallbackPolicy: FallbackPolicy): SecureRandomResult<SecureRandom> {
    // Windows platform uses CryptGenRandom API and has secure fallbacks only
    // fallbackPolicy parameter is ignored as Windows provides secure random generation only
    return createSecureRandom()
}

/**
 * Windows (MinGW) platform entry point for SecureRandom.
 * Uses WindowsSecureRandom which provides cryptographically secure
 * random number generation via Windows CryptGenRandom API.
 */
actual fun createSecureRandom(): SecureRandomResult<SecureRandom> {
    val logger = Logger.withTag("SecureRandom")
    return try {
        val secureRandom = WindowsSecureRandom()
        logger.i { "Windows SecureRandom created successfully" }
        SecureRandomResult.success(secureRandom)
    } catch (e: Exception) {
        logger.e(e) { "Failed to create Windows SecureRandom" }
        SecureRandomResult.failure(
            SecureRandomInitializationException("Windows SecureRandom initialization failed", e)
        )
    }
}