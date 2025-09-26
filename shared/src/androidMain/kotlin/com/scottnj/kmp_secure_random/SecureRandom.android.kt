package com.scottnj.kmp_secure_random

/**
 * Android implementation of SecureRandom.
 * TODO: Implement using Android's secure random APIs.
 */
internal class AndroidSecureRandom : SecureRandom {

    override fun nextBytes(bytes: ByteArray) {
        TODO("Android SecureRandom implementation not yet implemented")
    }

    override fun nextInt(): Int {
        TODO("Android SecureRandom implementation not yet implemented")
    }

    override fun nextInt(bound: Int): Int {
        TODO("Android SecureRandom implementation not yet implemented")
    }

    override fun nextLong(): Long {
        TODO("Android SecureRandom implementation not yet implemented")
    }

    override fun nextBoolean(): Boolean {
        TODO("Android SecureRandom implementation not yet implemented")
    }

    override fun nextDouble(): Double {
        TODO("Android SecureRandom implementation not yet implemented")
    }
}

actual fun createSecureRandom(): SecureRandom = AndroidSecureRandom()