package com.scottnj.kmp_secure_random

/**
 * tvOS implementation of SecureRandom.
 * TODO: Implement using tvOS secure random APIs.
 */
internal class TvosSecureRandom : SecureRandom {

    override fun nextBytes(bytes: ByteArray) {
        TODO("tvOS SecureRandom implementation not yet implemented")
    }

    override fun nextInt(): Int {
        TODO("tvOS SecureRandom implementation not yet implemented")
    }

    override fun nextInt(bound: Int): Int {
        TODO("tvOS SecureRandom implementation not yet implemented")
    }

    override fun nextLong(): Long {
        TODO("tvOS SecureRandom implementation not yet implemented")
    }

    override fun nextBoolean(): Boolean {
        TODO("tvOS SecureRandom implementation not yet implemented")
    }

    override fun nextDouble(): Double {
        TODO("tvOS SecureRandom implementation not yet implemented")
    }
}

actual fun createSecureRandom(): SecureRandom = TvosSecureRandom()