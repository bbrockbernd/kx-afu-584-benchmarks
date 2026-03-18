package org.example

import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

/**
 * Runs [action] on [threadCount] worker threads and returns the results.
 */
@OptIn(ObsoleteWorkersApi::class)
fun <T> runOnThreads(threadCount: Int, action: () -> T): List<T> {
    val workers = Array(threadCount) { Worker.start() }
    val futures = workers.map { worker ->
        worker.execute(TransferMode.SAFE, { action }) { it() }
    }
    val results = futures.map { it.result }
    workers.forEach { it.requestTermination().result }
    return results
}

/**
 * Like [runOnThreads] but passes the thread index (0 until [threadCount]) to [action].
 */
@OptIn(ObsoleteWorkersApi::class)
fun <T> runOnThreadsIndexed(threadCount: Int, action: (threadId: Int) -> T): List<T> {
    val workers = Array(threadCount) { Worker.start() }
    val futures = workers.mapIndexed { index, worker ->
        worker.execute(TransferMode.SAFE, { Pair(index, action) }) { (id, block) -> block(id) }
    }
    val results = futures.map { it.result }
    workers.forEach { it.requestTermination().result }
    return results
}

/**
 * Burns CPU for [iterations] loop steps. Returns an accumulated value to prevent dead-code elimination.
 */
fun doWork(iterations: Int): Int {
    var result = 0
    for (i in 0 until iterations) {
        result += i
    }
    return result
}
