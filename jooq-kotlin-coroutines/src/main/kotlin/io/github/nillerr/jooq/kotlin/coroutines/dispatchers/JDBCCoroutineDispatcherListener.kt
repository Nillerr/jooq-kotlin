package io.github.nillerr.jooq.kotlin.coroutines.dispatchers

/**
 * A listener interface for receiving notifications about events related to JDBC coroutine dispatcher operations.
 *
 * This interface enables tracking of timeout and performance-related events that occur during the acquisition
 * or usage of coroutine dispatchers for JDBC operations. Implementers can define custom behavior for responding
 * to these events, such as logging, metrics collection, or diagnostics.
 */
interface JDBCCoroutineDispatcherListener {
    /**
     * Handles the event triggered when the acquisition of a dispatcher times out.
     *
     * This method is called by the dispatcher implementation when a dispatcher cannot be acquired
     * within the specified timeout duration. The event carries details such as the timeout value
     * that was exceeded.
     *
     * @param event The event containing information about the dispatcher acquisition timeout.
     */
    fun onDispatcherAcquisitionTimeout(event: DispatcherAcquisitionTimeoutEvent)

    /**
     * Handles the event triggered when the acquisition time of a dispatcher exceeds the defined threshold.
     *
     * This method is called by the dispatcher implementation when the time taken to acquire a dispatcher surpasses the
     * configured threshold. It can be used for monitoring and diagnostics to identify delays in dispatcher acquisition.
     *
     * @param event The event containing information about the exceeded acquisition threshold,
     *              including the elapsed time and the threshold duration.
     */
    fun onDispatcherAcquisitionThresholdExceeded(event: DispatcherAcquisitionThresholdExceededEvent)
}
