package io.github.nillerr.micronaut.kotlin.coroutines.jooq

import io.github.nillerr.jooq.kotlin.coroutines.dispatchers.StickyJDBCCoroutineDispatcher
import io.github.nillerr.jooq.kotlin.coroutines.dispatchers.StickyJDBCCoroutineDispatcherConfiguration
import io.github.nillerr.jooq.kotlin.coroutines.configuration.jdbcCoroutineDispatcher
import io.github.nillerr.jooq.kotlin.coroutines.dispatchers.JDBCCoroutineDispatcherListener
import io.github.nillerr.jooq.kotlin.coroutines.dispatchers.JULJDBCCoroutineDispatcherListener
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import jakarta.inject.Singleton
import org.jooq.Configuration
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
            val config = coroutines.toStickyJDBCCoroutineDispatcherConfiguration(listeners)

            val dispatcher = StickyJDBCCoroutineDispatcher(config)
            configuration.jdbcCoroutineDispatcher = dispatcher
        }

        return configuration
    }
}

internal fun JDBCCoroutineConfigurationProperties.toStickyJDBCCoroutineDispatcherConfiguration(
    listeners: Collection<JDBCCoroutineDispatcherListener>,
): StickyJDBCCoroutineDispatcherConfiguration {
    val actualListeners = listeners.ifEmpty {
        acquisitionThreshold
            ?.let { listOf(JULJDBCCoroutineDispatcherListener()) }
            ?: emptyList()
    }

    return StickyJDBCCoroutineDispatcherConfiguration(
        poolSize = poolSize,
        idleTimeout = idleTimeout.toKotlinDuration(),
        acquisitionTimeout = acquisitionTimeout.toKotlinDuration(),
        acquisitionThreshold = acquisitionThreshold?.toKotlinDuration(),
        listeners = actualListeners,
    )
}
