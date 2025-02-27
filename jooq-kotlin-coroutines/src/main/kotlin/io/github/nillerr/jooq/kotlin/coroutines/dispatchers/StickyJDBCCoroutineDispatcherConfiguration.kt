package io.github.nillerr.jooq.kotlin.coroutines.dispatchers

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for the [StickyJDBCCoroutineDispatcher], defining properties for thread pool size,
 * thread keep-alive duration, dispatcher acquisition timeout, and optional acquisition time threshold.
 *
 * This configuration is used to manage the single-threaded thread pool-backed coroutine dispatcher for JDBC contexts,
 * ensuring thread-local resources like transactions are executed in a reliable and consistent environment.
 *
 * @property poolSize The number of threads in the dispatcher pool. Set this to the maximum size of your connection
 *                    pool. Defaults to 10.
 * @property idleTimeout The maximum time for which idle threads will remain alive. Defaults to 1 minute.
 * @property acquisitionTimeout The maximum timeout duration for acquiring a dispatcher from the pool.
 *                              Defaults to 30 seconds.
 * @property acquisitionThreshold An optional threshold for logging warnings when the dispatcher acquisition duration
 *                                exceeds this limit. Defaults to null.
 * @property listeners A collection of listeners that are notified of events occurring in the lifecycle of the
 *                      dispatcher, such as acquisition or release, and can be used for monitoring or debugging.
 */
data class StickyJDBCCoroutineDispatcherConfiguration(
    val poolSize: Int = 10,
    val idleTimeout: Duration = 1.minutes,
    val acquisitionTimeout: Duration = 30.seconds,
    val acquisitionThreshold: Duration? = null,
    val listeners: Collection<JDBCCoroutineDispatcherListener> = emptyList(),
)
