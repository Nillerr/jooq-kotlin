package io.github.nillerr.jooq.kotlin.coroutines.dispatchers

import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * Implementation of [JDBCCoroutineDispatcher] that manages a fixed pool of single-threaded [java.util.concurrent.ThreadPoolExecutor]
 * instances to execute coroutine-based operations in a JDBC context. It ensures that each thread executes tasks in
 * a sticky context, which can help in environments where thread-local resources or configurations need to remain
 * bound to a specific thread.
 *
 * Specifically, this ensures JDBC connections in transactions are not shared across threads for the duration of the
 * transaction.
 */
class StickyJDBCCoroutineDispatcher(
    configuration: StickyJDBCCoroutineDispatcherConfiguration,
) : JDBCCoroutineDispatcher {
    private val poolSize: Int = configuration.poolSize
    private val idleTimeout: Duration = configuration.idleTimeout
    private val acquisitionTimeout: Duration = configuration.acquisitionTimeout
    private val acquisitionThreshold: Duration? = configuration.acquisitionThreshold
    private val listeners: Collection<JDBCCoroutineDispatcherListener> = configuration.listeners

    private val dispatchers = Channel<ExecutorCoroutineDispatcher>(configuration.poolSize)

    init {
        val currentThread = Thread.currentThread()
        val threadGroup = currentThread.threadGroup

        repeat(poolSize) { index ->
            val keepAliveNanos = idleTimeout.inWholeNanoseconds
            val executor = ThreadPoolExecutor(0, 1, keepAliveNanos, TimeUnit.NANOSECONDS, LinkedBlockingQueue()) { r ->
                Thread(threadGroup, r, "StickyJDBCCoroutineDispatcher-$index")
            }
            val dispatcher = executor.asCoroutineDispatcher()
            dispatchers.trySendBlocking(dispatcher)
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun <T> launch(block: suspend (JDBCDispatch) -> T): T {
        val currentJdbcDispatch = coroutineContext[JDBCDispatch.Key]
        if (currentJdbcDispatch != null) {
            return block(currentJdbcDispatch)
        }

        val dispatcher = acquireDispatcher()
        try {
            val newJdbcDispatch = JDBCDispatch()
            return withContext(dispatcher) {
                block(newJdbcDispatch)
            }
        } finally {
            try {
                dispatchers.send(dispatcher)
            } catch (_: ClosedSendChannelException) {
                // The channel was closed through a call to `close`, in which case the application is going to
                // terminate shortly.
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun acquireDispatcher(): ExecutorCoroutineDispatcher {
        val (dispatcher, elapsed) = timedValue {
            try {
                withTimeout(acquisitionTimeout) {
                    dispatchers.receive()
                }
            } catch (e: TimeoutCancellationException) {
                val event = DispatcherAcquisitionTimeoutEvent(acquisitionTimeout)
                listeners.forEach { it.onDispatcherAcquisitionTimeout(event) }

                val message = "Timed out after '$acquisitionTimeout' while acquiring dispatcher"
                throw DispatcherAcquisitionTimeoutException(message, e)
            }
        }

        val threshold = acquisitionThreshold
        if (threshold != null && elapsed > threshold) {
            val event = DispatcherAcquisitionThresholdExceededEvent(elapsed, threshold)
            listeners.forEach { it.onDispatcherAcquisitionThresholdExceeded(event) }
        }

        return dispatcher
    }

    override fun close() {
        dispatchers.close()

        while (true) {
            val result = dispatchers.tryReceive()
            if (result.isClosed) {
                break
            }

            if (result.isSuccess) {
                val dispatcher = result.getOrThrow()
                dispatcher.close()
            }
        }
    }
}
