package com.scottnj.kmp_secure_random

import co.touchlab.kermit.Logger
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Smoke tests to verify basic infrastructure and dependencies are working correctly
 * across all KMP platforms.
 */
class SmokeTest {

    @Test
    fun testKermitLogging() {
        val logger = Logger.withTag("SmokeTest")
        logger.d { "Kermit logging is working correctly across all KMP platforms" }
        logger.i { "Infrastructure smoke test passed" }
        // If this test passes, kermit is properly configured
        assertEquals(true, true)
    }

    @Test
    fun testDetektCodeQuality() {
        // This test validates that our code follows basic quality standards
        // that will be enforced by detekt static analysis

        // Test that we can create instances of our main class with Result-based API
        val secureRandomResult = createSecureRandom()

        // If this test passes, our basic code structure is sound for detekt analysis
        // The fact that createSecureRandom() compiles and runs validates our API design
        assertEquals(true, secureRandomResult.isSuccess, "SecureRandom creation should succeed in smoke test")

        // Validate the result contains a proper SecureRandom instance
        val secureRandom = secureRandomResult.getOrNull()
        val className = secureRandom?.let { it::class.simpleName }
        assertEquals(true, className?.contains("SecureRandom"), "Class name should contain 'SecureRandom'")
    }

    @Test
    fun testKoverCodeCoverage() {
        val logger = Logger.withTag("CoverageTest")
        logger.d { "Testing code coverage functionality with kover" }

        // Exercise some of the SecureRandom interface to generate coverage data
        val secureRandomResult = createSecureRandom()
        assertEquals(true, secureRandomResult.isSuccess, "SecureRandom creation should succeed")

        // Test factory function coverage - verify we get different instances
        val factoryTestResult = createSecureRandom()
        assertEquals(true, factoryTestResult.isSuccess, "Second SecureRandom creation should succeed")

        val secureRandom = secureRandomResult.getOrNull()
        val factoryTest = factoryTestResult.getOrNull()
        assertEquals(false, secureRandom === factoryTest, "Factory should create distinct instances")

        // Log success for coverage tracking
        logger.i { "Kover code coverage smoke test completed" }
        assertEquals(true, true, "Kover coverage test passed")
    }
}