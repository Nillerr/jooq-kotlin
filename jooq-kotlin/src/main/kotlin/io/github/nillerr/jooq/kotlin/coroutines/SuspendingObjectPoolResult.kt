package io.github.nillerr.jooq.kotlin.coroutines

/**
 * Represents the result of an attempt to obtain an object from a suspending object pool.
 *
 * This sealed interface defines three possible outcomes:
 *
 * - [Success]: Indicates that the operation was successful, containing the retrieved element.
 * - [Failure]: Indicates that the operation failed, typically due to pool exhaustion or other constraints.
 * - [Closed]: Indicates that the object pool has been closed, optionally containing a cause for closure.
 *
 * Designed to provide a structured way of handling different outcomes of object pool operations
 * within a suspending coroutine-based context.
 */
sealed interface SuspendingObjectPoolResult<out E> {
    /**
     * Represents a successful result of retrieving an element from a suspending object pool.
     *
     * This class holds the successfully retrieved [element] of type [E], signifying that the
     * operation to obtain the object from the pool was completed without any errors.
     *
     * As part of the [SuspendingObjectPoolResult] hierarchy, this is one of the possible
     * outcomes when interacting with a suspending object pool.
     *
     * @param element The retrieved object of type [E].
     * @see SuspendingObjectPoolResult
     */
    data class Success<out E>(val element: E) : SuspendingObjectPoolResult<E>

    /**
     * Represents a failure result when attempting to retrieve an object from a suspending object pool.
     *
     * This object signifies that the retrieval operation did not succeed, typically due to pool exhaustion
     * or constraints preventing the operation from completing successfully. It serves as one of the possible
     * outcomes in the sealed hierarchy of results for interacting with suspending object pools.
     *
     * Part of the [SuspendingObjectPoolResult] sealed interface, which is designed to provide structured
     * handling of outcomes for these operations in a coroutine-based context.
     */
    object Failure : SuspendingObjectPoolResult<Nothing>

    /**
     * Represents the result of a failed attempt to obtain an object from a suspending object pool.
     *
     * This class extends the `SuspendingObjectPoolResult` with a generic type of `Nothing`, indicating
     * an unsuccessful outcome. The class encapsulates the cause of failure as a `Throwable`, providing
     * context about why the object retrieval was unsuccessful.
     *
     * @property cause The reason for the failure, represented as a `Throwable`. It may be `null`
     *                 if no specific cause is provided.
     */
    data class Closed(val cause: Throwable?) : SuspendingObjectPoolResult<Nothing>
}
