package io.github.nillerr.jooq.kotlin.coroutines.dispatchers

import com.zaxxer.hikari.HikariDataSource
import oracle.ucp.jdbc.PoolDataSource
import org.apache.commons.dbcp2.BasicDataSource
import org.apache.tomcat.jdbc.pool.DataSource
import org.jooq.Configuration
import org.jooq.impl.DataSourceConnectionProvider
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class UnknownPoolSizeException(message: String) : Exception(message)

class DataSourceConfiguration(
    val poolSize: Int,
    val idleTimeout: Duration,
    val acquisitionTimeout: Duration,
) {
    companion object {
        /**
         * Derives a `DataSourceConfiguration` from the provided `Configuration`.
         *
         * This method analyzes the `ConnectionProvider` and its underlying data source within the given `Configuration` to
         * determine the appropriate pool size, idle timeout, and acquisition timeout. If the data source is not recognized or
         * if the `ConnectionProvider` is invalid, an `UnknownPoolSizeException` is thrown.
         *
         * @param configuration The JOOQ `Configuration` object from which the data source and its properties are derived.
         * @return The derived `DataSourceConfiguration`, which includes pool size, idle timeout, and acquisition timeout settings.
         * @throws UnknownPoolSizeException If the pool size cannot be determined due to an unrecognized or unsupported `ConnectionProvider` or data source type.
         */
        fun derive(configuration: Configuration): DataSourceConfiguration {
            val connectionProvider = configuration.connectionProvider()
            if (connectionProvider !is DataSourceConnectionProvider) {
                throw UnknownPoolSizeException("Could not determine pool size from connection provider: $connectionProvider (${connectionProvider::class})")
            }

            val dataSource = connectionProvider.dataSource()
            return derive(dataSource)
        }

        /**
         * Derives a `DataSourceConfiguration` object based on the specific type of the provided `DataSource`.
         *
         * The method inspects the class type of the given `DataSource` instance and extracts relevant configuration
         * properties such as pool size, idle timeout, and acquisition timeout. Supported data source types include
         * HikariCP, Apache DBCP, Tomcat JDBC, and Oracle UCP. If the data source type is unsupported, an
         * `UnknownPoolSizeException` is thrown.
         *
         * @param dataSource The data source instance from which to derive a `DataSourceConfiguration`.
         *                   Must be one of the supported types: HikariCP, Apache DBCP, Tomcat JDBC, or Oracle UCP.
         * @return A `DataSourceConfiguration` object containing the derived properties from the provided `DataSource`.
         * @throws UnknownPoolSizeException If the type of the provided `DataSource` is unsupported or cannot determine pool size.
         */
        fun derive(dataSource: javax.sql.DataSource): DataSourceConfiguration {
            val dataSourceType = dataSource::class.java
            val dataSourceTypeName = dataSourceType.canonicalName
            when (dataSourceTypeName) {
                // HikariCP
                "com.zaxxer.hikari.HikariDataSource" -> {
                    return with(dataSource as HikariDataSource) {
                        DataSourceConfiguration(
                            poolSize = maximumPoolSize,
                            idleTimeout = idleTimeout.milliseconds,
                            acquisitionTimeout = connectionTimeout.milliseconds,
                        )
                    }
                }

                // DBCP
                "org.apache.commons.dbcp2.BasicDataSource" -> {
                    return with(dataSource as BasicDataSource) {
                        DataSourceConfiguration(
                            poolSize = maxTotal,
                            idleTimeout = softMinEvictableIdleTimeMillis.milliseconds,
                            acquisitionTimeout = validationQueryTimeout.milliseconds,
                        )
                    }
                }

                // Tomcat
                "org.apache.tomcat.jdbc.pool.DataSource" -> {
                    return with(dataSource as DataSource) {
                        DataSourceConfiguration(
                            poolSize = maxActive,
                            idleTimeout = minEvictableIdleTimeMillis.milliseconds,
                            acquisitionTimeout = validationQueryTimeout.milliseconds,
                        )
                    }
                }

                // UCP
                "oracle.ucp.jdbc.PoolDataSource" -> {
                    return with(dataSource as PoolDataSource) {
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
    }
}
