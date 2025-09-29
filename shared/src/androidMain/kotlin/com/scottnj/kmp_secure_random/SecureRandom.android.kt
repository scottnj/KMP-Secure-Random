package com.scottnj.kmp_secure_random

@AllowInsecureFallback
actual fun createSecureRandom(fallbackPolicy: FallbackPolicy): SecureRandomResult<SecureRandom> {
    // Android platform has secure fallbacks only - all provider algorithms are cryptographically secure
    // fallbackPolicy parameter is ignored as there are no insecure fallbacks to allow
    return AndroidSecureRandomAdapter.create().map { it as SecureRandom }
}

actual fun createSecureRandom(): SecureRandomResult<SecureRandom> {
    return AndroidSecureRandomAdapter.create().map { it as SecureRandom }
}