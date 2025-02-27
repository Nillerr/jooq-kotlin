# Kotlin Coroutines support for OpenTelemetry in Micronaut

The `micronaut-kotlin-coroutines-opentelemetry` module provides seamless integration
of [Micronaut](https://micronaut.io/), [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html),
and [OpenTelemetry](https://opentelemetry.io/) tracing capabilities. It simplifies the instrumentation of Kotlin
coroutine-based applications using OpenTelemetry for distributed tracing in the Micronaut framework.

## Features

- Automatically propagates OpenTelemetry context across `suspend` functions in Kotlin coroutines.
- Simplifies tracing of coroutine-based asynchronous workflows in Micronaut applications.

## Minimum Requirements

- JDK 11
- Micronaut 3.6.0
- Kotlin 1.6.21
- OpenTelemetry 1.15.0

## Installation

To include the module in your project, add the following dependency to your `build.gradle` or `build.gradle.kts` file:

```kotlin
dependencies {
    implementation("io.github.nillerr:micronaut-kotlin-coroutines-opentelemetry:<version>")
}
```

Replace `<version>` with the latest version of the library available in the releases section.

## License

This module is available under the MIT License.
