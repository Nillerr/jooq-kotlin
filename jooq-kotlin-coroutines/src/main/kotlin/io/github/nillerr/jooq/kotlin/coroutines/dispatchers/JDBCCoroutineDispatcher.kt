package io.github.nillerr.jooq.kotlin.coroutines.dispatchers

/**
 * This interface defines a coroutine-based executor for managing JDBC operations within a coroutine context.
 *
 * The executor provides the ability to execute suspendable blocks of code that interact with a JDBC context,
 * ensuring that operations are confined to the appropriate coroutine dispatcher. This is particularly useful in
 * environments where thread-local resources or configurations (such as database transactions) need to be managed
 * within a consistent execution context.
 */
interface JDBCCoroutineDispatcher : AutoCloseable {
    suspend fun <T> launch(block: suspend (JDBCDispatch) -> T): T
}
