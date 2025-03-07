# Kotlin Coroutines for jOOQ

This module implements an integration of [jOOQ](https://www.jooq.org/) with Kotlin coroutines for asynchronous database
access. By leveraging Kotlin's coroutine capabilities, it provides an efficient, non-blocking way to interact with your
database, enabling the development of high-performance and scalable applications.

## Features

- Seamless integration with `jOOQ` for coroutine-based database queries.
- Support for idiomatic Kotlin syntax, allowing cleaner and more readable code.
- Simplifies asynchronous programming with suspending functions.

## Requirements

- Kotlin 1.6.21 or higher
- jOOQ 3.17.0 or higher
- A configured database compatible with jOOQ

## Installation

To include this module in your project using Gradle, add the following dependency to your `build.gradle.kts` file:

```kotlin
dependencies {
    implementation("io.github.nillerr:jooq-kotlin:<version>")
}
```

Replace `<version>` with the latest version of the library available in the releases section.

## Configuration

This module is intended to be used in combination with a connection pool, and while the implementation itself is not 
tied to any particular connection pool, we will be using HikariCP in the following examples.

The first step is to configure the `DataSource`

```kotlin
// Configure Hikari Connection Pool
val hikariConfig = HikariConfig()
hikariConfig.jdbcUrl = "jdbc:postgresql://localhost:5432/postgres"
hikariConfig.username = "postgres"
hikariConfig.password = "postgres"
hikariConfig.maximumPoolSize = 25

val dataSource = HikariDataSource(hikariConfig)
```

Next we create the `DSLContext` and add a `JDBCCoroutineDispatcher` to it:

```kotlin
import io.github.nillerr.jooq.kotlin.coroutines.dispatchers.PooledJDBCCoroutineDispatcher

// Create jOOQ DSL Context
val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

// Configure JDBC Coroutine Dispatcher
val dispatcherConfiguration = PooledJDBCCoroutineDispatcherConfiguration(
    poolSize = dataSource.maximumPoolSize,
    keepAliveTime = dataSource.keepaliveTime.milliseconds,
    acquisitionTimeout = dataSource.connectionTimeout.milliseconds,
)

val dispatcher = PooledJDBCCoroutineDispatcher(dispatcherConfiguration)

// Assign JDBC Coroutine Dispatcher to the DSLContext (or Configuration)
dsl.jdbcCoroutineDispatcher = dispatcher
```

The `PooledJDBCCoroutineDispatcher` is the magic sauce of this module. It ensures that database operations are
dispatched on a dedicated thread pool where each connection is isolated to a single thread. This design prevents common
deadlock issues that can arise when multiple operations try to compete for the same connection resource simultaneously.

By mapping each connection to a dedicated thread within the pool, the dispatcher guarantees a level of isolation and
sequential processing that significantly simplifies transaction management and improves safety in concurrent
environments.

By leveraging the `PooledJDBCCoroutineDispatcher`, this module ensures that asynchronous database operations using
Kotlin coroutines are not only performant but also safe and robust in real-world scenarios.

Once you have added a `PooledJDBCCoroutineDispatcher` to the `DSLContext` (or `Configuration`), using the `suspend()` 
function on the `DSLContext` and queries will enable safe coroutine operations using the dedicated thread pool. 

```kotlin
// Suspend queries are not processed on threads dedicated to the connection pool
val user = dsl.selectFrom(USER)
    .where(USER.ID.eq(id))
    .suspend()
    .first()

// Likewise, suspend mutations are processed on the same set of connection pool threads
dsl.suspend().insert(user)
dsl.suspend().update(user)
dsl.suspend().delete(user)

// Transactions are confined to the same thread for the duration of the transaction
dsl.suspend().transaction {
    dsl.suspend().insert(user)

    dsl.select(DSL.count())
        .from(USER)
        .suspend()
        .first()
}
```

## Detecting connection pool starvation

This library includes a mechanism to help in detecting whether your application has insufficient connections available 
for the workload it is handling. To do so, specify an `acquisitionThreshold` in the 
`PooledJDBCCoroutineDispatcherConfiguration` as well as a `listener`, e.g. using the included 
`JULJDBCCoroutineDispatcherListener` that uses `java.util.logging` to log a warning whenever connection thread 
acquisition time exceeds the specific threshold.

```kotlin
val dispatcherConfiguration = JDBCCoroutineDispatcherConfiguration(
    poolSize = dataSource.maximumPoolSize,
    keepAliveTime = dataSource.keepaliveTime.milliseconds,
    acquisitionTimeout = dataSource.connectionTimeout.milliseconds,
    acquisitionThreshold = 1.seconds,
    listeners = listOf(JULJDBCCoroutineDispatcherListener()),
)
```

You can implement your own `JDBCCoroutineDispatcherListener` to integrate with your favorite monitoring tools like 
OpenTelemetry or Micrometer.

## Note on R2DBC

This library is specifically designed to enhance jOOQ's integration with Kotlin coroutines for JDBC-based connections.
It does not provide any additional features or benefits when used with R2DBC.

If an R2DBC connection is detected, the library will simply pass through calls to jOOQ's own R2DBC implementation. jOOQ
natively supports asynchronous R2DBC operations, and this library does not alter or extend that behavior in any way.

For applications that exclusively use R2DBC, you can directly rely on jOOQ's native R2DBC support to take full advantage
of reactive database programming.

## License

This module is available under the MIT License.
