package io.github.nillerr.opentelemetry

import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Requires
import io.micronaut.http.bind.binders.HttpCoroutineContextFactory
import io.opentelemetry.extension.kotlin.asContextElement
import jakarta.inject.Singleton
import kotlinx.coroutines.ThreadContextElement
import kotlin.coroutines.CoroutineContext

/**
 * This implementation ensures proper integration of context with coroutines, allowing OpenTelemetry to function
 * correctly in coroutine contexts.
 */
@Requires(classes = [ThreadContextElement::class, io.opentelemetry.context.Context::class])
@Context
@Singleton
class HttpCoroutineOpenTelemetryContextFactory : HttpCoroutineContextFactory<CoroutineContext> {
    override fun create(): CoroutineContext {
        return io.opentelemetry.context.Context.current().asContextElement()
    }
}
