package com.scottnj.kmp_secure_random

actual fun createSecureRandom(): SecureRandomResult<SecureRandom> {
    return AndroidSecureRandomAdapter.create().map { it as SecureRandom }
}