package com.scottnj.kmp_secure_random

/**
 * JavaScript implementation of SecureRandom.
 * TODO: Implement using Web Crypto API's crypto.getRandomValues().
 */
internal class JsSecureRandom : SecureRandom {

    override fun nextBytes(bytes: ByteArray) {
        TODO("JavaScript SecureRandom implementation not yet implemented")
    }

    override fun nextInt(): Int {
        TODO("JavaScript SecureRandom implementation not yet implemented")
    }

    override fun nextInt(bound: Int): Int {
        TODO("JavaScript SecureRandom implementation not yet implemented")
    }

    override fun nextLong(): Long {
        TODO("JavaScript SecureRandom implementation not yet implemented")
    }

    override fun nextBoolean(): Boolean {
        TODO("JavaScript SecureRandom implementation not yet implemented")
    }

    override fun nextDouble(): Double {
        TODO("JavaScript SecureRandom implementation not yet implemented")
    }
}

actual fun createSecureRandom(): SecureRandom = JsSecureRandom()