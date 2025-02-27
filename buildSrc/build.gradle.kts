plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Kotlin
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.kapt.gradle.plugin)

    // Dokka
    implementation(libs.dokka.gradle.plugin)

    // Micronaut
    implementation(libs.micronaut.library.gradle.plugin)

    // Apache Commons
    implementation(libs.commons.codec)

    // Testcontainers
    implementation(libs.testcontainers)
}
