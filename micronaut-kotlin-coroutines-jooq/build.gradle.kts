plugins {
    id("io.github.nillerr.kotlin-micronaut-library")
    id("io.github.nillerr.local-properties")
    id("io.github.nillerr.publishing")
}

version = "4.0.0"

val micronautVersion = "4.0.0"

micronaut {
    version = micronautVersion
}

configurations.all {
    resolutionStrategy {
        // Micronaut
        force("io.micronaut:micronaut-bom:$micronautVersion")
        force("io.micronaut:micronaut-inject:$micronautVersion")
        force("io.micronaut:micronaut-context:$micronautVersion")

        // SLF4J
        force("org.slf4j:slf4j-api:1.7.30")

        // Project Reactor
        force(libs.projectreactor.reactor.core)
        force(libs.reactivestreams)

        // KotlinX Coroutines
        force(libs.kotlinx.coroutines.bom)
        force(libs.kotlinx.coroutines.core)
        force(libs.kotlinx.coroutines.reactor)
        force(libs.kotlinx.coroutines.reactive)

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

    // Connection Pools
    compileOnly("io.micronaut.sql:micronaut-jdbc-tomcat")
    compileOnly("io.micronaut.sql:micronaut-jdbc-hikari")
    compileOnly("io.micronaut.sql:micronaut-jdbc-dbcp")
    compileOnly("io.micronaut.sql:micronaut-jdbc-ucp")
}
