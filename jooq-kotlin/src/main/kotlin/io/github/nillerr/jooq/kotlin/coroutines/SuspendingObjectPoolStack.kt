package io.github.nillerr.jooq.kotlin.coroutines

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * A stack-based implementation of the [SuspendingObjectPool] interface that supports suspending
 * operations for acquiring and releasing objects in a concurrent environment.
 *
 * This class maintains a fixed capacity pool of objects, which can be acquired, used, and
 * subsequently released back into the pool. It uses a channel to manage the availability of
 * objects and a concurrent deque for object storage.
 *
 * This pool implements a First-In-Last-Out access pattern.
 *
 * @param E The type of objects managed by the pool. Must be a non-nullable type.
 * @constructor Creates a new instance of the pool with the specified capacity.
 * @param capacity The maximum number of objects the pool can hold.
 */
internal class SuspendingObjectPoolStack<E : Any>(capacity: Int) : SuspendingObjectPool<E> {
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
        deque.addFirst(element)
        channel.send(Unit)
    }

    override fun close() {
        channel.close()
    }
}
