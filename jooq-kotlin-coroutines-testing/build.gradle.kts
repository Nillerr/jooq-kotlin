plugins {
    id("io.github.nillerr.kotlin-library")
    id("io.github.nillerr.local-properties")
    id("io.github.nillerr.publishing")
    id("io.github.nillerr.testcontainers")

    id("nu.studer.jooq") version "9.0"

    id("com.bnorm.power.kotlin-power-assert") version "0.11.0"
}

version = "1.0.0"

configurations.all {
    resolutionStrategy {
        // Apache Commons
        force(libs.apache.commons.lang)
        force(libs.apache.commons.text)

        // JUnit
        force("junit:junit:4.13.2")

        // SLF4J
        force("org.slf4j:slf4j-api:1.7.36")

        // jOOQ
        force(libs.jooq)
    }
}

val liquibaseVersion = "4.31.1"

dependencies {
    // Project
    api(project(":jooq-kotlin-coroutines"))

    // Kotlin
    implementation(kotlin("test"))

    // KotlinX Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)

    // jOOQ Generator
    jooqGenerator(libs.postgresql)

    // Test - Project
    testImplementation(project(":jooq-kotlin-coroutines-testing"))

    // Test - Kotlin
    testImplementation(kotlin("test"))

    // Test - Liquibase
    testImplementation("org.liquibase:liquibase-core:$liquibaseVersion")

    // Test - Testcontainers
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)

    // Test - Postgres
    testImplementation(libs.postgresql)

    // Test - Hikari
    testImplementation("com.zaxxer:HikariCP:6.2.1")
}

jooq {
    version = "3.17.35"
    edition = nu.studer.gradle.jooq.JooqEdition.OSS

    configurations {
        create("test") {
            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.TRACE

                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = "jdbc:postgresql://localhost:5432/jooq-kotlin-coroutines"
                    user = "postgres"
                    password = "postgres"
                }

                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"

                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "public"
                    }

                    generate.apply {
                        isKotlinNotNullPojoAttributes = true
                        isKotlinNotNullRecordAttributes = true
                        isKotlinNotNullInterfaceAttributes = true
                    }
                }
            }
        }
    }
}
