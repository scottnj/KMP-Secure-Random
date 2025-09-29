package com.scottnj.kmp_secure_random

@AllowInsecureFallback
actual fun createSecureRandom(fallbackPolicy: FallbackPolicy): SecureRandomResult<SecureRandom> {
    // iOS platform uses SecRandomCopyBytes API and has secure fallbacks only
    // fallbackPolicy parameter is ignored as iOS provides secure random generation only
    return createSecureRandom()
}

actual fun createSecureRandom(): SecureRandomResult<SecureRandom> {
    return AppleSecureRandomAdapter.create("iOS").map { it as SecureRandom }
}