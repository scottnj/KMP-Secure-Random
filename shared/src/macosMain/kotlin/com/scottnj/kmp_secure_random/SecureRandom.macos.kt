package com.scottnj.kmp_secure_random

/**
 * macOS implementation of SecureRandom.
 * TODO: Implement using macOS secure random APIs.
 */
internal class MacosSecureRandom : SecureRandom {

    override fun nextBytes(bytes: ByteArray) {
        TODO("macOS SecureRandom implementation not yet implemented")
    }

    override fun nextInt(): Int {
        TODO("macOS SecureRandom implementation not yet implemented")
    }

    override fun nextInt(bound: Int): Int {
        TODO("macOS SecureRandom implementation not yet implemented")
    }

    override fun nextLong(): Long {
        TODO("macOS SecureRandom implementation not yet implemented")
    }

    override fun nextBoolean(): Boolean {
        TODO("macOS SecureRandom implementation not yet implemented")
    }

    override fun nextDouble(): Double {
        TODO("macOS SecureRandom implementation not yet implemented")
    }
}

actual fun createSecureRandom(): SecureRandom = MacosSecureRandom()