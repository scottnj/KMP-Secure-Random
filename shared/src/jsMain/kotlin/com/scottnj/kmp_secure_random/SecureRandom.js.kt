package com.scottnj.kmp_secure_random

/**
 * JavaScript implementation of SecureRandom.
 * TODO: Implement using Web Crypto API's crypto.getRandomValues().
 */
internal class JsSecureRandom : SecureRandom {

    override fun nextBytes(bytes: ByteArray): SecureRandomUnitResult {
        TODO("JavaScript SecureRandom implementation not yet implemented")
    }

    override fun nextInt(): SecureRandomResult<Int> {
        TODO("JavaScript SecureRandom implementation not yet implemented")
    }

    override fun nextInt(bound: Int): SecureRandomResult<Int> {
        TODO("JavaScript SecureRandom implementation not yet implemented")
    }

    override fun nextInt(min: Int, max: Int): SecureRandomResult<Int> {
        TODO("JavaScript SecureRandom implementation not yet implemented")
    }

    override fun nextLong(): SecureRandomResult<Long> {
        TODO("JavaScript SecureRandom implementation not yet implemented")
    }

    override fun nextLong(bound: Long): SecureRandomResult<Long> {
        TODO("JavaScript SecureRandom implementation not yet implemented")
    }

    override fun nextLong(min: Long, max: Long): SecureRandomResult<Long> {
        TODO("JavaScript SecureRandom implementation not yet implemented")
    }

    override fun nextBoolean(): SecureRandomResult<Boolean> {
        TODO("JavaScript SecureRandom implementation not yet implemented")
    }

    override fun nextDouble(): SecureRandomResult<Double> {
        TODO("JavaScript SecureRandom implementation not yet implemented")
    }

    override fun nextFloat(): SecureRandomResult<Float> {
        TODO("JavaScript SecureRandom implementation not yet implemented")
    }

    override fun nextBytes(size: Int): SecureRandomResult<ByteArray> {
        TODO("JavaScript SecureRandom implementation not yet implemented")
    }
}

actual fun createSecureRandom(): SecureRandomResult<SecureRandom> =
    SecureRandomResult.success(JsSecureRandom())