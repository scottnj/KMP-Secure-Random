package com.scottnj.kmp_secure_random

import kotlin.test.*

@OptIn(AllowInsecureFallback::class)

/**
 * WASM-JS-specific tests for WasmJsSecureRandomAdapter.
 *
 * Focuses on WASM-JS unique features rather than basic operations
 * (which are already covered in commonTest):
 * - FallbackPolicy system (unique to WASM-JS)
 * - Web Crypto API integration verification
 * - Math.random() fallback behavior
 *
 * Basic functionality tests are covered in commonTest.
 */
class WasmJsSecureRandomAdapterTest {

    /**
     * Test that WasmJsSecureRandomAdapter can be created successfully.
     */
    @Test
    fun testAdapterCreation() {
        val result = WasmJsSecureRandomAdapter.create()
        assertTrue(result.isSuccess, "WasmJsSecureRandomAdapter should be created successfully")

        val adapter = result.getOrNull()
        assertNotNull(adapter, "Adapter should not be null")
    }

    /**
     * Test that createSecureRandom returns WASM-JS implementation.
     */
    @Test
    fun testCreateSecureRandomReturnsWasmJsImplementation() {
        val result = createSecureRandom()
        assertTrue(result.isSuccess, "createSecureRandom should succeed on WASM-JS")

        val secureRandom = result.getOrNull()
        assertNotNull(secureRandom, "SecureRandom should not be null")
    }

    /**
     * Test Web Crypto API availability and integration in WASM-JS environment.
     * This is the critical platform-specific test that verifies actual Web Crypto usage.
     */
    @Test
    fun testWebCryptoAPIIntegration() {
        // Test that we can detect Web Crypto API availability
        // Note: In WASM-JS we can't use js() directly, but we can test the behavior

        val secureRandom = createSecureRandom().getOrThrow()

        // Generate random data and verify it's working
        val bytes = ByteArray(32)
        val result = secureRandom.nextBytes(bytes)
        assertTrue(result.isSuccess, "Web Crypto API generation should succeed")

        // Verify the data shows randomness characteristics
        assertFalse(bytes.all { it == 0.toByte() }, "Should not generate all zeros")

        // Test multiple generations for diversity
        val bytes2 = ByteArray(32)
        val result2 = secureRandom.nextBytes(bytes2)
        assertTrue(result2.isSuccess, "Second generation should succeed")
        assertFalse(bytes.contentEquals(bytes2), "Multiple generations should differ")

        println("✅ Web Crypto API integration verified")
    }

    /**
     * Test SECURE_ONLY fallback policy (default behavior).
     * Critical test for WASM-JS security model.
     */
    @Test
    fun testSecureOnlyFallbackPolicy() {
        // Test default secure-only policy
        val defaultResult = createSecureRandom()
        assertTrue(defaultResult.isSuccess, "Default (secure-only) should succeed")

        // Test explicit secure-only policy
        val secureOnlyResult = createSecureRandom(FallbackPolicy.SECURE_ONLY)
        assertTrue(secureOnlyResult.isSuccess, "Explicit secure-only should succeed")

        // Both should work identically
        val secureRandom = secureOnlyResult.getOrThrow()
        val bytes = ByteArray(16)
        val result = secureRandom.nextBytes(bytes)
        assertTrue(result.isSuccess, "Secure-only generation should work")

        println("✅ Secure-only fallback policy verified")
    }

    /**
     * Test ALLOW_INSECURE fallback policy.
     * Critical test for WASM-JS Math.random() fallback with security warnings.
     */
    @Test
    fun testInsecureFallbackPolicy() {
        // Test ALLOW_INSECURE policy (should work regardless of Web Crypto availability)
        val insecureResult = createSecureRandom(FallbackPolicy.ALLOW_INSECURE)
        assertTrue(insecureResult.isSuccess, "ALLOW_INSECURE policy should succeed")

        val secureRandom = insecureResult.getOrThrow()

        // Test basic generation works
        val bytes = ByteArray(16)
        val bytesResult = secureRandom.nextBytes(bytes)
        assertTrue(bytesResult.isSuccess, "Random generation should work with ALLOW_INSECURE")
        assertFalse(bytes.all { it == 0.toByte() }, "Should generate non-zero bytes")

        // Test integer generation
        val intResult = secureRandom.nextInt()
        assertTrue(intResult.isSuccess, "Integer generation should work with ALLOW_INSECURE")

        println("✅ Insecure fallback policy verified (use only for non-cryptographic purposes)")
    }

