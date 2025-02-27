# Kotlin Coroutines support for SLF4J in Micronaut

The `micronaut-kotlin-coroutines-slf4j` module provides seamless integration
of [Micronaut](https://micronaut.io/), [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html),
and [SLF4J](https://www.slf4j.org/) logging capabilities. It simplifies the logging context propagation in Kotlin
coroutine-based applications within the Micronaut framework.

## Features

- Automatically propagates SLF4J MDC (Mapped Diagnostic Context) across `suspend` functions in Kotlin coroutines
- Simplifies logging in coroutine-based asynchronous workflows in Micronaut applications
- Maintains consistent logging context across coroutine boundaries

## Minimum Requirements

- JDK 11
- Micronaut 3.2.0
- Kotlin 1.6.21
- SLF4J 1.7.29

## Installation

To include the module in your project, add the following dependency to your `build.gradle` or `build.gradle.kts` file:

```kotlin
dependencies {
    implementation("io.github.nillerr:micronaut-kotlin-coroutines-slf4j:<version>")
}
```

Replace `<version>` with the latest version of the library available in the releases section.

## License

This module is available under the MIT License.
