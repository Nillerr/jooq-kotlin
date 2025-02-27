plugins {
    id("io.github.nillerr.kotlin-micronaut-library")
    id("io.github.nillerr.local-properties")
    id("io.github.nillerr.publishing")
}

version = "3.0.0"

val micronautVersion = "4.0.0"

micronaut {
    version = micronautVersion
}

configurations.all {
    resolutionStrategy {
        // KotlinX Coroutines
        force(libs.kotlinx.coroutines.bom)
        force(libs.kotlinx.coroutines.core)
        force(libs.kotlinx.coroutines.slf4j)
    }
}

dependencies {
    implementation("io.micronaut:micronaut-http")

    implementation(libs.kotlinx.coroutines.slf4j) {
        exclude("org.slf4j", "slf4j-api")
    }
}
