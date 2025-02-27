package io.github.nillerr

import libs
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

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
        failOnVersionConflict()

        // Kotlin
        force(libs.kotlin.bom)

        // Kotlin Standard Library
        force(libs.kotlin.stdlib)
        force(libs.kotlin.stdlib.common)
        force(libs.kotlin.stdlib.jdk7)
        force(libs.kotlin.stdlib.jdk8)

        // Kotlin Reflect
        force(libs.kotlin.reflect)

        // Jackson BOM
        force(libs.jackson.bom)

        // Jackson Core
        force(libs.jackson.core.annotations)
        force(libs.jackson.core.core)
        force(libs.jackson.core.databind)

        // Jackson DataType
        force(libs.jackson.datatype.jdk8)
        force(libs.jackson.datatype.jsr310)

        // JetBrains
        force(libs.jetbrains.annotations)
    }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
        }
    }
}
