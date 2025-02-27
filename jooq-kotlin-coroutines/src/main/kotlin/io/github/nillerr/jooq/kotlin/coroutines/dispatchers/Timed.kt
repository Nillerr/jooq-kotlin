package io.github.nillerr.jooq.kotlin.coroutines.dispatchers

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal inline fun <T> timedValue(block: () -> T): Pair<T, Duration> {
    val start = System.currentTimeMillis()
    val result = block()
    val end = System.currentTimeMillis()
    val elapsed = (end - start).milliseconds
    return result to elapsed
}
