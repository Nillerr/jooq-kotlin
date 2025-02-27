package io.github.nillerr.micronaut.kotlin.coroutines.slf4j

import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Requires
import io.micronaut.http.bind.binders.HttpCoroutineContextFactory
import jakarta.inject.Singleton
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.slf4j.MDCContext
import kotlin.coroutines.CoroutineContext

/**
 * This implementation ensures proper integration of MDC with coroutines, allowing logging frameworks
 * or any other MDC-dependent utilities to function correctly in coroutine contexts.
 */
@Requires(classes = [ThreadContextElement::class, CoroutineContext::class])
@Context
@Singleton
class HttpCoroutineMDCContextFactory : HttpCoroutineContextFactory<MDCContext> {
    override fun create(): MDCContext {
        return MDCContext()
    }
}
