package io.github.nillerr.micronaut.kotlin.coroutines.jooq

import io.github.nillerr.jooq.kotlin.coroutines.configuration.jdbcCoroutineDispatcher
import io.github.nillerr.jooq.kotlin.coroutines.dispatchers.DataSourceConfiguration
import io.github.nillerr.jooq.kotlin.coroutines.dispatchers.JDBCCoroutineDispatcherListener
import io.github.nillerr.jooq.kotlin.coroutines.dispatchers.JULJDBCCoroutineDispatcherListener
import io.github.nillerr.jooq.kotlin.coroutines.dispatchers.PooledJDBCCoroutineDispatcher
import io.github.nillerr.jooq.kotlin.coroutines.dispatchers.PooledJDBCCoroutineDispatcherConfiguration
import io.github.nillerr.jooq.kotlin.coroutines.dispatchers.UnknownPoolSizeException
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import jakarta.inject.Singleton
import org.jooq.Configuration
import org.jooq.impl.DataSourceConnectionProvider
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import javax.sql.DataSource
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toKotlinDuration

/**
 * A listener responsible for initializing the `jdbcCoroutineDispatcher` property in a JOOQ [Configuration] instance.
 *
 * This class integrates a [PooledJDBCCoroutineDispatcher] with a [Configuration] during its creation lifecycle.
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
            val config = coroutines.toPooledJDBCCoroutineDispatcherConfiguration(configuration, listeners)

            val dispatcher = PooledJDBCCoroutineDispatcher(config)
            configuration.jdbcCoroutineDispatcher = dispatcher
        }

        return configuration
    }
}

internal fun JDBCCoroutineConfigurationProperties.toPooledJDBCCoroutineDispatcherConfiguration(
    configuration: Configuration,
    listeners: Collection<JDBCCoroutineDispatcherListener>,
): PooledJDBCCoroutineDispatcherConfiguration {
    val actualListeners = listeners.ifEmpty {
        acquisitionThreshold
            .takeUnless { it.isZero }
            ?.let { listOf(JULJDBCCoroutineDispatcherListener()) }
            ?: emptyList()
    }

    val dataSourceConfiguration by lazy { DataSourceConfiguration.deriveForMicronaut(configuration) }

    return PooledJDBCCoroutineDispatcherConfiguration(
        poolSize = poolSize ?: dataSourceConfiguration.poolSize,
        idleTimeout = idleTimeout?.toKotlinDuration() ?: dataSourceConfiguration.idleTimeout,
        acquisitionTimeout = acquisitionTimeout?.toKotlinDuration() ?: dataSourceConfiguration.acquisitionTimeout,
        acquisitionThreshold = acquisitionThreshold.toKotlinDuration(),
        listeners = actualListeners,
    )
}

fun DataSourceConfiguration.Companion.deriveForMicronaut(configuration: Configuration): DataSourceConfiguration {
    val connectionProvider = configuration.connectionProvider()
    if (connectionProvider !is DataSourceConnectionProvider) {
        throw UnknownPoolSizeException("Could not determine pool size from connection provider: $connectionProvider (${connectionProvider::class})")
    }

    val dataSource = connectionProvider.dataSource()
    return deriveForMicronaut(dataSource)
}

private fun DataSourceConfiguration.Companion.deriveForMicronaut(dataSource: DataSource): DataSourceConfiguration {
    val dataSourceType = dataSource::class.java
    val dataSourceTypeName = dataSourceType.canonicalName
    return when (dataSourceTypeName) {
        // HikariCP
        "io.micronaut.configuration.jdbc.hikari.HikariUrlDataSource" -> {
            with(dataSource as io.micronaut.configuration.jdbc.hikari.HikariUrlDataSource) {
                DataSourceConfiguration(
                    poolSize = maximumPoolSize,
                    idleTimeout = idleTimeout.milliseconds,
                    acquisitionTimeout = connectionTimeout.milliseconds,
                )
            }
        }

        // DBCP
        "io.micronaut.configuration.jdbc.dbcp.DatasourceConfiguration" -> {
            with(dataSource as io.micronaut.configuration.jdbc.dbcp.DatasourceConfiguration) {
                DataSourceConfiguration(
                    poolSize = maxTotal,
                    idleTimeout = softMinEvictableIdleTimeMillis.milliseconds,
                    acquisitionTimeout = validationQueryTimeout.milliseconds,
                )
            }
        }

        // Tomcat
        "org.apache.tomcat.jdbc.pool.DataSource" -> {
            with(dataSource as org.apache.tomcat.jdbc.pool.DataSource) {
                DataSourceConfiguration(
                    poolSize = maxActive,
                    idleTimeout = minEvictableIdleTimeMillis.milliseconds,
                    acquisitionTimeout = validationQueryTimeout.milliseconds,
                )
            }
        }

        // UCP
        "oracle.ucp.jdbc.PoolDataSource" -> {
            with(dataSource as oracle.ucp.jdbc.PoolDataSource) {
                DataSourceConfiguration(
                    poolSize = maxPoolSize,
                    idleTimeout = inactiveConnectionTimeout.seconds,
                    acquisitionTimeout = connectionWaitTimeout.seconds
                )
            }
        }

        // Spring Transaction Management
        "org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy" -> {
            with(dataSource as TransactionAwareDataSourceProxy) {
                DataSourceConfiguration(
                    poolSize = 1,
                    idleTimeout = 30.seconds,
                    acquisitionTimeout = loginTimeout.seconds,
                )
            }
        }

        else -> {
            derive(dataSource)
        }
    }
}
