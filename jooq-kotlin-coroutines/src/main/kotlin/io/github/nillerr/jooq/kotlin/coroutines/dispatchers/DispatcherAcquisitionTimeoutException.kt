package io.github.nillerr.jooq.kotlin.coroutines.dispatchers

import io.github.nillerr.jooq.kotlin.coroutines.MicronautJOOQCoroutinesException

/**
 * Exception thrown when a dispatcher acquisition attempt times out.
 *
 * This exception is used to signal that the operation of acquiring a dispatcher for executing
 * JDBC operations within a coroutine context exceeded the allowed acquisition timeout duration.
 * It typically occurs when the resources in the dispatcher pool are unavailable within the specified
 * timeout.
 *
 * @param message The detailed message providing context about the timeout.
 * @param cause The underlying cause of the exception, if available.
 */
class DispatcherAcquisitionTimeoutException(message: String, cause: Throwable?) :
    MicronautJOOQCoroutinesException(message, cause)
