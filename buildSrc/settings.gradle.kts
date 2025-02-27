dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("dev.panuszewski.typesafe-conventions") version "0.4.1"
}

rootProject.name = "buildSrc"
