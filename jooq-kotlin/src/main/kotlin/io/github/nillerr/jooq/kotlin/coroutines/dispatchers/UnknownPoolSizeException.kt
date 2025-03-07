package io.github.nillerr.jooq.kotlin.coroutines.dispatchers

/**
 * Exception thrown when the pool size of a `DataSource` or `ConnectionProvider` cannot be determined.
 *
 * This is typically raised in scenarios where the `DataSource` or `ConnectionProvider` is unrecognized, unsupported,
 * or lacks the necessary information to derive the pool size. It is used during the process of deriving
 * a `DataSourceConfiguration` to indicate an unsupported or unidentifiable state.
 *
 * @param message The detail message describing the reason why the pool size could not be determined.
 */
class UnknownPoolSizeException(message: String) : Exception(message)
