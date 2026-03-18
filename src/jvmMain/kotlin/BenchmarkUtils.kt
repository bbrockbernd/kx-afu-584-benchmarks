package org.example

import kotlin.concurrent.thread

fun <T> runOnThreads(threadCount: Int, action: () -> T): List<T> {
    val results = arrayOfNulls<Any?>(threadCount)
    val threads = List(threadCount) { index ->
        thread { results[index] = action() }
    }
    threads.forEach { it.join() }
    @Suppress("UNCHECKED_CAST")
    return results.map { it as T }
}

fun <T> runOnThreadsIndexed(threadCount: Int, action: (threadId: Int) -> T): List<T> {
    val results = arrayOfNulls<Any?>(threadCount)
    val threads = List(threadCount) { index ->
        thread { results[index] = action(index) }
    }
    threads.forEach { it.join() }
    @Suppress("UNCHECKED_CAST")
    return results.map { it as T }
}

fun doWork(iterations: Int): Int {
    var result = 0
    for (i in 0 until iterations) {
        result += i
    }
    return result
}
