/**
 * Copyright (c) 2014 Rafael Brandão <rafa.bra@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.rafaelbrandao.gradle.scalabuff

import java.io.File
import java.util.{Collection => JCollection}

import _root_.scala.beans.BeanProperty
import _root_.scala.collection.JavaConverters._

import org.gradle.api.tasks._
import org.gradle.api.{DefaultTask, Task}

import com.github.rafaelbrandao.gradle.scalabuff.utils._


object ScalabuffTasks {

  final def SCALABUFF_TASK_GROUP = "Scalabuff"


  sealed trait TaskOutput {
    def destinationDir: File
  }


  sealed trait TaskAction {
    this: Task with TaskOutput =>

    @org.gradle.api.tasks.TaskAction
    def performAction(): Unit = {
      try {
        destinationDir.mkdirs()
        taskAction()
      }
      catch {
        case e: Exception => throw new TaskExecutionException(this, e)
      }
    }
    protected def taskAction(): Unit
  }


  sealed abstract class BaseTask extends DefaultTask with TaskAction with TaskOutput {
    protected final val project = getProject
  }


  class ScalabuffCompile extends BaseTask with utils.Exec with Logger {

    @OutputDirectory
    @BeanProperty var destinationDir: File = null

    @InputFiles
    @SkipWhenEmpty
    @BeanProperty var sourceSetDirs: JCollection[File] = null


    override protected def taskAction(): Unit = {
      def mainClass = "net.sandrogrzicic.scalabuff.compiler.ScalaBuff"
      def classpath =
        project.getConfiguration(ScalabuffCompile.CONFIGURATION_NAME).asScala.mkString(":")
      def args =
        sourceSetDirs.asScala.map(dir => s"--proto_path=${dir.getAbsolutePath}") ++
          Array(s"--scala_out=${project.scalabuff.generatedSourcesDir}", "--verbose")

      exec(Seq("java", "-cp", classpath, mainClass) ++ args).foreach(logger.info)
    }

  }


  class GenerateProtoDescriptors extends BaseTask with utils.Exec with utils.Logger {

    @OutputDirectory
    @BeanProperty var destinationDir: File = null

    @InputFiles
    @SkipWhenEmpty
    @BeanProperty var protoFiles: JCollection[File] = null

    override protected def taskAction(): Unit = {
      protoFiles.asScala.foreach { protoFile =>
        def descriptor = s"${protoFile.getName}.descriptor"
        exec {
          Seq(
            "protoc",
            "--include_imports",
            s"--proto_path=${protoFile.getParentFile.getAbsolutePath}",
            s"--descriptor_set_out=${project.scalabuff.generatedResourcesDir}/$descriptor",
            protoFile.getAbsolutePath
          )
        }.foreach(logger.info)
      }
    }

  }


  object ScalabuffCompile {
    final def CONFIGURATION_NAME = "scalabuffCompile"
    final def NAME = CONFIGURATION_NAME
  }

  object GenerateProtoDescriptors {
    final def NAME = "generateProtoDescriptors"
  }
}
