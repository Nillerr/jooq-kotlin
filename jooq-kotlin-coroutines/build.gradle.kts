plugins {
    id("io.github.nillerr.kotlin-library")
    id("io.github.nillerr.local-properties")
    id("io.github.nillerr.publishing")
}

version = "1.0.0"

dependencies {
    implementation(libs.jooq)
    implementation(libs.jooq.kotlin)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
}
