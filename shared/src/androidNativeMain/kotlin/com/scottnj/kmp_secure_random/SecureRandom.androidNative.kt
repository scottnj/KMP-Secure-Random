package com.scottnj.kmp_secure_random

/**
 * Android Native implementation of SecureRandom.
 * TODO: Implement using Android Native secure random APIs.
 */
internal class AndroidNativeSecureRandom : SecureRandom {

    override fun nextBytes(bytes: ByteArray): SecureRandomUnitResult {
        TODO("Android Native SecureRandom implementation not yet implemented")
    }

    override fun nextInt(): SecureRandomResult<Int> {
        TODO("Android Native SecureRandom implementation not yet implemented")
    }

    override fun nextInt(bound: Int): SecureRandomResult<Int> {
        TODO("Android Native SecureRandom implementation not yet implemented")
    }

    override fun nextInt(min: Int, max: Int): SecureRandomResult<Int> {
        TODO("Android Native SecureRandom implementation not yet implemented")
    }

    override fun nextLong(): SecureRandomResult<Long> {
        TODO("Android Native SecureRandom implementation not yet implemented")
    }

    override fun nextLong(bound: Long): SecureRandomResult<Long> {
        TODO("Android Native SecureRandom implementation not yet implemented")
    }

    override fun nextLong(min: Long, max: Long): SecureRandomResult<Long> {
        TODO("Android Native SecureRandom implementation not yet implemented")
    }

    override fun nextBoolean(): SecureRandomResult<Boolean> {
        TODO("Android Native SecureRandom implementation not yet implemented")
    }

    override fun nextDouble(): SecureRandomResult<Double> {
        TODO("Android Native SecureRandom implementation not yet implemented")
    }

    override fun nextFloat(): SecureRandomResult<Float> {
        TODO("Android Native SecureRandom implementation not yet implemented")
    }

    override fun nextBytes(size: Int): SecureRandomResult<ByteArray> {
        TODO("Android Native SecureRandom implementation not yet implemented")
    }
}

actual fun createSecureRandom(): SecureRandomResult<SecureRandom> =
    SecureRandomResult.success(AndroidNativeSecureRandom())