package com.kmp.securerandom

import com.scottnj.kmp_secure_random.createSecureRandom
import com.scottnj.kmp_secure_random.SecureRandom
import java.security.Security
import kotlin.test.*

/**
 * Integration tests specifically for JVM SecureRandom implementation.
 * Tests actual JVM SecureRandom integration, algorithm availability, and platform-specific behavior.
 */
class JvmSecureRandomIntegrationTest {

    private val secureRandom = createSecureRandom().getOrThrow()

    /**
     * Test that the implementation uses actual java.security.SecureRandom.
     */
    @Test
    fun testUsesJavaSecureRandom() {
        // Generate some random data
        val result = secureRandom.nextBytes(32)
        assertTrue(result.isSuccess, "Should successfully generate random bytes")

        val bytes = result.getOrNull()
        assertNotNull(bytes)
        assertEquals(32, bytes!!.size)

        // Verify bytes are not all zeros or all same value
        assertFalse(bytes.all { it == 0.toByte() }, "Bytes should not all be zero")
        assertFalse(bytes.all { it == bytes[0] }, "Bytes should not all be the same")
    }

    /**
     * Test available SecureRandom algorithms on the JVM.
     */
    @Test
    fun testAvailableAlgorithms() {
        val providers = Security.getProviders()
        val algorithms = mutableSetOf<String>()

        for (provider in providers) {
            for (service in provider.services) {
                if (service.type == "SecureRandom") {
                    algorithms.add(service.algorithm)
                }
            }
        }

        println("\n=== Available SecureRandom Algorithms ===")
        algorithms.sorted().forEach { algo ->
            println("  - $algo")
        }

        // Common algorithms that should be available
        val expectedAlgorithms = when {
            System.getProperty("os.name").contains("Windows") -> listOf("Windows-PRNG", "SHA1PRNG")
            else -> listOf("NativePRNG", "SHA1PRNG")
        }

        for (expected in expectedAlgorithms) {
            if (algorithms.contains(expected)) {
                println("✓ Found expected algorithm: $expected")
            } else {
                println("✗ Missing expected algorithm: $expected")
            }
        }

        assertTrue(algorithms.isNotEmpty(), "Should have at least one SecureRandom algorithm")
    }

    /**
     * Test behavior with different JVM implementations (if available).
     */
    @Test
    fun testJvmImplementationDetails() {
        val jvmVendor = System.getProperty("java.vendor", "Unknown")
        val jvmVersion = System.getProperty("java.version", "Unknown")
        val osName = System.getProperty("os.name", "Unknown")

        println("\n=== JVM Environment ===")
        println("JVM Vendor: $jvmVendor")
        println("JVM Version: $jvmVersion")
        println("OS: $osName")

        // Test random generation works on this JVM
        val result = secureRandom.nextBytes(1024)
        assertTrue(result.isSuccess, "Random generation should work on $jvmVendor JVM")

        // Verify entropy quality (basic check)
        val bytes = result.getOrNull()!!
        val uniqueBytes = bytes.toSet().size
        val uniquenessRatio = uniqueBytes.toDouble() / 256

        println("Unique byte values: $uniqueBytes/256 (${uniquenessRatio * 100}%)")
        assertTrue(uniquenessRatio > 0.3, "Should have reasonable byte diversity")
    }

    /**
     * Test that our implementation properly handles JVM security manager restrictions.
     */
    @Test
    fun testSecurityManagerCompatibility() {
        // Note: Security Manager is deprecated in newer Java versions
        val currentSecurityManager = System.getSecurityManager()

        if (currentSecurityManager != null) {
            println("Security Manager is active: ${currentSecurityManager.javaClass.name}")

            // Test that we can still generate random numbers
            val result = secureRandom.nextBytes(32)
            assertTrue(result.isSuccess, "Should work with Security Manager")
        } else {
            println("No Security Manager active (normal for modern Java)")

            // Still test generation
            val result = secureRandom.nextBytes(32)
            assertTrue(result.isSuccess, "Should work without Security Manager")
        }
    }

    /**
     * Test seed generation and reseeding behavior.
     */
    @Test
    fun testSeedingBehavior() {
        // Create multiple instances
        val instance1 = createSecureRandom().getOrThrow()
        val instance2 = createSecureRandom().getOrThrow()

        // Generate sequences from both using nextBytes
        val seq1 = mutableListOf<String>()
        val seq2 = mutableListOf<String>()

        for (i in 0..50) {
            val result1 = instance1.nextBytes(8)
            val result2 = instance2.nextBytes(8)

            assertTrue(result1.isSuccess && result2.isSuccess)
            seq1.add(result1.getOrNull()!!.contentToString())
            seq2.add(result2.getOrNull()!!.contentToString())
        }

        // Sequences should be different (different seeds)
        assertNotEquals(seq1, seq2, "Different instances should produce different sequences")

        // Check for independence
        var matches = 0
        for (i in seq1.indices) {
            if (seq1[i] == seq2[i]) matches++
        }

        val matchRatio = matches.toDouble() / seq1.size
        assertTrue(matchRatio < 0.1, "Too many matches between independent sequences: $matchRatio")
    }

