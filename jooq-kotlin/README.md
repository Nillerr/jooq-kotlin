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

### Derived Configuration

By default, this module will resolve configuration of the underlying `CoroutineDispatcher` pool using the connection 
pool passed to the jOOQ configuration, so in most cases, you don't have to do anything else.

## Note on R2DBC

This library is specifically designed to enhance jOOQ's integration with Kotlin coroutines for JDBC-based connections.
It does not provide any additional features or benefits when used with R2DBC.

If an R2DBC connection is detected, the library will simply pass through calls to jOOQ's own R2DBC implementation. jOOQ
natively supports asynchronous R2DBC operations, and this library does not alter or extend that behavior in any way.

For applications that exclusively use R2DBC, you can directly rely on jOOQ's native R2DBC support to take full advantage
of reactive database programming.

## License

This module is available under the MIT License.
