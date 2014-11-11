/*
 * Copyright (c) 2014 Rafael Brandão <rafa.bra@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.rafaelbrandao.gradle.scalabuff

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.language.jvm.tasks.ProcessResources

import fj.P1

import com.github.rafaelbrandao.gradle.scalabuff.domain.ScalabuffSourceSet
import com.github.rafaelbrandao.gradle.scalabuff.util.DetectProtobuff
import com.github.rafaelbrandao.gradle.scalabuff.util.DetectScalaLibrary
import com.github.rafaelbrandao.gradle.scalabuff.util.ScalaLibraryWrapper

class ScalabuffPlugin implements Plugin<Project>, DetectScalaLibrary, DetectProtobuff {

    void apply(Project project) {
        [
            applyScala,
            createScalabuffExtension,
            configureScalaClasspath,
            createTasks
        ].each {
            it.call(project)
        }

        project.afterEvaluate {
            [
                detectScalaLibrary,
                detectProtobuff,
                configureTasks,
                configureDependencies
            ].each {
                it.call(project)
            }
        }
    }

    private static def applyScala = { Project project ->
        project.plugins.apply(ScalaPlugin)
    }

    private static def createScalabuffExtension = { Project project ->
        project.extensions.create(
            ScalabuffExtension.NAME,
            ScalabuffExtension,
            project.container(ScalabuffSourceSet),
            new File(project.buildDir, ScalabuffExtension.NAME)
        )
    }


    private static def configureScalaClasspath = { Project project ->
        project.sourceSets.main.scala.srcDir(project.scalabuff.generatedSourcesDir)
    }


    private static def createTasks = { Project project ->
        project.tasks.create(ScalabuffCompile.NAME, ScalabuffCompile).with {
            group = ScalabuffTasks.SCALABUFF_TASK_GROUP
            description = "Compile '.proto' files in Scala code."
        }
        project.tasks.create(GenerateProtoDescriptors.NAME, GenerateProtoDescriptors).with { task ->
            group = ScalabuffTasks.SCALABUFF_TASK_GROUP
            description = "Generate protobuf descriptors from '.proto' files."
        }
    }

    private static def detectScalaLibrary = { Project project ->
        getScalaLibraryFrom(project).map { Dependency dependency ->
            getScalaBaseVersionFrom(dependency.version).map { String baseVersion ->
                project.scalabuff.extensions.create('scala', ScalaLibraryWrapper, dependency, baseVersion)
            }
        }
    }

    private static def detectProtobuff = { Project project ->
        getProtobuffVersion(project, project.scalabuff).map { version ->
            project.logger.info("Scalabuff plugin detected protobuff version $version")
        }.orElse({
            def errorMessage = 'Scalabuff plugin could not detect protoc in this system.'
            if (project.scalabuff.failIfProtocNotDetected) {
                throw new GradleException(errorMessage)
            }
            project.logger.warn(errorMessage + " Expect errors to occur during tasks execution phase.")
        } as P1)
    }

    private static def configureTasks = { Project project ->
        final scalabuff = project.scalabuff

        (project.tasks.findByName(ScalabuffCompile.NAME) as ScalabuffCompile).with { task ->
            project.tasks.withType(ScalaCompile).each { it.dependsOn task }
            outputDirectory = scalabuff.generatedSourcesDir
            scalaBaseVersion = scalabuff.scala.baseVersion
            sourceSetDirs =
                scalabuff.sourceSets
                    .collect { it.srcDirs }.flatten()
                    .collect { project.file(it) }
                    .findAll { it.isDirectory() }
        }

        (project.tasks.findByName(GenerateProtoDescriptors.NAME) as GenerateProtoDescriptors).with { task ->
            project.tasks.withType(ProcessResources).each { it.dependsOn task }
            dependsOn project.tasks.findByName(ScalabuffCompile.NAME)
            enabled = scalabuff.generateDescriptors
            outputDirectory = scalabuff.generatedResourcesDir
            protocPath = scalabuff.protocPath
            protoFiles =
                project.tasks.withType(ScalabuffCompile)
                    .collect { it.sourceSetDirs }.flatten()
                    .collect {
                        it.listFiles(
                            { dir, file -> file ==~ /.+\.proto/ } as FilenameFilter
                        ).toList()
                    }.flatten()
        }
    }

    private static def configureDependencies = { Project project ->
        final scalaLibrary = project.scalabuff.scala.library
        final scalaBaseVersion = project.scalabuff.scala.baseVersion

        final compileConfig = project.configurations.getByName('compile')
        final scalaCompileConfig =
            project.configurations
                .create(ScalabuffCompile.CONFIGURATION_NAME)
                .setVisible(false)

        [
            (scalaCompileConfig): [libs['scalabuff-compiler'](scalaBaseVersion)],
            (compileConfig)     : [libs['scalabuff-runtime'](scalaBaseVersion)],
        ].each { configuration, dependencyNotations ->
            dependencyNotations.each {
                project.dependencies.add(configuration.name, it)
            }
        }
        scalaCompileConfig.resolutionStrategy.force(
            "${scalaLibrary.group}:${scalaLibrary.name}:${scalaLibrary.version}"
        )
    }

    private static final Map libs = [
        'scalabuff-compiler': { String scalaBase ->
            "net.sandrogrzicic:scalabuff-compiler_$scalaBase:$SCALABUFF_TOOL_VERSION"
        },
        'scalabuff-runtime' : { String scalaBase ->
            "net.sandrogrzicic:scalabuff-runtime_$scalaBase:$SCALABUFF_TOOL_VERSION"
        }
    ]

    private static final String SCALABUFF_TOOL_VERSION = '1.3.8'
}
