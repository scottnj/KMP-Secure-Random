package com.scottnj.kmp_secure_random

/**
 * JVM implementation of SecureRandom.
 * TODO: Implement using java.security.SecureRandom.
 */
internal class JvmSecureRandom : SecureRandom {

    override fun nextBytes(bytes: ByteArray) {
        TODO("JVM SecureRandom implementation not yet implemented")
    }

    override fun nextInt(): Int {
        TODO("JVM SecureRandom implementation not yet implemented")
    }

    override fun nextInt(bound: Int): Int {
        TODO("JVM SecureRandom implementation not yet implemented")
    }

    override fun nextLong(): Long {
        TODO("JVM SecureRandom implementation not yet implemented")
    }

    override fun nextBoolean(): Boolean {
        TODO("JVM SecureRandom implementation not yet implemented")
    }

    override fun nextDouble(): Double {
        TODO("JVM SecureRandom implementation not yet implemented")
    }
}

actual fun createSecureRandom(): SecureRandom = JvmSecureRandom()