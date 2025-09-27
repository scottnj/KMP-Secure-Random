package com.kmp.securerandom

import com.scottnj.kmp_secure_random.createSecureRandom
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Performance benchmark tests for JVM SecureRandom implementation.
 * Measures throughput, latency, and scalability of random number generation.
 */
class PerformanceBenchmarkTest {

    private val secureRandom = createSecureRandom().getOrThrow()
    private val warmupIterations = 100
    private val benchmarkIterations = 1000

    /**
     * Warm up the JVM and SecureRandom to get stable measurements.
     */
    private fun warmup() {
        for (i in 0 until warmupIterations) {
            secureRandom.nextBytes(32)
            secureRandom.nextInt(1000)
            secureRandom.nextDouble()
        }
    }

    /**
     * Benchmark nextBytes() with various sizes.
     */
    @Test
    fun benchmarkNextBytes() {
        warmup()

        val sizes = listOf(16, 32, 64, 128, 256, 512, 1024)
        val results = mutableMapOf<Int, BenchmarkResult>()

        for (size in sizes) {
            val timings = mutableListOf<Long>()
            var successCount = 0

            // Run benchmark
            val totalTime = measureTimeMillis {
                for (i in 0 until benchmarkIterations) {
                    val time = measureNanoTime {
                        val result = secureRandom.nextBytes(size)
                        if (result.isSuccess) successCount++
                    }
                    timings.add(time)
                }
            }

            val avgTime = timings.average()
            val minTime = timings.minOrNull() ?: 0
            val maxTime = timings.maxOrNull() ?: 0
            val throughputMBps = (size * benchmarkIterations * 1000.0) / (totalTime * 1024 * 1024)

            results[size] = BenchmarkResult(
                avgTimeNanos = avgTime,
                minTimeNanos = minTime,
                maxTimeNanos = maxTime,
                throughputMBps = throughputMBps,
                successRate = successCount.toDouble() / benchmarkIterations
            )
        }

        // Print results
        println("\n=== nextBytes() Performance ===")
        println("Size (bytes) | Avg Time (µs) | Min Time (µs) | Max Time (µs) | Throughput (MB/s) | Success Rate")
        println("-".repeat(100))

        for ((size, result) in results.toSortedMap()) {
            println(String.format(
                "%12d | %13.2f | %13.2f | %13.2f | %17.2f | %12.2f%%",
                size,
                result.avgTimeNanos / 1000.0,
                result.minTimeNanos / 1000.0,
                result.maxTimeNanos / 1000.0,
                result.throughputMBps,
                result.successRate * 100
            ))
        }

        // Verify all operations succeeded
        results.values.forEach { result ->
            assertTrue(result.successRate == 1.0, "Some operations failed")
        }
    }

    /**
     * Benchmark nextInt() with various bounds.
     */
    @Test
    fun benchmarkNextInt() {
        warmup()

        val bounds = listOf(10, 100, 1000, 10000, 100000)
        val results = mutableMapOf<Int, BenchmarkResult>()

        for (bound in bounds) {
            val timings = mutableListOf<Long>()
            var successCount = 0

            val totalTime = measureTimeMillis {
                for (i in 0 until benchmarkIterations) {
                    val time = measureNanoTime {
                        val result = secureRandom.nextInt(bound)
                        if (result.isSuccess) successCount++
                    }
                    timings.add(time)
                }
            }

            val avgTime = timings.average()
            val opsPerSecond = (benchmarkIterations * 1000.0) / totalTime

            results[bound] = BenchmarkResult(
                avgTimeNanos = avgTime,
                minTimeNanos = timings.minOrNull() ?: 0,
                maxTimeNanos = timings.maxOrNull() ?: 0,
                opsPerSecond = opsPerSecond,
                successRate = successCount.toDouble() / benchmarkIterations
            )
        }

        // Print results
        println("\n=== nextInt() Performance ===")
        println("Bound        | Avg Time (ns) | Min Time (ns) | Max Time (ns) | Ops/Second    | Success Rate")
        println("-".repeat(95))

        for ((bound, result) in results.toSortedMap()) {
            println(String.format(
                "%12d | %13.2f | %13.2f | %13.2f | %13.0f | %12.2f%%",
                bound,
                result.avgTimeNanos,
                result.minTimeNanos.toDouble(),
                result.maxTimeNanos.toDouble(),
                result.opsPerSecond,
                result.successRate * 100
            ))
        }

        // Verify performance is reasonable
        results.values.forEach { result ->
            assertTrue(result.successRate == 1.0, "Some operations failed")
            assertTrue(result.opsPerSecond > 1000, "Performance too low: ${result.opsPerSecond} ops/s")
        }
    }

    /**
     * Benchmark all primitive type generation methods.
     */
    @Test
    fun benchmarkPrimitiveTypes() {
        warmup()

        val methods = listOf(
            "nextBoolean" to { secureRandom.nextBoolean() },
            "nextInt" to { secureRandom.nextInt() },
            "nextLong" to { secureRandom.nextLong() },
            "nextFloat" to { secureRandom.nextFloat() },
            "nextDouble" to { secureRandom.nextDouble() }
        )

        println("\n=== Primitive Types Performance ===")
        println("Method        | Avg Time (ns) | Min Time (ns) | Max Time (ns) | Ops/Second    | Success Rate")
        println("-".repeat(95))

        for ((name, method) in methods) {
            val timings = mutableListOf<Long>()
            var successCount = 0

            val totalTime = measureTimeMillis {
                for (i in 0 until benchmarkIterations) {
                    val time = measureNanoTime {
                        val result = method()
                        if (result.isSuccess) successCount++
                    }
                    timings.add(time)
                }
            }

            val avgTime = timings.average()
            val minTime = timings.minOrNull() ?: 0
            val maxTime = timings.maxOrNull() ?: 0
            val opsPerSecond = (benchmarkIterations * 1000.0) / totalTime
            val successRate = successCount.toDouble() / benchmarkIterations

            println(String.format(
                "%-13s | %13.2f | %13.2f | %13.2f | %13.0f | %12.2f%%",
                name,
                avgTime,
                minTime.toDouble(),
                maxTime.toDouble(),
                opsPerSecond,
                successRate * 100
            ))

            assertTrue(successRate == 1.0, "$name: Some operations failed")
            assertTrue(opsPerSecond > 1000, "$name: Performance too low")
        }
    }

