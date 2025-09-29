package com.scottnj.kmp_secure_random

@AllowInsecureFallback
actual fun createSecureRandom(fallbackPolicy: FallbackPolicy): SecureRandomResult<SecureRandom> {
    // macOS platform uses SecRandomCopyBytes API and has secure fallbacks only
    // fallbackPolicy parameter is ignored as macOS provides secure random generation only
    return createSecureRandom()
}

actual fun createSecureRandom(): SecureRandomResult<SecureRandom> {
    return AppleSecureRandomAdapter.create("macOS").map { it as SecureRandom }
}