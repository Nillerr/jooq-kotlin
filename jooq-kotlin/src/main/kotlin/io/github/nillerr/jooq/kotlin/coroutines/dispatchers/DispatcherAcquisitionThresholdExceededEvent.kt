package io.github.nillerr.jooq.kotlin.coroutines.dispatchers

import kotlin.time.Duration

/**
 * Represents an event triggered when the time taken to acquire a dispatcher exceeds a specified threshold.
 *
 * This event is used in conjunction with a [JDBCCoroutineDispatcherListener] to notify when dispatcher
 * acquisition duration surpasses the defined limit. It helps in monitoring and debugging scenarios
 * where the dispatcher acquisition process is taking longer than expected.
 *
 * @property elapsed The actual duration taken to acquire the dispatcher.
 * @property threshold The threshold duration that was exceeded.
 */
data class DispatcherAcquisitionThresholdExceededEvent(val elapsed: Duration, val threshold: Duration)
