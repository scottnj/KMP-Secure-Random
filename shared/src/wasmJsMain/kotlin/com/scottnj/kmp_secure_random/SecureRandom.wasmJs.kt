package com.scottnj.kmp_secure_random

/**
 * WASM implementation of SecureRandom.
 * TODO: Implement secure random generation for WASM target.
 */
internal class WasmJsSecureRandom : SecureRandom {

    override fun nextBytes(bytes: ByteArray) {
        TODO("WASM SecureRandom implementation not yet implemented")
    }

    override fun nextInt(): Int {
        TODO("WASM SecureRandom implementation not yet implemented")
    }

    override fun nextInt(bound: Int): Int {
        TODO("WASM SecureRandom implementation not yet implemented")
    }

    override fun nextLong(): Long {
        TODO("WASM SecureRandom implementation not yet implemented")
    }

    override fun nextBoolean(): Boolean {
        TODO("WASM SecureRandom implementation not yet implemented")
    }

    override fun nextDouble(): Double {
        TODO("WASM SecureRandom implementation not yet implemented")
    }
}

actual fun createSecureRandom(): SecureRandom = WasmJsSecureRandom()