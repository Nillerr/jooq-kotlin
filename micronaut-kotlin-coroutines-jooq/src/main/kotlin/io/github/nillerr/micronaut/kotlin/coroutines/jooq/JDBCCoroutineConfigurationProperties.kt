package io.github.nillerr.micronaut.kotlin.coroutines.jooq

import io.github.nillerr.jooq.kotlin.coroutines.dispatchers.StickyJDBCCoroutineDispatcher
import java.time.Duration

/**
 * Configuration properties to customize the behavior of a [StickyJDBCCoroutineDispatcher].
 *
 * This class defines various settings that manage the lifecycle and execution behavior of coroutines
 * within JDBC contexts. It serves as a central point for configuring the coroutine execution environment,
 * ensuring thread-local consistency and resource management.
 *
 * @property poolSize Specifies the fixed size of the coroutine execution pool.
 * @property idleTimeout The duration a thread in the pool remains alive when idle.
 * @property acquisitionTimeout The maximum time allowed to acquire a dispatcher before timing out.
 * @property acquisitionThreshold An optional duration that, if exceeded during dispatcher acquisition,
 *   may trigger warnings or log entries for monitoring.
 */
class JDBCCoroutineConfigurationProperties {
    /**
     * Determines whether coroutine-based JDBC operations are enabled.
     *
     * When set to `true`, the configuration will initialize and use a
     * coroutine dispatcher to manage asynchronous and thread-local
     * operations within JDBC contexts. This enables coroutine-based
     * execution for the related data source.
     *
     * If set to `false`, coroutine support will be disabled for this
     * configuration, and no coroutine dispatcher will be initialized.
     */
    var enabled: Boolean = true

    /**
     * Specifies the fixed size of the coroutine execution pool.
     *
     * This property determines the maximum number of threads allocated for coroutines
     * within the JDBC context. Adjusting this value can influence the concurrency level
     * and resource utilization of the dispatcher.
     *
     * Set this to the size of your connection pool for optimal results.
     */
    var poolSize: Int = 10

    /**
     * The duration a thread in the pool remains alive when idle.
     *
     * This property determines how long an idle thread in the coroutine execution pool
     * will be kept alive before being terminated. It helps to optimize resource management
     * by removing unused threads after the specified time interval, while still allowing flexibility
     * for future task execution if the threads are needed again.
     */
    var idleTimeout: Duration = Duration.ofMinutes(1)

    /**
     * The maximum duration allowed for acquiring a dispatcher before timing out.
     *
     * This property defines the time limit for obtaining a dispatcher from the coroutine pool.
     * If the timeout is exceeded, it indicates that the system failed to acquire the required
     * resources within the specified duration, potentially resulting in an error or fallback.
     *
     * Adjust this value based on system performance and operational requirements to ensure
     * efficient task scheduling and prevent excessive blocking.
     */
    var acquisitionTimeout: Duration = Duration.ofSeconds(30)

    /**
     * An optional duration that defines a threshold for the dispatcher acquisition process.
     *
     * When specified, this duration represents a time limit or boundary during dispatcher acquisition
     * operations. If the acquisition time exceeds this threshold, it may trigger monitoring mechanisms,
     * warnings, or log entries, allowing for observability and proactive diagnostics of potential issues.
     *
     * This property can assist in identifying performance bottlenecks or resource contention in coroutine
     * execution within JDBC contexts. Setting this value should depend on the expected performance
     * characteristics of the system.
     */
    var acquisitionThreshold: Duration? = null
}
