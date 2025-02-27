plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.kapt.gradle.plugin)
    implementation(libs.micronaut.library.gradle.plugin)
    implementation(libs.dokka.gradle.plugin)
    implementation(libs.commons.codec)
}
