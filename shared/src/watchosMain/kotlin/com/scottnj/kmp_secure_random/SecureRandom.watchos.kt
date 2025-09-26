package com.scottnj.kmp_secure_random

/**
 * watchOS implementation of SecureRandom.
 * TODO: Implement using watchOS secure random APIs.
 */
internal class WatchosSecureRandom : SecureRandom {

    override fun nextBytes(bytes: ByteArray) {
        TODO("watchOS SecureRandom implementation not yet implemented")
    }

    override fun nextInt(): Int {
        TODO("watchOS SecureRandom implementation not yet implemented")
    }

    override fun nextInt(bound: Int): Int {
        TODO("watchOS SecureRandom implementation not yet implemented")
    }

    override fun nextLong(): Long {
        TODO("watchOS SecureRandom implementation not yet implemented")
    }

    override fun nextBoolean(): Boolean {
        TODO("watchOS SecureRandom implementation not yet implemented")
    }

    override fun nextDouble(): Double {
        TODO("watchOS SecureRandom implementation not yet implemented")
    }
}

actual fun createSecureRandom(): SecureRandom = WatchosSecureRandom()