package io.github.nillerr.micronaut.kotlin.coroutines.jooq

import io.micronaut.context.annotation.EachProperty

/**
 * Configuration properties for coroutine-specific settings for JOOQ data sources.
 *
 * This class maps configuration settings from the `jooq.datasources` property path, associating them with their
 * respective coroutine execution behaviors. It encapsulates settings through an instance of
 * [JDBCCoroutineConfigurationProperties], which defines various parameters for fine-tuning the lifecycle and
 * concurrency properties of the coroutine dispatcher.
 *
 * These settings typically influence how coroutines operate in JDBC contexts associated with specific JOOQ data sources,
 * ensuring optimal performance and thread-local resource management.
 *
 * Use this class to supply values for coroutine-related configuration properties, such as pool size, keep-alive time,
 * acquisition timeout, and others, to control execution within JDBC environments.
 */
@EachProperty("jooq.datasources")
class JDBCDataSourceCoroutineConfigurationProperties {
    /**
     * Defines coroutine-specific configuration settings for this JDBC data source.
     * These settings include properties for thread pool sizes, timeouts, and thresholds
     * to control coroutine execution within the JDBC context of this data source.
     */
    var kotlinCoroutines = JDBCCoroutineConfigurationProperties()
}
