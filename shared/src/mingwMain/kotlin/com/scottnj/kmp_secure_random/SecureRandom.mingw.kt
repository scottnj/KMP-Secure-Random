package com.scottnj.kmp_secure_random

/**
 * Windows (MinGW) implementation of SecureRandom.
 * TODO: Implement using Windows secure random APIs (CryptGenRandom, BCryptGenRandom).
 */
internal class MingwSecureRandom : SecureRandom {

    override fun nextBytes(bytes: ByteArray) {
        TODO("Windows SecureRandom implementation not yet implemented")
    }

    override fun nextInt(): Int {
        TODO("Windows SecureRandom implementation not yet implemented")
    }

    override fun nextInt(bound: Int): Int {
        TODO("Windows SecureRandom implementation not yet implemented")
    }

    override fun nextLong(): Long {
        TODO("Windows SecureRandom implementation not yet implemented")
    }

    override fun nextBoolean(): Boolean {
        TODO("Windows SecureRandom implementation not yet implemented")
    }

    override fun nextDouble(): Double {
        TODO("Windows SecureRandom implementation not yet implemented")
    }
}

actual fun createSecureRandom(): SecureRandom = MingwSecureRandom()