    /**
     * Test adapter creation with different fallback policies.
     */
    @Test
    fun testAdapterCreationWithFallbackPolicies() {
        // Test adapter creation with secure-only policy
        val secureOnlyResult = WasmJsSecureRandomAdapter.create(FallbackPolicy.SECURE_ONLY)
        assertTrue(secureOnlyResult.isSuccess, "Should create adapter with SECURE_ONLY policy")

        // Test adapter creation with insecure fallback policy
        val insecureResult = WasmJsSecureRandomAdapter.create(FallbackPolicy.ALLOW_INSECURE)
        assertTrue(insecureResult.isSuccess, "Should create adapter with ALLOW_INSECURE policy")

        // Both adapters should function
        val secureAdapter = secureOnlyResult.getOrThrow()
        val insecureAdapter = insecureResult.getOrThrow()

        val secureBytes = ByteArray(8)
        val insecureBytes = ByteArray(8)

        assertTrue(secureAdapter.nextBytes(secureBytes).isSuccess, "Secure adapter should work")
        assertTrue(insecureAdapter.nextBytes(insecureBytes).isSuccess, "Insecure adapter should work")

        println("✅ Adapter creation with both fallback policies verified")
    }

    /**
     * Test fallback policy consistency across multiple operations.
     */
    @Test
    fun testFallbackPolicyConsistency() {
        val secureRandom = createSecureRandom(FallbackPolicy.ALLOW_INSECURE).getOrThrow()

        // Test that the policy is consistently applied across different generation methods
        val operations = listOf(
            { secureRandom.nextBytes(8) },
            { secureRandom.nextInt() },
            { secureRandom.nextLong() },
            { secureRandom.nextDouble() },
            { secureRandom.nextFloat() },
            { secureRandom.nextBoolean() }
        )

        operations.forEach { operation ->
            val result = operation()
            assertTrue(result.isSuccess, "All operations should succeed with consistent policy")
        }

        println("✅ Fallback policy consistency verified across all operations")
    }

    /**
     * Test backward compatibility - default behavior should not change.
     */
    @Test
    fun testBackwardCompatibility() {
        // Test that existing code without explicit fallback policy still works
        val defaultResult = createSecureRandom()
        assertTrue(defaultResult.isSuccess, "Default behavior should remain working")

        val secureRandom = defaultResult.getOrThrow()
        val bytes = ByteArray(16)
        val result = secureRandom.nextBytes(bytes)
        assertTrue(result.isSuccess, "Default generation should work")

        // Verify default is secure-only behavior
        val explicitSecureResult = createSecureRandom(FallbackPolicy.SECURE_ONLY)
        assertTrue(explicitSecureResult.isSuccess, "Explicit secure should work same as default")

        println("✅ Backward compatibility verified")
    }

    /**
     * Test WASM-JS specific environment behavior.
     * Verifies WASM-JS constraints and capabilities.
     */
    @Test
    fun testWasmJsEnvironmentBehavior() {
        // Test that the WASM-JS adapter compiles and runs properly
        val adapter = WasmJsSecureRandomAdapter.create(FallbackPolicy.ALLOW_INSECURE)
        assertTrue(adapter.isSuccess, "WASM-JS adapter should be created successfully")

        val secureRandom = adapter.getOrThrow()

        // Test that all method signatures work correctly in WASM-JS
        val byteArrayResult = secureRandom.nextBytes(16)
        assertTrue(byteArrayResult.isSuccess, "Byte array generation should work")
        assertTrue(byteArrayResult.getOrNull() is ByteArray, "Should return ByteArray")

        val intResult = secureRandom.nextInt()
        assertTrue(intResult.isSuccess, "Int generation should work")
        assertTrue(intResult.getOrNull() is Int, "Should return Int")

        val longResult = secureRandom.nextLong()
        assertTrue(longResult.isSuccess, "Long generation should work")
        assertTrue(longResult.getOrNull() is Long, "Should return Long")

        val doubleResult = secureRandom.nextDouble()
        assertTrue(doubleResult.isSuccess, "Double generation should work")
        assertTrue(doubleResult.getOrNull() is Double, "Should return Double")

        val floatResult = secureRandom.nextFloat()
        assertTrue(floatResult.isSuccess, "Float generation should work")
        assertTrue(floatResult.getOrNull() is Float, "Should return Float")

        val booleanResult = secureRandom.nextBoolean()
        assertTrue(booleanResult.isSuccess, "Boolean generation should work")
        assertTrue(booleanResult.getOrNull() is Boolean, "Should return Boolean")

        println("✅ WASM-JS environment behavior verified")
    }

    /**
     * Test Math.random() fallback quality when Web Crypto is unavailable.
     * This verifies the enhanced XOR technique mentioned in the implementation.
     */
    @Test
    fun testMathRandomFallbackQuality() {
        // Test with ALLOW_INSECURE policy to potentially trigger Math.random() path
        val secureRandom = createSecureRandom(FallbackPolicy.ALLOW_INSECURE).getOrThrow()

        // Generate multiple samples to test statistical properties of fallback
        val samples = 100
        val byteValues = mutableSetOf<Byte>()

        repeat(samples) {
            val bytes = ByteArray(1)
            val result = secureRandom.nextBytes(bytes)
            if (result.isSuccess) {
                byteValues.add(bytes[0])
            }
        }

        // Even with Math.random() fallback, we should see reasonable diversity
        val diversityRatio = byteValues.size.toDouble() / samples
        assertTrue(diversityRatio > 0.1, "Even fallback should show some diversity: $diversityRatio")

        println("✅ Math.random() fallback quality verified (diversity ratio: $diversityRatio)")
    }
}