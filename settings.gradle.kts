dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Use the Foojay Toolchains plugin to automatically download JDKs required by subprojects.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "micronaut-kotlin-coroutines"

include(":jooq-kotlin")
include(":jooq-kotlin-testing")

include(":micronaut-kotlin-jooq")
//include(":micronaut-kotlin-loom")
include(":micronaut-kotlin-opentelemetry")
include(":micronaut-kotlin-slf4j")

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.testcontainers:postgresql:1.20.5")
    }
}