    /**
     * Benchmark scalability with increasing load.
     */
    @Test
    fun benchmarkScalability() {
        warmup()

        val loads = listOf(100, 500, 1000, 2000)
        val results = mutableMapOf<Int, ScalabilityResult>()

        println("\n=== Scalability Test ===")

        for (load in loads) {
            val startTime = System.currentTimeMillis()
            var bytesGenerated = 0L
            var successCount = 0

            for (i in 0 until load) {
                val result = secureRandom.nextBytes(1024)
                if (result.isSuccess) {
                    successCount++
                    bytesGenerated += 1024
                }
            }

            val duration = System.currentTimeMillis() - startTime
            val throughput = (bytesGenerated * 1000.0) / (duration * 1024 * 1024) // MB/s

            results[load] = ScalabilityResult(
                load = load,
                durationMs = duration,
                throughputMBps = throughput,
                successRate = successCount.toDouble() / load
            )
        }

        // Print results
        println("Load (ops)   | Duration (ms) | Throughput (MB/s) | Success Rate | Efficiency")
        println("-".repeat(75))

        var previousThroughput = 0.0
        for ((load, result) in results.toSortedMap()) {
            val efficiency = if (previousThroughput > 0) {
                (result.throughputMBps / previousThroughput) * 100
            } else {
                100.0
            }

            println(String.format(
                "%12d | %13d | %17.2f | %12.2f%% | %10.2f%%",
                load,
                result.durationMs,
                result.throughputMBps,
                result.successRate * 100,
                efficiency
            ))

            previousThroughput = result.throughputMBps

            assertTrue(result.successRate == 1.0, "Some operations failed at load $load")
        }
    }

    /**
     * Benchmark latency distribution.
     */
    @Test
    fun benchmarkLatencyDistribution() {
        warmup()

        val iterations = 10000
        val timings = mutableListOf<Long>()

        for (i in 0 until iterations) {
            val time = measureNanoTime {
                secureRandom.nextBytes(32)
            }
            timings.add(time)
        }

        timings.sort()

        val p50 = timings[iterations / 2]
        val p90 = timings[(iterations * 0.9).toInt()]
        val p95 = timings[(iterations * 0.95).toInt()]
        val p99 = timings[(iterations * 0.99).toInt()]
        val p999 = timings[(iterations * 0.999).toInt()]
        val max = timings.last()

        println("\n=== Latency Distribution (32 bytes) ===")
        println("Percentile | Latency (µs)")
        println("-".repeat(30))
        println(String.format("P50        | %12.2f", p50 / 1000.0))
        println(String.format("P90        | %12.2f", p90 / 1000.0))
        println(String.format("P95        | %12.2f", p95 / 1000.0))
        println(String.format("P99        | %12.2f", p99 / 1000.0))
        println(String.format("P99.9      | %12.2f", p999 / 1000.0))
        println(String.format("Max        | %12.2f", max / 1000.0))

        // Verify reasonable latencies
        assertTrue(p50 < 1_000_000, "P50 latency too high") // < 1ms
        assertTrue(p99 < 10_000_000, "P99 latency too high") // < 10ms
    }

    /**
     * Compare performance with java.util.Random (baseline).
     */
    @Test
    fun benchmarkComparison() {
        warmup()

        val javaRandom = java.util.Random()
        val iterations = 10000

        // Warm up java.util.Random
        for (i in 0..100) {
            javaRandom.nextBytes(ByteArray(32))
        }

        // Benchmark SecureRandom
        val secureRandomTime = measureTimeMillis {
            for (i in 0 until iterations) {
                val result = secureRandom.nextBytes(32)
                assertTrue(result.isSuccess)
            }
        }

        // Benchmark java.util.Random
        val javaRandomTime = measureTimeMillis {
            for (i in 0 until iterations) {
                val bytes = ByteArray(32)
                javaRandom.nextBytes(bytes)
            }
        }

        val slowdownFactor = secureRandomTime.toDouble() / javaRandomTime

        println("\n=== Performance Comparison ===")
        println("Implementation  | Time (ms) | Ops/Second")
        println("-".repeat(45))
        println(String.format("SecureRandom    | %9d | %10.0f", secureRandomTime, iterations * 1000.0 / secureRandomTime))
        println(String.format("java.util.Random| %9d | %10.0f", javaRandomTime, iterations * 1000.0 / javaRandomTime))
        println(String.format("\nSlowdown factor: %.2fx (expected for cryptographic security)", slowdownFactor))

        // SecureRandom should be slower but not excessively so
        assertTrue(slowdownFactor < 100, "SecureRandom too slow compared to java.util.Random")
    }

    data class BenchmarkResult(
        val avgTimeNanos: Double,
        val minTimeNanos: Long,
        val maxTimeNanos: Long,
        val throughputMBps: Double = 0.0,
        val opsPerSecond: Double = 0.0,
        val successRate: Double
    )

    data class ScalabilityResult(
        val load: Int,
        val durationMs: Long,
        val throughputMBps: Double,
        val successRate: Double
    )
}