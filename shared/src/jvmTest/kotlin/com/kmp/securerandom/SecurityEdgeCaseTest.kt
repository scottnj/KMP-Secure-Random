package com.kmp.securerandom

import com.scottnj.kmp_secure_random.createSecureRandom
import com.scottnj.kmp_secure_random.InvalidParameterException
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.*

/**
 * JVM-specific security and edge case tests for SecureRandom implementation.
 * Tests thread safety, error conditions, resource constraints, and JVM-specific security properties.
 *
 * Note: Basic edge case tests have been moved to AdvancedEdgeCaseTest in commonTest
 * for cross-platform validation. This file contains JVM-specific tests using concurrent utilities.
 */
class SecurityEdgeCaseTest {

    private val secureRandom = createSecureRandom().getOrThrow()

    /**
     * Test thread safety with concurrent access from multiple threads.
     */
    @Test
    fun testThreadSafety() {
        val numThreads = 50
        val operationsPerThread = 100
        val barrier = CyclicBarrier(numThreads)
        val errors = AtomicInteger(0)
        val uniqueValues = mutableSetOf<String>()
        val lock = Any()

        val threads = List(numThreads) { threadIndex ->
            thread {
                try {
                    // Wait for all threads to start
                    barrier.await()

                    // Perform random operations
                    for (i in 0 until operationsPerThread) {
                        when (i % 4) {
                            0 -> {
                                val result = secureRandom.nextBytes(16)
                                if (result.isFailure) errors.incrementAndGet()
                                else {
                                    val bytes = result.getOrNull()!!
                                    synchronized(lock) {
                                        uniqueValues.add(bytes.contentToString())
                                    }
                                }
                            }
                            1 -> {
                                val result = secureRandom.nextInt(1000)
                                if (result.isFailure) errors.incrementAndGet()
                            }
                            2 -> {
                                val result = secureRandom.nextLong()
                                if (result.isFailure) errors.incrementAndGet()
                            }
                            3 -> {
                                val result = secureRandom.nextDouble()
                                if (result.isFailure) errors.incrementAndGet()
                            }
                        }
                    }
                } catch (e: Exception) {
                    errors.incrementAndGet()
                    println("Thread $threadIndex error: ${e.message}")
                }
            }
        }

        // Wait for all threads to complete
        threads.forEach { it.join() }

        println("Thread safety test: errors=${errors.get()}, unique values=${uniqueValues.size}")
        assertEquals(0, errors.get(), "Thread safety test failed with ${errors.get()} errors")

        // Verify we got unique values (high probability for secure random)
        val expectedUniqueValues = numThreads * (operationsPerThread / 4) * 0.95 // Allow 5% collision tolerance
        assertTrue(
            uniqueValues.size > expectedUniqueValues,
            "Too many duplicate values: ${uniqueValues.size} < $expectedUniqueValues"
        )
    }

    /**
     * Test behavior with invalid parameters.
     */
    @Test
    fun testInvalidParameters() {
        // Test negative byte array size
        val negativeSizeResult = secureRandom.nextBytes(-1)
        assertTrue(negativeSizeResult.isFailure, "Should fail with negative size")
        assertTrue(
            negativeSizeResult.exceptionOrNull() is InvalidParameterException,
            "Should throw InvalidParameterException for negative size"
        )

        // Test zero byte array size (should succeed with empty array)
        val zeroSizeResult = secureRandom.nextBytes(0)
        assertTrue(zeroSizeResult.isSuccess, "Should succeed with zero size")
        assertEquals(0, zeroSizeResult.getOrNull()?.size)

        // Test negative bound for nextInt
        val negativeBoundResult = secureRandom.nextInt(-10)
        assertTrue(negativeBoundResult.isFailure, "Should fail with negative bound")

        // Test zero bound for nextInt
        val zeroBoundResult = secureRandom.nextInt(0)
        assertTrue(zeroBoundResult.isFailure, "Should fail with zero bound")

        // Test invalid range for nextInt
        val invalidRangeResult = secureRandom.nextInt(10, 5)
        assertTrue(invalidRangeResult.isFailure, "Should fail with invalid range (from > until)")

        // Test equal range for nextInt (should fail)
        val equalRangeResult = secureRandom.nextInt(5, 5)
        assertTrue(equalRangeResult.isFailure, "Should fail with equal range")

        // Test invalid range for nextLong
        val invalidLongRangeResult = secureRandom.nextLong(100L, 50L)
        assertTrue(invalidLongRangeResult.isFailure, "Should fail with invalid long range")
    }

