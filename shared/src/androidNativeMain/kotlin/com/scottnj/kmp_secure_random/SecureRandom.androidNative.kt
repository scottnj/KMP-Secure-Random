package com.scottnj.kmp_secure_random

/**
 * Android Native implementation of SecureRandom.
 * TODO: Implement using Android Native secure random APIs.
 */
internal class AndroidNativeSecureRandom : SecureRandom {

    override fun nextBytes(bytes: ByteArray) {
        TODO("Android Native SecureRandom implementation not yet implemented")
    }

    override fun nextInt(): Int {
        TODO("Android Native SecureRandom implementation not yet implemented")
    }

    override fun nextInt(bound: Int): Int {
        TODO("Android Native SecureRandom implementation not yet implemented")
    }

    override fun nextLong(): Long {
        TODO("Android Native SecureRandom implementation not yet implemented")
    }

    override fun nextBoolean(): Boolean {
        TODO("Android Native SecureRandom implementation not yet implemented")
    }

    override fun nextDouble(): Double {
        TODO("Android Native SecureRandom implementation not yet implemented")
    }
}

actual fun createSecureRandom(): SecureRandom = AndroidNativeSecureRandom()