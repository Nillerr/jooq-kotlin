plugins {
    id("io.github.nillerr.kotlin-library")
    id("io.github.nillerr.local-properties")
    id("io.github.nillerr.publishing")

    id("com.bnorm.power.kotlin-power-assert") version "0.11.0"
}

version = "1.0.0"

val liquibaseVersion = "4.31.1"
val postgresSqlVersion = "42.7.5"

configurations.all {
    resolutionStrategy {
        // jOOQ
        force(libs.jooq)
        force(libs.jooq.kotlin)

        // Apache Commons
        force(libs.apache.commons.lang)
        force(libs.apache.commons.text)

        // JUnit
        force("junit:junit:4.13.2")

        // SLF4J
        force("org.slf4j:slf4j-api:1.7.36")
    }
}

dependencies {
    // KotlinX Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)

    // jOOQ
    api(libs.jooq)
    api(libs.jooq.kotlin)

    // Connection Pools
    compileOnly("org.apache.tomcat:tomcat-jdbc:10.0.8")
    compileOnly("com.zaxxer:HikariCP:4.0.3")
    compileOnly("org.apache.commons:commons-dbcp2:2.8.0")
    compileOnly("com.oracle.database.jdbc:ucp:21.1.0.0")
    compileOnly("org.springframework:spring-jdbc:5.3.25")

    // Test - Project
    testImplementation(project(":jooq-kotlin-testing"))

    // Test - Kotlin
    testImplementation(kotlin("test"))

    // Test - Liquibase
    testImplementation("org.liquibase:liquibase-core:$liquibaseVersion")

    // Test - Testcontainers
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)

    // Test - Postgres
    testImplementation("org.postgresql:postgresql:$postgresSqlVersion")

    // Test - Hikari
    testImplementation("com.zaxxer:HikariCP:6.2.1")
}

kotlinPowerAssert {
    functions += "kotlin.test.assertTrue"
    functions += "kotlin.test.assertEquals"
    functions += "kotlin.test.assertNull"
    functions += "kotlin.test.assertSame"
    functions += "kotlin.test.assertIs"
}
