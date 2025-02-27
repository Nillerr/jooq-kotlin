package io.github.nillerr

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.Properties

class LocalPropertiesPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val localProperties = Properties()
        val localPropertiesFile = target.rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }

        localProperties.forEach { (key, value) ->
            target.extensions.extraProperties["$key"] = value
        }
    }
}
