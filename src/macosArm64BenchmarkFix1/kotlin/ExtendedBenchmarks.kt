package org.example

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.withLock
import kotlinx.benchmark.*
import kotlin.concurrent.AtomicInt

// ---------------------------------------------------------------------------
// Single-thread: uncontended lock/unlock
// ---------------------------------------------------------------------------

@State(Scope.Benchmark)
open class SingleThreadBenchmark {
    val so = SynchronizedObject()

    @Benchmark
    fun withLock(bh: Blackhole) {
        so.withLock { bh.consume(42) }
    }
}

// ---------------------------------------------------------------------------
// Single-thread: reentrant locking
// ---------------------------------------------------------------------------

@State(Scope.Benchmark)
open class ReentrantBenchmark {
    val so = SynchronizedObject()

    @Benchmark
    fun reentrantDepth2(bh: Blackhole) {
        so.withLock {
            so.withLock {
                bh.consume(42)
            }
        }
    }

    @Benchmark
    fun reentrantDepth8(bh: Blackhole) {
        so.withLock { so.withLock { so.withLock { so.withLock {
            so.withLock { so.withLock { so.withLock { so.withLock {
                bh.consume(42)
            } } } }
        } } } }
    }
}

// ---------------------------------------------------------------------------
// Multi-thread: high contention – all threads share one lock, minimal work
// ---------------------------------------------------------------------------

@State(Scope.Benchmark)
open class HighContentionBenchmark {
    val so = SynchronizedObject()
    private val iterationsPerThread = 1000

    @Benchmark
    fun highContention4Threads(bh: Blackhole) {
        val results = runOnThreads(4) {
            var count = 0
            repeat(iterationsPerThread) { so.withLock { count++ } }
            count
        }
        bh.consume(results.sum())
    }
}

// ---------------------------------------------------------------------------
// Multi-thread: low contention – work outside critical section reduces pressure
// ---------------------------------------------------------------------------

@State(Scope.Benchmark)
open class LowContentionBenchmark {
    val so = SynchronizedObject()
    private val iterationsPerThread = 1000

    @Benchmark
    fun lowContention4Threads(bh: Blackhole) {
        val results = runOnThreads(4) {
            var count = 0
            repeat(iterationsPerThread) {
                so.withLock { count++ }
                count += doWork(200)
            }
            count
        }
        bh.consume(results.sum())
    }
}

// ---------------------------------------------------------------------------
// Multi-thread: fairness – round-robin counting, each thread only increments
// when counter % threadCount == its own id. Unfair locks starve threads and
// force many wasted lock acquisitions.
// ---------------------------------------------------------------------------

@State(Scope.Benchmark)
open class FairnessBenchmark {
    val so = SynchronizedObject()
    private val threadCount = 4
    private val target = 1000 // total increments across all threads

    @Benchmark
    fun roundRobin4Threads(bh: Blackhole) {
        val counter = AtomicInt(0)
        val results = runOnThreadsIndexed(threadCount) { threadId ->
            var increments = 0
            while (true) {
                so.withLock {
                    val current = counter.value
                    if (current >= target) return@runOnThreadsIndexed increments
                    if (current % threadCount == threadId) {
                        counter.incrementAndGet()
                        increments++
                    }
                }
            }
            @Suppress("UNREACHABLE_CODE")
            increments
        }
        bh.consume(results.sum())
    }
}
