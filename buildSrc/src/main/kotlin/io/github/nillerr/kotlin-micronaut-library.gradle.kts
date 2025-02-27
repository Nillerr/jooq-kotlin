package io.github.nillerr

import libs
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("io.github.nillerr.kotlin-library")
    kotlin("kapt")

    id("io.micronaut.library")
}

version = "3.0.0-SNAPSHOT"

tasks {
    withType<Test> {
        useJUnitPlatform()

        testLogging {
            events(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED)
        }
    }
}

configurations.all {
    resolutionStrategy {
        // Groovy BOM
        force(libs.codehaus.groovy.bom)

        // JUnit
        force(libs.junit.bom)

        // JUnit Platform
        force(libs.junit.platform.console)
        force(libs.junit.platform.launcher)
    }
}
