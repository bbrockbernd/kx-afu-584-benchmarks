package org.example

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.benchmark.*

// ---------------------------------------------------------------------------
// Single-thread: uncontended lock/unlock
// ---------------------------------------------------------------------------

@State(Scope.Benchmark)
open class SingleThreadBenchmark {
    val lock = ReentrantLock(true)

    @Benchmark
    fun withLock(bh: Blackhole) {
        lock.withLock { bh.consume(42) }
    }
}

// ---------------------------------------------------------------------------
// Single-thread: reentrant locking
// ---------------------------------------------------------------------------

@State(Scope.Benchmark)
open class ReentrantBenchmark {
    val lock = ReentrantLock(true)

    @Benchmark
    fun reentrantDepth2(bh: Blackhole) {
        lock.withLock {
            lock.withLock {
                bh.consume(42)
            }
        }
    }

    @Benchmark
    fun reentrantDepth8(bh: Blackhole) {
        lock.withLock { lock.withLock { lock.withLock { lock.withLock {
            lock.withLock { lock.withLock { lock.withLock { lock.withLock {
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
    val lock = ReentrantLock(true)
    private val iterationsPerThread = 1000

    @Benchmark
    fun highContention4Threads(bh: Blackhole) {
        val results = runOnThreads(4) {
            var count = 0
            repeat(iterationsPerThread) { lock.withLock { count++ } }
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
    val lock = ReentrantLock(true)
    private val iterationsPerThread = 1000

    @Benchmark
    fun lowContention4Threads(bh: Blackhole) {
        val results = runOnThreads(4) {
            var count = 0
            repeat(iterationsPerThread) {
                lock.withLock { count++ }
                count += doWork(200)
            }
            count
        }
        bh.consume(results.sum())
    }
}

// ---------------------------------------------------------------------------
// Multi-thread: fairness – round-robin counting
// ---------------------------------------------------------------------------

@State(Scope.Benchmark)
open class FairnessBenchmark {
    val lock = ReentrantLock(true)
    private val threadCount = 4
    private val target = 1000

    @Benchmark
    fun roundRobin4Threads(bh: Blackhole) {
        val counter = AtomicInteger(0)
        val results = runOnThreadsIndexed(threadCount) { threadId ->
            var increments = 0
            while (true) {
                lock.withLock {
                    val current = counter.get()
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
