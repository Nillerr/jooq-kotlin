package io.github.nillerr.jooq.kotlin.coroutines.dispatchers

import java.util.logging.Logger
import kotlin.reflect.jvm.jvmName

/**
 * A listener implementation for handling events related to JDBC coroutine dispatcher operations.
 *
 * This class listens to and processes notifications about dispatcher acquisition timeouts and threshold exceedances.
 * It uses Java Util Logging (JUL) to log these events for diagnostics, monitoring, or operational insights.
 *
 * [JULJDBCCoroutineDispatcherListener] is designed for integration with dispatcher pools where timeouts or delays in
 * acquiring dispatchers need to be tracked and logged. This can assist in identifying potential performance bottlenecks
 * or increasing visibility into the behavior of dispatcher operations.
 */
class JULJDBCCoroutineDispatcherListener : JDBCCoroutineDispatcherListener {
    private val logger = Logger.getLogger(this::class.jvmName)

    override fun onDispatcherAcquisitionTimeout(event: DispatcherAcquisitionTimeoutEvent) {
        val timeout = event.timeout
        logger.warning("Timed out after '$timeout' while acquiring dispatcher")
    }

    override fun onDispatcherAcquisitionThresholdExceeded(event: DispatcherAcquisitionThresholdExceededEvent) {
        val elapsed = event.elapsed
        val threshold = event.threshold
        logger.warning("Dispatcher acquisition time of '$elapsed' exceeded threshold of '$threshold'.")
    }
}
