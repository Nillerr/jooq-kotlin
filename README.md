# Kotlin Coroutines Extensions for Micronaut

This repository contains a collection of extension libraries for Micronaut that improves Kotlin Coroutines support for
various integrations.

## Repository Structure

The repository consists of the following libraries:

- [jooq-kotlin](./jooq-kotlin/README.md)
    - Provides coroutine-friendly APIs for JDBC database operations using jOOQ

- [micronaut-kotlin-jooq](./micronaut-kotlin-jooq/README.md)
    - Micronaut integration for jOOQ with Kotlin Coroutines support

- [micronaut-kotlin-opentelemetry](./micronaut-kotlin-opentelemetry/README.md)
    - OpenTelemetry integration for Micronaut with Kotlin Coroutines support

- [micronaut-kotlin-slf4j](./micronaut-kotlin-slf4j/README.md)
    - SLF4J integration for Micronaut with Kotlin Coroutines support

- [micronaut-kotlin-loom](./micronaut-kotlin-loom/README.md)
    - Uses Virtual Threads as the Dispatcher for `suspend` functions in Micronaut

Please refer to each library's individual README for more detailed information about their specific features and
components.

## Branch Structure

For any Micronaut module check out the `micronaut-3` or `micronaut-4` branch respectively, depending on the version of
Micronaut you're targeting.

Likewise, any Micronaut module is released with the major version matching the target Micronaut version, so any
Micronaut module with a major version of `3` targets Micronaut 3, while ones with a major version of `4` targets
Micronaut 4.

## License

This project is licensed under the MIT License - see the [LICENSE](./LICENSE) file for details.