    /**
     * Test resilience to rapid instance creation.
     */
    @Test
    fun testRapidInstanceCreation() {
        val instances = mutableListOf<SecureRandom>()
        val values = mutableSetOf<String>()

        // Create many instances rapidly
        for (i in 0..50) {
            val instance = createSecureRandom().getOrThrow()
            instances.add(instance)

            // Generate a value from each
            val result = instance.nextBytes(8)
            assertTrue(result.isSuccess)
            values.add(result.getOrNull()!!.contentToString())
        }

        // Should have mostly unique values
        val uniquenessRatio = values.size.toDouble() / instances.size
        println("Instance uniqueness: ${values.size}/${instances.size} (${uniquenessRatio * 100}%)")

        assertTrue(uniquenessRatio > 0.90, "Instances should produce unique values")
    }

    /**
     * Test cross-thread instance sharing.
     */
    @Test
    fun testCrossThreadSharing() {
        val sharedInstance = createSecureRandom().getOrThrow()
        val threadCount = 5
        val results = Array(threadCount) { mutableListOf<ByteArray>() }
        val threads = mutableListOf<Thread>()

        for (i in 0 until threadCount) {
            threads.add(Thread {
                for (j in 0..50) {
                    val result = sharedInstance.nextBytes(16)
                    if (result.isSuccess) {
                        results[i].add(result.getOrNull()!!)
                    }
                }
            })
        }

        // Start all threads
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // Check all threads got results
        for (i in 0 until threadCount) {
            assertTrue(results[i].size > 25, "Thread $i should have generated values")
        }

        // Check for uniqueness across all threads
        val allValues = results.flatMap { it.map { bytes -> bytes.contentToString() } }.toSet()
        val totalGenerated = results.sumOf { it.size }
        val uniquenessRatio = allValues.size.toDouble() / totalGenerated

        println("Cross-thread uniqueness: ${allValues.size}/$totalGenerated (${uniquenessRatio * 100}%)")
        assertTrue(uniquenessRatio > 0.90, "Should have unique values across threads")
    }

    /**
     * Test behavior with system entropy exhaustion simulation.
     */
    @Test
    fun testEntropyExhaustion() {
        // Generate a large amount of random data rapidly
        val bytesToGenerate = 10 * 1024 * 1024 // 10MB
        val chunkSize = 1024 * 1024 // 1MB chunks
        val chunks = bytesToGenerate / chunkSize

        var successCount = 0
        var totalTime = 0L

        println("\n=== Entropy Exhaustion Test ===")
        println("Generating ${bytesToGenerate / 1024 / 1024}MB in ${chunkSize / 1024}KB chunks...")

        for (i in 0 until chunks) {
            val startTime = System.currentTimeMillis()
            val result = secureRandom.nextBytes(chunkSize)
            val elapsed = System.currentTimeMillis() - startTime
            totalTime += elapsed

            if (result.isSuccess) {
                successCount++
                if (i % 2 == 0) {
                    println("Chunk ${i + 1}/$chunks: ${elapsed}ms")
                }
            }
        }

        val successRate = successCount.toDouble() / chunks
        val avgTimePerChunk = totalTime.toDouble() / chunks

        println("Success rate: $successCount/$chunks (${successRate * 100}%)")
        println("Average time per chunk: ${avgTimePerChunk}ms")

        assertTrue(successRate == 1.0, "All chunks should generate successfully")
    }

    /**
     * Test proper resource cleanup.
     */
    @Test
    fun testResourceCleanup() {
        // Create and use many temporary instances
        for (i in 0..100) {
            val tempInstance = createSecureRandom().getOrThrow()
            val result = tempInstance.nextBytes(1024)
            assertTrue(result.isSuccess)

            // Instance should be garbage collectable after this
        }

        // Force garbage collection
        System.gc()
        Thread.sleep(50)

        // Create new instance to verify system still works
        val newInstance = createSecureRandom().getOrThrow()
        val result = newInstance.nextBytes(32)
        assertTrue(result.isSuccess, "Should work after many temporary instances")
    }

    /**
     * Test compliance with SecureRandom contract.
     */
    @Test
    fun testSecureRandomContract() {
        // Test that successive calls produce different results
        val results = mutableSetOf<String>()
        for (i in 0..100) {
            val result = secureRandom.nextBytes(32)
            assertTrue(result.isSuccess)
            results.add(result.getOrNull()!!.contentToString())
        }

        assertEquals(101, results.size, "All results should be unique")

        // Test that bounded integers are within range
        for (i in 0..1000) {
            val bound = 100
            val result = secureRandom.nextInt(bound)
            assertTrue(result.isSuccess)
            val value = result.getOrNull()!!
            assertTrue(value >= 0 && value < bound, "Value $value outside [0, $bound)")
        }

        // Test double distribution properties
        val doubleValues = mutableListOf<Double>()
        for (i in 0..1000) {
            val result = secureRandom.nextDouble()
            assertTrue(result.isSuccess)
            doubleValues.add(result.getOrNull()!!)
        }

        val mean = doubleValues.average()

        // Should be approximately uniform [0,1)
        assertTrue(doubleValues.all { it >= 0.0 && it < 1.0 }, "All values should be in [0,1)")
        assertTrue(kotlin.math.abs(mean - 0.5) < 0.05, "Uniform mean should be near 0.5: $mean")
    }
}