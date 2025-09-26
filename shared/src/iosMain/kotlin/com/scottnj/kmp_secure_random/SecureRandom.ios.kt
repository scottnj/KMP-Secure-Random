package com.scottnj.kmp_secure_random

/**
 * iOS implementation of SecureRandom.
 * TODO: Implement using SecRandomCopyBytes from Security framework.
 */
internal class IosSecureRandom : SecureRandom {

    override fun nextBytes(bytes: ByteArray) {
        TODO("iOS SecureRandom implementation not yet implemented")
    }

    override fun nextInt(): Int {
        TODO("iOS SecureRandom implementation not yet implemented")
    }

    override fun nextInt(bound: Int): Int {
        TODO("iOS SecureRandom implementation not yet implemented")
    }

    override fun nextLong(): Long {
        TODO("iOS SecureRandom implementation not yet implemented")
    }

    override fun nextBoolean(): Boolean {
        TODO("iOS SecureRandom implementation not yet implemented")
    }

    override fun nextDouble(): Double {
        TODO("iOS SecureRandom implementation not yet implemented")
    }
}

actual fun createSecureRandom(): SecureRandom = IosSecureRandom()