package io.github.nillerr.jooq.kotlin.coroutines

/**
 * Represents a suspending object pool that allows the acquisition and release of objects
 * in a coroutine-based concurrent environment.
 *
 * The pool operates with a fixed capacity and supports both immediate and suspending methods
 * to acquire objects, enabling flexible handling of resource-limited scenarios.
 *
 * Type Parameters:
 * @param E The type of objects managed by the pool. Should be a non-nullable type.
 */
interface SuspendingObjectPool<E : Any> : AutoCloseable {
    /**
     * Attempts to acquire an object from the suspending object pool without suspending.
     *
     * This method provides a non-blocking way to check for the availability of an object in the pool.
     * It returns a result indicating whether the acquisition was successful, unsuccessful due to
     * pool exhaustion, or if the pool has been closed.
     *
     * This function may return one of three result types:
     * - [SuspendingObjectPoolResult.Success]: if an object was successfully acquired.
     * - [SuspendingObjectPoolResult.Failure]: if no objects are available in the pool.
     * - [SuspendingObjectPoolResult.Closed]: if the pool has been closed.
     *
     * @return A [SuspendingObjectPoolResult] representing the outcome of the acquisition attempt.
     */
    fun tryAcquire(): SuspendingObjectPoolResult<E?>

    /**
     * Suspends until an object is available for acquisition from the object pool,
     * then returns the acquired object. If the pool has been closed, returns null.
     *
     * This method ensures that callers are blocked in a suspending manner until
     * an object becomes available or the pool is closed.
     *
     * @return The acquired object of type [E], or `null` if the pool has been closed.
     */
    suspend fun acquire(): E?

    /**
     * Releases the specified element back to the object pool, making it available for reuse.
     *
     * @param element The object of type [E] to be released back into the pool.
     */
    suspend fun release(element: E)

    companion object {
        /**
         * Creates a suspending object pool with stack-like behavior and a fixed capacity.
         *
         * The pool is designed to support a First-In-Last-Out (FILO) access pattern,
         * allowing concurrent acquisition and release of objects. The capacity determines
         * the maximum number of objects the pool can manage at any given time.
         *
         * @param capacity The maximum number of objects the pool can hold. Must be a positive integer.
         * @return A suspending object pool implementing stack-like behavior with the specified capacity.
         */
        fun <E : Any> stack(capacity: Int): SuspendingObjectPool<E> {
            return SuspendingObjectPoolStack<E>(capacity)
        }

        /**
         * Creates a suspending object pool with a fixed capacity that manages objects
         * in a First-In-First-Out (FIFO) order. This allows for suspending operations on acquiring
         * and releasing objects in the pool.
         *
         * @param capacity The maximum number of objects the pool can contain at any given time.
         * @return A suspending object pool instance configured with the specified capacity.
         */
        fun <E : Any> queue(capacity: Int): SuspendingObjectPool<E> {
            return SuspendingObjectPoolQueue<E>(capacity)
        }
    }
}
