package com.scottnj.kmp_secure_random

actual fun createSecureRandom(): SecureRandomResult<SecureRandom> {
    return AppleSecureRandomAdapter.create("macOS").map { it as SecureRandom }
}