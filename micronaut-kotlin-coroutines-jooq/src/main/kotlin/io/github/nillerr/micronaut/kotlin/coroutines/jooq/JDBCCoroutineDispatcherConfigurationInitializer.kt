package io.github.nillerr.micronaut.kotlin.coroutines.jooq

import io.github.nillerr.jooq.kotlin.coroutines.configuration.jdbcCoroutineDispatcher
import io.github.nillerr.jooq.kotlin.coroutines.dispatchers.DataSourceConfiguration
import io.github.nillerr.jooq.kotlin.coroutines.dispatchers.JDBCCoroutineDispatcherListener
import io.github.nillerr.jooq.kotlin.coroutines.dispatchers.JULJDBCCoroutineDispatcherListener
import io.github.nillerr.jooq.kotlin.coroutines.dispatchers.StickyJDBCCoroutineDispatcher
import io.github.nillerr.jooq.kotlin.coroutines.dispatchers.StickyJDBCCoroutineDispatcherConfiguration
import io.github.nillerr.jooq.kotlin.coroutines.dispatchers.UnknownPoolSizeException
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import jakarta.inject.Singleton
import org.jooq.Configuration
import org.jooq.impl.DataSourceConnectionProvider
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toKotlinDuration

/**
 * A listener responsible for initializing the `jdbcCoroutineDispatcher` property in a JOOQ [Configuration] instance.
 *
 * This class integrates a [StickyJDBCCoroutineDispatcher] with a [Configuration] during its creation lifecycle.
 * The dispatcher is configured using the properties defined in [JDBCDataSourceCoroutineConfigurationProperties].
 *
 * The `jdbcCoroutineDispatcher` enables coroutine-based execution in JDBC contexts, ensuring tasks are executed
 * in a consistent and thread-local-aware environment. This is particularly useful for managing transactional
 * state and maintaining thread locality within coroutines.
 *
 * @property properties Provides configuration settings for the coroutine dispatcher associated with JOOQ data-sources.
 */
@Singleton
class JDBCCoroutineDispatcherConfigurationInitializer(
    private val properties: JDBCDataSourceCoroutineConfigurationProperties,
    private val listeners: Collection<JDBCCoroutineDispatcherListener>,
) : BeanCreatedEventListener<Configuration> {
    override fun onCreated(event: BeanCreatedEvent<Configuration>): Configuration {
        val configuration = event.bean

        val coroutines = properties.kotlinCoroutines
        if (coroutines.enabled) {
            val config = coroutines.toStickyJDBCCoroutineDispatcherConfiguration(configuration, listeners)

            val dispatcher = StickyJDBCCoroutineDispatcher(config)
            configuration.jdbcCoroutineDispatcher = dispatcher
        }

        return configuration
    }
}

internal fun JDBCCoroutineConfigurationProperties.toStickyJDBCCoroutineDispatcherConfiguration(
    configuration: Configuration,
    listeners: Collection<JDBCCoroutineDispatcherListener>,
): StickyJDBCCoroutineDispatcherConfiguration {
    val actualListeners = listeners.ifEmpty {
        acquisitionThreshold
            ?.let { listOf(JULJDBCCoroutineDispatcherListener()) }
            ?: emptyList()
    }

    val dataSourceConfiguration by lazy { DataSourceConfiguration.deriveForMicronaut(configuration) }

    return StickyJDBCCoroutineDispatcherConfiguration(
        poolSize = poolSize ?: dataSourceConfiguration.poolSize,
        idleTimeout = idleTimeout?.toKotlinDuration() ?: dataSourceConfiguration.idleTimeout,
        acquisitionTimeout = acquisitionTimeout?.toKotlinDuration() ?: dataSourceConfiguration.acquisitionTimeout,
        acquisitionThreshold = acquisitionThreshold?.toKotlinDuration(),
        listeners = actualListeners,
    )
}

fun DataSourceConfiguration.Companion.deriveForMicronaut(configuration: Configuration): DataSourceConfiguration {
    val connectionProvider = configuration.connectionProvider()
    if (connectionProvider !is DataSourceConnectionProvider) {
        throw UnknownPoolSizeException("Could not determine pool size from connection provider: $connectionProvider (${connectionProvider::class})")
    }

    val dataSource = connectionProvider.dataSource()
    val dataSourceType = dataSource::class.java
    val dataSourceTypeName = dataSourceType.canonicalName
    when (dataSourceTypeName) {
        // HikariCP
        "io.micronaut.configuration.jdbc.hikari.HikariUrlDataSource" -> {
            return with(dataSource as io.micronaut.configuration.jdbc.hikari.HikariUrlDataSource) {
                DataSourceConfiguration(
                    poolSize = maximumPoolSize,
                    idleTimeout = idleTimeout.milliseconds,
                    acquisitionTimeout = connectionTimeout.milliseconds,
                )
            }
        }

        // DBCP
        "io.micronaut.configuration.jdbc.dbcp.DatasourceConfiguration" -> {
            return with(dataSource as io.micronaut.configuration.jdbc.dbcp.DatasourceConfiguration) {
                DataSourceConfiguration(
                    poolSize = maxTotal,
                    idleTimeout = softMinEvictableIdleTimeMillis.milliseconds,
                    acquisitionTimeout = validationQueryTimeout.milliseconds,
                )
            }
        }

        // Tomcat
        "org.apache.tomcat.jdbc.pool.DataSource" -> {
            return with(dataSource as org.apache.tomcat.jdbc.pool.DataSource) {
                DataSourceConfiguration(
                    poolSize = maxActive,
                    idleTimeout = minEvictableIdleTimeMillis.milliseconds,
                    acquisitionTimeout = validationQueryTimeout.milliseconds,
                )
            }
        }

        // UCP
        "oracle.ucp.jdbc.PoolDataSource" -> {
            return with(dataSource as oracle.ucp.jdbc.PoolDataSource) {
                DataSourceConfiguration(
                    poolSize = maxPoolSize,
                    idleTimeout = inactiveConnectionTimeout.seconds,
                    acquisitionTimeout = connectionWaitTimeout.seconds
                )
            }
        }

        else -> {
            throw UnknownPoolSizeException("Could not determine pool size from data source: $dataSourceTypeName")
        }
    }
}
