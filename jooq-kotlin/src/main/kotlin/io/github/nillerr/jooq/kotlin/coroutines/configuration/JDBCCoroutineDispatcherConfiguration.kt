package io.github.nillerr.jooq.kotlin.coroutines.configuration

import io.github.nillerr.jooq.kotlin.coroutines.dispatchers.JDBCCoroutineDispatcher
import io.github.nillerr.jooq.kotlin.coroutines.dispatchers.PooledJDBCCoroutineDispatcher
import io.github.nillerr.jooq.kotlin.coroutines.dispatchers.PooledJDBCCoroutineDispatcherConfiguration
import org.jooq.Configuration
import org.jooq.DSLContext

private object JDBCCoroutineDispatcherKey : DataKey<JDBCCoroutineDispatcher>

/**
 * Provides a coroutine-based executor specifically designed for managing JDBC operations
 * within a coroutine context.
 *
 * This property is typically used to manage the lifecycle of executors that support coroutine-based
 * JDBC operations, ensuring resources are allocated and disposed of appropriately while maintaining
 * valid configuration settings.
 */
var Configuration.jdbcCoroutineDispatcher: JDBCCoroutineDispatcher
    get() = getOrSet(JDBCCoroutineDispatcherKey) {
        PooledJDBCCoroutineDispatcher(PooledJDBCCoroutineDispatcherConfiguration(this))
    }
    set(dispatcher) {
        set(JDBCCoroutineDispatcherKey, dispatcher)
    }

/**
 * Provides a coroutine-based executor specifically designed for managing JDBC operations
 * within a coroutine context.
 *
 * This property is typically used to manage the lifecycle of executors that support coroutine-based
 * JDBC operations, ensuring resources are allocated and disposed of appropriately while maintaining
 * valid configuration settings.
 */
var DSLContext.jdbcCoroutineDispatcher: JDBCCoroutineDispatcher
    get() = getOrSet(JDBCCoroutineDispatcherKey) {
        PooledJDBCCoroutineDispatcher(PooledJDBCCoroutineDispatcherConfiguration(configuration()))
    }
    set(dispatcher) {
        set(JDBCCoroutineDispatcherKey, dispatcher)
    }
