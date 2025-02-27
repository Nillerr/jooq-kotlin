dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

plugins {
    // Use the Foojay Toolchains plugin to automatically download JDKs required by subprojects.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "micronaut-kotlin-coroutines"

include(":jooq-kotlin-coroutines")
include(":jooq-kotlin-coroutines-testing")

include(":micronaut-kotlin-coroutines-jooq")
include(":micronaut-kotlin-coroutines-opentelemetry")
include(":micronaut-kotlin-coroutines-slf4j")

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.testcontainers:postgresql:1.20.5")
    }
}