    /**
     * Test large allocation requests.
     */
    @Test
    fun testLargeAllocations() {
        // Test moderately large allocation (1 MB)
        val moderateResult = secureRandom.nextBytes(1024 * 1024)
        assertTrue(moderateResult.isSuccess, "Should handle 1MB allocation")
        val moderateBytes = moderateResult.getOrNull()
        assertNotNull(moderateBytes)
        assertEquals(1024 * 1024, moderateBytes!!.size)

        // Verify randomness in large allocation (check first and last portions)
        val firstPortion = moderateBytes.slice(0..999)
        val lastPortion = moderateBytes.slice(moderateBytes.size - 1000 until moderateBytes.size)
        assertNotEquals(firstPortion, lastPortion, "Large allocation should have random content throughout")
    }

    // Rapid successive calls test moved to AdvancedEdgeCaseTest in commonTest for cross-platform validation

    // Seed independence test moved to AdvancedEdgeCaseTest in commonTest for cross-platform validation

    // No short cycles test moved to AdvancedEdgeCaseTest in commonTest for cross-platform validation

    /**
     * Test memory cleanup and no sensitive data leaks.
     */
    @Test
    fun testMemorySecurity() {
        // Generate sensitive random data
        val sensitiveResult = secureRandom.nextBytes(1024)
        assertTrue(sensitiveResult.isSuccess)
        val sensitiveData = sensitiveResult.getOrNull()!!

        // Create a copy to verify it's actually random
        val copy = sensitiveData.copyOf()

        // Clear the original array (simulating what secure implementations should do internally)
        sensitiveData.fill(0)

        // Verify the copy still has random data (not all zeros)
        assertFalse(
            copy.all { it == 0.toByte() },
            "Random data should not be all zeros"
        )

        // Generate new data to ensure generator still works after cleanup
        val newResult = secureRandom.nextBytes(1024)
        assertTrue(newResult.isSuccess)
        val newData = newResult.getOrNull()!!

        assertFalse(
            newData.contentEquals(copy),
            "New random data should differ from previous"
        )
    }

    /**
     * Test behavior under resource pressure.
     */
    @Test
    fun testResourcePressure() {
        val executor = Executors.newFixedThreadPool(20)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)

        try {
            // Submit many tasks simultaneously
            val futures = List(200) { taskIndex ->
                executor.submit {
                    try {
                        // Each task performs multiple operations
                        for (i in 0..5) {
                            val result = when (i % 3) {
                                0 -> secureRandom.nextBytes(512)
                                1 -> secureRandom.nextInt(100000).map { it.toString().toByteArray() }
                                else -> secureRandom.nextDouble().map { it.toString().toByteArray() }
                            }

                            if (result.isSuccess) {
                                successCount.incrementAndGet()
                            } else {
                                failureCount.incrementAndGet()
                            }
                        }
                    } catch (e: Exception) {
                        failureCount.incrementAndGet()
                    }
                }
            }

            // Wait for all tasks to complete
            futures.forEach { it.get(10, TimeUnit.SECONDS) }

            println("Resource pressure test: successes=${successCount.get()}, failures=${failureCount.get()}")

            // Should handle resource pressure gracefully
            assertTrue(
                successCount.get() > failureCount.get() * 10,
                "Too many failures under resource pressure"
            )
        } finally {
            executor.shutdown()
        }
    }

    // Boundary values test moved to AdvancedEdgeCaseTest in commonTest for cross-platform validation

    /**
     * Test that random values maintain security properties over time.
     */
    @Test
    fun testTemporalIndependence() {
        val samples = 100
        val delayMs = 5L

        val firstBatch = mutableListOf<ByteArray>()
        val secondBatch = mutableListOf<ByteArray>()

        // Collect first batch
        for (i in 0 until samples) {
            val result = secureRandom.nextBytes(16)
            assertTrue(result.isSuccess)
            firstBatch.add(result.getOrNull()!!)
            Thread.sleep(delayMs)
        }

        // Wait a bit
        Thread.sleep(50)

        // Collect second batch
        for (i in 0 until samples) {
            val result = secureRandom.nextBytes(16)
            assertTrue(result.isSuccess)
            secondBatch.add(result.getOrNull()!!)
            Thread.sleep(delayMs)
        }

        // Check that no values from first batch appear in second batch
        val firstBatchStrings = firstBatch.map { it.contentToString() }.toSet()
        val secondBatchStrings = secondBatch.map { it.contentToString() }.toSet()

        val intersection = firstBatchStrings.intersect(secondBatchStrings)
        assertTrue(
            intersection.isEmpty(),
            "Found ${intersection.size} duplicate values between batches"
        )
    }
}