package io.github.nillerr.jooq.kotlin.coroutines.dispatchers

import kotlin.time.Duration

/**
 * Represents an event triggered when the acquisition of a dispatcher times out.
 *
 * This event is used in conjunction with a [JDBCCoroutineDispatcherListener] to notify about timeout occurrences
 * during the dispatcher acquisition process. Such timeouts can occur when all dispatchers in the pool are occupied
 * and a new dispatcher request cannot be fulfilled within the specified acquisition timeout duration.
 *
 * @property timeout The duration that was exceeded before the timeout occurred.
 */
data class DispatcherAcquisitionTimeoutEvent(val timeout: Duration)
