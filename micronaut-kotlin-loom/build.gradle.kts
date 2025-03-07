plugins {
    id("io.github.nillerr.kotlin-micronaut-library")
    id("io.github.nillerr.local-properties")
    id("io.github.nillerr.publishing")
}

version = "3.0.0"

val micronautVersion = "3.2.0"

micronaut {
    version = micronautVersion
}

configurations.all {
    resolutionStrategy {
        // jOOQ
        force(libs.jooq)
        force(libs.jooq.kotlin)

        // Apache Commons
        force(libs.apache.commons.lang)
        force(libs.apache.commons.text)

        // Reactor
        force(libs.projectreactor.reactor.core)

        // KotlinX
        force(libs.kotlinx.coroutines.bom)
        force(libs.kotlinx.coroutines.core)

        // JUnit
        force("junit:junit:4.13.2")

        // SLF4J
        force("org.slf4j:slf4j-api:1.7.36")
    }
}

dependencies {
    // Micronaut
    implementation("io.micronaut:micronaut-http")

    // KotlinX Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
}
