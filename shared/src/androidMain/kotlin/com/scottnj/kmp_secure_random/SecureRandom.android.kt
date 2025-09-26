package com.scottnj.kmp_secure_random

/**
 * Android implementation of SecureRandom.
 * TODO: Implement using Android's secure random APIs.
 */
internal class AndroidSecureRandom : SecureRandom {

    override fun nextBytes(bytes: ByteArray): SecureRandomUnitResult {
        TODO("Android SecureRandom implementation not yet implemented")
    }

    override fun nextInt(): SecureRandomResult<Int> {
        TODO("Android SecureRandom implementation not yet implemented")
    }

    override fun nextInt(bound: Int): SecureRandomResult<Int> {
        TODO("Android SecureRandom implementation not yet implemented")
    }

    override fun nextInt(min: Int, max: Int): SecureRandomResult<Int> {
        TODO("Android SecureRandom implementation not yet implemented")
    }

    override fun nextLong(): SecureRandomResult<Long> {
        TODO("Android SecureRandom implementation not yet implemented")
    }

    override fun nextLong(bound: Long): SecureRandomResult<Long> {
        TODO("Android SecureRandom implementation not yet implemented")
    }

    override fun nextLong(min: Long, max: Long): SecureRandomResult<Long> {
        TODO("Android SecureRandom implementation not yet implemented")
    }

    override fun nextBoolean(): SecureRandomResult<Boolean> {
        TODO("Android SecureRandom implementation not yet implemented")
    }

    override fun nextDouble(): SecureRandomResult<Double> {
        TODO("Android SecureRandom implementation not yet implemented")
    }

    override fun nextFloat(): SecureRandomResult<Float> {
        TODO("Android SecureRandom implementation not yet implemented")
    }

    override fun nextBytes(size: Int): SecureRandomResult<ByteArray> {
        TODO("Android SecureRandom implementation not yet implemented")
    }
}

actual fun createSecureRandom(): SecureRandomResult<SecureRandom> =
    SecureRandomResult.success(AndroidSecureRandom())