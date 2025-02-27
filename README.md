# Kotlin Coroutines Extensions for Micronaut

This repository contains a collection of extension libraries for Micronaut that improves Kotlin Coroutines support for
various integrations.

## Repository Structure

The repository consists of the following libraries:

- [jooq-kotlin-coroutines](./jooq-kotlin-coroutines/README.md)
    - Kotlin Coroutines extensions for jOOQ
    - Provides coroutine-friendly APIs for JDBC database operations using jOOQ

- [micronaut-kotlin-coroutines-jooq](./micronaut-kotlin-coroutines-jooq/README.md)
    - Micronaut integration for jOOQ with Kotlin Coroutines support
    - Combines the Micronaut framework with jOOQ's type-safe SQL builder using coroutines

- [micronaut-kotlin-coroutines-opentelemetry](./micronaut-kotlin-coroutines-opentelemetry/README.md)
    - OpenTelemetry integration for Micronaut with Kotlin Coroutines support
    - Provides distributed tracing capabilities for coroutine-based applications

- [micronaut-kotlin-coroutines-slf4j](./micronaut-kotlin-coroutines-slf4j/README.md)
    - SLF4J integration for Micronaut with Kotlin Coroutines support
    - Logging utilities optimized for coroutine-based applications

Please refer to each library's individual README for more detailed information about their specific features and
components.

## License

This project is licensed under the MIT License - see the [LICENSE](./LICENSE) file for details.
