package io.github.nillerr.micronaut.kotlin.coroutines.loom

import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Requires
import io.micronaut.http.bind.binders.HttpCoroutineContextFactory
import jakarta.inject.Singleton
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/**
 * This implementation ensures proper integration of MDC with coroutines, allowing logging frameworks
 * or any other MDC-dependent utilities to function correctly in coroutine contexts.
 */
@Requires(classes = [ThreadContextElement::class, ExecutorCoroutineDispatcher::class])
@Context
@Singleton
@Requires(property = "benchmark.implementation", value = "loom")
class HttpCoroutineVirtualThreadDispatcherFactory : HttpCoroutineContextFactory<ExecutorCoroutineDispatcher> {
    val dispatcher: ExecutorCoroutineDispatcher = Executors
        .newThreadPerTaskExecutor(Thread.ofVirtual().name("VirtualThreadDispatcher-", 1).factory())
        .asCoroutineDispatcher()

    override fun create(): ExecutorCoroutineDispatcher {
        return dispatcher
    }
}
