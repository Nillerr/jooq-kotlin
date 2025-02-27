package io.github.nillerr.jooq.kotlin.coroutines

abstract class MicronautJOOQCoroutinesException(message: String? = null, cause: Throwable? = null) :
    Exception(message, cause)
