package io.github.nillerr.micronaut.kotlin.coroutines.jooq

import io.github.nillerr.jooq.kotlin.coroutines.configuration.jdbcCoroutineDispatcher
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.runtime.event.ApplicationShutdownEvent
import jakarta.inject.Singleton
import org.jooq.Configuration

/**
 * A listener that handles the shutdown of [JDBCCoroutineDispatcher] instances associated with any [Configuration] objects.
 *
 * This listener is triggered during the application shutdown phase (`ApplicationShutdownEvent`) and ensures that the
 * `JDBCCoroutineDispatcher` instances tied to each `Configuration` are properly closed, releasing resources and preventing
 * resource leaks in the process.
 *
 * @property configurations A collection of JOOQ `Configuration` objects, each containing a `JDBCCoroutineDispatcher` instance
 * whose shutdown lifecycle is managed by this listener.
 */
@Singleton
class JDBCCoroutineDispatcherShutdownListener(
    private val configurations: Collection<Configuration>,
) : ApplicationEventListener<ApplicationShutdownEvent> {
    override fun onApplicationEvent(event: ApplicationShutdownEvent) {
        for (configuration in configurations) {
            val dispatcher = configuration.jdbcCoroutineDispatcher
            dispatcher.close()
        }
    }
}
