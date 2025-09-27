package com.scottnj.kmp_secure_random

actual fun createSecureRandom(): SecureRandomResult<SecureRandom> {
    return WasmJsSecureRandomAdapter.create().map { it as SecureRandom }
}