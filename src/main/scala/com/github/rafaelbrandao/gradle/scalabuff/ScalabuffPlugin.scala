/**
 * Copyright (c) 2014 Rafael Brandão <rafa.bra@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.rafaelbrandao.gradle.scalabuff

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

import com.github.rafaelbrandao.gradle.scalabuff.ScalabuffTasks.{GenerateProtoDescriptors, SCALABUFF_TASK_GROUP, ScalabuffCompile}
import com.github.rafaelbrandao.gradle.scalabuff.utils.{ScalaVersion, SoftwareVersion}

import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.{GradleException, Plugin, Project, ProjectConfigurationException}
import org.gradle.language.jvm.tasks.ProcessResources

import org.apache.tools.ant.taskdefs.condition.Os


class ScalabuffPlugin extends Plugin[Project] {
  override def apply(project: Project): Unit = ConfigureScalabuff on project
}


object ConfigureScalabuff extends ApplyScala with CreateScalabuffExtension
with ConfigureClasspath with ConfigureTasks with DetectProtobuff
with ConfigureDependencies {

  def on(project: Project): Unit = configure(project)
}


trait ConfigureScalabuff {
  protected def configure(project: Project): Unit = {}
}


trait ApplyScala extends ConfigureScalabuff {
  abstract override protected def configure(project: Project): Unit = {
    super.configure(project)
    project.getPlugins.apply(classOf[ScalaPlugin])
  }
}


trait CreateScalabuffExtension extends ConfigureScalabuff {
  abstract override protected def configure(project: Project): Unit = {
    super.configure(project)
    project
      .getExtensions
      .create(ScalabuffExtension.NAME, classOf[ScalabuffExtension], project)
  }
}


trait ConfigureClasspath extends ConfigureScalabuff {
  abstract override protected def configure(project: Project): Unit = {
    super.configure(project)
    // Configuring Scala classpath
    project.sourceSets.main.scala.srcDir(project.scalabuff.generatedSourcesDir)

    // Configuring Intellij Idea classpath
    project.afterEvaluate {
      project.idea.map {
        _.getModule.getSourceDirs.add(project.scalabuff.generatedSourcesDir)
      }
    }
  }
}


trait ConfigureTasks extends ConfigureScalabuff with utils.Exec {
  abstract override protected def configure(project: Project): Unit = {
    super.configure(project)
    project.configure(compileProtoTask, generateProtoDescriptorsTask)
    project.afterEvaluate(postConfigureTasks)
  }


  private def compileProtoTask = { project: Project =>
    project.tasks.createTask[ScalabuffCompile](ScalabuffCompile.NAME) { task =>
      import task._
      setGroup(SCALABUFF_TASK_GROUP)
      setDescription("Compile '.proto' files in Scala code.")
      project.getTasks.withType(classOf[ScalaCompile]).asScala.foreach(_.dependsOn(task))

      destinationDir = {
        project.scalabuff.generatedSourcesDir
      }
      sourceSetDirs = {
        {
          for {
            sourceSet <- project.scalabuff.sourceSets.asScala
            sourceSetDir = project.file(sourceSet.srcDir)
            if sourceSetDir.exists()
          } yield sourceSetDir
        }.asJavaCollection
      }
    }
  }

  private def generateProtoDescriptorsTask = { project: Project =>
    project.tasks.createTask[GenerateProtoDescriptors](GenerateProtoDescriptors.NAME) { task =>
      import task._
      setGroup(SCALABUFF_TASK_GROUP)
      setDescription("Generate protobuf descriptors from '.proto' files.")
      dependsOn(project.tasks.findByName(ScalabuffCompile.NAME))
      project.tasks.withType(classOf[ProcessResources]).asScala.foreach(_.dependsOn(task))

      destinationDir = {
        project.scalabuff.generatedResourcesDir
      }
      protoFiles = {
        {
          for {
            sourceSetDir <- project.tasks.getByName(ScalabuffCompile.NAME)
              .asInstanceOf[ScalabuffCompile].sourceSetDirs.asScala
            file <- sourceSetDir.listFiles()
            if file.isFile && file.getName.endsWith(".proto")
          } yield file
        }.asJavaCollection
      }
    }
  }

  private def postConfigureTasks = { project: Project =>
    project.tasks
      .getByName(GenerateProtoDescriptors.NAME)
      .setEnabled(project.scalabuff.generateDescriptors)
  }
}


trait DetectProtobuff extends ConfigureScalabuff with utils.Exec with utils.Logger {
  abstract override protected def configure(project: Project): Unit = {
    super.configure(project)

    project.afterEvaluate {
      getProtobuffVersion(project) match {
        case Success(version) =>
          logger.info(s"Scalabuff plugin detected protobuff version $version")

        case Failure(ex) =>
          val errorMessage = "Scalabuff plugin could not detect protoc in this system. " +
            "Expect errors to occur during tasks execution phase."
          if (project.scalabuff.failIfProtocNotDetected)
            throw new GradleException(errorMessage, ex)
          else
            logger.warn(errorMessage)
      }
    }
  }

  private def getProtobuffVersion(project: Project): Try[String] = Try {
    def command: Seq[String] = Os.isFamily(Os.FAMILY_WINDOWS) match {
      case true => Seq("cmd", "/c", project.scalabuff.protocPath)
      case false => Seq(project.scalabuff.protocPath)
    }
    def args = Seq("--version")

    {
      for {
        rawVersion <- exec(command ++ args).headOption
        softwareVersion <- SoftwareVersion.of(rawVersion)
      } yield softwareVersion.print()
    }.get
  }
}


trait ConfigureDependencies extends ConfigureScalabuff with ScalaVersion {
  abstract override protected def configure(project: Project): Unit = {
    super.configure(project)
    project.afterEvaluate(configureDependencies)
  }

  private def configureDependencies = { project: Project =>
    for {
      scalaLibrary <- getScalaLibrary(project)
      scalaBase <- scalaBaseVersion(scalaLibrary.getVersion)
    } yield {

      val scalabuffCompileConfig =
        project.getConfigurations
          .maybeCreate(ScalabuffCompile.CONFIGURATION_NAME)
          .setVisible(false)

      val compileConfig = project.getConfiguration("compile")

      Map(
        scalabuffCompileConfig -> Seq(Libs.`scalabuff-compiler`(scalaBase)),
        compileConfig -> Seq(Libs.`scalabuff-runtime`(scalaBase))
      ).foreach {
        case (configuration, dependencyNotations) =>
          dependencyNotations.foreach {
            project.getDependencies.add(configuration.getName, _)
          }
      }
      scalabuffCompileConfig.getResolutionStrategy.force(scalaLibrary.notation)
    }

  }

  private def getScalaLibrary(project: Project) = {
    project
      .getConfiguration("compile")
      .getDependencies.asScala
      .find(_.getName == "scala-library")
      .orElse(
        throw new ProjectConfigurationException(
          s"Project ${project.getName} must add a 'compile' dependency to 'scala-library'.", null
        )
      )
  }

  private object Libs {
    private def SCALABUFF_TOOL_VERSION = "1.3.8"

    def `scalabuff-compiler` = { scalaBase: String =>
      s"net.sandrogrzicic:scalabuff-compiler_$scalaBase:$SCALABUFF_TOOL_VERSION"
    }
    def `scalabuff-runtime` = { scalaBase: String =>
      s"net.sandrogrzicic:scalabuff-runtime_$scalaBase:$SCALABUFF_TOOL_VERSION"
    }
  }
}
