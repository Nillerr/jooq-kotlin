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
        // Micronaut
        force("io.micronaut:micronaut-bom:$micronautVersion")
        force("io.micronaut:micronaut-inject:$micronautVersion")
        force("io.micronaut:micronaut-runtime:$micronautVersion")

        // Micronaut AWS
        force("io.micronaut.aws:micronaut-aws-bom:$micronautVersion")
    }
}

dependencies {
    // KotlinX
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    // Micronaut
    implementation("io.micronaut:micronaut-http")

    // Micronaut Tracing
    implementation("io.micronaut.tracing:micronaut-tracing-opentelemetry")

    // OpenTelemetry
    implementation("io.opentelemetry:opentelemetry-extension-kotlin")
}
