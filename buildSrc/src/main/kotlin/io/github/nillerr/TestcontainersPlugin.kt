package io.github.nillerr

import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.cc.base.logger
import org.gradle.kotlin.dsl.create
import org.testcontainers.containers.GenericContainer

interface TestcontainersExtension {
    val containers: ListProperty<GenericContainer<*>>
}

interface TestcontainersParameters : BuildServiceParameters {
    var containers: ListProperty<GenericContainer<*>>
}

abstract class TestcontainersService : BuildService<TestcontainersParameters>, AutoCloseable {
    init {
        logger.lifecycle("[Testcontainers] Starting all containers")

        parameters.containers.get().forEach {
            it.start()
        }
    }

    fun start() {
        // Does nothing
    }

    override fun close() {
        logger.lifecycle("[Testcontainers] Stopping all containers")

        parameters.containers.get().forEach {
            it.stop()
        }
    }
}

abstract class Testcontainers : DefaultTask() {
    @get:ServiceReference("testcontainers")
    abstract val service: Property<TestcontainersService>

    @TaskAction
    fun start() {
        val service = service.get()
        service.start()
    }
}

class TestcontainersPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create<TestcontainersExtension>("testcontainers")

        val serviceProvider = project.gradle.sharedServices.registerIfAbsent("testcontainers", TestcontainersService::class.java) {
            parameters.containers = extension.containers
        }

        val testcontainers = project.tasks.register("testcontainers", Testcontainers::class.java)
        testcontainers.configure {
            description = "Starts all Testcontainers"
            group = "Testcontainers"

            service.set(serviceProvider)
            usesService(serviceProvider)
        }

        project.gradle.buildFinished { extension.containers.get().forEach { it.close()} }
    }
}
