package com.scottnj.kmp_secure_random

/**
 * macOS implementation of SecureRandom.
 * TODO: Implement using macOS secure random APIs.
 */
internal class MacosSecureRandom : SecureRandom {

    override fun nextBytes(bytes: ByteArray): SecureRandomUnitResult {
        TODO("macOS SecureRandom implementation not yet implemented")
    }

    override fun nextInt(): SecureRandomResult<Int> {
        TODO("macOS SecureRandom implementation not yet implemented")
    }

    override fun nextInt(bound: Int): SecureRandomResult<Int> {
        TODO("macOS SecureRandom implementation not yet implemented")
    }

    override fun nextInt(min: Int, max: Int): SecureRandomResult<Int> {
        TODO("macOS SecureRandom implementation not yet implemented")
    }

    override fun nextLong(): SecureRandomResult<Long> {
        TODO("macOS SecureRandom implementation not yet implemented")
    }

    override fun nextLong(bound: Long): SecureRandomResult<Long> {
        TODO("macOS SecureRandom implementation not yet implemented")
    }

    override fun nextLong(min: Long, max: Long): SecureRandomResult<Long> {
        TODO("macOS SecureRandom implementation not yet implemented")
    }

    override fun nextBoolean(): SecureRandomResult<Boolean> {
        TODO("macOS SecureRandom implementation not yet implemented")
    }

    override fun nextDouble(): SecureRandomResult<Double> {
        TODO("macOS SecureRandom implementation not yet implemented")
    }

    override fun nextFloat(): SecureRandomResult<Float> {
        TODO("macOS SecureRandom implementation not yet implemented")
    }

    override fun nextBytes(size: Int): SecureRandomResult<ByteArray> {
        TODO("macOS SecureRandom implementation not yet implemented")
    }
}

actual fun createSecureRandom(): SecureRandomResult<SecureRandom> =
    SecureRandomResult.success(MacosSecureRandom())