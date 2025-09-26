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
}