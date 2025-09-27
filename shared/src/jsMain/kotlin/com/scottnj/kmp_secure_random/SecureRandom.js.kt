package com.scottnj.kmp_secure_random

actual fun createSecureRandom(): SecureRandomResult<SecureRandom> {
    return JsSecureRandomAdapter.create().map { it as SecureRandom }
}