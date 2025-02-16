package io.github.danakuban.docsgradleplugin

import io.github.danakuban.docsgradleplugin.processor.EntityAnnotationProcessor
import io.github.danakuban.docsgradleplugin.processor.JobAnnotationProcessor
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KaptExtension

/**
 * Plugin to automate the documentation of a system in the Alsterfleet project
 * Every project this plugin is applied to will generate a system-docs entry in the Alsterfleet documentation.
 * You need to create a "description.md" in the project and in every submodule describing what it does.
 * Furthermore you need to document every @Entity with @EntityDocumentation and
 * every @Scheduled/@PostConstruct/@EventListener with a @JobDocumentation.
 * You also need to create a "./docs/system.puml" Plantuml file describing the system you want to document.
 */
class DocsPlugin : Plugin<Project> {
    override fun apply(project: Project) {

        addKapt(project)
        project.subprojects {
            addKapt(it)
        }

        project.tasks.register("buildDocs", BuildDocsTask::class.java) {
            it.dependsOn("compileKotlin")
            it.description = "Builds the documentation"
            it.group = "local"
        }
        if (project.rootProject.tasks.findByName("buildDocsIndex") == null) {
            project.rootProject.tasks.register("buildDocsIndex", BuildIndexTask::class.java)
        }
        (project.rootProject.tasks.findByName("buildDocsIndex") ?: error("task has to be created")).let {
            it.dependsOn(project.tasks.findByName("buildDocs") ?: error("task has to be created"))
        }

        if (project.rootProject.tasks.findByName("buildSystemView") == null) {
            project.rootProject.tasks.register("buildSystemView", BuildSystemsPlantUmlTask::class.java)
        }
        (project.rootProject.tasks.findByName("buildSystemView") ?: error("task has to be created")).let {
            it.dependsOn(project.tasks.findByName("buildDocs") ?: error("task has to be created"))
        }

    }

    private fun addKapt(project: Project) {

        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply("org.jetbrains.kotlin.kapt")
        project.configurations.getByName("kapt").dependencies.add(
            project.dependencies.create("io.github.danakuban.docs-gradle-plugin:plugin")
        )

        project.pluginManager.withPlugin("org.jetbrains.kotlin.kapt") {
            project.afterEvaluate {
                (it.tasks.findByName("kaptKotlin") ?: error("kapt task should be defined here")).let {
                    it.outputs.dir(project.buildDir.absolutePath + "/docs/jobs")
                    it.outputs.dir(project.buildDir.absolutePath + "/docs/entities")
                }
            }
        }

        project.extensions.configure<KaptExtension>("kapt") {
            it.useBuildCache = true
            it.arguments {
                arg(JobAnnotationProcessor.JOBS_OUTPUT_DIR, project.buildDir.absolutePath + "/docs/jobs")
                arg(EntityAnnotationProcessor.ENTITY_OUTPUT_DIR, project.buildDir.absolutePath + "/docs/entities")
            }
        }
    }
}

