package io.github.nillerr

import org.apache.commons.codec.digest.DigestUtils
import org.gradle.internal.extensions.stdlib.capitalized
import org.gradle.jvm.tasks.Jar
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.util.Base64

plugins {
    `java-library`
    `maven-publish`
    signing
    id("org.jetbrains.dokka")
}

signing {
    val secretKey = findProperty("signing.secret-key")
    val password = findProperty("signing.password")
    if (secretKey != null && password != null) {
        val defaultKeyId = findProperty("signing.default-key-id")?.toString()
        useInMemoryPgpKeys(defaultKeyId, "$secretKey", "$password")
    }

    sign(publishing.publications)
}

val dokkaJar = tasks.register("dokkaJar", Jar::class.java) {
    group = "documentation"
    description = "Assembles Kotlin docs with Dokka"
    archiveClassifier.set("javadoc")
    from(tasks.named("dokkaHtml"))
}

val sourcesJar = tasks.register("sourcesJar", Jar::class.java) {
    group = "documentation"
    description = "Assembles sources JAR"
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            artifact(dokkaJar)
            artifact(sourcesJar)

            pom {
                name = project.name
                artifactId = project.name
                description = project.name
                url = "https://github.com/Nillerr/micronaut-kotlin-coroutines/${project.name}"

                licenses {
                    license {
                        name = "MIT License"
                        url = "https://github.com/Nillerr/micronaut-kotlin-coroutines/LICENSE"
                    }
                }

                developers {
                    developer {
                        id = "Nillerr"
                        name = "Nicklas Jensen"
                        url = "https://github.com/Nillerr"
                    }
                }

                scm {
                    connection = "scm:git:ssh://github.com:Nillerr/micronaut-kotlin-coroutines.git"
                    developerConnection = "scm:git:ssh://github.com:Nillerr/micronaut-kotlin-coroutines.git"
                    url = "https://github.com/Nillerr/micronaut-kotlin-coroutines"
                }

                issueManagement {
                    system = "GitHub"
                    url = "https://github.com/Nillerr/micronaut-kotlin-coroutines/issues"
                }
            }
        }
    }
}

fun mavenLocalPath(): String {
    // Get the local Maven repository path
    val userHome = System.getProperty("user.home")
    val mavenLocal = "$userHome/.m2/repository"
    return mavenLocal
}

fun artifactPath(): String {
    // Calculate the artifact path based on the group and name
    val groupPath = "${project.group}".replace('.', '/')
    val artifactPath = "$groupPath/${project.name}/${project.version}"
    return artifactPath
}

tasks.register("cleanMavenLocalBundle", Delete::class.java) {
    group = "publishing"
    description = "Cleans up locally published artifacts from Maven local repository"

    val mavenLocal = mavenLocalPath()
    val artifactPath = artifactPath()

    // Delete the artifact directory
    val artifactFile = File(mavenLocal, artifactPath)
    delete(artifactFile)
}

publishing {
    publications {
        withType<MavenPublication> {
            val publicationName = name.capitalized()

            val publishPublicationToMavenLocal = tasks["publish${publicationName}PublicationToMavenLocal"]

            val generateMavenCentralBundleChecksums by tasks.register("generate${publicationName}MavenCentralBundleChecksums") {
                group = "publishing"
                description = "Generates SHA-1 and MD5 checksums for Maven Central bundle files"

                dependsOn(publishPublicationToMavenLocal)

                doLast {
                    val mavenLocal = mavenLocalPath()
                    val artifactPath = artifactPath()
                    val bundleDir = file("$mavenLocal/$artifactPath")

                    bundleDir.listFiles()?.forEach { file ->
                        // Skip existing checksum files
                        if (file.name.endsWith(".sha1") || file.name.endsWith(".md5")) {
                            return@forEach
                        }

                        val fileContent = file.readBytes()

                        // Generate SHA-1 checksum
                        val sha1File = file.resolveSibling("${file.name}.sha1")
                        val sha1 = DigestUtils.sha1Hex(fileContent)
                        sha1File.writeText(sha1)

                        // Generate MD5 checksum
                        val md5File = file.resolveSibling("${file.name}.md5")
                        val md5 = DigestUtils.md5Hex(fileContent)
                        md5File.writeText(md5)
                    }
                }
            }

            val createMavenCentralBundle by tasks.register("create${publicationName}PublicationMavenCentralBundle", Zip::class.java) {
                group = "publishing"
                description = "Creates a bundle of artifacts for manual upload to Maven Central"

                dependsOn(publishPublicationToMavenLocal)
                dependsOn(generateMavenCentralBundleChecksums)

                val artifactPath = artifactPath()

                // Include all artifacts from Maven local repository
                val artifactFile = File(mavenLocalPath(), artifactPath)
                from(artifactFile) {
                    include("*.jar", "*.pom", "*.asc", "*.sha1", "*.module", "*.md5")
                    into(artifactPath)
                }

                // Set the archive name
                archiveFileName = "${project.name}-${project.version}-bundle.zip"
                destinationDirectory = layout.buildDirectory.dir("bundles")
            }

            tasks.register("publish${publicationName}PublicationToMavenCentral") {
                group = "publishing"
                description = "Publishes the artifact bundle to Maven Central using the Publisher API"

                dependsOn(createMavenCentralBundle)

                doLast {
                    val username = findProperty("sonatype.username")?.toString()
                        ?: throw GradleException("sonatype.username property is required")
                    val password = findProperty("sonatype.password")?.toString()
                        ?: throw GradleException("sonatype.password property is required")

                    // Get the bundle file created by createMavenCentralBundle
                    val bundleFile = createMavenCentralBundle.archiveFile.get().asFile
                    if (!bundleFile.exists()) {
                        throw GradleException("Bundle file not found: ${bundleFile.absolutePath}")
                    }

                    val httpClient = HttpClient.newBuilder().build()

                    // Step 1: Upload the bundle
                    logger.lifecycle("Uploading bundle to Maven Central...")

                    val boundary = "----WebKitFormBoundary" + System.currentTimeMillis()
                    val prefix = "--"

                    val bundleContent = Files.readAllBytes(bundleFile.toPath())
                    val requestBody = buildString {
                        // Add file part
                        append("$prefix$boundary\r\n")
                        append("Content-Disposition: form-data; name=\"bundle\"; filename=\"${bundleFile.name}\"\r\n")
                        append("Content-Type: application/zip\r\n")
                    }.toByteArray() + bundleContent + "\r\n$prefix$boundary$prefix\r\n".toByteArray()

                    val publishingType = "USER_MANAGED"
                    val uri = URI.create("https://central.sonatype.com/api/v1/publisher/upload?publishingType=$publishingType")

                    val credentials = "$username:$password".encodeToByteArray()
                    val encodedCredentials = Base64.getEncoder().encodeToString(credentials)

                    val authorization = "Basic $encodedCredentials"

                    val uploadRequest = HttpRequest.newBuilder()
                        .uri(uri)
                        .header("Authorization", authorization)
                        .header("Content-Type", "multipart/form-data; boundary=$boundary")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                        .build()

                    val response = httpClient.send(uploadRequest, HttpResponse.BodyHandlers.ofString())
                    if (response.statusCode() !in 200..299) {
                        throw GradleException("Failed to upload bundle: ${response.body()}")
                    }

                    logger.lifecycle("Bundle uploaded to Maven Central")
                }
            }
        }
    }
}

tasks {
    withType<PublishToMavenLocal> {
        dependsOn("signMavenPublication")
    }
}
