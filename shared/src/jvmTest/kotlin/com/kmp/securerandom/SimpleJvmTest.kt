package com.kmp.securerandom

import com.scottnj.kmp_secure_random.createSecureRandom
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertEquals

/**
 * Simple JVM tests for SecureRandom implementation.
 */
class SimpleJvmTest {

    private val secureRandom = createSecureRandom().getOrThrow()

    @Test
    fun testBasicGeneration() {
        // Test nextBytes
        val bytesResult = secureRandom.nextBytes(32)
        assertTrue(bytesResult.isSuccess, "Should generate random bytes")
        val bytes = bytesResult.getOrNull()
        assertNotNull(bytes)
        assertEquals(32, bytes!!.size)

        // Test nextInt
        val intResult = secureRandom.nextInt()
        assertTrue(intResult.isSuccess, "Should generate random int")

        // Test nextInt with bound
        val boundedIntResult = secureRandom.nextInt(100)
        assertTrue(boundedIntResult.isSuccess, "Should generate bounded int")
        val boundedInt = boundedIntResult.getOrNull()
        assertNotNull(boundedInt)
        assertTrue(boundedInt!! in 0 until 100, "Bounded int should be in range")

        // Test nextLong
        val longResult = secureRandom.nextLong()
        assertTrue(longResult.isSuccess, "Should generate random long")

        // Test nextDouble
        val doubleResult = secureRandom.nextDouble()
        assertTrue(doubleResult.isSuccess, "Should generate random double")
        val double = doubleResult.getOrNull()
        assertNotNull(double)
        assertTrue(double!! >= 0.0 && double < 1.0, "Double should be in [0,1)")

        // Test nextFloat
        val floatResult = secureRandom.nextFloat()
        assertTrue(floatResult.isSuccess, "Should generate random float")
        val float = floatResult.getOrNull()
        assertNotNull(float)
        assertTrue(float!! >= 0.0f && float < 1.0f, "Float should be in [0,1)")

        // Test nextBoolean
        val boolResult = secureRandom.nextBoolean()
        assertTrue(boolResult.isSuccess, "Should generate random boolean")
    }

    @Test
    fun testInvalidParameters() {
        // Test negative size
        val negativeResult = secureRandom.nextBytes(-1)
        assertTrue(negativeResult.isFailure, "Should fail with negative size")

        // Test zero bound
        val zeroBoundResult = secureRandom.nextInt(0)
        assertTrue(zeroBoundResult.isFailure, "Should fail with zero bound")

        // Test negative bound
        val negativeBoundResult = secureRandom.nextInt(-10)
        assertTrue(negativeBoundResult.isFailure, "Should fail with negative bound")
    }

    @Test
    fun testRandomness() {
        val samples = 1000
        val values = mutableSetOf<String>()

        // Generate many samples and check for uniqueness
        for (i in 0 until samples) {
            val result = secureRandom.nextBytes(16)
            assertTrue(result.isSuccess)
            values.add(result.getOrNull()!!.contentToString())
        }

        // Should have high uniqueness
        val uniquenessRatio = values.size.toDouble() / samples
        assertTrue(uniquenessRatio > 0.95, "Should have high uniqueness: $uniquenessRatio")
    }

    @Test
    fun testStatisticalProperties() {
        val samples = 10000

        // Test double distribution
        val doubles = mutableListOf<Double>()
        for (i in 0 until samples) {
            val result = secureRandom.nextDouble()
            assertTrue(result.isSuccess)
            doubles.add(result.getOrNull()!!)
        }

        // Check mean is around 0.5
        val mean = doubles.average()
        assertTrue(kotlin.math.abs(mean - 0.5) < 0.02, "Mean should be around 0.5: $mean")

        // Test boolean distribution
        var trueCount = 0
        for (i in 0 until samples) {
            val result = secureRandom.nextBoolean()
            assertTrue(result.isSuccess)
            if (result.getOrNull() == true) trueCount++
        }

        val trueRatio = trueCount.toDouble() / samples
        assertTrue(kotlin.math.abs(trueRatio - 0.5) < 0.02, "Boolean ratio should be around 0.5: $trueRatio")
    }
}