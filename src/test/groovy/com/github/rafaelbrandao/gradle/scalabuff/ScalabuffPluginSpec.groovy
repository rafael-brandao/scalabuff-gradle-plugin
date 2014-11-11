/*
 * Copyright (c) 2014 Rafael Brandão <rafa.bra@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.rafaelbrandao.gradle.scalabuff

import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.testfixtures.ProjectBuilder

import spock.lang.Specification
import spock.lang.Subject

import com.github.rafaelbrandao.gradle.scalabuff.domain.ScalabuffSourceSet
import com.github.rafaelbrandao.gradle.scalabuff.util.ScalaLibraryWrapper

class ScalabuffPluginSpec extends Specification {


    def 'Project only applies scalabuff plugin and is not evaluated'() {
        given:
            @Subject
            final project =
                buildProject {
                    apply plugin: ScalabuffPlugin
                }

        expect: 'it should have ScalabuffPlugin defined'
            project.plugins.hasPlugin(ScalabuffPlugin)

        and: 'it should have ScalaPlugin defined'
            project.plugins.hasPlugin(ScalaPlugin)

        and: 'it should have ScalabuffPluginExtension defined'
            project.extensions.findByType(ScalabuffExtension)

        and: 'it should have configured ScalabuffExtension defaults'
            with(project.scalabuff) { ScalabuffExtension extension ->
                extension != null
                extension.protocPath == 'protoc'
                !extension.failIfProtocNotDetected
                extension.generateDescriptors
                extension.sourceSets
                extension.sourceSets.size() == 1

                with(extension.sourceSets.findByName('proto')) { ScalabuffSourceSet sourceSet ->
                    sourceSet != null
                    sourceSet.name == 'proto'
                    sourceSet.srcDirs.size() == 1
                    sourceSet.srcDirs.contains('src/proto')
                }
            }

        and: 'it should have the notion of sourceSets'
            project.sourceSets
            project.sourceSets.main
            project.sourceSets.main.scala

        and: 'it should have configured the scala classpath correctly'
            def scala = project.sourceSets.main.scala
            scala != null
            scala.srcDirs != null
            scala.srcDirs.size() == 2
            scala.srcDirs.contains(project.scalabuff.generatedSourcesDir)

        and: 'it should have created tasks ScalabuffCompile and GenerateProtoDescriptors'
            [
                project.tasks.findByName(ScalabuffCompile.NAME),
                project.tasks.findByName(GenerateProtoDescriptors.NAME)
            ].every { task ->
                task != null
                task.description != null
                task.group == ScalabuffTasks.SCALABUFF_TASK_GROUP
            }

        and: 'it is not evaluated'
            !project.state.executed
    }


    def 'Project only applies scalabuff and is evaluated'() {
        given:
            @Subject
            final project =
                buildProject {
                    apply plugin: ScalabuffPlugin
                }

        when: 'project is evaluated'
            project.evaluate()

        then:
            project.state.executed

        and:
            ProjectConfigurationException exception = thrown()
            exception.cause != null
            exception.cause instanceof ProjectConfigurationException
            exception.cause.message == "Project ${project.name} must add a 'compile' library to 'scala-library'."
    }


    def 'Project applies scalabuff with valid requirements except scala version, and is evaluated'() {
        final scalaVersion = 'TOTAL INVALID VERSION'
        @Subject
        final project =
            buildProject {
                apply plugin: ScalabuffPlugin
                dependencies {
                    compile "org.scala-lang:scala-library:$scalaVersion"
                }
            }
        when: 'project is evaluated'
            project.evaluate()

        then:
            project.state.executed

        and:
            ProjectConfigurationException exception = thrown()
            exception.cause != null
            exception.cause instanceof InvalidUserDataException
            exception.cause.message == "'$scalaVersion' is not a valid scala version string."
    }

    def 'Project applies scalabuff and idea plugins with valid requirements and is not evaluated'() {
        given:
            @Subject
            final project =
                buildProject {
                    apply plugin: IdeaPlugin
                    apply plugin: ScalabuffPlugin
                }

        expect: 'idea sourceSets contain scalabuff sources directory even before evaluate'
            !project.state.executed
            project.idea.module.sourceDirs.contains(project.scalabuff.generatedSourcesDir)
    }


    def 'Project applies scalabuff plugin with valid requirements and is evaluated'() {
        given:
            String scalaVersion = '2.11.4'
            String scalaBase = '2.11'

            @Subject
            final project = buildProject {
                apply plugin: ScalabuffPlugin
                dependencies {
                    compile "org.scala-lang:scala-library:$scalaVersion"
                }
            }
            boolean scalabuffConfigurationExistsBeforeEvaluation =
                project.configurations.findByName(ScalabuffCompile.NAME) ? true : false

        when: 'project is evaluated'
            project.evaluate()

        then: 'project state is evaluated'
            project.state.executed

        and: 'no GradleException is thrown'
            notThrown(GradleException)

        and: "'scalabuffCompile' configuration does not exist before project evaluation"
            !scalabuffConfigurationExistsBeforeEvaluation

        and: 'scala library is correctly detected and stored in a wrapper class'
            def wrapper = project.scalabuff.scala as ScalaLibraryWrapper
            wrapper
            wrapper.library
            wrapper.library.name == 'scala-library'
            wrapper.library.version == scalaVersion
            wrapper.baseVersion == scalaBase

        and: 'it should have configured ScalabuffCompile task'
            with(project.tasks.findByName(ScalabuffCompile.NAME)) {
                ScalabuffCompile task ->
                    task != null
                    project.tasks.withType(ScalaCompile).every {
                        it.dependsOn.contains(task)
                    }
                    task.outputDirectory == project.scalabuff.generatedSourcesDir
                    task.scalaBaseVersion == scalaBase
                    task.sourceSetDirs != null
                    task.sourceSetDirs.size() == 0
            }

        and: 'it should have configured GenerateProtoDescriptors task'
            with(project.tasks.findByName(GenerateProtoDescriptors.NAME)) {
                GenerateProtoDescriptors task ->
                    task != null
                    project.tasks.withType(ProcessResources).every {
                        it.dependsOn.contains(task)
                    }
                    project.tasks.withType(ScalabuffCompile).every {
                        task.dependsOn.contains(it)
                    }
                    task.enabled
                    task.outputDirectory == project.scalabuff.generatedResourcesDir
                    task.protocPath == project.scalabuff.protocPath
                    task.protoFiles != null
                    task.protoFiles.size() == 0
            }

        and: "'scalabuffCompile' configuration exists after project evaluation"
            project.configurations.findByName(ScalabuffCompile.CONFIGURATION_NAME)

        and: "project should have a 'scalabuffCompile' dependency to 'scalabuff-compiler' library"
            project
                .configurations.getByName(ScalabuffCompile.CONFIGURATION_NAME)
                .dependencies.any { it.name == "scalabuff-compiler_$scalaBase" }

        and: "project should have a 'compile' dependency to 'scalabuff-runtime' library"
            project
                .configurations.getByName('compile')
                .dependencies.any { it.name == "scalabuff-runtime_$scalaBase" }

        and: "'scalabuffCompile' configuration forced modules should contain scala-library"
            project
                .configurations.getByName(ScalabuffCompile.CONFIGURATION_NAME)
                .resolutionStrategy.forcedModules
                .any { it.name == 'scala-library' && it.version == scalaVersion }
    }


    def 'The fully configured project should not resolve any configuration when evaluated'() {
        given:
            @Subject
            final project = fullyConfiguredProject
        when:
            project.evaluate()
        then:
            project.state.executed
            notThrown(ForbiddenResolutionException)
    }


    def 'The fully configured project should raise a ForbiddenResolutionException when a configuration is resolved'() {
        given:
            @Subject
            final project = fullyConfiguredProject
        when:
            project.configurations.getByName('compile').resolve()
        then:
            !project.state.executed
            ForbiddenResolutionException exception = thrown()
            exception.message == "'compile' configuration is being resolved."
    }


    private static Project buildProject(Closure closure = {}) {
        final project = ProjectBuilder.builder().build()
        project.configure(project, closure)
        project
    }

    private static final Project getFullyConfiguredProject() {
        buildProject {
            apply plugin: ScalabuffPlugin
            dependencies {
                compile "org.scala-lang:scala-library:2.11.4"
            }
            repositories {
                jcenter()
            }
            configurations.all { configuration ->
                configuration.incoming.beforeResolve {
                    throw new ForbiddenResolutionException(configuration)
                }
            }
        }
    }

    private static final class ForbiddenResolutionException extends GradleException {
        ForbiddenResolutionException(Configuration configuration) {
            super("'${configuration.name}' configuration is being resolved.")
        }
    }
}
