# Kotlin Coroutines support for jOOQ in Micronaut

Integrates [jooq-kotlin](https://github.com/Nillerr/jooq-kotlin/jooq-kotlin-coroutines) into a Micronaut application

## Overview

The `micronaut-kotlin-jooq` module integrates [Micronaut](https://micronaut.io/), Kotlin Coroutines,
and [jOOQ](https://www.jooq.org/) to provide a modern and efficient way to manage database access in Kotlin
applications. It combines jOOQ's type-safe SQL building capabilities with the non-blocking, reactive programming
paradigm enabled by Kotlin Coroutines and Micronaut.

## Features

- **Micronaut Framework Integration**: Utilizes Micronaut's fast startup, dependency injection, and AOP capabilities to
  streamline application development.
- **Kotlin Coroutines Support**: Allows building reactive and non-blocking applications without compromising
  readability.
- **jOOQ Integration**: Facilitates building type-safe SQL queries and mapping results to Kotlin data classes.
- **Transaction Management**: Handles database transactions seamlessly using Micronaut's declarative transaction
  management.
- **Deadlock Prevention**: Ensures proper resource locking and transaction management to avoid potential deadlock
  scenarios during concurrent database operations.

## Installation

To include this module in your Kotlin application, add the following dependencies to your `build.gradle.kts` file:

```kotlin
dependencies {
    implementation("io.github.nillerr:micronaut-kotlin-jooq:<version>")
}
```

Replace `<version>` with the latest version of the library available in the releases section.

## Configuration

Add the following configuration to your `application.yml` to define data source properties:

```yaml
datasources:
  default:
    maximum-pool-size: 25

jooq:
  datasources:
    default:
      kotlin-coroutines:
        # For optimal results please set this to the size of your connection pool (e.g. `datasources.*.maximumPoolSize`)
        pool-size: 25
        # Optional: Specifying a value enables acquisition monitoring
        acquisition-threshold: PT1S
```

## Usage

See [jooq-kotlin-coroutines](https://github.com/Nillerr/jooq-kotlin/jooq-kotlin-coroutines) for example usages.

## License

This module is available under the MIT License.
