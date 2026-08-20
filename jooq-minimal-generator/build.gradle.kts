plugins {
    id("io.github.nillerr.kotlin-library")
    id("io.github.nillerr.local-properties")
    id("io.github.nillerr.publishing")

    id("com.bnorm.power.kotlin-power-assert") version "0.11.0"

    id("com.google.devtools.ksp") version "1.6.21-1.0.6"
}

version = "1.0.0"

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

    // KSP
    implementation("com.google.devtools.ksp:symbol-processing-api:1.6.21-1.0.6")

    // KotlinPoet
    implementation("com.squareup:kotlinpoet-ksp:2.1.0")

    // jOOQ
    api(libs.jooq)
    api(libs.jooq.kotlin)
    api(libs.jooq.codegen)

    // Test - Kotlin
    testImplementation(kotlin("test"))
}

kotlinPowerAssert {
    functions += "kotlin.test.assertTrue"
    functions += "kotlin.test.assertEquals"
    functions += "kotlin.test.assertNull"
    functions += "kotlin.test.assertSame"
    functions += "kotlin.test.assertIs"
}
