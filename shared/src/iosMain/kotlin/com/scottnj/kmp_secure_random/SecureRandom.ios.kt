package com.scottnj.kmp_secure_random

/**
 * iOS implementation of SecureRandom.
 * TODO: Implement using SecRandomCopyBytes from Security framework.
 */
internal class IosSecureRandom : SecureRandom {

    override fun nextBytes(bytes: ByteArray): SecureRandomUnitResult {
        TODO("iOS SecureRandom implementation not yet implemented")
    }

    override fun nextInt(): SecureRandomResult<Int> {
        TODO("iOS SecureRandom implementation not yet implemented")
    }

    override fun nextInt(bound: Int): SecureRandomResult<Int> {
        TODO("iOS SecureRandom implementation not yet implemented")
    }

    override fun nextInt(min: Int, max: Int): SecureRandomResult<Int> {
        TODO("iOS SecureRandom implementation not yet implemented")
    }

    override fun nextLong(): SecureRandomResult<Long> {
        TODO("iOS SecureRandom implementation not yet implemented")
    }

    override fun nextLong(bound: Long): SecureRandomResult<Long> {
        TODO("iOS SecureRandom implementation not yet implemented")
    }

    override fun nextLong(min: Long, max: Long): SecureRandomResult<Long> {
        TODO("iOS SecureRandom implementation not yet implemented")
    }

    override fun nextBoolean(): SecureRandomResult<Boolean> {
        TODO("iOS SecureRandom implementation not yet implemented")
    }

    override fun nextDouble(): SecureRandomResult<Double> {
        TODO("iOS SecureRandom implementation not yet implemented")
    }

    override fun nextFloat(): SecureRandomResult<Float> {
        TODO("iOS SecureRandom implementation not yet implemented")
    }

    override fun nextBytes(size: Int): SecureRandomResult<ByteArray> {
        TODO("iOS SecureRandom implementation not yet implemented")
    }
}

actual fun createSecureRandom(): SecureRandomResult<SecureRandom> =
    SecureRandomResult.success(IosSecureRandom())