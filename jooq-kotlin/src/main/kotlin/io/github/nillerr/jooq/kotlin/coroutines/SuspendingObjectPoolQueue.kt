package io.github.nillerr.jooq.kotlin.coroutines

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Implementation of the `SuspendingObjectPool` interface that manages a pool of objects with a fixed capacity.
 * It utilizes a `Channel` to coordinate the acquisition and release of objects, enabling suspending operations
 * for both acquiring and releasing.
 *
 * This pool ensures that it does not exceed the given capacity by using a `Channel` to manage tokens representing
 * available spots, and a `ConcurrentLinkedDeque` to store the pooled objects.
 *
 * This pool implements a First-In-First-Out access pattern.
 *
 * @param E The type of object managed by the pool. Must be of type `Any`.
 * @param capacity The maximum number of objects the pool can contain at any given time.
 */
internal class SuspendingObjectPoolQueue<E : Any>(capacity: Int) : SuspendingObjectPool<E> {
    private val channel = Channel<Unit>(capacity)
    private val deque = ConcurrentLinkedDeque<E>()

    init {
        repeat(capacity) {
            channel.trySendBlocking(Unit)
        }
    }

    override fun tryAcquire(): SuspendingObjectPoolResult<E?> {
        val result = channel.tryReceive()
        if (result.isClosed) {
            return SuspendingObjectPoolResult.Closed(result.exceptionOrNull())
        }

        if (result.isSuccess) {
            return SuspendingObjectPoolResult.Success(deque.pollFirst())
        }

        return SuspendingObjectPoolResult.Failure
    }

    override suspend fun acquire(): E? {
        channel.receive()
        return deque.pollFirst()
    }

    override suspend fun release(element: E) {
        deque.addLast(element)
        channel.send(Unit)
    }

    override fun close() {
        channel.close()
    }
}
