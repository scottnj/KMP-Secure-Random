package com.scottnj.kmp_secure_random

@AllowInsecureFallback
actual fun createSecureRandom(fallbackPolicy: FallbackPolicy): SecureRandomResult<SecureRandom> {
    // JavaScript platform has no insecure fallbacks - all methods are secure or fail
    // fallbackPolicy parameter is ignored as there are no insecure fallbacks to allow
    return JsSecureRandomAdapter.create().map { it as SecureRandom }
}

actual fun createSecureRandom(): SecureRandomResult<SecureRandom> {
    return JsSecureRandomAdapter.create().map { it as SecureRandom }
}