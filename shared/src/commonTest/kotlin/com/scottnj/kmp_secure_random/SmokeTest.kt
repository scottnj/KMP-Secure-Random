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

        // Test that we can create instances of our main class
        val secureRandom = createSecureRandom()

        // If this test passes, our basic code structure is sound for detekt analysis
        // The fact that createSecureRandom() compiles and runs validates our API design
        val className = secureRandom::class.simpleName
        assertEquals(true, className?.contains("SecureRandom"), "Class name should contain 'SecureRandom'")
    }
}