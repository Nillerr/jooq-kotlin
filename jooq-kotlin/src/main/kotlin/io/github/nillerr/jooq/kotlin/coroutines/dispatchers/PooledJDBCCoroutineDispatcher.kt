package io.github.nillerr.jooq.kotlin.coroutines.dispatchers

import io.github.nillerr.jooq.kotlin.PlatformThreadFactory
import io.github.nillerr.jooq.kotlin.coroutines.SuspendingObjectPool
import io.github.nillerr.jooq.kotlin.coroutines.SuspendingObjectPoolResult
import io.github.nillerr.jooq.kotlin.coroutines.SuspendingObjectPoolStack
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

/**
 * A coroutine dispatcher implementation for managing JDBC operations using a thread pool.
 *
 * This class provides a coroutine-based mechanism to execute JDBC operations within a controlled
 * execution context backed by a thread pool. It utilizes an object pool to acquire and manage
 * single-threaded dispatcher instances for executing JDBC-related operations, ensuring proper lifecycle
 * and resource management.
 *
 * The dispatcher ensures thread-local consistency, making it suitable for environments where threading
 * and transactional context integrity are critical.
 *
 * @param configuration The configuration object defining the thread pool size, idle timeout, dispatcher
 *                       acquisition timeout, acquisition threshold, and listeners for lifecycle events.
 * @param threadFactory A thread factory used to create threads for the dispatcher pool. Defaults to
 *                      `PlatformThreadFactory` with a specified prefix.
 */
class PooledJDBCCoroutineDispatcher(
    configuration: PooledJDBCCoroutineDispatcherConfiguration,
    private val dispatchers: SuspendingObjectPool<ExecutorCoroutineDispatcher> = SuspendingObjectPoolStack(configuration.poolSize),
    private val threadFactory: ThreadFactory = PlatformThreadFactory("PooledJDBCCoroutineDispatcher"),
) : JDBCCoroutineDispatcher {
    private val idleTimeout: Duration = configuration.idleTimeout
    private val acquisitionTimeout: Duration = configuration.acquisitionTimeout
    private val acquisitionThresholdMs: Long = configuration.acquisitionThreshold.inWholeMilliseconds
    private val listeners: Collection<JDBCCoroutineDispatcherListener> = configuration.listeners

    override suspend fun <T> launch(block: suspend (JDBCDispatch) -> T): T {
        // Checks whether we're already executing on a known dispatcher (e.g. when inside a transaction block), in
        // which case we don't need to dispatch this call.
        val currentJdbcDispatch = coroutineContext[JDBCDispatch.Key]
        if (currentJdbcDispatch != null) {
            return block(currentJdbcDispatch)
        }

        // If we're not already executing on a known dispatcher, we first wait (suspending) for one to be available,
        // then dispatch the block to it.
        val dispatcher = acquireDispatcher()
        try {
            val newJdbcDispatch = JDBCDispatch
            return withContext(dispatcher + newJdbcDispatch) {
                block(newJdbcDispatch)
            }
        } finally {
            try {
                // Once we're done we need to return the dispatcher to the pool
                dispatchers.release(dispatcher)
            } catch (_: ClosedSendChannelException) {
                // The channel was closed through a call to `close`, in which case the application is going to
                // terminate shortly.
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun acquireDispatcher(): ExecutorCoroutineDispatcher {
        // Using `System.currentTimeMillis()` to avoid allocations of `Duration` since this is a hot path
        val startTime = System.currentTimeMillis()

        val dispatcher = try {
            withTimeout(acquisitionTimeout) {
                dispatchers.acquire() ?: createDispatcher()
            }
        } catch (e: TimeoutCancellationException) {
            val event = DispatcherAcquisitionTimeoutEvent(acquisitionTimeout)
            listeners.forEach { it.onDispatcherAcquisitionTimeout(event) }

            val message = "Timed out after '$acquisitionTimeout' while acquiring dispatcher"
            throw DispatcherAcquisitionTimeoutException(message, e)
        }

        val endTime = System.currentTimeMillis()
        val elapsed = startTime - endTime

        val threshold = acquisitionThresholdMs
        if (threshold > 0 && elapsed > threshold) {
            val event = DispatcherAcquisitionThresholdExceededEvent(elapsed.milliseconds, threshold.milliseconds)
            listeners.forEach { it.onDispatcherAcquisitionThresholdExceeded(event) }
        }

        return dispatcher
    }

    private fun createDispatcher(): ExecutorCoroutineDispatcher {
        return ScheduledThreadPoolExecutor(1, threadFactory)
            .apply {
                allowCoreThreadTimeOut(true)
                setKeepAliveTime(idleTimeout.inWholeNanoseconds, TimeUnit.SECONDS)
                setMaximumPoolSize(1)
            }
            .asCoroutineDispatcher()
    }

    override fun close() {
        dispatchers.close()

        while (true) {
            when (val result = dispatchers.tryAcquire()) {
                is SuspendingObjectPoolResult.Closed -> break
                is SuspendingObjectPoolResult.Failure -> continue
                is SuspendingObjectPoolResult.Success -> result.element?.close()
            }
        }
    }
}
