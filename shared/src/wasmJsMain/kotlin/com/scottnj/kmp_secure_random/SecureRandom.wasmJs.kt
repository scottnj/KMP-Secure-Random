package com.scottnj.kmp_secure_random

@AllowInsecureFallback
actual fun createSecureRandom(fallbackPolicy: FallbackPolicy): SecureRandomResult<SecureRandom> {
    return WasmJsSecureRandomAdapter.create(fallbackPolicy).map { it as SecureRandom }
}

actual fun createSecureRandom(): SecureRandomResult<SecureRandom> {
    return WasmJsSecureRandomAdapter.create().map { it as SecureRandom }
}