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
        // Project Reactor
        force(libs.projectreactor.reactor.core)

        // jOOQ
        force(libs.jooq)
        force(libs.jooq.kotlin)

        // R2DBC
        force(libs.r2dbc.spi)
    }
}

dependencies {
    // Project
    api(project(":jooq-kotlin-coroutines"))

    // jOOQ
    implementation(libs.bundles.jooq)

    // Micronaut
    implementation("io.micronaut:micronaut-runtime")
}
