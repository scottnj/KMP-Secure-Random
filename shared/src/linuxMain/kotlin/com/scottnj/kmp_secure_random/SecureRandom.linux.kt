package com.scottnj.kmp_secure_random

/**
 * Linux implementation of SecureRandom.
 * TODO: Implement using Linux secure random APIs (/dev/urandom, getrandom syscall).
 */
internal class LinuxSecureRandom : SecureRandom {

    override fun nextBytes(bytes: ByteArray) {
        TODO("Linux SecureRandom implementation not yet implemented")
    }

    override fun nextInt(): Int {
        TODO("Linux SecureRandom implementation not yet implemented")
    }

    override fun nextInt(bound: Int): Int {
        TODO("Linux SecureRandom implementation not yet implemented")
    }

    override fun nextLong(): Long {
        TODO("Linux SecureRandom implementation not yet implemented")
    }

    override fun nextBoolean(): Boolean {
        TODO("Linux SecureRandom implementation not yet implemented")
    }

    override fun nextDouble(): Double {
        TODO("Linux SecureRandom implementation not yet implemented")
    }
}

actual fun createSecureRandom(): SecureRandom = LinuxSecureRandom()