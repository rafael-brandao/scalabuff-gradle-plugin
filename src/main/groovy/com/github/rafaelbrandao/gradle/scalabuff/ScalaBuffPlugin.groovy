/*
 * Copyright (c) 2014 Rafael Brandão <rafa.bra@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.rafaelbrandao.gradle.scalabuff

import com.github.rafaelbrandao.gradle.scalabuff.domain.ScalaBuffSourceSet
import com.github.rafaelbrandao.gradle.scalabuff.tasks.ScalaBuffCleanTask
import com.github.rafaelbrandao.gradle.scalabuff.tasks.ScalaBuffCompileTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.scala.ScalaPlugin

import static com.github.rafaelbrandao.gradle.scalabuff.Config.*


class ScalaBuffPlugin implements Plugin<Project> {


  @Override
  void apply(Project project) {
    // apply plugin: 'scala' to the project
    project.plugins.apply(ScalaPlugin)

    // Add scalabuff-runtime to project runtime dependencies
    project.dependencies.add('compile', PROTOBUFF_DEPENDENCY)
    project.dependencies.add('compile', SCALABUFF_RUNTIME_DEPENDENCY)

    project.task(COMPILE_PROTO_TASK_NAME, type: ScalaBuffCompileTask) {
      group = 'ScalaBuff'
      description = 'Compile .proto files into the Scala language.'
    }

    project.task(CLEAN_PROTO_TASK_NAME, type: ScalaBuffCleanTask) {
      group = 'ScalaBuff'
      description = 'Delete the output directories for Scala compiled protobuf files.'
    }

    // compileScala.dependsOn compileProto
    project.getTasksByName(COMPILE_SCALA_TASK_NAME, true).each {
      it.dependsOn(COMPILE_PROTO_TASK_NAME)
    }

    // clean.dependsOn cleanProto
    project.getTasksByName(CLEAN_TASK_NAME, true).each {
      it.dependsOn(CLEAN_PROTO_TASK_NAME)
    }

    // Create the NamedDomainObjectContainers
    def sourceSets = project.container(ScalaBuffSourceSet)

    // Create and install the ScalaBuffPluginExtension object
    project.extensions.create(ScalaBuffPluginExtension.NAME, ScalaBuffPluginExtension, sourceSets)

    // Applying defaults
    project.scalabuff.sourceSets {
      "$DEFAULT_SOURCE_SET_NAME" {
        srcDir = "src/$DEFAULT_SOURCE_SET_NAME"
      }
    }
    project.scalabuff.outputDir = new File(project.buildDir, DEFAULT_OUTPUT_DIR)
    project.scalabuff.generatedSourcesDir = new File(project.scalabuff.outputDir, 'generated-sources')
    project.scalabuff.generatedResourcesDir = new File(project.scalabuff.outputDir, 'generated-resources')
    project.scalabuff.generateDescriptor = true

    project.gradle.afterProject {
      configureOutputDir(project)
    }
  }

  static def configureOutputDir(Project project) {
    // add generated-sources to scala source set
    project.sourceSets.main.scala.srcDirs += project.scalabuff.generatedSourcesDir

    if (project.plugins.hasPlugin('idea')) {
      project.idea.module.sourceDirs += project.scalabuff.generatedSourcesDir
    }
  }

}

class Config {
  public static final String PROTOBUFF_DEPENDENCY
  public static final String SCALABUFF_RUNTIME_DEPENDENCY

  static {
    def classLoader = Thread.currentThread().contextClassLoader
    InputStream inputStream = classLoader.getResourceAsStream('META-INF/runtime-config.groovy')
    def config = new ConfigSlurper().parse(inputStream.text)
    PROTOBUFF_DEPENDENCY = config.dependencies.protobuff
    SCALABUFF_RUNTIME_DEPENDENCY = config.dependencies.scalabuffRuntime
  }


  public static final String CLEAN_TASK_NAME = BasePlugin.CLEAN_TASK_NAME
  public static final String CLEAN_PROTO_TASK_NAME = 'cleanProto'
  public static final String COMPILE_PROTO_TASK_NAME = 'compileProto'
  public static final String COMPILE_SCALA_TASK_NAME = 'compileScala'

  public static final String DEFAULT_SOURCE_SET_NAME = 'protobuff'
  public static final String DEFAULT_OUTPUT_DIR = 'scalabuff'
}
